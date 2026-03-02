# Manga Panel Downloader

Full-stack application for downloading and storing manga chapter panels from **MangaDex**. Enter a chapter URL, download all pages, and browse them in a local gallery.

## Tech stack

- **Backend:** Java 17, Spring Boot 4, Spring Data JPA, WebFlux (WebClient), REST API
- **Frontend:** Angular 21
- **Database:** PostgreSQL
- **Containers:** Docker + Docker Compose

## Supported source

| Source | URL pattern |
|--------|-------------|
| MangaDex | `https://mangadex.org/chapter/{uuid}` |

## Requirements

- Docker and Docker Compose (recommended)
- Or locally: JDK 17, Maven, Node.js 20+, PostgreSQL

## Run with Docker Compose

```bash
docker compose up --build
```

- **Frontend:** http://localhost:4200
- **Backend API:** http://localhost:8080/api
- **PostgreSQL:** localhost:5432 (default: postgres/postgres)

## Local development

### Backend

1. Start PostgreSQL: `docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=postgres postgres:16-alpine`
2. Create database: `createdb mangapanel`
3. Run: `./mvnw spring-boot:run`
4. API: http://localhost:8080

### Frontend

1. `cd frontend && npm install && npm start`
2. App: http://localhost:4200 (proxies to API on 8080)

## Configuration

Environment variables (optional; set before `docker compose up` or in shell):

| Variable | Description |
|----------|-------------|
| `DATABASE_URL` | JDBC URL (default: `jdbc:postgresql://localhost:5432/mangapanel`) |
| `DATABASE_USER` | DB user |
| `DATABASE_PASSWORD` | DB password |
| `STORAGE_PATH` | Panel storage path (default: `./data/panels`) |
| `MANGA_SOURCE_QUALITY` | `data` or `data-saver` for MangaDex |
| `MANGA_SOURCE_FORCE_PORT_443` | `true` if MangaDex is DNS-blocked |

## REST API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api` | API info |
| POST | `/api/download` | Download chapter (body: `{ "chapterUrl": "https://..." }`) |
| GET | `/api/chapters` | List chapters (params: `title`, `chapter`, `page`, `size`) |
| GET | `/api/chapters/{id}` | Chapter details |
| GET | `/api/chapters/{id}/panels` | List panels |
| GET | `/api/panels/{id}/image` | Panel image (binary) |
| DELETE | `/api/chapters/{id}` | Delete chapter |
| GET | `/api/search` | Search (params: `title`, `chapter`, `page`, `size`) |

## MangaDex acceptable usage

This project uses the **public MangaDex API** and is intended strictly for personal / educational use.

- **We credit MangaDex** as the manga data and image provider.
- **We credit scanlation groups** indirectly by always showing the chapter metadata returned by MangaDex, and we are prepared to honor any content removal requests that reach us through MangaDex or the scanlation groups.
- **We do not run ads, tracking, or paid services** in this project. The application is purely non-commercial.

If you deploy this project publicly, you are responsible for ensuring continued compliance with the latest MangaDex acceptable usage policy.

## Project structure

```
MangaPanel/
├── src/main/java/.../downloader/
│   ├── controller/     # REST
│   ├── service/        # ChapterService, PanelService, ChapterDownloadService
│   ├── repository/     # JPA
│   ├── entity/         # Manga, Chapter, Panel
│   ├── client/         # MangaDex API client
│   ├── config/
│   └── dto/
├── frontend/           # Angular app
├── docker/
├── docker-compose.yml
└── README.md
```