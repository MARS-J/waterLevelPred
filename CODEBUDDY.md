# CODEBUDDY.md

This file provides guidance to CodeBuddy Code when working with code in this repository.

## Project Overview

This is a water plant flow prediction and visualization system based on the TimeMixer deep learning model. It is a full-stack graduation design project with three main components:

- **Frontend (Vue 3)**: `/project/fe` — visualization dashboard, large screen, 3D scene, AI assistant chat
- **Backend (Spring Boot)**: `/project/server` — REST API, task orchestration, database access, algorithm service client, AI assistant proxy
- **Algorithm (Python)**: `/algorithm/TimeMixer` — TimeMixer model training and prediction HTTP service

## Project Structure

```
waterLevelPred/
├── algorithm/TimeMixer/          # Python algorithm module
│   ├── train_waterlevel.py       # Training script entrypoint
│   ├── timemixer_service.py      # Prediction HTTP service (port 18081)
│   ├── data/                     # Datasets (CSV)
│   ├── models/                   # Model implementations
│   ├── layers/                   # Neural network layers
│   └── exp/                      # Experiment runners
├── data/                         # Shared data files
├── doc/                          # Project documentation (Chinese)
│   ├── 03-系统架构与技术方案.md
│   ├── 开发进度.md               # Must update after each dev session
│   └── ...
├── project/
│   ├── fe/                       # Vue 3 frontend
│   │   ├── package.json
│   │   ├── vite.config.ts
│   │   └── src/
│   │       ├── views/            # Page-level components
│   │       ├── router/
│   │       ├── layouts/
│   │       └── components/
│   └── server/                   # Spring Boot backend
│       ├── build.gradle
│       ├── settings.gradle
│       └── src/main/java/com/waterlevel/server/
│           ├── controller/
│           ├── service/
│           ├── client/           # TimeMixer HTTP client
│           ├── config/
│           └── common/
```

## Common Commands

### Frontend
```bash
cd /home/xingyu/projects/my/parttime/waterLevelPred/project/fe
npm run dev          # Start dev server on port 5173
npm run build        # Type-check and build for production
npm run preview      # Preview production build on port 4173
```

### Backend
```bash
cd /home/xingyu/projects/my/parttime/waterLevelPred/project/server
./gradlew bootRun    # Run Spring Boot application
./gradlew clean build # Build and run tests
./gradlew test       # Run tests only
```

### Algorithm
```bash
cd /home/xingyu/projects/my/parttime/waterLevelPred/algorithm/TimeMixer
conda run -n tm python train_waterlevel.py      # Train model
conda run -n tm python timemixer_service.py     # Start prediction service (port 18081)
conda run -n tm python timemixer_service.py --port 18082  # Custom port
```

## Architecture Notes

### Component Communication
- **Frontend** (`5173`) → proxies `/api` → **Backend** (`12010/api`)
- **Backend** calls **Python Algorithm Service** via HTTP at `127.0.0.1:18081`
- **Backend** proxies AI assistant requests to SiliconFlow API (`api.siliconflow.cn/v1`)

### Key Backend Modules
- `AlgorithmService` / `AlgorithmController`: Training and prediction task orchestration
- `TimeMixerClient`: OkHttp client that communicates with Python service
- `AiAssistantService` / `AiAssistantController`: Proxies chat requests to Qwen/Qwen3.5-4B via SiliconFlow
- `VisualController` / `SystemController`: Dashboard data and system status

### Algorithm Service Endpoints (Python)
- `GET /health` — service health and model metadata
- `POST /predict` — accepts `csv_path` or `records` array, returns predictions

### Frontend Stack
- Vue 3 + Composition API + TypeScript
- Vite with `unplugin-vue-components` (Naive UI auto-import)
- Naive UI, ECharts, Three.js, Pinia, Vue Router

## Development Rules

1. **Conda environment**: All Python algorithm code must run in the `tm` conda environment. Use `conda run -n tm python <script>`.
2. **Gradle wrapper**: Always use `./gradlew` for backend builds.
3. **Database**: PostgreSQL at `192.168.31.221:5432` (or `10.1.21.17:5432`), database `wd`, username `postgres`, password `root`. Use MCP to query when needed.
4. **Proxy**: Network requests should go through local proxy `192.168.31.221:7897` when external network access is needed.
5. **SQL migrations**: Any new or modified table SQL should be placed in `/home/xingyu/projects/my/parttime/waterLevelPred/project/server/dbs/`.
6. **Progress tracking**: After completing any development work, append a short entry to `/home/xingyu/projects/my/parttime/waterLevelPred/doc/开发进度.md` with the date and what was done.
7. **Frontend design**: Use Naive UI. Keep pages minimalist, premium, and color-restrained, following Apple design guidelines.

## Environment & Configuration

- Java 21 (Spring Boot 4.0.5)
- Node.js with npm for frontend
- Conda env `tm` for Python algorithm
- PostgreSQL driver: `org.postgresql.Driver`
- Hibernate DDL auto is set to `none`; schema changes require manual SQL
