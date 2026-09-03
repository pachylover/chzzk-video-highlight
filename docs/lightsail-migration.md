# AWS Lightsail 이전 계획 (Railway + Supabase → Lightsail $12)

작성일: 2026-09-03 · 상태: 계획(미실행)

## 0. 지금 당장 (오늘 안에)

Supabase free plan 사용량을 이미 초과한 상태이므로, **프로젝트가 정지(pause)되기 전에 덤프부터 확보**한다.
정지된 뒤에는 복구까지 시간이 걸리고, 그 사이 이전 작업 자체가 막힌다.

```bash
# Supabase 대시보드 > Project Settings > Database > Connection string
# 5432 면 direct/pooler 어느 쪽이든 된다. 6543(transaction mode)만 pg_dump 가 안 된다.
# pooler 를 쓰면 사용자 이름이 postgres 가 아니라 postgres.<ref> 다. 자세한 내용은 postgres-backup.md
export SUPA="postgresql://postgres.<ref>@aws-1-ap-southeast-2.pooler.supabase.com:5432/postgres"

# 1) 크기부터 확인 (이전 시간/디스크 산정의 기준)
psql "$SUPA" -c "SELECT pg_size_pretty(pg_database_size(current_database()));"
psql "$SUPA" -c "
SELECT relname, pg_size_pretty(pg_total_relation_size(c.oid)) AS total,
       pg_size_pretty(pg_relation_size(c.oid)) AS heap
FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
WHERE n.nspname='public' AND c.relkind='r'
ORDER BY pg_total_relation_size(c.oid) DESC;"
psql "$SUPA" -c "SELECT count(*) FROM chats;"

# 2) 전체 덤프 (custom format — 병렬 복원 가능)
pg_dump "$SUPA" -Fc -f hiphant-$(date +%Y%m%d).dump --no-owner --no-privileges
```

덤프는 로컬 + 외부 스토리지(S3/구글드라이브) 두 곳에 둔다.

## 1. 권장 구성 — 프론트는 Vercel에 남긴다

| 구성요소 | 현재 | 이전 후 | 이유 |
| --- | --- | --- | --- |
| Postgres | Supabase free (한도 초과) | **Lightsail 인스턴스 내 Docker Postgres 15** | 병목의 원인. 용량 제한 사라짐 |
| API (Spring Boot) | Railway hobby | **같은 Lightsail 인스턴스** | DB와 같은 호스트 → 네트워크 왕복 제거, 대량 배치 insert에 유리 |
| 프론트 (Next.js) | Vercel free | **Vercel free 유지** | 옮길 이유가 없고, 2GB 인스턴스에서 RAM·빌드 메모리를 가장 많이 잡아먹는 부분 |

$12 플랜은 **2GB RAM / 2 vCPU(버스터블) / 60GB SSD / 3TB 전송**이다. API + Postgres 두 개만 올리면 여유가 있지만,
Next.js까지 같이 올리면 빠듯하다(아래 4번 메모리 예산 참고). 프론트를 Vercel에 두면 이전 범위도 절반으로 줄어
다운타임과 롤백 리스크가 함께 낮아진다.

> 프론트까지 반드시 한 서버로 모아야 한다면 4번의 "3개 모두 올리는 경우"를 따르되, **Next.js 빌드는 절대 서버에서 하지 않는다**
> (2GB에서 `next build`는 OOM 위험이 크다). GitHub Actions에서 이미지를 빌드해 GHCR에 올리고 서버는 pull만 한다.

## 2. 비용

| 항목 | 월 비용 |
| --- | --- |
| Lightsail 2GB 인스턴스 | $12 |
| 고정 IP | $0 (인스턴스에 연결되어 있는 동안) |
| 자동 스냅샷 (60GB 중 실사용분) | 약 $1~3 (GB당 $0.05) |
| Vercel free | $0 |
| **합계** | **약 $13~15/월** |

현재(Railway hobby $5 + free tier들)보다 $8~10 오르지만, Supabase 용량 한도와 Railway 실행시간 한도가 사라진다.

## 3. 인스턴스 준비

리전은 **ap-northeast-2(서울)** — 사용자가 국내이고 치지직 API도 국내다.
OS는 Ubuntu 24.04 LTS 블루프린트(“OS Only”)를 쓰고 Docker는 직접 설치한다.

```bash
# 접속 후
sudo apt-get update && sudo apt-get install -y ca-certificates curl gnupg
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu     # 재로그인 필요

# 스왑 4GB — 2GB 인스턴스에서는 필수 (JVM/PG가 순간적으로 튈 때 OOM Killer 방지)
sudo fallocate -l 4G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
sudo sysctl -w vm.swappiness=10 && echo 'vm.swappiness=10' | sudo tee /etc/sysctl.d/99-swap.conf
```

**방화벽**: Lightsail 콘솔의 네트워킹 탭에서 22/80/443만 연다. **5432는 절대 열지 않는다**(Postgres는 Docker 내부 네트워크로만 접근).
SSH는 가능하면 콘솔에서 내 IP로 제한한다.

## 4. 메모리 예산 (2GB)

API + Postgres만 올리는 경우 (권장):

| 프로세스 | 설정 | 상주 메모리 |
| --- | --- | --- |
| Postgres 15 | `shared_buffers=384MB`, `work_mem=8MB`, `max_connections=30` | ~500MB |
| Spring Boot API | `-Xmx640m -Xms256m` | ~750MB |
| OS + Docker + Caddy | | ~350MB |
| **합계** | | **~1.6GB** (+ 스왑 4GB 여유) |

3개 모두 올리는 경우: Postgres `shared_buffers=256MB`, API `-Xmx512m`, Next.js는 `output: 'standalone'`으로
빌드해 런타임 이미지를 줄인다(현재 Dockerfile은 `node_modules` 전체를 복사해 ~970MB). 이때 합계가 2GB에 근접하므로
스왑 의존도가 커진다.

`chats` 테이블에 걸린 `gin_trgm_ops` 인덱스 2개(message, username)가 메모리·디스크를 가장 많이 쓴다.
0번에서 확인한 인덱스 크기가 테이블보다 크다면 5번의 정리 작업을 먼저 한다.

## 5. 이전 전 DB 다이어트 (선택이지만 권장)

`chats`는 계속 증가하는 테이블이고, 사용자 폭주 상황에서는 증가 속도가 더 빨라진다.

```sql
-- 어떤 영상이 용량을 잡아먹는지
SELECT video_id, count(*) FROM chats GROUP BY video_id ORDER BY 2 DESC LIMIT 20;

-- 예: 하이라이트가 생성된 적 없는 영상의 채팅 정리
DELETE FROM chats c
WHERE NOT EXISTS (SELECT 1 FROM highlights h WHERE h.video_id = c.video_id);

VACUUM FULL chats;   -- 디스크 실제 반환 (테이블 잠금, 이전 직전에만)
```

장기적으로는 **보존 기간 정책**(예: 180일 지난 채팅 삭제 배치)을 두는 편이 좋다.
치지직도 일정 기간 후 채팅을 제공하지 않고, 앱에 이미 "채팅 다시 불러오기" 복구 경로가 있다.

## 6. 서버 구성 파일

`/opt/hiphant/docker-compose.yml` (프론트를 Vercel에 두는 구성):

```yaml
name: hiphant

services:
  postgres:
    image: postgres:15
    restart: unless-stopped
    environment:
      POSTGRES_DB: highlight
      POSTGRES_USER: highlight
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    command: >
      postgres -c shared_buffers=384MB -c work_mem=8MB
               -c maintenance_work_mem=128MB -c max_connections=30
               -c effective_cache_size=1GB
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U highlight -d highlight"]
      interval: 10s
      timeout: 5s
      retries: 12
    # 포트를 호스트에 노출하지 않는다 (내부 네트워크로만 접근)

  api:
    image: ghcr.io/pachylover/hiphant-api:latest   # CI에서 빌드해 push
    restart: unless-stopped
    depends_on:
      postgres: { condition: service_healthy }
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/highlight
      SPRING_DATASOURCE_USERNAME: highlight
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      ADMIN_JWT_SECRET: ${ADMIN_JWT_SECRET}
      GEMINI_API_KEY: ${GEMINI_API_KEY}
      RATE_LIMIT_PER_MINUTE: ${RATE_LIMIT_PER_MINUTE:-100}
      JAVA_OPTS: "-Xms256m -Xmx640m -XX:MaxRAMPercentage=70 -Dspring.profiles.active=prod"
    expose: ["8080"]

  caddy:
    image: caddy:2-alpine
    restart: unless-stopped
    ports: ["80:80", "443:443"]
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile:ro
      - caddydata:/data
    depends_on: [api]

volumes:
  pgdata:
  caddydata:
```

`/opt/hiphant/Caddyfile` — TLS 인증서를 자동으로 발급/갱신한다:

```
api.hiphant.pachylover.com {
    encode gzip
    reverse_proxy api:8080 {
        flush_interval -1        # SSE(진행상황 스트리밍)가 버퍼링되지 않도록 필수
    }
}
```

> `flush_interval -1`을 빼면 `/api/v1/highlights/{id}/stream`의 진행률 표시가 끊겨 보인다.

### 코드에서 함께 바꿔야 하는 것

1. `SecurityConfig.corsConfigurationSource()` 의 `setAllowedOrigins` — 프론트 도메인이 그대로면 수정 불필요,
   API 도메인을 새로 판다면 프론트 origin만 유지되면 된다. (API 도메인은 origin이 아니라 대상이다.)
2. Vercel 환경변수 `NEXT_PUBLIC_API_URL` → `https://api.hiphant.pachylover.com/api` 로 변경 후 **재배포**
   (`NEXT_PUBLIC_*`는 빌드 시점에 인라인되므로 재배포하지 않으면 반영되지 않는다).
3. `RateLimitFilter`는 `X-FORWARDED-FOR` 첫 IP를 쓴다 — Caddy가 이 헤더를 넣어주므로 그대로 동작한다.

## 7. 컷오버 절차

전날 준비:

- [ ] DNS TTL을 300초로 낮춘다 (하루 전).
- [ ] Lightsail 인스턴스 + 고정 IP + 방화벽 구성 완료.
- [ ] `api.hiphant.pachylover.com` A 레코드를 고정 IP로 미리 만들어 두고 Caddy 인증서 발급까지 확인.
- [ ] 0번의 덤프로 **리허설 복원**을 끝내 둔다 (소요 시간 실측 = 실제 다운타임 추정치).

```bash
# 리허설/실제 복원 공통
docker compose up -d postgres
docker compose exec -T postgres psql -U highlight -d highlight -c "CREATE EXTENSION IF NOT EXISTS pgcrypto; CREATE EXTENSION IF NOT EXISTS pg_trgm;"
cat hiphant-YYYYMMDD.dump | docker compose exec -T postgres pg_restore -U highlight -d highlight --no-owner --no-privileges -j 2
docker compose exec -T postgres psql -U highlight -d highlight -c "ANALYZE;"   # 복원 후 통계 갱신 필수
```

당일 (예상 다운타임: 덤프 크기에 비례, 1GB 기준 10~20분):

1. 안내문구(관리자 > 안내문구 관리)에 점검 공지 등록.
2. Railway API를 정지해 **쓰기를 멈춘다** (하이라이트 생성/채팅 수집이 도는 중에 덤프하면 데이터가 갈린다).
3. 최종 `pg_dump` → Lightsail로 전송 → `pg_restore`.
4. `docker compose up -d api caddy` → Flyway가 자동 실행된다.
   V5까지 이미 반영된 덤프라면 아무 것도 하지 않고 통과한다.
5. 스모크 테스트 (아래 8번).
6. Vercel `NEXT_PUBLIC_API_URL` 변경 후 재배포.
7. 정상 확인 후 안내문구 내리기.
8. **48시간은 Railway/Supabase를 지우지 않는다** (롤백 경로).

롤백: Vercel 환경변수를 원래 값으로 되돌리고 재배포 + Railway 재기동. 컷오버 이후 새로 쌓인 데이터는 유실되므로,
롤백 판단은 컷오버 후 30분 이내에 내린다.

## 8. 스모크 테스트

```bash
BASE=https://api.hiphant.pachylover.com/api
curl -s "$BASE/v1/banners" | head -c 200                       # 공개 API + DB 연결
curl -s "$BASE/v1/videos/<videoId>" | head -c 200              # 치지직 아웃바운드
curl -s "$BASE/v1/highlights/<videoId>" | head -c 200          # 복원된 하이라이트 조회
curl -s "$BASE/v1/chats/<videoId>?keyword=ㅋㅋ&page=0&size=5"   # trgm 인덱스 동작
curl -N "$BASE/v1/highlights/<videoId>/stream" &               # SSE 가 흐르는지 (Caddy 버퍼링 확인)
curl -s -X POST "$BASE/v1/admin/auth/login" -H 'Content-Type: application/json' \
     -d '{"username":"...","password":"..."}'                  # 관리자 로그인
```

프론트에서는 홈 최근 하이라이트, 하이라이트 생성(진행률 표시), 채팅 검색, 관리자 페이지를 눈으로 확인한다.

## 9. 이전 후 상시 운영

**백업** — 스냅샷만으로는 부족하다(파일 시스템 시점 백업이라 복구 단위가 크다). 둘 다 건다.
설치·cron 등록·S3 설정·복원 리허설까지의 상세 절차는 **[postgres-backup.md](postgres-backup.md)** 에 따로 정리했다.

```bash
# /opt/hiphant/backup.sh  — cron: 0 4 * * *
set -e
cd /opt/hiphant
docker compose exec -T postgres pg_dump -U highlight -Fc highlight > /opt/backup/hiphant-$(date +\%F).dump
find /opt/backup -name 'hiphant-*.dump' -mtime +14 -delete
aws s3 cp /opt/backup/hiphant-$(date +\%F).dump s3://<bucket>/hiphant/   # 또는 rclone
```

- Lightsail 자동 스냅샷을 켠다 (콘솔에서 일일 스냅샷, 7일 보관).
- **복원 리허설을 분기 1회** 한다. 해보지 않은 백업은 백업이 아니다. (절차: [postgres-backup.md](postgres-backup.md) 6-3)

**모니터링**

- Lightsail 알람: CPU 사용률, **버스트 용량(burst capacity)** — 이 플랜은 버스터블이라 채팅 수집처럼 CPU를 오래 쓰는
  작업이 겹치면 버스트가 소진되며 급격히 느려진다. 수집이 몰리는 시간대의 그래프를 첫 주에 반드시 확인한다.
- 디스크 사용률 알람 (60GB의 70%).
- 외부 업타임 체크(UptimeRobot 등)로 `https://api.hiphant.pachylover.com/actuator/health` 감시.
  (actuator가 이미 의존성에 있다. 엔드포인트 노출 설정만 확인한다.)
- `docker compose logs` 가 무한히 쌓이지 않도록 로그 로테이션 설정:
  `/etc/docker/daemon.json` → `{"log-driver":"json-file","log-opts":{"max-size":"10m","max-file":"3"}}`

**배포 파이프라인** — 서버에서 빌드하지 않는다.

1. GitHub Actions에서 `docker build` → GHCR push (태그: 커밋 SHA).
2. 서버에서 `docker compose pull && docker compose up -d`.
3. 롤백은 이전 SHA 태그로 `up -d`.

## 10. 언제 $12를 넘겨야 하나

다음 중 하나라도 지속되면 4GB($24) 플랜으로 올린다. 스왑으로 버티는 상태를 오래 끌면 응답 지연으로 나타난다.

- 스왑 사용량이 상시 500MB 이상
- 버스트 용량이 매일 소진
- 채팅 수집 중 API 응답 지연이 체감될 정도

디스크가 먼저 문제가 되면(60GB의 70% 초과) 5번의 보존 정책을 먼저 적용한다.
