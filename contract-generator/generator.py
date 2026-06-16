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
