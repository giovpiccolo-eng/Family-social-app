"""
Contract Generator — calculation engine.

Pure functions only (no Flask, no DOCX). All monetary values are Decimal.
Reference: technical brief §5 (Calculation Engine) and §4 (CCNL tables).
"""

from __future__ import annotations

from datetime import date
from decimal import Decimal, ROUND_HALF_UP

from ccnl_data import (
    TABELLARE,
    ORE_BASE,
    PROLUNGAMENTO,
    DOPOSCUOLA_RATE_HOUR,
    SCHOOL_CAMP_RATE_WEEK,
    MENSILITA_ANNUE,
)


# ---------------------------------------------------------------------------
# Rounding helper
# ---------------------------------------------------------------------------

_TWO_PLACES = Decimal("0.01")


def _money(value: Decimal) -> Decimal:
    """Round a Decimal to 2 decimal places (HALF_UP, as in payroll rounding)."""
    return value.quantize(_TWO_PLACES, rounding=ROUND_HALF_UP)


# ---------------------------------------------------------------------------
# 5.1 — Tabellare lookup keyed on calendar year of contract start
# ---------------------------------------------------------------------------

def get_tabellare(livello: str, start_date: date) -> Decimal:
    """
    Returns the CCNL monthly base salary (tabellare lordo, full-time) for the
    given livello, selecting the rate in force at start_date's calendar year.

    The CCNL ANINSEI 2024-2027 publishes rates for 2025, 2026, 2027:
    contracts starting in 2025 or earlier use the 2025 rate; 2026 from 01/01/2026;
    2027 from 01/01/2027 (and forward, until a new CCNL is signed).
    """
    year = start_date.year
    if year >= 2027:
        key = "2027"
    elif year >= 2026:
        key = "2026"
    else:
        key = "2025"
    return TABELLARE[livello][key]


# ---------------------------------------------------------------------------
# 5.2 — Part-time proportioning (linear on weekly hours)
# ---------------------------------------------------------------------------

def proportion_parttime(value: Decimal, ore_pt: int, ore_base: int) -> Decimal:
    """value × (ore_pt / ore_base), rounded to cents."""
    if ore_base <= 0:
        raise ValueError("ore_base must be > 0")
    return _money(value * Decimal(ore_pt) / Decimal(ore_base))


# ---------------------------------------------------------------------------
# 5.3 — Prolungamento orario primaria (art. 35)
# ---------------------------------------------------------------------------

def calc_prolungamento_mensile(tabellare: Decimal, ore_prol: int) -> Decimal:
    """
    Monthly amount due for prolungamento orario (V livello — docenti primaria).
        quota_h      = tabellare / 104
        quota_h_prol = quota_h × 0.80
        importo     = quota_h_prol × ore_prol × 4.333
    """
    if ore_prol < 0 or ore_prol > PROLUNGAMENTO["max_ore_settimanali"]:
        raise ValueError(
            f"ore_prol must be in [0, {PROLUNGAMENTO['max_ore_settimanali']}]"
        )
    quota_h = tabellare / PROLUNGAMENTO["divisore_quota_oraria"]
    quota_prol = quota_h * PROLUNGAMENTO["coefficiente_riduzione"]
    importo = quota_prol * Decimal(ore_prol) * PROLUNGAMENTO["moltiplicatore_mensile"]
    return _money(importo)


# ---------------------------------------------------------------------------
# 5.4 — Doposcuola compenso
# ---------------------------------------------------------------------------

def calc_doposcuola(ore_totali: int) -> Decimal:
    """Total compensation for doposcuola: ore × €40,00."""
    if ore_totali < 0:
        raise ValueError("ore_totali must be >= 0")
    return _money(Decimal(ore_totali) * DOPOSCUOLA_RATE_HOUR)


# ---------------------------------------------------------------------------
# 5.5 — School Camp compenso
# ---------------------------------------------------------------------------

def calc_school_camp(settimane: int) -> Decimal:
    """Total compensation for school camp: settimane × €380,00."""
    if settimane < 0:
        raise ValueError("settimane must be >= 0")
    return _money(Decimal(settimane) * SCHOOL_CAMP_RATE_WEEK)


# ---------------------------------------------------------------------------
# 5.6 — RAL annua lorda
# ---------------------------------------------------------------------------

def calc_ral(
    tabellare: Decimal,
    afac: Decimal,
    prolungamento_annuale: Decimal = Decimal("0"),
    indennita_funzione_annuale: Decimal = Decimal("0"),
    doposcuola: Decimal = Decimal("0"),
    camp: Decimal = Decimal("0"),
) -> Decimal:
    """
    RAL = (tabellare + AFAC) × 13 mensilità + voci aggiuntive annue.

    The voci aggiuntive (prolungamento, indennità funzione, doposcuola, camp)
    are already total-annual amounts at this point and are summed verbatim.
    """
    base_annuale = (tabellare + afac) * MENSILITA_ANNUE
    return _money(
        base_annuale
        + prolungamento_annuale
        + indennita_funzione_annuale
        + doposcuola
        + camp
    )


# ---------------------------------------------------------------------------
# 5.7 — 24-month limit check for determinato + rinnovo (art. 19 D.lgs. 81/2015)
# ---------------------------------------------------------------------------

def check_24months(mesi_prec: int, durata_nuovo: int) -> dict:
    """Returns {'totale': int, 'warning': bool}. Warning fires when totale >= 24."""
    if mesi_prec < 0 or durata_nuovo < 0:
        raise ValueError("mesi must be >= 0")
    totale = mesi_prec + durata_nuovo
    return {"totale": totale, "warning": totale >= 24}


# ===========================================================================
# DOCX generation (§7) and PDF conversion via LibreOffice headless
# ===========================================================================

import subprocess
from pathlib import Path

from docx import Document
from docx.enum.table import WD_ALIGN_VERTICAL
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
from docx.oxml import OxmlElement
from docx.shared import Cm, Pt, RGBColor

from ccnl_data import (
    AREA,
    INFLESSIONI,
    ORE_BASE,
    PENALE_DEFAULT,
    PERIODO_PROVA_MESI,
    SEDI,
    STATIC_BLOCKS,
    TEXT_BLOCKS,
)
from helpers import (
    anno_scolastico,
    format_currency_it,
    format_date_long,
    format_date_short,
    num_to_words_it,
)


_FONT_NAME = "Arial"
_FONT_SIZE = Pt(11)
_MARGIN = Cm(2.5)
_HEADER_GREY = "E8E8E8"
_BORDER_GREY = "CCCCCC"

ASSETS_DIR = Path(__file__).resolve().parent / "assets"
OUTPUT_DIR = Path(__file__).resolve().parent / "output"


# ---------------------------------------------------------------------------
# Document setup helpers
# ---------------------------------------------------------------------------

def _set_margins(doc: Document, margin: Cm) -> None:
    for section in doc.sections:
        section.top_margin = margin
        section.bottom_margin = margin
        section.left_margin = margin
        section.right_margin = margin


def _set_default_font(doc: Document) -> None:
    style = doc.styles["Normal"]
    style.font.name = _FONT_NAME
    style.font.size = _FONT_SIZE
    rpr = style.element.get_or_add_rPr()
    rfonts = rpr.find(qn("w:rFonts"))
    if rfonts is None:
        rfonts = OxmlElement("w:rFonts")
        rpr.append(rfonts)
    rfonts.set(qn("w:ascii"), _FONT_NAME)
    rfonts.set(qn("w:hAnsi"), _FONT_NAME)
    rfonts.set(qn("w:cs"), _FONT_NAME)


def _style_paragraph(p, *, align=WD_ALIGN_PARAGRAPH.JUSTIFY,
                     space_after=Pt(8), line_spacing=1.15) -> None:
    p.alignment = align
    pf = p.paragraph_format
    pf.line_spacing = line_spacing
    pf.space_after = space_after


def _add_run(p, text: str, *, bold: bool = False) -> None:
    run = p.add_run(text)
    run.font.name = _FONT_NAME
    run.font.size = _FONT_SIZE
    run.bold = bold
    # Force Arial in the run too — some docx clients need this explicitly
    rpr = run._element.get_or_add_rPr()
    rfonts = rpr.find(qn("w:rFonts"))
    if rfonts is None:
        rfonts = OxmlElement("w:rFonts")
        rpr.append(rfonts)
    rfonts.set(qn("w:ascii"), _FONT_NAME)
    rfonts.set(qn("w:hAnsi"), _FONT_NAME)


def _add_para(doc, text: str = "", *, bold: bool = False,
              align=WD_ALIGN_PARAGRAPH.JUSTIFY, space_after=Pt(8),
              line_spacing=1.15):
    p = doc.add_paragraph()
    _style_paragraph(p, align=align, space_after=space_after,
                     line_spacing=line_spacing)
    if text:
        _add_run(p, text, bold=bold)
    return p


def _add_header(doc, text: str):
    """Section heading in bold (Periodo di prova, Orario di lavoro, ...)."""
    return _add_para(doc, text, bold=True, align=WD_ALIGN_PARAGRAPH.LEFT,
                     space_after=Pt(4))


# ---------------------------------------------------------------------------
# Cell shading + borders for the retribution table
# ---------------------------------------------------------------------------

def _shade_cell(cell, hex_color: str) -> None:
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear")
    shd.set(qn("w:color"), "auto")
    shd.set(qn("w:fill"), hex_color)
    tc_pr.append(shd)


def _set_table_borders(table, hex_color: str) -> None:
    tbl = table._tbl
    tbl_pr = tbl.tblPr
    borders = OxmlElement("w:tblBorders")
    for edge in ("top", "left", "bottom", "right", "insideH", "insideV"):
        b = OxmlElement(f"w:{edge}")
        b.set(qn("w:val"), "single")
        b.set(qn("w:sz"), "4")
        b.set(qn("w:space"), "0")
        b.set(qn("w:color"), hex_color)
        borders.append(b)
    tbl_pr.append(borders)


# ---------------------------------------------------------------------------
# Inflection / payload helpers
# ---------------------------------------------------------------------------

def _inflect(template: str, sesso: str) -> str:
    """Substitute gender placeholders {il_la}, {ore_rice}, {lo_la} in a template.

    {il_la}     -> il / la            (determiner: 'il docente' / 'la docente')
    {ore_rice}  -> ore / rice         (suffix:    'lavoratore' / 'lavoratrice')
    {lo_la}     -> lo / la            (enclitic pronoun: 'Pregandolo' / 'Pregandola')
    """
    infl = INFLESSIONI[sesso]
    return (template
            .replace("{il_la}", infl["il_la_word"])
            .replace("{ore_rice}", infl["ore_rice"])
            .replace("{lo_la}", infl["lo_la"]))


def _saluto(sesso: str) -> str:
    return INFLESSIONI[sesso]["saluto"]


def _nato_a(sesso: str) -> str:
    return INFLESSIONI[sesso]["nato_a"]


def _d(x) -> Decimal:
    """Coerce JSON-friendly value to Decimal. Accepts str, int, float, Decimal."""
    if isinstance(x, Decimal):
        return x
    return Decimal(str(x))


# ---------------------------------------------------------------------------
# Section builders — one per §7.1 entry
# ---------------------------------------------------------------------------

def _add_logo(doc, sede_key: str) -> None:
    """§7.1 #1 — logo top-right at 2.5 cm height (width auto)."""
    p = doc.add_paragraph()
    _style_paragraph(p, align=WD_ALIGN_PARAGRAPH.RIGHT,
                     space_after=Pt(12), line_spacing=1.0)
    logo_file = SEDI[sede_key]["logo_file"]
    logo_path = ASSETS_DIR / logo_file
    if logo_path.exists():
        run = p.add_run()
        run.add_picture(str(logo_path), height=Cm(2.5))
    # else: silently skip — logo file not yet uploaded (brief allows this)


def _add_recipient(doc, payload: dict) -> None:
    """§7.1 #2 — destinatario block (right-aligned)."""
    sesso = payload["sesso"]
    nome = payload["nome"]

    # "Gent.ma Sig.ra {nome}" / "Egr. Sig. {nome}"
    _add_para(doc, f"{_saluto(sesso)} {nome}",
              align=WD_ALIGN_PARAGRAPH.RIGHT, space_after=Pt(0))

    if payload.get("data_nascita"):
        d = _parse_date(payload["data_nascita"])
        _add_para(doc, f"{_nato_a(sesso)} {format_date_short(d)}",
                  align=WD_ALIGN_PARAGRAPH.RIGHT, space_after=Pt(0))

    if payload.get("codice_fiscale"):
        _add_para(doc, f"CF: {payload['codice_fiscale']}",
                  align=WD_ALIGN_PARAGRAPH.RIGHT, space_after=Pt(12))
    else:
        # add spacing block even if no CF, to separate from oggetto
        _add_para(doc, "", align=WD_ALIGN_PARAGRAPH.RIGHT)


def _add_oggetto(doc, payload: dict) -> None:
    """§7.1 #3 — bold object line."""
    tipo_orario = "pieno" if payload["tipo_contratto"] == "tempo_pieno" else "part time"
    durata = payload["durata"]  # 'indeterminato' | 'determinato'
    p = doc.add_paragraph()
    _style_paragraph(p, align=WD_ALIGN_PARAGRAPH.LEFT, space_after=Pt(8))
    _add_run(p, f"Oggetto: Proposta di assunzione a tempo {tipo_orario} e {durata}.",
             bold=True)


def _add_intese_e_inquadramento(doc, payload: dict) -> None:
    """§7.1 #4 — intese verbali + decorrenza, then §7.1 #5/#6 inquadramento/riserva/sede."""
    tipo_orario = "pieno" if payload["tipo_contratto"] == "tempo_pieno" else "part time"
    data_inizio = format_date_short(_parse_date(payload["data_inizio"]))

    if payload["durata"] == "determinato":
        data_fine = format_date_short(_parse_date(payload["data_fine"]))
        intese = STATIC_BLOCKS["intese_determinato"].format(
            tipo_orario=tipo_orario, data_inizio=data_inizio, data_fine=data_fine,
        )
    else:
        intese = STATIC_BLOCKS["intese_indeterminato"].format(
            tipo_orario=tipo_orario, data_inizio=data_inizio,
        )
    _add_para(doc, intese)

    # Inquadramento
    livello = payload["livello"]
    inq = STATIC_BLOCKS["inquadramento"].format(
        mansione=payload["mansione"],
        livello=livello,
        area=AREA[livello],
    )
    _add_para(doc, inq)

    # Riserva CCNL
    _add_para(doc, STATIC_BLOCKS["riserva_ccnl"])

    # Sede di lavoro
    sede = SEDI[payload["sede"]]
    _add_para(doc, STATIC_BLOCKS["sede_lavoro"].format(
        indirizzo_sede=sede["indirizzo"],
    ))


def _add_motivazione_rinnovo(doc, payload: dict) -> None:
    """§7.1 #7 — only if rinnovo=Yes. Placed before periodo di prova."""
    sesso = payload["sesso"]
    tpl = _inflect(TEXT_BLOCKS["motivazione_rinnovo"], sesso)
    text = tpl.format(
        cognome_nome=payload["nome"],
        durata_rinnovo=payload["rinnovo"]["durata_mesi"],
    )
    _add_para(doc, text)


def _add_periodo_prova(doc, payload: dict) -> None:
    """§7.1 #8 — periodo di prova section."""
    _add_header(doc, STATIC_BLOCKS["header_prova"])
    if payload["durata"] == "determinato":
        _add_para(doc, TEXT_BLOCKS["prova_determinato"])
    else:
        mesi = PERIODO_PROVA_MESI[("indeterminato", payload["livello"])]
        durata_prova = f"{mesi} ({num_to_words_it(mesi)}) mesi"
        text = TEXT_BLOCKS["prova_indeterminato"].format(durata_prova=durata_prova)
        _add_para(doc, text)


def _add_orario(doc, payload: dict) -> None:
    """§7.1 #9 + #10 — orario di lavoro + optional clausola prolungamento."""
    _add_header(doc, STATIC_BLOCKS["header_orario"])
    ore = payload["ore_settimanali"]
    _add_para(doc, STATIC_BLOCKS["orario_base"].format(ore_settimanali=ore))

    prol = payload.get("prolungamento")
    if prol and prol.get("attivo"):
        sesso = payload["sesso"]
        tpl = _inflect(TEXT_BLOCKS["prolungamento_orario"], sesso)
        ore_prol = prol["ore"]
        text = tpl.format(
            anno_scolastico=anno_scolastico(_parse_date(payload["data_inizio"])),
            ore_prolungamento=ore_prol,
            ore_lettere=num_to_words_it(ore_prol),
        )
        _add_para(doc, text)


def _retribution_amounts(payload: dict) -> tuple[Decimal, Decimal, Decimal]:
    """Apply part-time proportioning per brief §3 Step 6 and §4.2.

    Tabellare and AFAC are submitted as FULL-TIME values; the backend
    proportions both by ore_settimanali / ORE_BASE[livello] when part-time.
    Indennità di funzione is a flat allowance — not proportioned.
    """
    tabellare = _d(payload["tabellare"])
    afac = _d(payload.get("afac", 0))
    indennita = _d(payload.get("indennita_funzione", 0))

    is_pt = payload["tipo_contratto"] != "tempo_pieno"
    if is_pt:
        livello = payload["livello"]
        ore_base = ORE_BASE.get(livello)
        ore = int(payload["ore_settimanali"])
        if ore_base:
            tabellare = proportion_parttime(tabellare, ore, ore_base)
            afac = proportion_parttime(afac, ore, ore_base)
    return tabellare, afac, indennita


def _add_retribuzione(doc, payload: dict) -> Decimal:
    """§7.1 #11 — intro + 2-column retribution table. Returns totale mensile."""
    _add_header(doc, STATIC_BLOCKS["header_retribuzione"])

    tabellare, afac, indennita = _retribution_amounts(payload)
    totale_mensile = tabellare + afac + indennita

    _add_para(doc, STATIC_BLOCKS["retribuzione_intro"].format(
        totale_mensile=format_currency_it(totale_mensile),
    ))

    livello = payload["livello"]
    is_pt = payload["tipo_contratto"] != "tempo_pieno"
    ratio_label = ""
    if is_pt and ORE_BASE[livello]:
        ratio_label = f" — part-time {payload['ore_settimanali']}/{ORE_BASE[livello]}"

    rows = [(f"Minimo Conglobato ({livello}{ratio_label})", tabellare)]
    if afac > 0:
        rows.append(("AFAC", afac))
    if indennita > 0:
        rows.append(("Indennità di funzione", indennita))
    rows.append(("TOTALE", totale_mensile))

    table = doc.add_table(rows=1 + len(rows), cols=2)
    table.autofit = False
    _set_table_borders(table, _BORDER_GREY)

    # Header row
    hdr = table.rows[0].cells
    hdr[0].text = ""
    hdr[1].text = ""
    _write_cell(hdr[0], "Voce retributiva", bold=True)
    _write_cell(hdr[1], "Importo mensile lordo", bold=True)
    for c in hdr:
        _shade_cell(c, _HEADER_GREY)
        c.vertical_alignment = WD_ALIGN_VERTICAL.CENTER

    # Body rows
    for i, (label, importo) in enumerate(rows, start=1):
        row = table.rows[i].cells
        row[0].text = ""
        row[1].text = ""
        is_total = (label == "TOTALE")
        _write_cell(row[0], label, bold=is_total)
        _write_cell(row[1], f"€ {format_currency_it(importo)}", bold=is_total)

    # spacing after table
    _add_para(doc, "")
    return totale_mensile


def _write_cell(cell, text: str, *, bold: bool = False) -> None:
    p = cell.paragraphs[0]
    p.paragraph_format.space_after = Pt(0)
    _add_run(p, text, bold=bold)


def _add_doposcuola(doc, payload: dict) -> None:
    """§7.1 #12."""
    block = payload["doposcuola"]
    text = TEXT_BLOCKS["doposcuola"].format(
        anno_scolastico=anno_scolastico(_parse_date(payload["data_inizio"])),
        ore_doposcuola=block["ore"],
        ambito=block.get("ambito", ""),
        compenso_doposcuola=format_currency_it(_d(block["compenso"])),
    )
    _add_para(doc, text)


def _add_school_camp(doc, payload: dict) -> None:
    """§7.1 #13."""
    block = payload["camp"]
    settimane = block["settimane"]
    text = TEXT_BLOCKS["school_camp"].format(
        anno_scolastico=anno_scolastico(_parse_date(payload["data_inizio"])),
        n_settimane=settimane,
        settimane_lettere=num_to_words_it(settimane),
        compenso_camp=format_currency_it(_d(block["compenso"])),
    )
    _add_para(doc, text)


def _add_afac_clause(doc, payload: dict) -> None:
    """§7.1 #14 — assorbimento futuri aumenti. Uses the PT-proportioned AFAC."""
    sesso = payload["sesso"]
    _, afac_effective, _ = _retribution_amounts(payload)
    tpl = _inflect(STATIC_BLOCKS["afac_assorbimento"], sesso)
    text = tpl.format(afac_pt=format_currency_it(afac_effective))
    _add_para(doc, text)


def _add_ral_summary(doc, payload: dict, ral: Decimal) -> None:
    """§7.1 #15 — RAL summary sentence."""
    voci = []
    if (payload.get("prolungamento") or {}).get("attivo"):
        voci.append("prolungamento orario")
    if _d(payload.get("indennita_funzione", 0)) > 0:
        voci.append("indennità di funzione (rinnovabile annualmente)")
    if payload.get("doposcuola"):
        voci.append("attività di doposcuola (rinnovabile annualmente)")
    if payload.get("camp"):
        voci.append("School Camp (rinnovabile annualmente)")

    if voci:
        voci_clause = ", unitamente a quanto erogato a titolo di " + _it_join(voci)
    else:
        voci_clause = ""

    ral_int = int(ral)  # words form ignores cents (... /00)
    text = STATIC_BLOCKS["ral_summary"].format(
        voci_aggiuntive_clause=voci_clause,
        anno_scolastico=anno_scolastico(_parse_date(payload["data_inizio"])),
        ral=format_currency_it(ral),
        ral_lettere=num_to_words_it(ral_int),
    )
    _add_para(doc, text)


def _it_join(items: list[str]) -> str:
    """['a','b','c'] -> 'a, b e c'."""
    if not items:
        return ""
    if len(items) == 1:
        return items[0]
    return ", ".join(items[:-1]) + " e " + items[-1]


def _add_modalita_pagamento(doc) -> None:
    """§7.1 #16."""
    _add_para(doc, STATIC_BLOCKS["modalita_pagamento"])


def _add_fringe(doc, payload: dict) -> None:
    """§7.1 #17."""
    _add_header(doc, STATIC_BLOCKS["fringe_heading"])
    fringe = payload["fringe"]
    con_figli = fringe.get("con_figli", False)
    importo = _d(2000 if con_figli else 1000)
    clausola = (
        "in presenza di figli a carico"
        if con_figli else "qualora il lavoratore non abbia figli a carico"
    )
    text = STATIC_BLOCKS["fringe_paragraph"].format(
        importo_fringe=format_currency_it(importo),
        clausola_figli=clausola,
    )
    _add_para(doc, text)


def _add_patto_durata(doc, payload: dict) -> None:
    """§7.1 #18 — patto di durata minima (between RAL summary and disciplina)."""
    _add_header(doc, TEXT_BLOCKS["patto_durata_heading"])
    patto = payload["patto"]
    penale = _d(patto.get("penale", PENALE_DEFAULT))
    inizio = _parse_date(payload["data_inizio"])
    fine = _parse_date(payload["data_fine"])

    par1 = TEXT_BLOCKS["patto_durata_par1"].format(
        data_inizio_estesa=format_date_long(inizio),
        data_fine_estesa=format_date_long(fine),
    )
    _add_para(doc, par1)

    par2 = TEXT_BLOCKS["patto_durata_par2"].format(
        penale_cifre=format_currency_it(penale),
        penale_lettere=num_to_words_it(int(penale)),
    )
    _add_para(doc, par2)


def _add_disciplina(doc, payload: dict) -> None:
    """§7.1 #19/#20 — disciplina del rapporto + D.lgs. 152/1997."""
    _add_header(doc, STATIC_BLOCKS["disciplina_heading"])
    sesso = payload["sesso"]
    text = _inflect(STATIC_BLOCKS["disciplina_paragraph"], sesso)
    _add_para(doc, text)


def _add_chiusura_e_firma(doc, payload: dict) -> None:
    """§7.1 #21/#22 — chiusura + firma block."""
    sesso = payload["sesso"]
    _add_para(doc, _inflect(STATIC_BLOCKS["chiusura"], sesso))

    citta = SEDI[payload["sede"]]["citta"]
    today = date.today()
    _add_para(doc, STATIC_BLOCKS["data_firma"].format(
        citta=citta, data_oggi=format_date_short(today),
    ), align=WD_ALIGN_PARAGRAPH.LEFT)

    # Signature block — right side for company, then left side for accettazione
    _add_para(doc, "___________________________",
              align=WD_ALIGN_PARAGRAPH.RIGHT, space_after=Pt(0))
    _add_para(doc, STATIC_BLOCKS["timbro_firma"],
              align=WD_ALIGN_PARAGRAPH.RIGHT, space_after=Pt(20))
    _add_para(doc, STATIC_BLOCKS["per_accettazione"],
              align=WD_ALIGN_PARAGRAPH.LEFT)


# ---------------------------------------------------------------------------
# Top-level orchestrator
# ---------------------------------------------------------------------------

def _parse_date(value) -> date:
    if isinstance(value, date):
        return value
    # accept 'YYYY-MM-DD' or 'DD/MM/YYYY'
    s = str(value)
    if "/" in s:
        d, m, y = s.split("/")
        return date(int(y), int(m), int(d))
    return date.fromisoformat(s)


def _slugify(name: str) -> str:
    out = []
    for ch in name.lower():
        if ch.isalnum():
            out.append(ch)
        elif ch in (" ", "-", "_"):
            out.append("_")
    return "".join(out).strip("_") or "contratto"


def build_contract(payload: dict, output_dir: Path | None = None) -> Path:
    """
    Build the contract DOCX. Returns the path to the saved file.
    Implements all 22 sections of §7.1 in order (with conditional blocks).
    """
    output_dir = Path(output_dir) if output_dir else OUTPUT_DIR
    output_dir.mkdir(parents=True, exist_ok=True)

    doc = Document()
    _set_margins(doc, _MARGIN)
    _set_default_font(doc)

    # Pre-compute RAL so it can appear in §15 / RAL summary.
    # tabellare and afac here are already proportioned to PT if applicable.
    tabellare, afac, indennita = _retribution_amounts(payload)
    prol_block = payload.get("prolungamento") or {}
    if prol_block.get("attivo"):
        # If a precomputed yearly amount was sent, use it; otherwise derive
        # mensile from tabellare (post-proportioning) × 13.
        if prol_block.get("importo_annuale"):
            prol_annuale = _d(prol_block["importo_annuale"])
        else:
            prol_mensile = calc_prolungamento_mensile(tabellare, int(prol_block["ore"]))
            prol_annuale = prol_mensile * MENSILITA_ANNUE
    else:
        prol_annuale = Decimal("0")
    indennita_ann = indennita * MENSILITA_ANNUE
    dopo = _d(payload["doposcuola"]["compenso"]) if payload.get("doposcuola") else Decimal("0")
    camp = _d(payload["camp"]["compenso"]) if payload.get("camp") else Decimal("0")
    ral = calc_ral(tabellare, afac, prol_annuale, indennita_ann, dopo, camp)

    # 1 logo
    _add_logo(doc, payload["sede"])
    # 2 recipient
    _add_recipient(doc, payload)
    # 3 oggetto
    _add_oggetto(doc, payload)
    # 4 intese verbali + 5 inquadramento + 6 riserva + sede
    _add_intese_e_inquadramento(doc, payload)
    # 7 motivazione rinnovo (conditional)
    if payload.get("rinnovo"):
        _add_motivazione_rinnovo(doc, payload)
    # 8 periodo di prova
    _add_periodo_prova(doc, payload)
    # 9 + 10 orario + prolungamento
    _add_orario(doc, payload)
    # 11 retribuzione (intro + table)
    _add_retribuzione(doc, payload)
    # 12 doposcuola (conditional)
    if payload.get("doposcuola"):
        _add_doposcuola(doc, payload)
    # 13 school camp (conditional)
    if payload.get("camp"):
        _add_school_camp(doc, payload)
    # 14 AFAC assorbimento clause (conditional)
    if afac > 0:
        _add_afac_clause(doc, payload)
    # 15 RAL summary
    _add_ral_summary(doc, payload, ral)
    # 16 modalità pagamento
    _add_modalita_pagamento(doc)
    # 17 fringe (conditional)
    if payload.get("fringe"):
        _add_fringe(doc, payload)
    # 18 patto durata minima (conditional)
    if payload.get("patto"):
        _add_patto_durata(doc, payload)
    # 19/20 disciplina + D.lgs. 152/1997
    _add_disciplina(doc, payload)
    # 21/22 chiusura + firma
    _add_chiusura_e_firma(doc, payload)

    slug = _slugify(payload["nome"])
    out_path = output_dir / f"contratto_{slug}.docx"
    doc.save(out_path)
    return out_path


def convert_to_pdf(docx_path: Path, output_dir: Path | None = None) -> Path:
    """
    Convert a DOCX file to PDF using LibreOffice headless.
    Returns the path to the generated PDF.
    """
    docx_path = Path(docx_path)
    output_dir = Path(output_dir) if output_dir else docx_path.parent
    output_dir.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        ["soffice", "--headless", "--convert-to", "pdf",
         "--outdir", str(output_dir), str(docx_path)],
        check=True, capture_output=True,
    )
    return output_dir / (docx_path.stem + ".pdf")
