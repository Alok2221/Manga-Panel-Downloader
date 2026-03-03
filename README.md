# Manga Panel Studio

Full-stack application for downloading, organising, and reading manga chapter panels from **[MangaDex](https://mangadex.org)**. Add chapters by URL, browse by manga and volume, read in-app with a fullscreen viewer, and export chapters as ZIP.

---

## Features

- **Main Page** — Add a chapter by MangaDex chapter URL; panels download in the background. Activity log and list of downloaded chapters.
- **Search** — Search and filter your downloaded chapters (by manga title and chapter number).
- **Read Manga** — Chapters listed in **expandable sections by manga title**, with **volumes** inside each manga. Filter by manga title, chapter number, and volume. **Read** opens the in-app reader; **Download ZIP** packs all panels into a single ZIP named after the chapter.
- **Reader** — One panel per view, fit-to-page; **fullscreen** with black letterboxing and prev/next + close controls. Keyboard: ←/→ for panels, Escape to exit fullscreen.
- **Reindex** — After deleting chapters, reindex so IDs are again 1, 2, 3, …
- **MangaDex API** — Backend proxies MangaDex for manga search and chapter lists (e.g. to discover chapters before adding).

---

## Tech stack

| Layer      | Stack |
|-----------|--------|
| Backend   | Java 17, Spring Boot 4, Spring Data JPA, WebFlux (WebClient), REST API |
| Frontend  | Angular 21, standalone components |
| Database  | PostgreSQL (Flyway migrations) |
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

---

## Local development

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
| GET | `/api/mangadex/manga` | Search MangaDex manga (params: `title`, `limit`, `offset`) |
| GET | `/api/mangadex/manga/{id}/chapters` | MangaDex chapter feed (params: `limit`, `offset`, `translatedLanguage`) |

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
│   ├── controller/       # ChapterController, MangadexController
│   ├── service/          # ChapterService, ChapterDownloadService, MangadexApiService, PanelService
│   ├── repository/       # JPA repositories
│   ├── entity/           # Manga, Chapter (with volume), Panel
│   ├── client/           # MangaDex API client (chapter, manga, feed, at-home)
│   ├── client/dto/       # MangaDex response DTOs
│   ├── config/
│   └── dto/
├── src/main/resources/
│   ├── application.properties
│   └── db/migration/     # Flyway (e.g. V3 add chapter.volume)
├── frontend/             # Angular SPA (Main, Search, Read, Reader)
├── docker-compose.yml
└── README.md
```

---

## License

Use and adapt as you like. Ensure MangaDex and scanlation groups are credited and that your use complies with MangaDex’s terms.
