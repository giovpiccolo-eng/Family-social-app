"""
Storage abstraction — file persistence + contract metadata.

Two backends, picked by config.IS_PROD:

  - LocalBackend   (dev): files in OUTPUT_DIR, metadata in a JSON file.
  - CloudBackend   (prod): Google Cloud Storage for files, Firestore for
                           metadata.

API surface (used by app.py):

    backend.save_file(local_path, dest_name) -> public_url
    backend.signed_url(stored_path)          -> download URL
    backend.save_record(record)              -> doc_id
    backend.list_records(user_email)         -> [record, ...]
    backend.get_record(doc_id, user_email)   -> record | None
"""

from __future__ import annotations

import json
import threading
import uuid
from datetime import datetime, timedelta, timezone
from pathlib import Path

from flask import url_for

import config


# --------------------------------------------------------------------------
# Dev/local backend — filesystem + JSON file
# --------------------------------------------------------------------------

class LocalBackend:
    """Stores files in OUTPUT_DIR and contract records in a JSON file."""

    def __init__(self) -> None:
        self.lock = threading.Lock()
        config.OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
        if not config.LOCAL_DB_PATH.exists():
            config.LOCAL_DB_PATH.write_text("[]", encoding="utf-8")

    # ---- file storage ----
    def save_file(self, local_path: Path, dest_name: str) -> str:
        """File is already in OUTPUT_DIR (build_contract wrote it there).
        Returns the relative storage key used by signed_url()."""
        # In local mode the file is its own storage key.
        return dest_name

    def signed_url(self, stored_path: str) -> str:
        return url_for("download", filename=stored_path)

    # ---- metadata ----
    def _load(self) -> list[dict]:
        try:
            return json.loads(config.LOCAL_DB_PATH.read_text("utf-8"))
        except (json.JSONDecodeError, FileNotFoundError):
            return []

    def _save(self, records: list[dict]) -> None:
        config.LOCAL_DB_PATH.write_text(
            json.dumps(records, ensure_ascii=False, indent=2, default=str),
            encoding="utf-8",
        )

    def save_record(self, record: dict) -> str:
        with self.lock:
            records = self._load()
            doc_id = record.get("id") or uuid.uuid4().hex
            record["id"] = doc_id
            # most-recent first; replace if id already exists
            records = [r for r in records if r.get("id") != doc_id]
            records.insert(0, record)
            self._save(records)
        return doc_id

    def list_records(self, user_email: str) -> list[dict]:
        return [
            r for r in self._load()
            if r.get("created_by", "").lower() == user_email.lower()
        ]

    def get_record(self, doc_id: str, user_email: str) -> dict | None:
        for r in self._load():
            if r.get("id") == doc_id and r.get("created_by", "").lower() == user_email.lower():
                return r
        return None


# --------------------------------------------------------------------------
# Cloud backend — GCS for files, Firestore for metadata
# --------------------------------------------------------------------------

class CloudBackend:
    """GCS + Firestore. Requires google-cloud-storage and google-cloud-firestore."""

    def __init__(self) -> None:
        from google.cloud import firestore, storage
        if not config.FIREBASE_PROJECT_ID:
            raise RuntimeError("FIREBASE_PROJECT_ID is required in prod mode.")
        self.fs = firestore.Client(project=config.FIREBASE_PROJECT_ID)
        self.collection = config.FIRESTORE_COLLECTION
        if not config.GCS_BUCKET:
            raise RuntimeError("GCS_BUCKET is required in prod mode.")
        self.storage_client = storage.Client(project=config.FIREBASE_PROJECT_ID)
        self.bucket = self.storage_client.bucket(config.GCS_BUCKET)

    # ---- file storage ----
    def save_file(self, local_path: Path, dest_name: str) -> str:
        """Upload local file to GCS. Returns the GCS object name."""
        # Namespaced by year-month so the bucket browser stays usable.
        prefix = datetime.now(timezone.utc).strftime("%Y/%m")
        blob_name = f"{prefix}/{uuid.uuid4().hex[:8]}_{dest_name}"
        blob = self.bucket.blob(blob_name)
        blob.upload_from_filename(str(local_path))
        return blob_name

    def signed_url(self, stored_path: str) -> str:
        """Return a 30-minute signed URL — survives the request lifecycle."""
        blob = self.bucket.blob(stored_path)
        return blob.generate_signed_url(
            version="v4",
            expiration=timedelta(minutes=30),
            method="GET",
        )

    # ---- metadata ----
    def save_record(self, record: dict) -> str:
        doc_id = record.get("id") or uuid.uuid4().hex
        record["id"] = doc_id
        self.fs.collection(self.collection).document(doc_id).set(record)
        return doc_id

    def list_records(self, user_email: str) -> list[dict]:
        from google.cloud import firestore
        q = (self.fs.collection(self.collection)
             .where(filter=firestore.FieldFilter("created_by", "==", user_email.lower()))
             .order_by("created_at", direction=firestore.Query.DESCENDING)
             .limit(200))
        return [d.to_dict() for d in q.stream()]

    def get_record(self, doc_id: str, user_email: str) -> dict | None:
        doc = self.fs.collection(self.collection).document(doc_id).get()
        if not doc.exists:
            return None
        data = doc.to_dict()
        if data.get("created_by", "").lower() != user_email.lower():
            return None
        return data


# --------------------------------------------------------------------------
# Singleton accessor
# --------------------------------------------------------------------------

_backend: LocalBackend | CloudBackend | None = None


def get_backend():
    global _backend
    if _backend is None:
        _backend = CloudBackend() if config.IS_PROD else LocalBackend()
    return _backend
