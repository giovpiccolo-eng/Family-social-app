FROM python:3.11-slim

# LibreOffice for DOCX -> PDF conversion (headless).
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libreoffice \
        libreoffice-writer \
        fonts-liberation \
        fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

# App
WORKDIR /app
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

# Cloud Run sets PORT to 8080
ENV PORT=8080 \
    PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    # Use /tmp for soffice profile (Cloud Run filesystem is read-only outside /tmp)
    HOME=/tmp

EXPOSE 8080

# 1 worker, 4 threads — DOCX/PDF generation is I/O- and process-bound,
# Cloud Run scales horizontally.
CMD ["gunicorn", "--bind", "0.0.0.0:8080", "--workers", "1", "--threads", "4",
     "--timeout", "120", "app:app"]
