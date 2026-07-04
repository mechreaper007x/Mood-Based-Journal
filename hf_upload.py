"""
Upload backend and frontend source to Hugging Face Spaces.
Uses huggingface_hub upload_folder (no git needed).
"""
import os
import shutil
import tempfile
from pathlib import Path
from huggingface_hub import HfApi

TOKEN = os.environ.get("HF_TOKEN") or input("Enter Hugging Face Space Token: ")
HF_USER = "mechreaper007x"
BACKEND_SPACE = f"{HF_USER}/mood-journal-backend"
FRONTEND_SPACE = f"{HF_USER}/mood-journal-frontend"

BASE = Path(r"C:\Users\Savyasachi Mishra\Desktop\Mood based journal\moodjournal\moodjournal")
BACKEND_SRC = BASE / "backend"
FRONTEND_SRC = BASE / "frontend"

api = HfApi(token=TOKEN)

# ── BACKEND ──────────────────────────────────────────────────────────────────
print("=" * 60)
print("Uploading BACKEND to HF Space...")
print("=" * 60)

# Build list of files to upload (exclude build artifacts & IDE files)
BACKEND_IGNORE = {
    "target", ".git", "node_modules", "data",
    "rag_cache.dat", "startup.log", "startup_err.log",
    "startup_retry.log", "startup_retry_err.log",
    "src.zip", "export_codebase.py", ".agent",
}

with tempfile.TemporaryDirectory() as tmp:
    tmp_path = Path(tmp)
    
    # Copy selected backend files
    for item in BACKEND_SRC.iterdir():
        if item.name in BACKEND_IGNORE:
            continue
        dest = tmp_path / item.name
        if item.is_dir():
            shutil.copytree(item, dest)
        else:
            shutil.copy2(item, dest)
    
    # Write the README.md (it's gitignored so we create it fresh in tmp)
    readme_content = """\
---
title: Mood Journal Backend
emoji: 📓
colorFrom: purple
colorTo: indigo
sdk: docker
app_port: 7860
pinned: false
license: mit
short_description: GraalVM native Spring Boot backend for Mood-Based Journal
---

# Mood Journal — Backend API

A GraalVM-native Spring Boot 3 REST API for the **Mood-Based Journal** application.

## Features
- 🧠 AI-powered mood analysis using Gemini 2.0 Flash
- 🔐 JWT authentication with fingerprint validation
- 📊 Sentiment analysis & emotion tracking  
- 📄 PDF export of journal entries
- 📧 Email notifications via Resend SMTP

## Tech Stack
- **Runtime**: GraalVM Native Image (near-instant startup)
- **Framework**: Spring Boot 3.5 + Spring AI 1.1
- **Database**: H2 (file-based on HF) / PostgreSQL (production)
- **Security**: Spring Security + JWT + BCrypt

## Environment Variables (set as Space Secrets)

| Variable | Description |
|---|---|
| `GOOGLE_API_KEY` | Google Gemini API key |
| `JWT_SECRET` | Secret for JWT signing |
| `SPRING_MAIL_PASSWORD` | Resend SMTP API key |
| `FRONTEND_URL` | URL of the frontend Space |
| `MISTRAL_API_KEY` | Mistral AI API key |

## API Docs
Swagger UI at `/swagger-ui.html` once running.
"""
    (tmp_path / "README.md").write_text(readme_content, encoding="utf-8")
    
    print(f"Files to upload from: {tmp_path}")
    for f in sorted(tmp_path.rglob("*"))[:30]:
        print(f"  {f.relative_to(tmp_path)}")
    print("  ...")
    
    api.upload_folder(
        folder_path=str(tmp_path),
        repo_id=BACKEND_SPACE,
        repo_type="space",
        commit_message="Deploy: GraalVM native backend (pom.xml native profile + HF Dockerfile)",
        ignore_patterns=["*.class", "*.jar", "*.log"],
    )
    print(f"\n✅ Backend uploaded → https://huggingface.co/spaces/{BACKEND_SPACE}\n")

# ── FRONTEND ─────────────────────────────────────────────────────────────────
print("=" * 60)
print("Uploading FRONTEND to HF Space...")
print("=" * 60)

FRONTEND_IGNORE = {
    "node_modules", "dist", ".git",
}

with tempfile.TemporaryDirectory() as tmp:
    tmp_path = Path(tmp)
    
    for item in FRONTEND_SRC.iterdir():
        if item.name in FRONTEND_IGNORE:
            continue
        dest = tmp_path / item.name
        if item.is_dir():
            shutil.copytree(item, dest)
        else:
            shutil.copy2(item, dest)
    
    # Write the README.md
    readme_content = """\
---
title: Mood Journal
emoji: 🌈
colorFrom: purple
colorTo: pink
sdk: docker
app_port: 7860
pinned: false
license: mit
short_description: AI-powered mood journal — React + Vite frontend
---

# Mood Journal — Frontend

A beautiful React + Vite frontend for the **Mood-Based Journal** AI application.

## Features
- 📓 Write & track journal entries with AI mood analysis
- 🎨 Emotion-driven UI theming
- 📈 Mood trends and analytics dashboard
- 📄 PDF export of your journal

## Environment Variables (set as Space Variables)

| Variable | Description |
|---|---|
| `VITE_API_URL` | Full URL of the backend Space |

## Stack
- React 18 + Vite + TailwindCSS + Axios + Nginx (port 7860)
"""
    (tmp_path / "README.md").write_text(readme_content, encoding="utf-8")
    
    api.upload_folder(
        folder_path=str(tmp_path),
        repo_id=FRONTEND_SPACE,
        repo_type="space",
        commit_message="Deploy: React frontend (port 7860, non-root nginx, HF Spaces compatible)",
        ignore_patterns=["*.log"],
    )
    print(f"\n✅ Frontend uploaded → https://huggingface.co/spaces/{FRONTEND_SPACE}\n")

print("=" * 60)
print("🚀 DEPLOYMENT COMPLETE!")
print("=" * 60)
print(f"\nBackend  : https://huggingface.co/spaces/{BACKEND_SPACE}")
print(f"Frontend : https://huggingface.co/spaces/{FRONTEND_SPACE}")
print(f"\nBackend API URL  : https://mechreaper007x-mood-journal-backend.hf.space")
print(f"Frontend Live URL: https://mechreaper007x-mood-journal-frontend.hf.space")
print("\nNote: GraalVM native image build takes 10-20 min on HF. Monitor at:")
print(f"  https://huggingface.co/spaces/{BACKEND_SPACE}/logs")
