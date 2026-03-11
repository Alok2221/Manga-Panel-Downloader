"""OpenAI API calls for OCR (vision) and translation (text)."""
import base64
import json
import logging
from typing import Optional

from openai import OpenAI
from openai import APIError as OpenAIAPIError

from config import Config
from models import OcrSegment, OcrRequest, TranslateRequest, TranslateResponse, TranslateSegmentOut, PanelImage

logger = logging.getLogger(__name__)


def _build_ocr_prompt(sequence_start: int) -> str:
    return (
        "You are an OCR for manga panels. List every speech bubble / text in this panel. "
        "Return a JSON array of objects, each with: sequenceIndex (int, 0-based within this panel), text (string). "
        "Optional: bboxX, bboxY, bboxW, bboxH (integers) if you can infer approximate bounding box. "
        f"First item in this panel has sequenceIndex={sequence_start}. "
        "Reply with ONLY the JSON array, no markdown or explanation."
    )


def _parse_ocr_json(reply: str, panel_id: int, sequence_start: int) -> list[OcrSegment]:
    segments: list[OcrSegment] = []
    reply = reply.strip()
    if reply.startswith("```"):
        reply = reply.split("```")[1]
        if reply.startswith("json"):
            reply = reply[4:]
        reply = reply.strip()
    try:
        data = json.loads(reply)
    except json.JSONDecodeError as e:
        logger.warning("OCR response not valid JSON: %s", e)
        return segments
    if not isinstance(data, list):
        return segments
    for i, item in enumerate(data):
        if not isinstance(item, dict):
            continue
        seq = item.get("sequenceIndex", item.get("sequence_index", i))
        if isinstance(seq, str):
            try:
                seq = int(seq)
            except ValueError:
                seq = sequence_start + i
        text = item.get("text", item.get("sourceText", ""))
        if not text:
            continue
        segments.append(
            OcrSegment(
                panel_id=panel_id,
                sequence_index=seq if isinstance(seq, int) else sequence_start + i,
                text=str(text).strip(),
                bbox_x=item.get("bboxX") or item.get("bbox_x"),
                bbox_y=item.get("bboxY") or item.get("bbox_y"),
                bbox_w=item.get("bboxW") or item.get("bbox_w"),
                bbox_h=item.get("bboxH") or item.get("bbox_h"),
            )
        )
    return segments


def _image_content(panel: PanelImage, config: Config) -> dict:
    if panel.image_base64:
        return {
            "type": "image_url",
            "image_url": {"url": f"data:image/png;base64,{panel.image_base64}"},
        }
    if panel.image_url:
        return {"type": "image_url", "image_url": {"url": panel.image_url}}
    raise ValueError("Panel must have image_base64 or image_url")


def perform_panel_ocr(
    client: OpenAI,
    panel: PanelImage,
    panel_index: int,
    config: Config,
) -> list[OcrSegment]:
    """Run vision OCR on a single panel. sequence_index starts at panel_index * 100 to keep order."""
    sequence_start = panel_index * 100
    prompt = _build_ocr_prompt(sequence_start)
    image_content = _image_content(panel, config)
    messages = [
        {"role": "user", "content": [{"type": "text", "text": prompt}, image_content]},
    ]
    response = client.chat.completions.create(
        model=config.openai_model_ocr,
        messages=messages,
        max_tokens=2048,
    )
    reply = (response.choices[0].message.content or "").strip()
    segments = _parse_ocr_json(reply, panel_id=panel.id, sequence_start=sequence_start)
    return segments


def perform_ocr(request: OcrRequest, config: Config) -> list[OcrSegment]:
    """Run OCR on all panels and return sorted segments."""
    client = OpenAI(api_key=config.openai_api_key)
    all_segments: list[OcrSegment] = []
    for i, panel in enumerate(request.panels):
        try:
            segs = perform_panel_ocr(client, panel, i, config)
            for s in segs:
                all_segments.append(s)
        except OpenAIAPIError as e:
            logger.exception("OpenAI API error for panel %s", panel.id)
            raise
    all_segments.sort(key=lambda s: (s.panel_id, s.sequence_index))
    return all_segments


def perform_translate(request: TranslateRequest, config: Config) -> TranslateResponse:
    """Call text model to translate segments; returns TranslateResponse."""
    client = OpenAI(api_key=config.openai_api_key)
    ctx = request.context
    intro = "You are a manga translator. Translate each line preserving tone, gender, register, and slang. Do not change numbering or IDs."
    if ctx:
        parts = []
        if ctx.manga_title:
            parts.append(f"Manga: {ctx.manga_title}")
        if ctx.chapter_number:
            parts.append(f"Chapter: {ctx.chapter_number}")
        if ctx.notes:
            parts.append(f"Notes: {ctx.notes}")
        if parts:
            intro += " Context: " + "; ".join(parts) + "."
    intro += f" Source language: {request.source_language}. Target language: {request.target_language}."
    lines = [f"#{s.id}: {s.source_text}" for s in request.segments]
    user_text = intro + "\n\nTranslate the following. Reply with ONLY a JSON array: [{\"id\": <id>, \"translatedText\": \"...\"}] for each line.\n\n" + "\n".join(lines)

    response = client.chat.completions.create(
        model=config.openai_model_translate,
        messages=[{"role": "user", "content": user_text}],
        max_tokens=4096,
    )
    reply = (response.choices[0].message.content or "").strip()
    if reply.startswith("```"):
        reply = reply.split("```")[1]
        if reply.lower().startswith("json"):
            reply = reply[4:]
        reply = reply.strip()
    try:
        data = json.loads(reply)
    except json.JSONDecodeError as e:
        logger.warning("Translate response not valid JSON: %s", e)
        raise ValueError("Translation model did not return valid JSON") from e
    if not isinstance(data, list):
        raise ValueError("Translation model did not return a list")
    id_to_text: dict[int, str] = {}
    for item in data:
        if not isinstance(item, dict):
            continue
        seg_id = item.get("id")
        text = item.get("translatedText", item.get("translated_text", ""))
        if seg_id is not None:
            id_to_text[int(seg_id)] = str(text).strip() if text else ""
    out = [
        TranslateSegmentOut(id=s.id, translated_text=id_to_text.get(s.id, s.source_text))
        for s in request.segments
    ]
    return TranslateResponse(segments=out)
