"""
End-to-end integration tests — Prompt 5.

Exercises the Flask app via its test client for three scenarios:
  A) Alba Gulino    — Acorn, IV livello FT determinato + AFAC 206 + 4h prol
                      + doposcuola 65h + camp 7w, no patto
  B) Anne-Lise Tropato — Acorn, VI livello PT 12/18 determinato + AFAC 333,50,
                         no voci aggiuntive
  C) Rinnovo 10+10 mesi — verify warning does NOT fire (20 < 24), and that
                          motivazione rinnovo paragraph appears in the DOCX

For each scenario:
  - POST payload to /generate
  - Verify response shape (docx_url + name, optional pdf_url)
  - Download the DOCX through /download/<filename> and verify expected
    field values appear in the document text.
"""

import re
import shutil
import unittest
import zipfile
from datetime import date
from decimal import Decimal
from pathlib import Path

from app import app
from generator import (
    MENSILITA_ANNUE,
    calc_doposcuola,
    calc_prolungamento_annuale,
    calc_ral,
    calc_school_camp,
    check_24months,
    get_tabellare,
    proportion_parttime,
)
from helpers import format_currency_it


OUTPUT_DIR = Path(__file__).resolve().parent / "output"


def _extract_text(docx_path: Path) -> str:
    with zipfile.ZipFile(docx_path) as z:
        xml = z.read("word/document.xml").decode("utf-8")
    no_tags = re.sub(r"<[^>]+>", " ", xml)
    return re.sub(r"\s+", " ", no_tags).strip()


# ---------------------------------------------------------------------------
# Scenario builders
# ---------------------------------------------------------------------------

def payload_alba_gulino() -> dict:
    """A — Acorn, IV livello, FT determinato 26/08/2026-30/06/2027.
    PO uses new Ingenium worksheet formula. Indennità is annual."""
    start = date(2026, 8, 26)
    tab = get_tabellare("IV", start)                       # 1491.38
    afac = Decimal("206.00")
    prol_annuale = calc_prolungamento_annuale(tab, afac, "IV", 4)  # 1441.04
    dopo = calc_doposcuola(65)                              # 2600.00
    camp = calc_school_camp(7)                              # 2660.00
    return {
        "nome": "Alba Gulino",
        "sesso": "F",
        "data_nascita": "1985-04-12",
        "codice_fiscale": "GLNLBA85D52H501Z",
        "tipo_contratto": "tempo_pieno",
        "durata": "determinato",
        "sede": "Acorn",
        "livello": "IV",
        "mansione": "Atelierista",
        "data_inizio": start.isoformat(),
        "data_fine": "2027-06-30",
        "ore_settimanali": 34,
        "tabellare": str(tab),
        "afac": str(afac),
        "indennita_funzione": "0",                         # annual
        "prolungamento": {
            "attivo": True, "ore": 4,
            "importo_annuale": str(prol_annuale),
        },
        "doposcuola": {"ore": 65, "ambito": " nell'ambito Atelier",
                       "compenso": str(dopo)},
        "camp": {"settimane": 7, "compenso": str(camp)},
        "fringe": None,
        "patto": None,
        "rinnovo": None,
    }


def payload_anne_lise_tropato() -> dict:
    """B — Acorn, VI livello PT 12/18 determinato 26/08/2026-30/06/2027.

    Per brief §3 Step 6, AFAC is entered as full-time and the backend
    proportions it. Same for tabellare.
    """
    start = date(2026, 8, 26)
    tab_ft = get_tabellare("VI", start)                    # 1589.64
    return {
        "nome": "Anne-Lise Tropato",
        "sesso": "F",
        "data_nascita": None,
        "codice_fiscale": None,
        "tipo_contratto": "tempo_parziale",
        "durata": "determinato",
        "sede": "Acorn",
        "livello": "VI",
        "mansione": "Docente Scuola Secondaria",
        "data_inizio": start.isoformat(),
        "data_fine": "2027-06-30",
        "ore_settimanali": 12,
        "tabellare": str(tab_ft),    # FT — backend proportions to PT 12/18
        "afac": "333.50",            # FT — backend proportions to 222.33
        "indennita_funzione": "0",
        "prolungamento": None,
        "doposcuola": None,
        "camp": None,
        "fringe": None,
        "patto": None,
        "rinnovo": None,
    }


def payload_rinnovo() -> dict:
    """C — VI livello FT determinato, 10 mesi precedenti + 10 mesi rinnovo."""
    start = date(2026, 9, 1)
    tab = get_tabellare("VI", start)                       # 1589.64
    return {
        "nome": "Giorgia Rossi",
        "sesso": "F",
        "data_nascita": None,
        "codice_fiscale": None,
        "tipo_contratto": "tempo_pieno",
        "durata": "determinato",
        "sede": "Acorn",
        "livello": "VI",
        "mansione": "Docente Scuola Secondaria",
        "data_inizio": start.isoformat(),
        "data_fine": "2027-06-30",
        "ore_settimanali": 18,
        "tabellare": str(tab),
        "afac": "0",
        "indennita_funzione": "0",
        "prolungamento": None,
        "doposcuola": None,
        "camp": None,
        "fringe": None,
        "patto": None,
        "rinnovo": {
            "mesi_precedenti": 10,
            "durata_mesi": 10,
        },
    }


# ---------------------------------------------------------------------------
# Helper: hit /generate, follow /download, return DOCX path + text
# ---------------------------------------------------------------------------

class _ScenarioBase(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.client = app.test_client()
        # Clean previous outputs for determinism
        for f in OUTPUT_DIR.glob("contratto_*"):
            try: f.unlink()
            except OSError: pass

    def _generate(self, payload: dict) -> tuple[dict, Path, str]:
        res = self.client.post("/generate", json=payload)
        self.assertEqual(res.status_code, 200, f"Server error: {res.data!r}")
        data = res.get_json()
        self.assertIn("docx_url", data)
        self.assertIn("docx_name", data)

        # Download via /download/<file>
        dl = self.client.get(data["docx_url"])
        self.assertEqual(dl.status_code, 200)
        self.assertTrue(dl.data.startswith(b"PK"))   # ZIP magic — DOCX is a zip

        docx_path = OUTPUT_DIR / data["docx_name"]
        self.assertTrue(docx_path.is_file())
        return data, docx_path, _extract_text(docx_path)


# ---------------------------------------------------------------------------
# Scenario A — Alba Gulino
# ---------------------------------------------------------------------------

class TestScenarioA(_ScenarioBase):

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        cls.payload = payload_alba_gulino()
        cls.data, cls.docx_path, cls.text = cls()._generate(cls.payload)

    def test_recipient_and_contract_type(self):
        self.assertIn("Gent.ma Sig.ra Alba Gulino", self.text)
        self.assertIn("CF: GLNLBA85D52H501Z", self.text)
        self.assertIn(
            "Oggetto: Proposta di assunzione a tempo pieno e determinato",
            self.text,
        )

    def test_dates(self):
        self.assertIn("a decorrere dal 26/08/2026", self.text)
        self.assertIn("fino al 30/06/2027", self.text)

    def test_inquadramento_iv_atelierista(self):
        self.assertIn("mansione di Atelierista", self.text)
        self.assertIn("livello IV", self.text)
        self.assertIn("Area Seconda (DOC)", self.text)

    def test_sede_acorn(self):
        self.assertIn("Via della Giustiniana n. 1200, Roma (RM)", self.text)

    def test_periodo_prova_determinato_1_mese(self):
        self.assertIn("periodo di prova della durata di 1 (un) mese", self.text)

    def test_orario_and_prolungamento_4h(self):
        self.assertIn("34 ore settimanali", self.text)
        self.assertIn("art. 35", self.text)
        self.assertIn("4 (quattro) ore settimanali", self.text)

    def test_retribuzione_table(self):
        # Tabellare 1.491,38 + AFAC 206,00 -> Totale 1.697,38
        self.assertIn("1.491,38", self.text)
        self.assertIn("206,00", self.text)
        self.assertIn("1.697,38", self.text)
        self.assertIn("Minimo Conglobato", self.text)
        self.assertIn("TOTALE", self.text)

    def test_doposcuola_65h_2600(self):
        self.assertIn("65 ore pomeridiane", self.text)
        self.assertIn("nell'ambito Atelier", self.text)
        self.assertIn("2.600,00", self.text)

    def test_school_camp_7w_2660(self):
        self.assertIn("7 (sette) settimane di School Camp", self.text)
        self.assertIn("2.660,00", self.text)

    def test_afac_assorbimento_clause(self):
        self.assertIn("Anticipo Futuri Aumenti Contrattuali", self.text)

    def test_no_patto_section(self):
        self.assertNotIn("Patto di durata minima:", self.text)

    def test_ral_value_appears(self):
        # New PO formula (Ingenium worksheet) — RAL changes vs old brief value
        tab = get_tabellare("IV", date(2026, 8, 26))
        from decimal import Decimal
        prol_annuale = calc_prolungamento_annuale(tab, Decimal("206.00"), "IV", 4)
        ral = calc_ral(
            tab, Decimal("206.00"),
            prolungamento_annuale=prol_annuale,
            doposcuola=Decimal("2600.00"),
            camp=Decimal("2660.00"),
        )
        self.assertIn(format_currency_it(ral), self.text)

    def test_anno_scolastico_2026_2027(self):
        self.assertIn("anno scolastico 2026/2027", self.text)


# ---------------------------------------------------------------------------
# Scenario B — Anne-Lise Tropato
# ---------------------------------------------------------------------------

class TestScenarioB(_ScenarioBase):

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        cls.payload = payload_anne_lise_tropato()
        cls.data, cls.docx_path, cls.text = cls()._generate(cls.payload)

    def test_recipient(self):
        self.assertIn("Gent.ma Sig.ra Anne-Lise Tropato", self.text)

    def test_part_time(self):
        self.assertIn(
            "Oggetto: Proposta di assunzione a tempo part time e determinato",
            self.text,
        )
        self.assertIn("12 ore settimanali", self.text)

    def test_tabellare_pt_12_18(self):
        # VI livello 2026 FT = 1589,64 -> PT 12/18 = 1.059,76
        self.assertIn("1.059,76", self.text)
        # Table label includes the part-time ratio
        self.assertIn("12/18", self.text)

    def test_afac_pt_12_18(self):
        # AFAC FT 333,50 -> PT 12/18 = 222,33
        self.assertIn("222,33", self.text)

    def test_totale_mensile_1282_09(self):
        # Totale = tabellare PT + AFAC PT = 1059.76 + 222.33 = 1282.09
        self.assertIn("1.282,09", self.text)

    def test_periodo_prova_1_mese(self):
        self.assertIn("periodo di prova della durata di 1 (un) mese", self.text)

    def test_no_voci_aggiuntive(self):
        self.assertNotIn("School Camp", self.text)
        self.assertNotIn("doposcuola", self.text)
        self.assertNotIn("Patto di durata minima:", self.text)
        # No prolungamento clause either
        self.assertNotIn("prolungamento del proprio orario", self.text)

    def test_ral_excludes_voci(self):
        # RAL = (1059.76 + 222.33) * 13 = 16667.17
        from decimal import Decimal
        ral = calc_ral(Decimal("1059.76"), Decimal("222.33"))
        self.assertEqual(ral, Decimal("16667.17"))
        self.assertIn(format_currency_it(ral), self.text)


# ---------------------------------------------------------------------------
# Scenario C — Rinnovo 10 + 10 mesi
# ---------------------------------------------------------------------------

class TestScenarioCRinnovo(_ScenarioBase):

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        cls.payload = payload_rinnovo()
        cls.data, cls.docx_path, cls.text = cls()._generate(cls.payload)

    def test_24month_warning_does_not_fire(self):
        """Critical assertion: 10 + 10 = 20 < 24 -> no warning."""
        result = check_24months(10, 10)
        self.assertEqual(result["totale"], 20)
        self.assertFalse(result["warning"])

    def test_motivazione_rinnovo_present(self):
        # Distinctive snippets from §6.6
        self.assertIn("rinnovato in considerazione", self.text)
        self.assertIn("Middle Years Programme", self.text)
        self.assertIn("non è in possesso dei requisiti abilitativi", self.text)
        self.assertIn("ulteriore periodo di 10 mesi", self.text)
        self.assertIn("art. 8 Parte Seconda CCNL ANINSEI", self.text)

    def test_motivazione_rinnovo_appears_before_periodo_prova(self):
        idx_rinnovo = self.text.find("rinnovato in considerazione")
        idx_prova = self.text.find("Periodo di prova:")
        self.assertGreater(idx_rinnovo, 0)
        self.assertGreater(idx_prova, idx_rinnovo)

    def test_warning_does_fire_at_24(self):
        """Sanity-check the boundary: 12 + 12 -> warning."""
        result = check_24months(12, 12)
        self.assertTrue(result["warning"])


# ---------------------------------------------------------------------------
# Optional PDF coverage for the three scenarios (skipped if soffice
# can't open files in this environment)
# ---------------------------------------------------------------------------

class TestPdfThreeScenarios(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        import subprocess
        if not (shutil.which("soffice") or shutil.which("libreoffice")):
            raise unittest.SkipTest("LibreOffice not installed")
        # Probe whether soffice can actually convert anything in this env.
        probe = Path("/tmp/_soffice_probe.txt")
        probe.write_text("probe")
        r = subprocess.run(
            ["soffice", "--headless", "--convert-to", "pdf",
             "--outdir", "/tmp", str(probe)],
            capture_output=True, text=True,
        )
        probe_pdf = Path("/tmp/_soffice_probe.pdf")
        probe.unlink(missing_ok=True)
        if r.returncode != 0 or not probe_pdf.exists():
            raise unittest.SkipTest("LibreOffice headless not functional")
        probe_pdf.unlink(missing_ok=True)
        cls.client = app.test_client()

    def _run(self, payload):
        res = self.client.post("/generate", json=payload)
        self.assertEqual(res.status_code, 200)
        data = res.get_json()
        self.assertIn("pdf_url", data, f"PDF not generated: {data}")
        dl = self.client.get(data["pdf_url"])
        self.assertEqual(dl.status_code, 200)
        self.assertTrue(dl.data.startswith(b"%PDF-"))

    def test_pdf_scenario_a(self): self._run(payload_alba_gulino())
    def test_pdf_scenario_b(self): self._run(payload_anne_lise_tropato())
    def test_pdf_scenario_c(self): self._run(payload_rinnovo())


if __name__ == "__main__":
    unittest.main(verbosity=2)
