# HiPhant API

CHZZK VOD 채팅 데이터를 수집/저장하고, 채팅 집중 구간(하이라이트)을 생성해 제공하는 **백엔드 API 서버**입니다.

프론트엔드는 이 저장소에 포함되어 있지 않으며, 별도 애플리케이션에서 본 API를 호출해 사용하는 구조를 전제로 합니다.

## 배포 주소

- Production: https://hiphant.pachylover.com/

## 기술 스택

- Language: Java 21
- Framework: Spring Boot 3.5.x
- Build: Gradle
- Database: PostgreSQL 15
- Migration: Flyway
- Infra/Container: Docker, Docker Compose
- External API: CHZZK Open API, Gemini API

## 주요 기능

- CHZZK 영상 메타데이터 조회
- 영상 채팅 데이터 기반 하이라이트 생성
- 하이라이트 결과 조회
- Flyway 기반 스키마 버전 관리

## API 엔드포인트

### 1) 영상 정보 조회

- `GET /api/v1/videos/{id}`

예시:

```bash
curl https://hiphant.pachylover.com/api/v1/videos/{videoId}
```

### 2) 하이라이트 생성

- `POST /api/v1/highlights/{id}`
- optional query parameter `type` may be passed to categorize the highlight (defaults to `AUTO`).

예시:

```bash
# 기본: 자동 생성
curl -X POST https://hiphant.pachylover.com/api/v1/highlights/{videoId}

# 타입 지정
curl -X POST "https://hiphant.pachylover.com/api/v1/highlights/{videoId}?type=MANUAL"
```

### 3) 하이라이트 조회

- `GET /api/v1/highlights/{id}`

예시:

```bash
curl https://hiphant.pachylover.com/api/v1/highlights/{videoId}
```

## 로컬 실행

### 1) DB 실행

```bash
docker compose up -d postgres
```

### 2) 환경 변수 설정

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `GEMINI_API_KEY`

### 3) 애플리케이션 실행

```bash
./gradlew bootRun
```

## 문서

- 설계 문서: `DESIGN.md`
- DDL: `docs/DDL.sql`
- Flyway 가이드: `docs/flyway.md`
