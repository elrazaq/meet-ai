import os
import uuid
import shutil
import logging
from typing import Optional
from fastapi import FastAPI, UploadFile, File, Form, BackgroundTasks, HTTPException
from fastapi.responses import FileResponse, JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv

load_dotenv()

from services.gemini_service import GeminiService

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s")
logger = logging.getLogger("MeetAI.Server")

app = FastAPI(
    title="MeetAI Backend Server",
    description="Receiver audio rapat internal dan ekstraksi 4 dokumen terpisah menggunakan Gemini AI Multimodal",
    version="1.0.0"
)

# Aktifkan CORS agar dapat diakses oleh aplikasi Android / Web dashboard
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

STORAGE_DIR = os.getenv("STORAGE_DIR", "./storage")
os.makedirs(STORAGE_DIR, exist_ok=True)

# In-memory status store (dapat diganti database SQLite / PostgreSQL jika diperlukan)
meetings_db = {}

gemini_service = GeminiService()

def process_audio_background(meeting_id: str, audio_path: str, meeting_dir: str):
    try:
        meetings_db[meeting_id]["status"] = "AI_PROCESSING"
        result = gemini_service.process_meeting_audio(audio_path, meeting_dir)
        meetings_db[meeting_id]["status"] = "COMPLETED"
        meetings_db[meeting_id]["result"] = result
        logger.info(f"Meeting {meeting_id} selesai diproses dengan 4 dokumen terstruktur!")
    except Exception as e:
        logger.error(f"Gagal memproses meeting {meeting_id}: {e}", exc_info=True)
        meetings_db[meeting_id]["status"] = "FAILED"
        meetings_db[meeting_id]["error"] = str(e)


@app.get("/health")
def health_check():
    return {"status": "ok", "service": "MeetAI Backend", "model": os.getenv("GEMINI_MODEL", "gemini-2.5-flash")}


@app.post("/api/meetings/upload")
async def upload_meeting_audio(
    background_tasks: BackgroundTasks,
    title: str = Form("Meeting Internal"),
    audio_file: UploadFile = File(...)
):
    """
    Endpoint receiver untuk menerima file audio .m4a dari aplikasi Android MeetAI
    """
    meeting_id = str(uuid.uuid4())
    meeting_dir = os.path.join(STORAGE_DIR, meeting_id)
    os.makedirs(meeting_dir, exist_ok=True)

    # Simpan file audio yang dikirim dari Android
    file_extension = os.path.splitext(audio_file.filename)[1] or ".m4a"
    saved_audio_path = os.path.join(meeting_dir, f"audio{file_extension}")

    with open(saved_audio_path, "wb") as buffer:
        shutil.copyfileobj(audio_file.file, buffer)

    file_size_mb = os.path.getsize(saved_audio_path) / (1024 * 1024)
    logger.info(f"Audio meeting diterima: '{title}', Ukuran: {file_size_mb:.2f} MB, ID: {meeting_id}")

    meetings_db[meeting_id] = {
        "id": meeting_id,
        "title": title,
        "filename": audio_file.filename,
        "file_size_mb": round(file_size_mb, 2),
        "status": "QUEUED",
        "audio_path": saved_audio_path,
        "dir": meeting_dir,
        "result": None
    }

    # Jalankan ekstraksi AI di background agar respons ke Android instan
    background_tasks.add_task(process_audio_background, meeting_id, saved_audio_path, meeting_dir)

    return {
        "meeting_id": meeting_id,
        "title": title,
        "status": "QUEUED",
        "message": "File audio berhasil diterima dan sedang diproses oleh Google Gemini AI."
    }


@app.get("/api/meetings/{meeting_id}")
def get_meeting_status(meeting_id: str):
    """
    Cek status pemrosesan dan ambil 4 dokumen hasil ekstraksi AI
    """
    meeting = meetings_db.get(meeting_id)
    if not meeting:
        raise HTTPException(status_code=404, detail="Meeting ID tidak ditemukan")

    response_data = {
        "id": meeting["id"],
        "title": meeting["title"],
        "status": meeting["status"],
        "file_size_mb": meeting.get("file_size_mb"),
    }

    if meeting["status"] == "COMPLETED" and meeting.get("result"):
        res = meeting["result"]
        response_data.update({
            "detected_title": res.get("meeting_title"),
            "detected_participants": res.get("detected_participants", []),
            "documents": {
                "notulen": res.get("notulen_rapat"),
                "ringkasan": res.get("ringkasan_eksekutif"),
                "poin_penting": res.get("poin_penting_dan_data"),
                "tanya_jawab_anti_plot_hole": res.get("rekap_tanya_jawab_dan_anti_plot_hole"),
            },
            "download_urls": {
                "notulen": f"/api/meetings/{meeting_id}/download/notulen",
                "ringkasan": f"/api/meetings/{meeting_id}/download/ringkasan",
                "poin_penting": f"/api/meetings/{meeting_id}/download/poin_penting",
                "tanya_jawab": f"/api/meetings/{meeting_id}/download/tanya_jawab",
            }
        })
    elif meeting["status"] == "FAILED":
        response_data["error"] = meeting.get("error", "Terjadi kesalahan saat memproses audio.")

    return response_data


@app.get("/api/meetings/{meeting_id}/download/{doc_type}")
def download_document(meeting_id: str, doc_type: str):
    """
    Unduh salah satu dari 4 file Markdown (.md) terpisah
    Pilihan doc_type: notulen, ringkasan, poin_penting, tanya_jawab
    """
    meeting = meetings_db.get(meeting_id)
    if not meeting:
        raise HTTPException(status_code=404, detail="Meeting ID tidak ditemukan")

    file_mapping = {
        "notulen": "1_notulen_rapat.md",
        "ringkasan": "2_ringkasan_eksekutif.md",
        "poin_penting": "3_poin_penting_dan_data.md",
        "tanya_jawab": "4_rekap_tanya_jawab_anti_plot_hole.md"
    }

    filename = file_mapping.get(doc_type)
    if not filename:
        raise HTTPException(status_code=400, detail=f"Tipe dokumen tidak valid. Pilih dari: {list(file_mapping.keys())}")

    file_path = os.path.join(meeting["dir"], filename)
    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="File dokumen belum selesai di-generate")

    return FileResponse(
        path=file_path,
        media_type="text/markdown",
        filename=filename
    )


if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("PORT", 8000))
    host = os.getenv("HOST", "0.0.0.0")
    uvicorn.run("main:app", host=host, port=port, reload=True)
