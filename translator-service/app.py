"""FastAPI translator service: OCR and translation endpoints."""
import logging
import os

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from config import get_config, Config
from models import (
    OcrRequest,
    OcrResponse,
    OcrSegment,
    TranslateRequest,
    TranslateResponse,
)
from openai_client import perform_ocr, perform_translate
from openai import APIError as OpenAIAPIError

# Optional: load .env if python-dotenv is installed
try:
    from dotenv import load_dotenv
    load_dotenv()
except ImportError:
    pass

app = FastAPI(title="Manga Panel Translator", version="0.1.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


def _config() -> Config:
    try:
        return get_config()
    except ValueError as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/ocr", response_model=OcrResponse)
def ocr(request: OcrRequest):
    """Run OCR on manga panels; returns segments (text + optional bbox) per panel."""
    if not request.panels:
        raise HTTPException(status_code=400, detail="panels must not be empty")
    for p in request.panels:
        if not p.image_base64 and not p.image_url:
            raise HTTPException(
                status_code=400,
                detail=f"Panel {p.id} must have image_base64 or image_url",
            )
    try:
        config = _config()
    except HTTPException:
        raise
    try:
        segments: list[OcrSegment] = perform_ocr(request, config)
        return OcrResponse(segments=segments)
    except OpenAIAPIError as e:
        logging.exception("OpenAI error in OCR")
        raise HTTPException(status_code=502, detail=f"OpenAI error: {e.message or str(e)}")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.post("/translate", response_model=TranslateResponse)
def translate(request: TranslateRequest):
    """Translate a list of segments (e.g. speech bubbles) with optional context."""
    if not request.segments:
        raise HTTPException(status_code=400, detail="segments must not be empty")
    try:
        config = _config()
    except HTTPException:
        raise
    try:
        return perform_translate(request, config)
    except OpenAIAPIError as e:
        logging.exception("OpenAI error in translate")
        raise HTTPException(status_code=502, detail=f"OpenAI error: {e.message or str(e)}")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.get("/health")
def health():
    return {"status": "ok"}


if __name__ == "__main__":
    level = os.environ.get("TRANSLATOR_LOG_LEVEL", "INFO").upper()
    logging.basicConfig(level=getattr(logging, level, logging.INFO))
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
