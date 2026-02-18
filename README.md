# CHZZK 하이라이트 서비스

이 저장소는 방송 채팅을 `api.chzzk`에서 수집하여 Postgres에 저장하고, 1분 단위 집계(player_message_time 기반 epoch ms)로 가장 채팅이 집중된 분을 찾아 하이라이트 메타데이터와 Gemini(요약 모델)로 생성한 제목/요약을 제공하는 API 서버입니다.

## 빠른 시작 (로컬)

1. Postgres 인스턴스 시작(권장 이미지: `postgres:15`). `compose.yaml`을 참고하거나 환경에 맞게 조정하세요.

2. `src/main/resources/application.yaml`에 DB 접속정보를 설정하세요.

3. 애플리케이션 실행:

```
./gradlew bootRun
```

4. 하이라이트 생성 요청 보내기:

```
POST /api/v1/highlights
{ "url": "https://.../watch?v=VIDEO_ID" }
```

5. 결과 확인: `GET /api/v1/highlights/{taskId}`로 폴링하거나 `callbackUrl`을 등록하여 Webhook으로 받으세요.

## 참고
- Flyway 마이그레이션 파일: `src/main/resources/db/migration` (V1: 초기 테이블, V2: `message_time`, `player_message_time`, 고유 인덱스 추가).
- 로컬 개발용으로 `MockChzzkClient` 및 `MockGeminiClient`가 제공됩니다. 실제 통합 시에는 `client/` 아래에 실 구현(`ChzzkClientImpl`)을 사용하세요.
- 상세 아키텍처, DB 스키마 및 Gemini 프롬프트는 `DESIGN.md`를 참고하세요.

---

원하시면 OpenAPI 스펙(엔드포인트/응답 예시)을 한글로 정리해 드리거나, `api.chzzk`의 실제 클라이언트 구현을 바로 진행하겠습니다.
