"""Configuration loaded from environment variables."""
import os
from dataclasses import dataclass
from typing import Optional


@dataclass(frozen=True)
class Config:
    openai_api_key: str
    openai_model_ocr: str
    openai_model_translate: str
    translator_log_level: str
    max_segments_per_request: int

    @classmethod
    def from_env(cls) -> "Config":
        api_key = os.environ.get("OPENAI_API_KEY", "").strip()
        if not api_key:
            raise ValueError("OPENAI_API_KEY must be set")
        return cls(
            openai_api_key=api_key,
            openai_model_ocr=os.environ.get("OPENAI_MODEL_OCR", "gpt-4o-mini"),
            openai_model_translate=os.environ.get("OPENAI_MODEL_TRANSLATE", "gpt-4o-mini"),
            translator_log_level=os.environ.get("TRANSLATOR_LOG_LEVEL", "INFO").upper(),
            max_segments_per_request=int(os.environ.get("MAX_SEGMENTS_PER_REQUEST", "100")),
        )


def get_config() -> Config:
    return Config.from_env()
