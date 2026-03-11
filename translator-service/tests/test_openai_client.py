"""Unit tests for OCR/translate prompt building and response parsing (no real API calls)."""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from openai_client import _build_ocr_prompt, _parse_ocr_json


class TestBuildOcrPrompt(unittest.TestCase):
    def test_prompt_contains_sequence_start(self):
        prompt = _build_ocr_prompt(0)
        self.assertIn("sequenceIndex", prompt)
        self.assertIn("0", prompt)

    def test_prompt_requests_json_array(self):
        prompt = _build_ocr_prompt(10)
        self.assertIn("JSON", prompt)
        self.assertIn("10", prompt)


class TestParseOcrJson(unittest.TestCase):
    def test_parses_valid_json_array(self):
        reply = '[{"sequenceIndex": 0, "text": "Hello"}, {"sequenceIndex": 1, "text": "World"}]'
        segments = _parse_ocr_json(reply, panel_id=5, sequence_start=0)
        self.assertEqual(len(segments), 2)
        self.assertEqual(segments[0].panel_id, 5)
        self.assertEqual(segments[0].sequence_index, 0)
        self.assertEqual(segments[0].text, "Hello")
        self.assertEqual(segments[1].text, "World")

    def test_parses_snake_case_keys(self):
        reply = '[{"sequence_index": 0, "text": "Hi"}]'
        segments = _parse_ocr_json(reply, panel_id=1, sequence_start=0)
        self.assertEqual(len(segments), 1)
        self.assertEqual(segments[0].sequence_index, 0)
        self.assertEqual(segments[0].text, "Hi")

    def test_strips_markdown_code_block(self):
        reply = '```json\n[{"sequenceIndex": 0, "text": "Bubble"}]\n```'
        segments = _parse_ocr_json(reply, panel_id=2, sequence_start=0)
        self.assertEqual(len(segments), 1)
        self.assertEqual(segments[0].text, "Bubble")

    def test_returns_empty_list_for_invalid_json(self):
        segments = _parse_ocr_json("not json at all", panel_id=1, sequence_start=0)
        self.assertEqual(segments, [])

    def test_returns_empty_list_for_non_array_json(self):
        segments = _parse_ocr_json('{"foo": "bar"}', panel_id=1, sequence_start=0)
        self.assertEqual(segments, [])

    def test_parses_bbox_fields(self):
        reply = '[{"sequenceIndex": 0, "text": "X", "bbox_x": 10, "bbox_y": 20, "bbox_w": 100, "bbox_h": 30}]'
        segments = _parse_ocr_json(reply, panel_id=1, sequence_start=0)
        self.assertEqual(len(segments), 1)
        self.assertEqual(segments[0].bbox_x, 10)
        self.assertEqual(segments[0].bbox_y, 20)
        self.assertEqual(segments[0].bbox_w, 100)
        self.assertEqual(segments[0].bbox_h, 30)


if __name__ == "__main__":
    unittest.main()
