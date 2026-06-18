"""
CCNL ANINSEI 2024-2027 — Data tables and contract text blocks.

Reference: CCNL Scuole Private Laiche, signed 15/06/2024.
All monetary values are stored as Decimal to avoid floating-point errors.
"""

from decimal import Decimal


# ---------------------------------------------------------------------------
# 4.1 Livelli, Aree, Mansioni tipiche, Orario base settimanale
# ---------------------------------------------------------------------------

LIVELLI = ["I", "II", "III", "IV", "V", "VI", "VII", "VIII-A", "VIII-B"]

AREA = {
    "I":      "Prima (ATA)",
    "II":     "Prima (ATA)",
    "III":    "Prima (ATA)",
    "IV":     "Seconda (DOC)",
    "V":      "Seconda (DOC)",
    "VI":     "Seconda (DOC)",
    "VII":    "Seconda (DOC)",
    "VIII-A": "Direttiva",
    "VIII-B": "Direttiva",
}

PROFILO_TIPICO = {
    "I":      "Ausiliario",
    "II":     "Operaio qualificato",
    "III":    "Impiegato d'ordine / Ass. infanzia",
    "IV":     "Docente infanzia / Atelierista",
    "V":      "Docente primaria",
    "VI":     "Docente secondaria I e II grado",
    "VII":    "Docente accademia / specializzazione",
    "VIII-A": "Direttore / Preside I grado",
    "VIII-B": "Preside II grado",
}

# Weekly hours base for each livello. VIII-A / VIII-B have no fixed orario (N/A).
ORE_BASE = {
    "I":      38,
    "II":     38,
    "III":    38,
    "IV":     34,
    "V":      24,
    "VI":     18,
    "VII":    18,
    "VIII-A": None,
    "VIII-B": None,
}


# ---------------------------------------------------------------------------
# 4.2 Tabellare mensile lordo (art. 22)
# Key: TABELLARE[livello][year_string] -> Decimal monthly base salary (full-time)
# Year strings: '2025', '2026', '2027'. See get_tabellare() in generator.py.
# ---------------------------------------------------------------------------

TABELLARE = {
    "I": {
        "2025": Decimal("1280.90"),
        "2026": Decimal("1322.50"),
        "2027": Decimal("1347.46"),
    },
    "II": {
        "2025": Decimal("1311.02"),
        "2026": Decimal("1354.06"),
        "2027": Decimal("1379.61"),
    },
    "III": {
        "2025": Decimal("1374.80"),
        "2026": Decimal("1419.45"),
        "2027": Decimal("1446.24"),
    },
    "IV": {
        "2025": Decimal("1444.47"),
        "2026": Decimal("1491.38"),
        "2027": Decimal("1519.53"),
    },
    "V": {
        "2025": Decimal("1539.64"),
        "2026": Decimal("1589.64"),
        "2027": Decimal("1619.64"),
    },
    "VI": {
        "2025": Decimal("1539.64"),
        "2026": Decimal("1589.64"),
        "2027": Decimal("1619.64"),
    },
    "VII": {
        "2025": Decimal("1563.21"),
        "2026": Decimal("1615.68"),
        "2027": Decimal("1646.17"),
    },
    "VIII-A": {
        "2025": Decimal("1661.64"),
        "2026": Decimal("1716.64"),
        "2027": Decimal("1748.60"),
    },
    "VIII-B": {
        "2025": Decimal("1751.92"),
        "2026": Decimal("1809.92"),
        "2027": Decimal("1843.62"),
    },
}


# ---------------------------------------------------------------------------
# 4.3 Periodo di prova (art. 15, rinvio R.D.L. 1825/1924)
# Months calendar.
# ---------------------------------------------------------------------------

PERIODO_PROVA_MESI = {
    # Indeterminato
    ("indeterminato", "I"):      2,
    ("indeterminato", "II"):     2,
    ("indeterminato", "III"):    2,
    ("indeterminato", "IV"):     4,
    ("indeterminato", "V"):      4,
    ("indeterminato", "VI"):     4,
    ("indeterminato", "VII"):    4,
    ("indeterminato", "VIII-A"): 4,
    ("indeterminato", "VIII-B"): 4,
    # Determinato — qualunque livello: 1 mese
    ("determinato", "I"):      1,
    ("determinato", "II"):     1,
    ("determinato", "III"):    1,
    ("determinato", "IV"):     1,
    ("determinato", "V"):      1,
    ("determinato", "VI"):     1,
    ("determinato", "VII"):    1,
    ("determinato", "VIII-A"): 1,
    ("determinato", "VIII-B"): 1,
}


# ---------------------------------------------------------------------------
# 4.4 Preavviso dimissioni / licenziamento (art. 57)
# Returns months of notice.
# ---------------------------------------------------------------------------

def preavviso_mesi(tipo_contratto: str, livello: str, anzianita_anni: float) -> int:
    """
    tipo_contratto: 'determinato' | 'indeterminato' | 'apprendistato'
    livello: 'I'..'VIII-B'
    anzianita_anni: years of service
    """
    if tipo_contratto in ("determinato", "apprendistato"):
        return 1

    livelli_bassi = livello in ("I", "II", "III")

    if livelli_bassi:
        if anzianita_anni < 5:
            return 1
        elif anzianita_anni <= 10:
            return 2
        else:
            return 3
    else:  # IV-VIII
        if anzianita_anni < 5:
            return 2
        elif anzianita_anni <= 10:
            return 3
        else:
            return 4


# ---------------------------------------------------------------------------
# 4.5 Prolungamento Orario — formula da foglio "Calcolo PO stipendi" (Ingenium).
#
#   annuale = (tabellare + AFAC) / divisore  ×  0,80  ×  ore_prol  ×  39 settimane
#
# - divisore: dipende dall'orario base settimanale del livello (lookup
#   DIVISORE_ORARIO sotto).  Es. 34h => 147, 24h => 104, 18h => 78.
# - 80%: percentuale di pagamento PO sul costo orario base.
# - 39: settimane scolastiche per cui il PO è erogato (anno scolastico).
#
# Applicabilità: IV (Infanzia, max 4h), V (Primaria, max 8h), VI (MHS, max 6h).
# ---------------------------------------------------------------------------

PROLUNGAMENTO = {
    "livelli_applicabili":   ["IV", "V", "VI"],
    "max_ore_per_livello":   {"IV": 4, "V": 8, "VI": 6},
    "coefficiente_riduzione": Decimal("0.80"),
    "settimane_scolastiche":  39,
}

# Divisore orario per il calcolo del costo orario (CCNL ANINSEI).
# divisore ≈ orario_settimanale × 13/3 arrotondato.
DIVISORE_ORARIO = {
    38: 164,   # ATA
    36: 156,
    34: 147,   # Infanzia (IV)
    32: 139,
    24: 104,   # Primaria (V)
    21:  91,
    18:  78,   # Secondaria / MHS (VI, VII)
}


# ---------------------------------------------------------------------------
# Tariffe voci aggiuntive
# ---------------------------------------------------------------------------

DOPOSCUOLA_RATE_HOUR = Decimal("40.00")
SCHOOL_CAMP_RATE_WEEK = Decimal("380.00")

# Fringe benefit (Legge di Bilancio: €2.000 con figli a carico; €1.000 senza)
FRINGE_BENEFIT = {
    "con_figli": Decimal("2000.00"),
    "senza_figli": Decimal("1000.00"),
}

# Patto di durata minima — penale di default (modificabile in input)
PENALE_DEFAULT = Decimal("5000.00")

# Numero di mensilità (tredicesima inclusa)
MENSILITA_ANNUE = 13

# ---------------------------------------------------------------------------
# Sedi: indirizzo e file logo
# ---------------------------------------------------------------------------

SEDI = {
    "Acorn": {
        "nome_completo": "Acorn International School",
        "indirizzo": "Via della Giustiniana n. 1200, Roma (RM)",
        "citta": "Roma",
        "logo_file": "logo_acorn.png",
    },
    "St Francis": {
        "nome_completo": "St Francis International School",
        "indirizzo": "[inserire indirizzo]",
        "citta": "Roma",
        "logo_file": "logo_sfis.png",
    },
}


# ---------------------------------------------------------------------------
# 6. Contract text blocks — string templates with {placeholder} syntax.
# ---------------------------------------------------------------------------

TEXT_BLOCKS = {

    # 6.1 Periodo di prova — determinato (1 mese, testo fisso)
    "prova_determinato": (
        "L'assunzione si intenderà subordinata al superamento di un periodo di prova "
        "della durata di 1 (un) mese, ai sensi dell'art. 15 Parte Seconda CCNL ANINSEI. "
        "Durante tale periodo il rapporto di lavoro può essere risolto da entrambe le "
        "parti senza obbligo di preavviso. In caso di sopravvenienza di eventi quali "
        "malattia, infortunio, congedo di maternità o paternità obbligatorio, il "
        "periodo di prova è prolungato in misura corrispondente alla durata "
        "dell'assenza."
    ),

    # 6.1 Periodo di prova — indeterminato (durata variabile in base al livello)
    "prova_indeterminato": (
        "L'assunzione si intenderà subordinata al superamento di un periodo di prova "
        "della durata di {durata_prova}, ai sensi dell'art. 15 Parte Seconda CCNL "
        "ANINSEI. Durante tale periodo il rapporto di lavoro può essere risolto da "
        "entrambe le parti senza obbligo di preavviso. In caso di sopravvenienza di "
        "eventi quali malattia, infortunio, congedo di maternità o paternità "
        "obbligatorio, il periodo di prova è prolungato in misura corrispondente alla "
        "durata dell'assenza."
    ),

    # 6.2 Clausola prolungamento orario (art. 35) — solo livello V
    "prolungamento_orario": (
        "Con la sottoscrizione della presente proposta di assunzione, {il_la} docente "
        "si dichiara fin d'ora disponibile per l'anno scolastico {anno_scolastico}, "
        "ad accettare l'assegnazione del prolungamento del proprio orario di lavoro, "
        "ai sensi dell'art. 35 CCNL Scuole Private Laiche, pari a un massimo di "
        "{ore_prolungamento} ({ore_lettere}) ore settimanali. "
        "L'assegnazione del prolungamento orario sarà oggetto di ulteriore "
        "comunicazione all'atto dell'assunzione."
    ),

    # 6.3 Doposcuola — {ambito} opzionale (es. " nell'ambito Atelier"); vuoto altrimenti
    "doposcuola": (
        "Con la sottoscrizione della presente proposta di assunzione, Ella s'impegna "
        "a svolgere, sempre per l'anno scolastico {anno_scolastico}, per "
        "{ore_doposcuola} ore pomeridiane, attività di doposcuola didattico{ambito}, "
        "fermo restando l'attivazione dei corsi doposcuola con almeno 12 studenti per "
        "classe, con compenso omnicomprensivo di € {compenso_doposcuola} lordi "
        "(rinnovabile annualmente)."
    ),

    # 6.4 School Camp
    "school_camp": (
        "In aggiunta, s'impegna a svolgere, sempre per l'anno scolastico "
        "{anno_scolastico}, {n_settimane} ({settimane_lettere}) settimane di School "
        "Camp, con compenso omnicomprensivo di € {compenso_camp} lordi (rinnovabile "
        "annualmente)."
    ),

    # 6.5 Patto di durata minima — heading + 2 paragrafi
    "patto_durata_heading": "Patto di durata minima:",

    "patto_durata_par1": (
        "Trascorso il periodo di prova, il rapporto di lavoro che si costituirà in "
        "data {data_inizio_estesa}, non potrà essere risolto dalle parti, salvo che "
        "per giusta causa e, o per impossibilità sopravvenuta della prestazione ex "
        "artt. 2119, 1463 e 1464 c.c., prima che sia raggiunta la scadenza del "
        "termine ({data_fine_estesa}), pattuita con la sottoscrizione del contratto "
        "di lavoro."
    ),

    "patto_durata_par2": (
        "In caso di mancato rispetto del predetto vincolo, le parti si impegnano a "
        "corrispondere al soggetto inadempiente, un importo a titolo di penale "
        "(artt. 1382 e 1384 c.c.), pari ad euro {penale_cifre} ({penale_lettere}), "
        "da corrispondersi all'atto della risoluzione del rapporto, anche mediante "
        "compensazione con le competenze correnti e, con quelle di fine rapporto."
    ),

    # 6.6 Motivazione rinnovo contratto determinato
    "motivazione_rinnovo": (
        "Il presente contratto a tempo determinato viene rinnovato in considerazione "
        "delle specifiche ed attuali esigenze organizzative e didattiche dell'Istituto, "
        "il quale, in quanto scuola paritaria ai sensi della L. 62/2000, è tenuto al "
        "rispetto dei requisiti di qualificazione professionale del personale docente "
        "previsti dalla normativa vigente e dal CCNL ANINSEI applicato. In tale "
        "contesto, l'Istituto si trova nella fase di valutazione dell'introduzione del "
        "programma Middle Years Programme (MYP) dell'International Baccalaureate "
        "Organization per il ciclo di scuola secondaria di primo grado, processo che "
        "richiede una verifica approfondita dei profili professionali e delle "
        "competenze necessarie per l'implementazione del curricolo. Considerato che "
        "{il_la} {cognome_nome} non è in possesso dei requisiti abilitativi richiesti "
        "per l'insegnamento nelle scuole paritarie italiane, e tenuto conto della "
        "natura transitoria delle esigenze didattiche e organizzative sopra descritte, "
        "l'Istituto procede al rinnovo del rapporto di lavoro a tempo determinato per "
        "un ulteriore periodo di {durata_rinnovo} mesi, ai sensi dell'art. 8 Parte "
        "Seconda CCNL ANINSEI 2024-2027 e nel rispetto dei limiti previsti dal "
        "D.lgs. 81/2015 in materia di contratti a tempo determinato."
    ),

    # Warning UI per superamento 24 mesi (D.lgs. 81/2015)
    "warning_24_mesi": (
        "Attenzione: la durata complessiva supera i 24 mesi previsti dall'art. 19 "
        "D.lgs. 81/2015. Valutare conversione a tempo indeterminato."
    ),
}


# ---------------------------------------------------------------------------
# 11. Appendix — Static text blocks that appear verbatim in every contract.
# Templates use {placeholder} syntax for the few inflections still needed
# (gender suffixes, AFAC amount).
# ---------------------------------------------------------------------------

STATIC_BLOCKS = {

    # Apertura: intese verbali / decorrenza — variante per durata.
    "intese_indeterminato": (
        "In relazione ed a conferma delle intese verbali intercorse, Le formuliamo la "
        "ns. proposta di assunzione, con contratto a tempo {tipo_orario} e "
        "indeterminato, a decorrere dal {data_inizio}."
    ),

    "intese_determinato": (
        "In relazione ed a conferma delle intese verbali intercorse, Le formuliamo la "
        "ns. proposta di assunzione, con contratto a tempo {tipo_orario} e "
        "determinato, a decorrere dal {data_inizio} e fino al {data_fine}."
    ),

    # Inquadramento: mansione, livello, area, CCNL
    "inquadramento": (
        "Il Suo inquadramento sarà quello di impiegato, Le sarà affidata la mansione "
        "di {mansione} e, Le sarà attribuito il livello {livello}, Area {area}, "
        "del CCNL Scuole Private Laiche, del 15/06/2024, le cui parti stipulanti sono "
        "ANINSEI – Confindustria, federazione UIL-SCUOLA RUA."
    ),

    # Riserva CCNL — clausola fissa
    "riserva_ccnl": (
        "Alla scadenza del CCNL applicato, La scrivente si riserva di verificare "
        "l'applicazione di un altro CCNL."
    ),

    # Sede di lavoro — testo costruito dall'indirizzo sede selezionata
    "sede_lavoro": "La Sua sede di lavoro sarà in {indirizzo_sede}.",

    # Orario di lavoro base (precede la clausola di prolungamento se applicabile)
    "orario_base": (
        "L'orario di lavoro contrattualmente previsto per il Suo inquadramento, è "
        "pari a {ore_settimanali} ore settimanali, distribuite dal lunedì al "
        "venerdì, nel rispetto della fruizione del riposo settimanale, che di norma "
        "coincide con la giornata della domenica, distribuite secondo le esigenze "
        "tecnico organizzative della scrivente."
    ),

    # Retribuzione — frase di intro che precede la tabella
    "retribuzione_intro": (
        "In relazione alla Sua mansione, al Suo orario di lavoro ed al corrispondente "
        "livello d'inquadramento, il valore mensile della Sua retribuzione "
        "complessiva, al momento dell'assunzione, sarà pari all'importo di euro "
        "{totale_mensile} lordi, comprensivi di tutti gli elementi retributivi che "
        "riportiamo di seguito:"
    ),

    # Clausola AFAC (assorbimento futuri aumenti) — appendice 11
    "afac_assorbimento": (
        "L'importo di € {afac_pt} assegnato a titolo di Anticipo Futuri Aumenti "
        "Contrattuali (AFAC), eccedente il minimo stabilito dal CCNL applicato, sarà "
        "riassorbito fino a concorrenza di ogni aumento retributivo stabilito dal "
        "CCNL, comunque disposto in prosieguo, anche se attuato mediante particolari "
        "istituti, di qualunque fonte, anche aventi effetto retroattivo, lasciando "
        "quindi invariata la RAL concordata. In caso di divieto di assorbimento di "
        "aumenti retributivi previsto dal CCNL, il patto relativo all'AFAC si "
        "intenderà automaticamente revocato dalla data di decorrenza degli aumenti. "
        "Tuttavia, al fine di preservare il rapporto con {il_la} lavorat{ore_rice}, "
        "contestualmente alla revoca e con la medesima decorrenza, la società "
        "riconoscerà automaticamente un nuovo acconto su futuri aumenti contrattuali, "
        "il cui importo sarà ricalcolato in modo tale da garantire il mantenimento "
        "della retribuzione annua lorda complessiva, tenendo conto dei nuovi minimi "
        "tempo per tempo aggiornati."
    ),

    # 7.3 RAL summary — voci aggiuntive elencate solo se presenti
    "ral_summary": (
        "La retribuzione lorda sopra indicata{voci_aggiuntive_clause}, Le consentirà, "
        "per l'anno scolastico {anno_scolastico}, di percepire una retribuzione annua "
        "lorda pari a € {ral} ({ral_lettere}/00)."
    ),

    # Modalità di pagamento
    "modalita_pagamento": (
        "Si precisa che detta retribuzione verrà corrisposta tramite bonifico "
        "bancario entro il giorno 10 del mese successivo all'avvenuta prestazione."
    ),

    # Heading + paragrafo fringe benefit
    "fringe_heading": "Possibilità di erogare premi e/o fringe benefit",

    "fringe_paragraph": (
        "In aggiunta alla retribuzione lorda mensile prevista contrattualmente per il "
        "livello di inquadramento e come dettagliatamente indicata in tabella, la "
        "scrivente si riserva la facoltà di erogare somme aggiuntive anche a titolo "
        "di premi di risultato e/o di procedere all'attribuzione di fringe benefit "
        "e/o attivare piani di welfare aziendale nel rispetto della Legge vigente e, "
        "del CCNL applicato al rapporto di lavoro, per un valore complessivo di "
        "€ {importo_fringe} annui, {clausola_figli}, fino massimo al 31/12/2027."
    ),

    # Heading + paragrafo disciplina del rapporto / D.lgs. 152/1997
    "disciplina_heading": "Disciplina del rapporto e comportamento in servizio:",

    "disciplina_paragraph": (
        "Il/La lavorat{ore_rice}, in virtù del presente contratto, si obbliga al "
        "rispetto del codice di disciplina e delle prescrizioni in materia di "
        "diligenza, fedeltà e riservatezza nonché delle policy aziendali in materia "
        "di privacy. In ottemperanza a quanto previsto dal D.lgs. n. 152/1997, come "
        "modificato dal D.lgs. n. 104/2022, si rinvia altresì all'informativa in "
        "allegato, nel rispetto dell'obbligo di comunicazione al lavoratore delle "
        "informazioni relative al rapporto di lavoro."
    ),

    # Chiusura lettera — pronome enclitico lo/la (Pregandolo / Pregandola)
    "chiusura": (
        "Pregando{lo_la} di restituirci copia della presente proposta di assunzione, "
        "firmata per accettazione, porgiamo distinti saluti."
    ),

    # Firma / data
    "data_firma": "{citta}, {data_oggi}",
    "timbro_firma": "(Timbro e Firma)",
    "per_accettazione": "Per accettazione: ____________________________",

    # Sezione header (in grassetto nel documento)
    "header_prova": "Periodo di prova:",
    "header_orario": "Orario di lavoro:",
    "header_retribuzione": "Retribuzione:",
    "header_doposcuola": "Doposcuola:",
    "header_school_camp": "School Camp:",
}


# ---------------------------------------------------------------------------
# Inflessioni di genere per i placeholder dei template.
# Utility helpers — usati da generator.py per le sostituzioni.
# ---------------------------------------------------------------------------

INFLESSIONI = {
    "M": {
        "saluto":     "Egr. Sig.",
        "nato_a":     "Nato il",
        "il_la_word": "il",     # "il docente"
        "ore_rice":   "ore",    # lavorat + "ore"   -> lavoratore
        "lo_la":      "lo",     # Pregando + "lo"   -> Pregandolo
    },
    "F": {
        "saluto":     "Gent.ma Sig.ra",
        "nato_a":     "Nata il",
        "il_la_word": "la",     # "la docente"
        "ore_rice":   "rice",   # lavorat + "rice"  -> lavoratrice
        "lo_la":      "la",     # Pregando + "la"   -> Pregandola
    },
}
