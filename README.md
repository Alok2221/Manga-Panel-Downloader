# Manga Panel Studio

Full-stack application for downloading, organising, and reading manga chapter panels from **[MangaDex](https://mangadex.org)**. Add chapters by URL, browse by manga and volume, read in-app with a fullscreen viewer, and export chapters as ZIP.

---

## Features

- **Main Page** — Add a chapter by MangaDex chapter URL; panels download in the background. Activity log and list of downloaded chapters.
- **Search** — Search and filter your downloaded chapters (by manga title and chapter number).
- **Read Manga** — Chapters listed in **expandable sections by manga title**, with **volumes** inside each manga. Filter by manga title, chapter number, and volume. **Read** opens the in-app reader; **Download ZIP** packs all panels into a single ZIP named after the chapter.
- **Reader** — One panel per view, fit-to-page; **fullscreen** with black letterboxing and prev/next + close controls. Keyboard: ←/→ for panels, Escape to exit fullscreen.
- **Panel translation (AI)** — In the reader’s **Translation** tab: **Generate text map** runs OCR on chapter panels (OpenAI vision), **Translate to Polish** translates extracted text (EN→PL). Toggle view per bubble: Original / Translated / Both. Preferences (view mode, active tab) are stored in `data_base`.
- **MangaDex API** — Backend proxies MangaDex for manga search and chapter lists (e.g. to discover chapters before adding).

---

## Tech stack

| Layer      | Stack |
|-----------|--------|
| Backend   | Java 17, Spring Boot 4, Spring Data JPA, WebFlux (WebClient), REST API |
| Frontend  | Angular 21, standalone components |
| Database  | PostgreSQL (Flyway migrations) |
| Translator| Python FastAPI, OpenAI (vision + chat) for OCR and EN→PL translation |
| Run       | Docker + Docker Compose |

---

## Requirements

- **Docker and Docker Compose** (recommended), or
- **Local:** JDK 17, Maven, Node.js 20+, PostgreSQL 16

---

## Quick start (Docker)

```bash
docker compose up --build
```

- **App:** http://localhost:4200  
- **API:** http://localhost:8080/api  
- **DB:** localhost:5432 (default user/pass: `postgres` / `postgres`)
- **Translator:** http://localhost:8000 (Python FastAPI, called by backend)

Create a `.env` file next to `docker-compose.yml` with at least:

```env
OPENAI_API_KEY=sk-...
```

---

## Local development

For a more detailed dev setup (including OpenAI key management and running the translator service), see **`DEV.md`**.

### Backend

1. Start PostgreSQL, e.g.:  
   `docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=postgres postgres:16-alpine`
2. Create DB: `createdb mangapanel`
3. Run: `./mvnw spring-boot:run`  
   API: http://localhost:8080

### Frontend

1. `cd frontend && npm install && npm start`
2. App: http://localhost:4200 (proxies `/api` to backend)

---

## Configuration

| Variable | Description |
|----------|-------------|
| `DATABASE_URL` | JDBC URL (default: `jdbc:postgresql://localhost:5432/mangapanel`) |
| `DATABASE_USER` | DB user |
| `DATABASE_PASSWORD` | DB password |
| `MANGA_SOURCE_QUALITY` | `data` or `data-saver` for MangaDex image quality |
| `MANGA_SOURCE_FORCE_PORT_443` | `true` if MangaDex is DNS-blocked |
| `TRANSLATOR_BASE_URL` | Translator service URL (default: `http://localhost:8000`) |
| `TRANSLATOR_TIMEOUT` | Request timeout for translator (default: `30s`) |

**Translator service (Python)** — set in the environment where the service runs:

| Variable | Description |
|----------|-------------|
| `OPENAI_API_KEY` | **Required.** OpenAI API key for vision (OCR) and chat (translation). |
| `OPENAI_MODEL_OCR` | Vision model (default: `gpt-4o-mini`) |
| `OPENAI_MODEL_TRANSLATE` | Text model for translation (default: `gpt-4o-mini`) |

---

## REST API

### App & chapters

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api` | API info |
| POST | `/api/download` | Start chapter download (body: `{ "chapterUrl": "https://mangadex.org/chapter/..." }`) |
| GET | `/api/chapters` | List chapters (params: `title`, `chapter`, `page`, `size`) |
| GET | `/api/chapters/grouped` | Chapters grouped by manga and volume (params: `title`, `chapter`, `volume`) |
| GET | `/api/chapters/{id}` | Chapter details |
| GET | `/api/chapters/{id}/panels` | List panels for a chapter |
| DELETE | `/api/chapters/{id}` | Delete chapter and its panels |
| POST | `/api/chapters/reindex` | Reindex chapter IDs to 1, 2, 3, … |
| GET | `/api/search` | Search chapters (params: `title`, `chapter`, `page`, `size`) |

### Panels & MangaDex proxy

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/panels/{id}/image` | Panel image (binary) |
| GET | `/api/chapters/{id}/texts` | Text segments (OCR/translation) for a chapter |
| POST | `/api/chapters/{id}/ocr` | Run OCR on chapter panels (params: optional `sourceLanguage`) |
| POST | `/api/chapters/{id}/translate` | Translate chapter segments (params: optional `targetLanguage`) |
| GET | `/api/mangadex/manga` | Search MangaDex manga (params: `title`, `limit`, `offset`) |
| GET | `/api/mangadex/manga/{id}/chapters` | MangaDex chapter feed (params: `limit`, `offset`, `translatedLanguage`) |

---

## Translation architecture

Panel text extraction and translation use a **separate Python service** so that OpenAI is called from one place and the backend stays language-agnostic.

1. **Frontend** — In the reader (`/chapters/:id`), the user clicks **Generate text map** (OCR) or **Translate to Polish**. The UI shows segments under the current panel and lets you switch Original / Translated / Both.
2. **Backend (Spring)** — `POST /api/chapters/{id}/ocr` loads panel images, encodes them as base64, and sends them to the translator service. The service returns segments (text + optional bbox), which are stored in `panel_text_segment`. `POST /api/chapters/{id}/translate` loads existing segments, sends them to the translator with context (manga title, chapter number), and writes back `translated_text`. `GET /api/chapters/{id}/texts` returns all segments for the chapter.
3. **Translator service (Python, FastAPI)** — Runs next to the app (e.g. `translator-service/` with `uvicorn` or Docker). **Requires `OPENAI_API_KEY`.**  
   - `POST /ocr` — Receives panels (base64 images), uses OpenAI vision to list speech bubbles per panel, returns a list of segments.  
   - `POST /translate` — Receives segments + source/target language + optional context, calls OpenAI chat to translate in one batch (for consistency), returns translated text per segment ID.

**Limitations:** OCR and translation depend on the OpenAI API (usage and cost). The translator service must be running and reachable at `TRANSLATOR_BASE_URL`. Other manga sources than MangaDex are supported as long as panels are stored in the DB; target language is configurable (default PL).

---

## Testing

- **Backend:** `mvn test` — runs unit tests (e.g. `PanelTextService`, `TranslatorClient` with MockWebServer, `PanelTextController` with standalone MockMvc) and integration tests (e.g. OCR flow with mocked translator, chapter/panel persistence). Uses H2 and test `application.properties` in `src/test/resources`.
- **Translator service (Python):** From `translator-service/`, install deps with `pip install -r requirements.txt`, then run `python -m unittest discover -s tests -v` to run OCR prompt/parse unit tests (no OpenAI call).
- **Frontend:** A spec for the panel-gallery (reader) component is in `frontend/src/app/components/panel-gallery/panel-gallery.component.spec.ts`; add a test runner (e.g. Karma or Vitest) in `angular.json` if you want to run it via `ng test`.

---

## MangaDex acceptable use

This project uses the **public [MangaDex API](https://api.mangadex.org/docs)** and is for **personal / educational use** only.

- We **credit MangaDex** as the manga data and image provider.
- We **credit scanlation groups** via chapter metadata from MangaDex and will honour content removal requests.
- We **do not** run ads, tracking, or paid services.

If you deploy this project publicly, you must comply with the [current MangaDex acceptable use policy](https://api.mangadex.org/docs/).

---

## Project structure

```
MangaPanel/
├── src/main/java/.../downloader/
│   ├── web/controller/   # ChapterController, MangadexController, PanelTextController
│   ├── service/          # ChapterService, PanelTextService, PanelService, ...
│   ├── persistence/repository/  # JPA repositories
│   ├── domain/entity/    # Manga, Chapter, Panel, PanelTextSegment
│   ├── integration/translator/  # TranslatorClient, DTOs for OCR/translate
│   ├── config/
│   └── web/dto/
├── src/main/resources/
│   ├── application.properties
│   └── db/migration/     # Flyway (e.g. V5 panel_text_segment)
├── translator-service/   # Python FastAPI: /ocr, /translate (OpenAI)
├── frontend/             # Angular SPA (Main, Search, Read, Reader + translation UI)
├── docker-compose.yml
└── README.md
```

---

## License

Use and adapt as you like. Ensure MangaDex and scanlation groups are credited and that your use complies with MangaDex’s terms.
