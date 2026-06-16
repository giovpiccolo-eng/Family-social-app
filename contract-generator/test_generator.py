"""
Unit tests for generator.py — calculation engine.

Run with either:
    pytest test_generator.py
    python  test_generator.py     (uses unittest as fallback)
"""

import unittest
from datetime import date
from decimal import Decimal

from generator import (
    get_tabellare,
    proportion_parttime,
    calc_prolungamento_mensile,
    calc_doposcuola,
    calc_school_camp,
    calc_ral,
    check_24months,
)
from ccnl_data import PROLUNGAMENTO


# ---------------------------------------------------------------------------
# 5.1 — get_tabellare
# ---------------------------------------------------------------------------

class TestGetTabellare(unittest.TestCase):

    def test_iv_livello_2026_full_time(self):
        """Brief known-good: IV livello, 2026 full-time = € 1.491,38."""
        self.assertEqual(
            get_tabellare("IV", date(2026, 8, 26)),
            Decimal("1491.38"),
        )

    def test_2025_rate_selected_for_dates_before_2026(self):
        self.assertEqual(get_tabellare("IV", date(2025, 9, 1)), Decimal("1444.47"))
        self.assertEqual(get_tabellare("IV", date(2024, 12, 31)), Decimal("1444.47"))

    def test_2027_rate_selected_for_dates_in_2027(self):
        self.assertEqual(get_tabellare("VIII-B", date(2027, 1, 1)), Decimal("1843.62"))
        self.assertEqual(get_tabellare("VIII-B", date(2030, 6, 30)), Decimal("1843.62"))

    def test_boundary_first_of_january_2026(self):
        self.assertEqual(get_tabellare("V", date(2026, 1, 1)), Decimal("1589.64"))


# ---------------------------------------------------------------------------
# 5.2 — proportion_parttime
# ---------------------------------------------------------------------------

class TestProportionParttime(unittest.TestCase):

    def test_vi_livello_2026_parttime_12_18(self):
        """Brief known-good: VI livello 2026 PT 12/18 = € 1.059,76."""
        tab_ft = get_tabellare("VI", date(2026, 8, 26))  # 1589.64
        self.assertEqual(proportion_parttime(tab_ft, 12, 18), Decimal("1059.76"))

    def test_full_time_returns_full_value(self):
        self.assertEqual(
            proportion_parttime(Decimal("1491.38"), 34, 34),
            Decimal("1491.38"),
        )

    def test_afac_parttime_12_18(self):
        # Brief test case B AFAC: 333.50 FT -> 222.33 PT 12/18
        self.assertEqual(
            proportion_parttime(Decimal("333.50"), 12, 18),
            Decimal("222.33"),
        )

    def test_zero_pt_hours_returns_zero(self):
        self.assertEqual(
            proportion_parttime(Decimal("1500.00"), 0, 18),
            Decimal("0.00"),
        )

    def test_invalid_ore_base_raises(self):
        with self.assertRaises(ValueError):
            proportion_parttime(Decimal("1500.00"), 12, 0)


# ---------------------------------------------------------------------------
# 5.3 — calc_prolungamento_mensile
# ---------------------------------------------------------------------------

class TestCalcProlungamentoMensile(unittest.TestCase):

    def test_v_livello_4_ore_formula_identity(self):
        """
        Brief known-good: V livello prolungamento 4 ore =
        quota_h × 0.80 × 4 × 4.333.
        Verifies the function follows that formula exactly (no off-by-one,
        no rounding before the multiplication chain).
        """
        tabellare = get_tabellare("V", date(2026, 8, 26))  # 1589.64
        quota_h = tabellare / Decimal("104")
        expected_raw = quota_h * Decimal("0.80") * Decimal("4") * Decimal("4.333")
        expected = expected_raw.quantize(Decimal("0.01"))
        self.assertEqual(calc_prolungamento_mensile(tabellare, 4), expected)

    def test_v_livello_4_ore_value(self):
        # 1589.64 / 104 = 15.2850000 -> ×0.80 = 12.2280 -> ×4 = 48.9120 -> ×4.333 = 211.9377...
        self.assertEqual(
            calc_prolungamento_mensile(Decimal("1589.64"), 4),
            Decimal("211.94"),
        )

    def test_zero_ore_yields_zero(self):
        self.assertEqual(
            calc_prolungamento_mensile(Decimal("1589.64"), 0),
            Decimal("0.00"),
        )

    def test_max_8_ore_accepted(self):
        # Boundary: 8 ore is the legal max under art. 35.
        result = calc_prolungamento_mensile(Decimal("1589.64"), 8)
        self.assertGreater(result, Decimal("0"))

    def test_over_max_ore_raises(self):
        with self.assertRaises(ValueError):
            calc_prolungamento_mensile(
                Decimal("1589.64"),
                PROLUNGAMENTO["max_ore_settimanali"] + 1,
            )

    def test_negative_ore_raises(self):
        with self.assertRaises(ValueError):
            calc_prolungamento_mensile(Decimal("1589.64"), -1)


# ---------------------------------------------------------------------------
# 5.4 — calc_doposcuola
# ---------------------------------------------------------------------------

class TestCalcDoposcuola(unittest.TestCase):

    def test_65_hours_test_case_a(self):
        """Brief test case A: 65 ore × €40 = €2.600,00."""
        self.assertEqual(calc_doposcuola(65), Decimal("2600.00"))

    def test_zero(self):
        self.assertEqual(calc_doposcuola(0), Decimal("0.00"))

    def test_negative_raises(self):
        with self.assertRaises(ValueError):
            calc_doposcuola(-1)


# ---------------------------------------------------------------------------
# 5.5 — calc_school_camp
# ---------------------------------------------------------------------------

class TestCalcSchoolCamp(unittest.TestCase):

    def test_7_settimane_test_case_a(self):
        """Brief test case A: 7 settimane × €380 = €2.660,00."""
        self.assertEqual(calc_school_camp(7), Decimal("2660.00"))

    def test_zero(self):
        self.assertEqual(calc_school_camp(0), Decimal("0.00"))

    def test_negative_raises(self):
        with self.assertRaises(ValueError):
            calc_school_camp(-1)


# ---------------------------------------------------------------------------
# 5.6 — calc_ral
# ---------------------------------------------------------------------------

class TestCalcRal(unittest.TestCase):

    def test_base_only_thirteen_mensilita(self):
        """RAL with no voci aggiuntive = (tabellare + afac) × 13."""
        ral = calc_ral(Decimal("1491.38"), Decimal("206.00"))
        self.assertEqual(ral, Decimal("22065.94"))  # (1491.38 + 206) × 13

    def test_full_sum_with_all_voci(self):
        ral = calc_ral(
            tabellare=Decimal("1491.38"),
            afac=Decimal("206.00"),
            prolungamento_annuale=Decimal("661.60"),
            indennita_funzione_annuale=Decimal("0"),
            doposcuola=Decimal("2600.00"),
            camp=Decimal("2660.00"),
        )
        # 22065.94 + 661.60 + 2600.00 + 2660.00 = 27987.54
        self.assertEqual(ral, Decimal("27987.54"))

    def test_zero_afac(self):
        # Pure tabellare × 13 (no AFAC).
        self.assertEqual(
            calc_ral(Decimal("1059.76"), Decimal("0")),
            Decimal("13776.88"),
        )


# ---------------------------------------------------------------------------
# 5.7 — check_24months
# ---------------------------------------------------------------------------

class TestCheck24Months(unittest.TestCase):

    def test_test_case_c_rinnovo_10_plus_10_no_warning(self):
        """Brief test case C: 10 + 10 = 20 mesi → no warning."""
        result = check_24months(10, 10)
        self.assertEqual(result, {"totale": 20, "warning": False})

    def test_exactly_24_fires_warning(self):
        # >= 24 per the brief
        self.assertEqual(
            check_24months(12, 12),
            {"totale": 24, "warning": True},
        )

    def test_over_24_fires_warning(self):
        self.assertEqual(
            check_24months(18, 12),
            {"totale": 30, "warning": True},
        )

    def test_zero_zero(self):
        self.assertEqual(
            check_24months(0, 0),
            {"totale": 0, "warning": False},
        )

    def test_negative_raises(self):
        with self.assertRaises(ValueError):
            check_24months(-1, 12)


if __name__ == "__main__":
    unittest.main(verbosity=2)
