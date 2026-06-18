"""
Helpers: Italian formatting and number-to-words.

- format_currency_it(d)         -> "1.234,56"
- format_date_long(d)           -> "26 agosto 2026"
- format_date_short(d)          -> "26/08/2026"
- anno_scolastico(start_date)   -> "2026/2027"
- num_to_words_it(n)            -> "duemilacentoventitre"  (lowercase, no spaces)
- months_between(start, end)    -> int (full calendar months)

num_to_words_it is intentionally minimal: handles non-negative integers up to
999,999 — sufficient for monthly figures, RAL totals and penale defaults.
"""

from __future__ import annotations

from datetime import date
from decimal import Decimal


# ---------------------------------------------------------------------------
# Currency / dates
# ---------------------------------------------------------------------------

_ITALIAN_MONTHS = [
    "gennaio", "febbraio", "marzo", "aprile", "maggio", "giugno",
    "luglio", "agosto", "settembre", "ottobre", "novembre", "dicembre",
]


def format_currency_it(value: Decimal) -> str:
    """1234.56 -> '1.234,56'  (Italian thousand sep, decimal comma)."""
    q = value.quantize(Decimal("0.01"))
    sign = "-" if q < 0 else ""
    n = abs(q)
    s = f"{n:.2f}"  # "1234.56"
    int_part, dec_part = s.split(".")
    # group thousands with dots
    rev = int_part[::-1]
    grouped = ".".join(rev[i:i + 3] for i in range(0, len(rev), 3))[::-1]
    return f"{sign}{grouped},{dec_part}"


def format_date_long(d: date) -> str:
    """date(2026,8,26) -> '26 agosto 2026' (extended Italian form)."""
    return f"{d.day} {_ITALIAN_MONTHS[d.month - 1]} {d.year}"


def format_date_short(d: date) -> str:
    """date(2026,8,26) -> '26/08/2026'."""
    return d.strftime("%d/%m/%Y")


def anno_scolastico(start: date) -> str:
    """
    School year inferred from start_date.
    August onward starts the new school year.
    e.g. 26/08/2026 -> '2026/2027'; 15/02/2026 -> '2025/2026'.
    """
    if start.month >= 8:
        return f"{start.year}/{start.year + 1}"
    return f"{start.year - 1}/{start.year}"


def months_between(start: date, end: date) -> int:
    """Whole calendar months between two dates (inclusive end day)."""
    months = (end.year - start.year) * 12 + (end.month - start.month)
    if end.day >= start.day:
        months += 1
    return max(months, 0)


# ---------------------------------------------------------------------------
# Italian number-to-words (0 .. 999,999)
#
# Rules:
#   - venti, trenta, quaranta etc. drop the trailing vowel before "uno"/"otto"
#     (es. ventuno, ventotto)
#   - "tre" becomes "tré" with accent when used as a non-initial suffix in
#     compounds (ventitré, trentatré)
#   - "mille" / "mila": one thousand = "mille"; n thousands = "{n}mila"
#   - written without spaces (standard for contracts)
# ---------------------------------------------------------------------------

_UNITA = [
    "zero", "uno", "due", "tre", "quattro", "cinque",
    "sei", "sette", "otto", "nove",
]

_TEEN = [
    "dieci", "undici", "dodici", "tredici", "quattordici",
    "quindici", "sedici", "diciassette", "diciotto", "diciannove",
]

_DECINE = {
    2: "venti", 3: "trenta", 4: "quaranta", 5: "cinquanta",
    6: "sessanta", 7: "settanta", 8: "ottanta", 9: "novanta",
}


def _under_hundred(n: int) -> str:
    """Unaccented spelling. Accent on 'tre' is applied at the top level."""
    if n < 10:
        return _UNITA[n]
    if n < 20:
        return _TEEN[n - 10]
    tens, units = divmod(n, 10)
    base = _DECINE[tens]
    if units == 0:
        return base
    # elision: ventuno, ventotto; trentuno, trentotto
    if units in (1, 8):
        base = base[:-1]
    return base + _UNITA[units]


def _under_thousand(n: int) -> str:
    if n < 100:
        return _under_hundred(n)
    hundreds, rest = divmod(n, 100)
    if hundreds == 1:
        head = "cento"
    else:
        head = _UNITA[hundreds] + "cento"
    if rest == 0:
        return head
    return head + _under_hundred(rest)


def num_to_words_it(n: int) -> str:
    """Lowercase Italian spelling for 0..999_999. Raises ValueError otherwise."""
    if n < 0 or n >= 1_000_000:
        raise ValueError("num_to_words_it supports 0..999_999")
    if n < 1000:
        result = _under_thousand(n)
    else:
        thousands, rest = divmod(n, 1000)
        if thousands == 1:
            head = "mille"
        else:
            head = _under_thousand(thousands) + "mila"
        result = head if rest == 0 else head + _under_thousand(rest)
    # Final-syllable accent: ventitré, trentatré, milletré, etc.
    # Standalone "tre" (n == 3) stays unaccented.
    if len(result) > 3 and result.endswith("tre"):
        result = result[:-3] + "tré"
    return result
