"""Pydantic request/response models for OCR and translation."""
from typing import Optional

from pydantic import BaseModel, Field


class PanelImage(BaseModel):
    id: int
    image_base64: Optional[str] = None
    image_url: Optional[str] = None


class OcrRequest(BaseModel):
    chapter_id: int
    panels: list[PanelImage]
    source_language: str = "en"


class OcrSegment(BaseModel):
    panel_id: int
    sequence_index: int
    text: str
    bbox_x: Optional[int] = None
    bbox_y: Optional[int] = None
    bbox_w: Optional[int] = None
    bbox_h: Optional[int] = None


class OcrResponse(BaseModel):
    segments: list[OcrSegment] = Field(default_factory=list)


# --- Translate ---


class TranslateSegmentIn(BaseModel):
    id: int
    panel_id: int
    source_text: str


class TranslateContext(BaseModel):
    manga_title: Optional[str] = None
    chapter_number: Optional[str] = None
    notes: Optional[str] = None


class TranslateRequest(BaseModel):
    segments: list[TranslateSegmentIn]
    source_language: str = "en"
    target_language: str = "pl"
    context: Optional[TranslateContext] = None


class TranslateSegmentOut(BaseModel):
    id: int
    translated_text: str


class TranslateResponse(BaseModel):
    segments: list[TranslateSegmentOut] = Field(default_factory=list)
