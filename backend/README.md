# MeetAI Backend Service (FastAPI + Google Gemini Multimodal Audio)

Lightweight Python backend server that receives audio recordings from the MeetAI Android client and leverages **Google Gemini Multimodal Audio** (`gemini-3.5-flash`) to generate **4 standalone structured meeting documents** with zero plot holes.

---

## 📄 The 4 AI-Generated Documents
1. **`1_notulen_rapat.md`**: Corporate-standard formal minutes, recognized attendee list, discussion timeline, decisions, and an **Action Items table** (Assignees + Deadlines).
2. **`2_ringkasan_eksekutif.md`**: Concise 1–2 page executive summary tailored for C-Level leadership.
3. **`3_poin_penting_dan_data.md`**: Quantifiable facts, numerical data, financial figures, growth metrics, and critical milestones.
4. **`4_rekap_tanya_jawab_anti_plot_hole.md`**: Complete Q&A catalog, with an explicit section highlighting **unresolved questions, deferred issues, or information gaps (anti-plot-hole)**.

---

## 🚀 Setup & Execution

### 1. Virtual Environment & Dependencies
```bash
cd backend
python -m venv venv

# Windows:
.\venv\Scripts\activate
# Linux / macOS:
source venv/bin/activate

pip install -r requirements.txt
```

### 2. Environment Configuration
Copy `.env.example` to `.env`:
```bash
copy .env.example .env
```
Populate `.env` with your Google AI Studio API Key:
```env
GEMINI_API_KEY=your_gemini_api_key_here
GEMINI_MODEL=gemini-3.5-flash
PORT=8000
HOST=0.0.0.0
STORAGE_DIR=./storage
```

### 3. Run the Server
```bash
python main.py
```
The server will run on `http://0.0.0.0:8000`. Interactive Swagger UI is accessible at:
`http://localhost:8000/docs`

---

## 📡 API Endpoints Overview

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/health` | Server health check and active Gemini model info |
| `POST` | `/api/meetings/upload` | Multipart endpoint receiving `.m4a` audio from Android client |
| `GET` | `/api/meetings/{id}` | Poll processing status and retrieve JSON with all 4 documents |
| `GET` | `/api/meetings/{id}/download/{doc_type}` | Download raw `.md` file (`notulen`, `ringkasan`, `poin_penting`, `tanya_jawab`) |

---

## 🐳 Docker Deployment
```bash
docker build -t meetai-backend .
docker run -p 8000:8000 --env GEMINI_API_KEY=your_api_key meetai-backend
```
