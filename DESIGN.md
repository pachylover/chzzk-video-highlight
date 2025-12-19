# CHZZK 하이라이트 API — 설계 문서

> 짧게: 이 프로젝트는 인터넷 방송의 채팅을 자동으로 수집·분석하여 ‘하이라이트(가장 채팅이 많았던 분 단위 구간)’를 생성하고, 해당 구간과 요약(제목 포함)을 반환하는 API 서버입니다.

---

## 목표 (MVP) ✅
- 클라이언트가 비디오 URL을 보내면 비디오 ID를 추출
- `api.chzzk`에 요청하여 해당 비디오의 채팅 데이터를 재귀적으로(페이징 끝까지) 수집
- TimescaleDB에 채팅 저장 (하이퍼테이블, 타임스탬프 기반)
- 1분(time_bucket('1 minute')) 단위로 집계하여 채팅이 가장 많은 분을 찾음
- 그 분을 기반으로 하이라이트 메타데이터를 생성하고 `highlights` 테이블에 저장
- 사용자에게 채팅 원본 + 해당 분의 요약(제목 포함, Gemini로 생성)을 반환

## 아키텍처 (개요) 🔧
- API 서버 (Spring Boot / Kotlin 또는 Java) — 클라이언트 요청 수신, 입력 검증, 작업 생성
- 작업 큐 (Redis + Bull/Resque 혹은 Spring Task Executor + Redis) — 수집/처리 비동기화
- 워커(백그라운드) — api.chzzk 호출, 페이징/재귀 수집, DB 저장, 집계, 요약 호출
- TimescaleDB (Postgres 확장) — 채팅 시계열 데이터 보관 및 집계
- 모델요약 (Gemini) — 채팅 텍스트 요약 및 제목 생성
- 옵저버빌리티: 로깅, 메트릭(Prometheus), 에러 (Sentry)

## 데이터 흐름 (시퀀스) 🔁
1. 클라이언트 -> POST /api/v1/highlights { url }
2. API 서버: URL 검증, 비디오ID 추출, 새 작업 생성 → 202 Accepted (task_id)
3. 워커: `api.chzzk` 호출해서 채팅 데이터 재귀 수집(페이징 끝까지)
4. 채팅을 TimescaleDB에 배치로 저장 (chat rows)
5. 집계 쿼리로 가장 많은 채팅이 발생한 1분 구간을 찾음
6. 하이라이트 범위(예: 해당 분 시작 -30s ~ +90s) 설정, `highlights` 테이블에 메타 저장
7. Gemini에 채팅 데이터(요약 대상) 전송 → 제목 + 요약 수신
8. 하이라이트에 요약 저장 및 최종 상태 `done`으로 표시
9. 클라이언트는 GET /api/v1/highlights/{id} 또는 Webhook으로 결과 수신

## API 설계 (예시) 🧭
### POST /api/v1/highlights
- 설명: 하이라이트 생성 요청
- 요청 본문:
```json
{ "url": "https://.../watch?v=VIDEO_ID", "callback_url": "https://.../hook" }
```
- 응답: 202 Accepted
```json
{ "task_id": "uuid", "status": "accepted" }
```

### GET /api/v1/highlights/{task_id}
- 응답 (예: 완료됨):
```json
{
  "task_id": "...",
  "status": "done",
  "video_id": "abc123",
  "highlight": {
    "start": "2025-12-19T12:34:00Z",
    "end": "2025-12-19T12:36:30Z",
    "minute": "2025-12-19T12:35:00Z",
    "chat_count": 254,
    "title": "격렬한 댓글 폭발: 추격전의 순간",
    "summary": "요약 텍스트...",
    "chat_snippet": [ { "ts": "...", "user": "x", "msg": "..." }, ... ]
  }
}
```

### Errors
- 400: invalid URL
- 422: unsupported video host
- 429: rate limit
- 500: server error

## DB 스키마 (Timescale 권장) 🗄️
### chats (하이퍼테이블)
- id: uuid (PK)
- video_id: text
- ts: timestamptz (indexed, hypertable time column)
- user_id: text
- username: text
- message: text
- raw: jsonb (원본 이벤트)
- created_at: timestamptz default now()

SQL (요약):
```sql
CREATE TABLE chats (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  video_id text NOT NULL,
  ts timestamptz NOT NULL,
  user_id text,
  username text,
  message text,
  raw jsonb,
  created_at timestamptz DEFAULT now()
);
SELECT create_hypertable('chats', 'ts');
CREATE INDEX ON chats (video_id, ts DESC);
```

### highlights
- id: uuid (PK)
- video_id: text
- minute: timestamptz (time_bucket start)
- start_ts: timestamptz
- end_ts: timestamptz
- chat_count: integer
- title: text
- summary: text
- chat_snapshot: jsonb (대표 채팅 또는 요약용 데이터)
- status: enum('pending','processing','done','failed')
- created_at, updated_at

SQL (요약):
```sql
CREATE TABLE highlights (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  video_id text NOT NULL,
  minute timestamptz NOT NULL,
  start_ts timestamptz,
  end_ts timestamptz,
  chat_count integer,
  title text,
  summary text,
  chat_snapshot jsonb,
  status text,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);
CREATE INDEX ON highlights (video_id, minute);
```

## 핵심 쿼리 (예시) 🔍
- 최다 채팅 1분 찾기:
```sql
SELECT time_bucket('1 minute', ts) AS minute, count(*) AS cnt
FROM chats
WHERE video_id = $1
GROUP BY minute
ORDER BY cnt DESC
LIMIT 1;
```
- 범위 선택(예: 해당 minute 중심으로 앞뒤 몇 초 설정)

## 채팅 수집 (api.chzzk) 구현 지침 🧩
- API 정보(실제 사용한 엔드포인트):
  - GET http://api.chzzk.naver.com/service/v1/videos/{videoId}/chats?playerMessageTime={time}&previousVideoChatSize=50
  - 응답 예시에서 `content.nextPlayerMessageTime`을 다음 페이지 커서로 사용하며 `videoChats` 배열을 반복해서 가져옵니다.
- 응답 필드 매핑 요약:
  - `messageTime` (ms) -> `chats.message_time` (bigint), DB/엔티티 `Chat.messageTime`
  - `playerMessageTime` -> `chats.player_message_time` (bigint), DB/엔티티 `Chat.playerMessageTime`
  - `userIdHash` -> `chats.user_id`
  - `content` -> `chats.message`
  - `profile` (JSON 문자열) -> `chats.username`(nickname 추출, 선택적), 전체 `videoChat` 객체는 `chats.raw`(jsonb)에 보관
- 배치/중복 처리
  - 한 번에 여러 페이지를 받아 `List<Chat>`으로 매핑 후 DB에 배치로 저장(예: 500건 단위)
  - 중복 방지: DB에 `message_time` 기준으로 이미 존재하는 레코드를 조회하여 제외하거나, 고유 인덱스 (`uniq_chats_video_message_user` on (video_id, message_time, user_id))로 중복 삽입을 막음
- 오류/재시도
  - HTTP 429: exponential backoff(예: 500ms * 2^retry, 최대 5회)
  - 5xx: 재시도 제한(최대 5회)
- 구현체: `ChzzkClientImpl` (WebClient 사용), `HighlightService`에서 수집 결과를 받아 중복 제거 후 `ChatRepository.saveAll(...)`로 배치 저장

## 하이라이트 구간 결정 전략 🧠
- 기본: 가장 채팅이 많았던 1분을 찾고, 그 중심을 기준으로 전후(예: -30s/+90s)로 확장
- 옵션: peak minute 주변의 상위 N분을 합쳐서 구간을 넓히기
- 고려사항: 가중치(채팅 수), 연속성(연속된 높은 빈도 구간)

## 요약(Gemini) 프롬프트 제안 ✍️
- 입력: 해당 분의 채팅 텍스트(상위 n개, 또는 시간순 샘플), 컨텍스트(게임/쇼 정보), 출력 형식 요청(제목 한 줄 + 2-3문단 요약)
- 예시:
```
You are an assistant. Summarize the chat messages into a concise title (max 10 words) and a 2-3 sentence summary in Korean that captures the key events, reactions, and sentiment.
Messages:
- [12:35:01] user1: ...
- [12:35:10] user2: ...
```
- 안전성: 모델 출력 검증, 허위정보 필터링, 금지어 필터링

## 운영/배포 고려사항 🚀
- 비동기 우선: 수집/처리 길어질 수 있으므로 202+polling/webhook 방식
- 스케일: 워커 수 수준에서 수집 병렬화, DB connection pool과 bulk insert 조절
- 비용: Gemini 호출 빈도 최소화 (요약은 하이라이트당 1회)
- 모니터링: 작업 처리 시간, 실패율, 수집 API 호출률

## 보안 & 개인정보 🛡️
- 입력 URL 검증, 호스트 화이트리스트
- API Key 인증 (Bearer Token)
- 민감정보 필터링(채팅 내 개인식별정보 제거/마스킹)
- 비밀값은 Vault/환경변수로 관리

## 테스팅 계획 ✅
- 유닛: 비디오ID 추출, time_bucket 쿼리, 요약 프롬프트 생성
- 통합: Mock `api.chzzk`로 전체 pipeline 시뮬레이션
- E2E: 로컬 Timescale + 워커로 실제 데이터 수집부터 요약까지 검증

## MVP 범위 및 향후 개선 아이디어 💡
- MVP: URL로부터 비디오ID 추출 → 채팅 수집 → 하이라이트 발견 → 요약 생성 → API 제공
- 추가 기능: 하이라이트 비디오 클립 자동 생성(연동 서비스 필요), UI 대시보드, 사용자 피드백 반영한 제목 튜닝

---

## 다음 단계 (권장 순서) ▶️
1. API 계약(위의 엔드포인트) 확정 및 요청/응답 스펙 정리
2. TimescaleDB 스키마 적용 및 초기 테스트 데이터 적재
3. `api.chzzk` 통신 모듈 구현(페이징/재시도 포함)
4. 워커 + 작업 큐 구현, 로컬 E2E 테스트
5. Gemini 연결 및 프롬프트 튜닝
6. 모니터링/알림 구성

---

문서 검토 후 반영할 내용(우선순위, 요약 수준, 하이라이트 시간 범위 조정 등)을 알려주시면 설계 반영해서 업데이트하겠습니다.