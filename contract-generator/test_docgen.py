"""
Prompt 3 sample-payload test: build a DOCX (+ optional PDF) for an Acorn
IV livello full-time determinato contract with AFAC €206 and 4 hours
prolungamento orario; verify document structure and key values.
"""

import shutil
import unittest
import zipfile
from datetime import date
from decimal import Decimal
from pathlib import Path

from generator import (
    build_contract,
    calc_doposcuola,
    calc_prolungamento_mensile,
    calc_ral,
    calc_school_camp,
    convert_to_pdf,
    get_tabellare,
    MENSILITA_ANNUE,
)


OUTPUT_DIR = Path(__file__).resolve().parent / "output"


def _sample_payload_iv() -> dict:
    """
    Acorn IV livello, full-time determinato 26/08/2026 - 30/06/2027.
    AFAC €206, 4h prolungamento, doposcuola 65h, camp 7w (Test Case A).
    """
    start = date(2026, 8, 26)
    end = date(2027, 6, 30)
    tabellare = get_tabellare("IV", start)             # 1491.38
    prol_mensile = calc_prolungamento_mensile(tabellare, 4)
    prol_annuale = prol_mensile * MENSILITA_ANNUE
    dopo = calc_doposcuola(65)                          # 2600.00
    camp = calc_school_camp(7)                          # 2660.00

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
        "data_fine": end.isoformat(),
        "ore_settimanali": 34,
        "tabellare": str(tabellare),
        "afac": "206.00",
        "indennita_funzione": "0",
        "prolungamento": {
            "attivo": True,
            "ore": 4,
            "importo_mensile": str(prol_mensile),
            "importo_annuale": str(prol_annuale),
        },
        "doposcuola": {"ore": 65, "ambito": " nell'ambito Atelier",
                       "compenso": str(dopo)},
        "camp": {"settimane": 7, "compenso": str(camp)},
        "fringe": None,
        "patto": None,
        "rinnovo": None,
    }


def _docx_text(path: Path) -> str:
    """Extract all paragraph text from a DOCX for assertions."""
    with zipfile.ZipFile(path) as z:
        xml = z.read("word/document.xml").decode("utf-8")
    # crude text extraction: strip XML tags
    import re
    no_tags = re.sub(r"<[^>]+>", " ", xml)
    return re.sub(r"\s+", " ", no_tags).strip()


class TestBuildContractCaseA(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        # Clean previous output for a deterministic test
        if OUTPUT_DIR.exists():
            for f in OUTPUT_DIR.glob("contratto_*"):
                f.unlink()
        cls.payload = _sample_payload_iv()
        cls.docx_path = build_contract(cls.payload, OUTPUT_DIR)
        cls.text = _docx_text(cls.docx_path)

    def test_docx_file_created(self):
        self.assertTrue(self.docx_path.exists())
        self.assertGreater(self.docx_path.stat().st_size, 5_000)

    def test_recipient_block_present(self):
        self.assertIn("Gent.ma Sig.ra Alba Gulino", self.text)
        self.assertIn("Nata il 12/04/1985", self.text)
        self.assertIn("CF: GLNLBA85D52H501Z", self.text)

    def test_oggetto_line(self):
        self.assertIn(
            "Oggetto: Proposta di assunzione a tempo pieno e determinato",
            self.text,
        )

    def test_intese_decorrenza_includes_both_dates(self):
        self.assertIn("a decorrere dal 26/08/2026", self.text)
        self.assertIn("fino al 30/06/2027", self.text)

    def test_inquadramento(self):
        self.assertIn("mansione di Atelierista", self.text)
        self.assertIn("livello IV", self.text)
        self.assertIn("Area Seconda (DOC)", self.text)
        self.assertIn("ANINSEI", self.text)

    def test_sede_acorn(self):
        self.assertIn("Via della Giustiniana n. 1200, Roma (RM)", self.text)

    def test_periodo_prova_determinato(self):
        self.assertIn("periodo di prova della durata di 1 (un) mese", self.text)

    def test_orario_and_prolungamento(self):
        self.assertIn("34 ore settimanali", self.text)
        self.assertIn("art. 35", self.text)
        self.assertIn("4 (quattro) ore settimanali", self.text)

    def test_retribuzione_table_values(self):
        # Totale mensile = 1491.38 + 206.00 = 1697.38 → "1.697,38"
        self.assertIn("1.697,38", self.text)
        self.assertIn("Minimo Conglobato", self.text)
        self.assertIn("AFAC", self.text)
        self.assertIn("TOTALE", self.text)

    def test_doposcuola_block(self):
        self.assertIn("65 ore pomeridiane", self.text)
        self.assertIn("nell'ambito Atelier", self.text)
        self.assertIn("2.600,00", self.text)

    def test_school_camp_block(self):
        self.assertIn("7 (sette) settimane di School Camp", self.text)
        self.assertIn("2.660,00", self.text)

    def test_afac_clause(self):
        self.assertIn("Anticipo Futuri Aumenti Contrattuali", self.text)
        self.assertIn("206,00", self.text)

    def test_ral_summary_includes_anno_scolastico(self):
        self.assertIn("anno scolastico 2026/2027", self.text)
        # voci aggiuntive enumerated
        self.assertIn("prolungamento orario", self.text)
        self.assertIn("School Camp", self.text)
        self.assertIn("doposcuola", self.text)

    def test_ral_computed_value_appears(self):
        """RAL = (1491.38+206)*13 + 12*prol_mensile + 2600 + 2660."""
        tabellare = Decimal("1491.38")
        prol_mensile = calc_prolungamento_mensile(tabellare, 4)
        ral = calc_ral(
            tabellare, Decimal("206.00"),
            prolungamento_annuale=prol_mensile * MENSILITA_ANNUE,
            doposcuola=Decimal("2600.00"),
            camp=Decimal("2660.00"),
        )
        from helpers import format_currency_it
        self.assertIn(format_currency_it(ral), self.text)

    def test_modalita_pagamento(self):
        self.assertIn(
            "bonifico bancario entro il giorno 10",
            self.text,
        )

    def test_disciplina_and_dlgs_152(self):
        self.assertIn("codice di disciplina", self.text)
        self.assertIn("D.lgs. n. 152/1997", self.text)

    def test_chiusura_and_signature(self):
        # F payload -> "Pregandola"; lavoratrice (not "lavoratorice")
        self.assertIn("Pregandola di restituirci", self.text)
        self.assertNotIn("Pregandala", self.text)
        self.assertIn("lavoratrice", self.text)
        self.assertNotIn("lavoratorice", self.text)
        self.assertIn("Timbro e Firma", self.text)
        self.assertIn("Per accettazione", self.text)


class TestPdfConversion(unittest.TestCase):
    """Optional — runs only if a working LibreOffice headless is on PATH."""

    @classmethod
    def setUpClass(cls):
        import subprocess
        if not (shutil.which("soffice") or shutil.which("libreoffice")):
            raise unittest.SkipTest("LibreOffice not installed")
        # Probe: can soffice actually convert anything in this environment?
        probe = Path("/tmp/_soffice_probe.txt")
        probe.write_text("probe")
        result = subprocess.run(
            ["soffice", "--headless", "--convert-to", "pdf",
             "--outdir", "/tmp", str(probe)],
            capture_output=True, text=True,
        )
        probe_pdf = Path("/tmp/_soffice_probe.pdf")
        probe.unlink(missing_ok=True)
        if result.returncode != 0 or not probe_pdf.exists():
            raise unittest.SkipTest(
                "LibreOffice headless cannot convert in this environment "
                f"(stderr: {result.stderr.strip()[:160]})"
            )
        probe_pdf.unlink(missing_ok=True)
        cls.payload = _sample_payload_iv()
        cls.docx_path = build_contract(cls.payload, OUTPUT_DIR)

    def test_pdf_generated(self):
        pdf_path = convert_to_pdf(self.docx_path, OUTPUT_DIR)
        self.assertTrue(pdf_path.exists())
        with open(pdf_path, "rb") as f:
            self.assertEqual(f.read(5), b"%PDF-")
        self.assertGreater(pdf_path.stat().st_size, 5_000)


if __name__ == "__main__":
    unittest.main(verbosity=2)
