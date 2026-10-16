# MeetAI Backend Server (FastAPI + Google Gemini Multimodal Audio)

Backend server ringan untuk menerima rekaman audio rapat dari aplikasi Android MeetAI dan mengekstraknya menjadi **4 Dokumen Rapat Terpisah** tanpa celah informasi (*anti-plot-hole*) menggunakan model Google Gemini (Gemini 2.5/1.5 Flash).

---

## 📄 4 Dokumen Output yang Dihasilkan:
1. **`1_notulen_rapat.md`**: Format formal korporat, daftar hadir teridentifikasi, agenda, keputusan final, dan tabel *Action Items (PIC + Deadline)*.
2. **`2_ringkasan_eksekutif.md`**: Ringkasan padat 1–2 halaman untuk konsumsi Direksi / C-Level.
3. **`3_poin_penting_dan_data.md`**: Seluruh angka, persentase, nominal keuangan, metrik kinerja, dan milestone kritis.
4. **`4_rekap_tanya_jawab_anti_plot_hole.md`**: Seluruh pertanyaan dan jawaban, serta katalog **pertanyaan yang masih menggantung / belum terjawab / plot hole** beserta rekomendasi tindak lanjutnya.

---

## 🚀 Panduan Menjalankan Backend

### 1. Buat Virtual Environment & Install Dependensi
```bash
cd backend
python -m venv venv
# Di Windows:
venv\Scripts\activate
# Di Linux/macOS:
source venv/bin/activate

pip install -r requirements.txt
```

### 2. Atur File Konfigurasi `.env`
Salin file `.env.example` menjadi `.env`:
```bash
cp .env.example .env
```
Lalu masukkan API Key Gemini organisasi perusahaan Anda:
```env
GEMINI_API_KEY=AIzaSy...
GEMINI_MODEL=gemini-2.5-flash
PORT=8000
```

### 3. Jalankan Server
```bash
python main.py
```
Server akan aktif di `http://localhost:8000`. Dokumentasi interaktif Swagger UI dapat diakses di:
`http://localhost:8000/docs`

---

## 📡 Endpoint API Utama:
| Method | Endpoint | Deskripsi |
|---|---|---|
| `GET` | `/health` | Cek status server dan model Gemini yang aktif |
| `POST` | `/api/meetings/upload` | Menerima file `.m4a` dari Android (Multipart Form) |
| `GET` | `/api/meetings/{id}` | Cek status AI dan ambil 4 dokumen dalam format JSON |
| `GET` | `/api/meetings/{id}/download/{doc_type}` | Unduh file `.md` fisik (`notulen`, `ringkasan`, `poin_penting`, `tanya_jawab`) |

---

## 🐳 Menjalankan dengan Docker:
```bash
docker build -t meetai-backend .
docker run -p 8000:8000 --env GEMINI_API_KEY=AIzaSy... meetai-backend
```
