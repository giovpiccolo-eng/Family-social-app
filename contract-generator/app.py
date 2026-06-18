"""
Flask app — Contract Generator.

Routes:
    GET  /                      questionnaire UI (templates/index.html)
    POST /generate              JSON payload -> generates DOCX (+ PDF if soffice
                                is available) and returns download URLs.
    GET  /download/<filename>   serve a generated file
    GET  /result                download page with name / RAL / download buttons
"""

from __future__ import annotations

import shutil
from pathlib import Path

from flask import (
    Flask,
    abort,
    jsonify,
    render_template,
    request,
    send_file,
    url_for,
)

from generator import OUTPUT_DIR, build_contract, convert_to_pdf


app = Flask(__name__)


@app.get("/")
def index():
    return render_template("index.html")


@app.post("/generate")
def generate():
    payload = request.get_json(force=True, silent=False)
    if not payload:
        return jsonify({"error": "Empty payload"}), 400

    try:
        docx_path = build_contract(payload, OUTPUT_DIR)
    except KeyError as e:
        return jsonify({"error": f"Missing field: {e}"}), 400
    except Exception as e:  # noqa: BLE001
        return jsonify({"error": str(e)}), 500

    response = {
        "docx_url": url_for("download", filename=docx_path.name),
        "docx_name": docx_path.name,
    }

    if shutil.which("soffice") or shutil.which("libreoffice"):
        try:
            pdf_path = convert_to_pdf(docx_path, OUTPUT_DIR)
            response["pdf_url"] = url_for("download", filename=pdf_path.name)
            response["pdf_name"] = pdf_path.name
        except Exception as e:  # noqa: BLE001
            response["pdf_error"] = f"LibreOffice conversion failed: {e}"

    return jsonify(response)


@app.get("/download/<path:filename>")
def download(filename: str):
    # Restrict to OUTPUT_DIR; reject path-traversal attempts.
    target = (OUTPUT_DIR / filename).resolve()
    output_root = OUTPUT_DIR.resolve()
    try:
        target.relative_to(output_root)
    except ValueError:
        abort(403)
    if not target.is_file():
        abort(404)
    return send_file(target, as_attachment=True, download_name=target.name)


@app.get("/result")
def result_page():
    return render_template(
        "result.html",
        nome=request.args.get("nome", ""),
        docx=request.args.get("docx", ""),
        pdf=request.args.get("pdf", ""),
        ral=request.args.get("ral", ""),
    )


if __name__ == "__main__":
    app.run(debug=True)
