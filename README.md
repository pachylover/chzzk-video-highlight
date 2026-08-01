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

### 4) 채팅 검색 (전체 사용자)

- `GET /api/v1/chats/{videoId}?keyword=&username=&page=0&size=30`
- `keyword`(메시지 부분검색), `username`(닉네임 부분검색) 중 하나 이상 지정. 페이지네이션 지원.

```bash
curl "https://hiphant.pachylover.com/api/v1/chats/{videoId}?keyword=하이라이트&page=0&size=30"
```

### 5) 배너 / 안내문구 (공개 조회)

- `GET /api/v1/banners` — 활성 배너 목록
- `GET /api/v1/announcements` — 활성 안내문구 목록

## 관리자 API

관리자 API 는 JWT 인증(`Authorization: Bearer <token>`)이 필요합니다. (`ROLE_ADMIN`)

- `POST /api/v1/admin/auth/login` — `{ "username", "password" }` → `{ token, username, role }`
- `GET  /api/v1/admin/auth/me` — 토큰 유효성 확인
- `GET  /api/v1/admin/stats` — 통계(영상/하이라이트/채팅 수, 일자별 추이, 상위 영상)
- `GET  /api/v1/admin/highlights/recent?limit=20` — 최근 생성 하이라이트
- `GET/POST/PUT/DELETE /api/v1/admin/banners` — 배너 관리
- `GET/POST/PUT/DELETE /api/v1/admin/announcements` — 안내문구 관리

### 관리자 계정 생성 (부트스트랩)

`ADMIN_USERNAME` / `ADMIN_PASSWORD` 환경변수를 설정하고 앱을 실행하면, 동일 아이디가 없을 때 관리자 계정이 자동 생성됩니다(BCrypt 저장). 이후에는 환경변수를 제거해도 됩니다.

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
- `ADMIN_JWT_SECRET` — 관리자 JWT 서명 키(32바이트 이상 랜덤 권장)
- `ADMIN_USERNAME` / `ADMIN_PASSWORD` — 최초 관리자 계정 부트스트랩(선택)

### 3) 애플리케이션 실행

```bash
./gradlew bootRun
```

## 문서

- 설계 문서: `DESIGN.md`
- DDL: `docs/DDL.sql`
- Flyway 가이드: `docs/flyway.md`
