# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Water plant flow prediction and visualization system based on TimeMixer deep learning. Full-stack graduation project:

- **Frontend (Vue 3 + TypeScript)**: `/project/fe` — dashboard, large screen, 3D scene, AI assistant chat
- **Backend (Spring Boot 4.0.5 / Java 21)**: `/project/server` — REST API, task orchestration, database, algorithm client, AI proxy
- **Algorithm (Python)**: `/algorithm/TimeMixer` — TimeMixer model training and prediction HTTP service

## Common Commands

### Frontend
```bash
cd project/fe
npm run dev          # Dev server on 0.0.0.0:5173 (network-accessible)
npm run build        # Type-check (vue-tsc) then production build
npm run preview      # Preview production build on 0.0.0.0:4173
```

### Backend
```bash
cd project/server
./gradlew bootRun          # Spring Boot on port 12010, context-path /api
./gradlew clean build      # Full build including tests
./gradlew test             # Run tests only
```

### Algorithm
```bash
cd algorithm/TimeMixer
conda run -n tm python train_waterlevel.py                    # Train model
conda run -n tm python timemixer_service.py                   # Prediction HTTP service on 0.0.0.0:18081
conda run -n tm python timemixer_service.py --port 18082      # Custom port
```

## Architecture

```
Vue 3 (0.0.0.0:5173)
  └── proxy /api → Spring Boot (127.0.0.1:12010/api)
        ├── PostgreSQL (10.1.21.17:5432 / wd)
        ├── Python TimeMixer service (127.0.0.1:18081)
        └── SiliconFlow API (api.siliconflow.cn/v1, model Qwen/Qwen3.5-4B)
```

### Python Service Endpoints
- `GET /health` — service health and model metadata
- `POST /predict` — accepts `csv_path` or `records` array, returns predictions

### Key Backend Modules
- `AlgorithmService` / `AlgorithmController` — training and prediction task orchestration
- `TimeMixerClient` — OkHttp client for Python service
- `AiAssistantService` / `AiAssistantController` — proxies chat to Qwen via SiliconFlow
- `VisualController` / `SystemController` — dashboard data and system status

### Algorithm Internals
- `waterlevel_common.py` — shared utilities: argument building, CSV loading, scaler, time features
- `train_waterlevel.py` — training entrypoint using `exp/exp_long_term_forecasting.py`
- `timemixer_service.py` — standalone `ThreadingHTTPServer` loading model from `artifacts/` checkpoint
- Model weights saved to `checkpoints/`, deployment artifacts to `artifacts/`

### Frontend Stack
- Vue 3 + Composition API + TypeScript
- Vite with `unplugin-vue-components` (Naive UI auto-import)
- Naive UI, ECharts (via vue-echarts), Three.js, Pinia, Vue Router, Axios

## Development Rules

1. **Conda environment**: All Python code runs in the `tm` conda environment. Use `conda run -n tm python <script>`.
2. **Gradle wrapper**: Always use `./gradlew` for backend builds.
3. **Database**: PostgreSQL at `192.168.31.221:5432` (or `10.1.21.17:5432`), database `wd`, user `postgres`, password `root`. Use MCP for queries.
4. **Network proxy**: Route through `192.168.31.221:7897` when external network access is needed.
5. **SQL migrations**: Place new or modified table SQL in `project/server/dbs/` (create the directory if needed).
6. **Progress tracking**: Append a dated entry to `doc/开发进度.md` after completing any development work.
7. **Frontend design**: Use Naive UI components. Keep pages minimalist, premium, and color-restrained — follow Apple design principles.

## Configuration

- **Java**: 21 with Spring Boot 4.0.5
- **Frontend**: Node.js + npm, Vite 8, Vue 3.5, TypeScript 6.0
- **Python**: conda env `tm`
- **Database**: PostgreSQL, Hibernate DDL auto = `none` (manual schema changes only)
- **SiliconFlow API key**: via `SILICONFLOW_API_KEY` environment variable (falls back to empty)
