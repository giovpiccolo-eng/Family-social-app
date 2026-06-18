"""
Flask app — Contract Generator (auth + persistence).

Routes:
    GET  /                       wizard (login required)
    POST /generate               create contract, save record, return URLs
    GET  /contracts              list contracts created by the current user
    GET  /contracts/<id>         download links for a saved contract
    GET  /download/<filename>    serve local file (dev mode)
    GET  /result                 post-generation download page
    GET  /healthz                config snapshot (no auth)
    GET  /login                  start OAuth
    GET  /auth/callback          OAuth return
    GET  /logout                 clear session
"""

from __future__ import annotations

import shutil
from datetime import datetime, timezone

from flask import (
    Flask,
    abort,
    jsonify,
    redirect,
    render_template,
    request,
    send_file,
    session,
    url_for,
)

import config
from auth import bp as auth_bp, current_user, init_auth, login_required
from generator import OUTPUT_DIR, build_contract, convert_to_pdf
from storage_backend import get_backend


def create_app() -> Flask:
    app = Flask(__name__)
    app.config["SECRET_KEY"] = config.SECRET_KEY
    app.config["SESSION_COOKIE_SECURE"] = config.SESSION_COOKIE_SECURE
    app.config["SESSION_COOKIE_SAMESITE"] = config.SESSION_COOKIE_SAMESITE
    app.config["MAX_CONTENT_LENGTH"] = 1 * 1024 * 1024  # 1 MB JSON cap

    init_auth(app)
    app.register_blueprint(auth_bp)

    # Make the current user available in every template.
    @app.context_processor
    def inject_user():
        return {"user": current_user(), "config_summary": config.summary()}

    # -- Wizard ---------------------------------------------------------
    @app.get("/")
    @login_required
    def index():
        return render_template("index.html")

    # -- Generate -------------------------------------------------------
    @app.post("/generate")
    @login_required
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

        pdf_path = None
        pdf_error = None
        if shutil.which("soffice") or shutil.which("libreoffice"):
            try:
                pdf_path = convert_to_pdf(docx_path, OUTPUT_DIR)
            except Exception as e:  # noqa: BLE001
                pdf_error = f"LibreOffice conversion failed: {e}"

        # Persist to storage backend
        backend = get_backend()
        docx_key = backend.save_file(docx_path, docx_path.name)
        pdf_key = backend.save_file(pdf_path, pdf_path.name) if pdf_path else None

        # Save record
        user = current_user() or {"email": "unknown"}
        record = {
            "created_by": user["email"].lower(),
            "created_by_name": user.get("name", ""),
            "created_at": datetime.now(timezone.utc).isoformat(),
            "nome": payload.get("nome", ""),
            "sede": payload.get("sede", ""),
            "livello": payload.get("livello", ""),
            "mansione": payload.get("mansione", ""),
            "tipo_contratto": payload.get("tipo_contratto", ""),
            "durata": payload.get("durata", ""),
            "data_inizio": payload.get("data_inizio", ""),
            "data_fine": payload.get("data_fine", ""),
            "docx_key": docx_key,
            "pdf_key": pdf_key,
            "payload": payload,
        }
        doc_id = backend.save_record(record)

        response = {
            "id": doc_id,
            "docx_url": backend.signed_url(docx_key),
            "docx_name": docx_path.name,
        }
        if pdf_path:
            response["pdf_url"] = backend.signed_url(pdf_key)
            response["pdf_name"] = pdf_path.name
        if pdf_error:
            response["pdf_error"] = pdf_error

        return jsonify(response)

    # -- Contract history ----------------------------------------------
    @app.get("/contracts")
    @login_required
    def contracts_list():
        user = current_user()
        records = get_backend().list_records(user["email"])
        return render_template("contracts.html", records=records)

    @app.get("/contracts/<doc_id>")
    @login_required
    def contracts_detail(doc_id: str):
        user = current_user()
        backend = get_backend()
        record = backend.get_record(doc_id, user["email"])
        if not record:
            abort(404)
        docx_url = backend.signed_url(record["docx_key"]) if record.get("docx_key") else None
        pdf_url = backend.signed_url(record["pdf_key"]) if record.get("pdf_key") else None
        return render_template(
            "contract_detail.html",
            record=record,
            docx_url=docx_url,
            pdf_url=pdf_url,
        )

    # -- Local file download (dev mode only) ---------------------------
    @app.get("/download/<path:filename>")
    @login_required
    def download(filename: str):
        target = (OUTPUT_DIR / filename).resolve()
        root = OUTPUT_DIR.resolve()
        try:
            target.relative_to(root)
        except ValueError:
            abort(403)
        if not target.is_file():
            abort(404)
        return send_file(target, as_attachment=True, download_name=target.name)

    # -- Result page (post-generation) ----------------------------------
    @app.get("/result")
    @login_required
    def result_page():
        return render_template(
            "result.html",
            nome=request.args.get("nome", ""),
            docx=request.args.get("docx", ""),
            pdf=request.args.get("pdf", ""),
            ral=request.args.get("ral", ""),
        )

    # -- Health check ---------------------------------------------------
    @app.get("/healthz")
    def healthz():
        return jsonify({"status": "ok", **config.summary()})

    return app


app = create_app()


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=int(__import__("os").environ.get("PORT", 8080)),
            debug=config.IS_DEV)
