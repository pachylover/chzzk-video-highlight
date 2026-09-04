# Lightsail 배포 실행 순서

덤프를 받았고 인스턴스를 만든 시점부터의 순서다. **1~7번은 운영 서비스에 아무 영향이 없다**(리허설).
실제 전환은 8번 하나뿐이고, 그 전까지는 언제든 멈추거나 며칠 뒤에 이어서 해도 된다.

명령 앞의 **[내 PC]** / **[서버]** 가 입력하는 위치다. 서버는 `cd /opt/hiphant` 기준.

관련 문서: [../docs/lightsail-migration.md](../docs/lightsail-migration.md) (계획·근거) ·
[../docs/postgres-backup.md](../docs/postgres-backup.md) (백업 상세)

---

## 1. 받아 둔 덤프부터 살펴본다 [내 PC]

Supabase 전체 덤프에는 `auth`, `storage`, `realtime` 같은 Supabase 전용 스키마가 섞여 있다.
우리가 옮길 것은 `public` 뿐이다.

```bash
cd ~/hiphant-backup    # 덤프를 받아 둔 곳

# 어떤 스키마의 테이블이 들어 있는지
pg_restore --list hiphant-*.dump | grep "TABLE DATA"
# public 의 chats / highlights / banners / announcements / admin_users /
# flyway_schema_history 가 보이면 정상

# 확장이 어느 스키마에 설치돼 있는지 — 복원 성패가 여기서 갈린다
psql "$SUPA" -c "SELECT e.extname, n.nspname AS schema
                 FROM pg_extension e JOIN pg_namespace n ON n.oid = e.extnamespace
                 WHERE e.extname IN ('pg_trgm','pgcrypto');"
```

마지막 쿼리 결과를 적어 둔다. `chats`의 검색 인덱스가 `gin_trgm_ops`를 쓰기 때문에,
**대상 서버에도 같은 스키마에 `pg_trgm`이 있어야** 인덱스 복원이 통과한다. (보통 `public`)

---

## 2. 서버 기본 세팅 [서버]

```bash
ssh -i ~/LightsailDefaultKey.pem ubuntu@43.202.59.44

# 시간대 — 이후 cron 과 로그가 전부 KST 기준이 된다
sudo timedatectl set-timezone Asia/Seoul

# Docker
sudo apt-get update
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu     # 적용하려면 로그아웃 후 재접속

# 스왑 4GB — 2GB 인스턴스에서는 필수. 없으면 피크에 OOM Killer 가 JVM 을 죽인다.
sudo fallocate -l 4G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
echo 'vm.swappiness=10' | sudo tee /etc/sysctl.d/99-swap.conf && sudo sysctl -p /etc/sysctl.d/99-swap.conf

# 컨테이너 로그가 디스크를 채우지 않도록 (compose 에도 걸어 뒀지만 기본값도 바꿔 둔다)
sudo tee /etc/docker/daemon.json > /dev/null <<'JSON'
{ "log-driver": "json-file", "log-opts": { "max-size": "10m", "max-file": "3" } }
JSON
sudo systemctl restart docker

# 확인
free -h        # Swap 4.0Gi 가 보여야 한다
docker version
```

**Lightsail 콘솔 → 네트워킹 → IPv4 방화벽**: `22`, `80`, `443`만 남긴다.
**5432는 열지 않는다** — DB는 compose 내부 네트워크로만 접근한다.

> 22번을 "제한된 소스 IP"로 잠그면 **GitHub Actions 배포가 접속하지 못한다**(러너 IP 가 매번 바뀐다).
> Actions 로 배포할 거라면 22번은 열어 두고 비밀번호 로그인 없이 키 인증만 쓴다
> (`sudo sed -i 's/^#\?PasswordAuthentication.*/PasswordAuthentication no/' /etc/ssh/sshd_config && sudo systemctl reload ssh`).

---

## 3. 배포 파일 올리고 .env 작성

**[내 PC]**

```bash
cd ~/hiphant/hiphant-api/deploy
ssh -i ~/LightsailDefaultKey.pem ubuntu@43.202.59.44 'sudo mkdir -p /opt/hiphant/logs/caddy /opt/backup && sudo chown -R ubuntu:ubuntu /opt/hiphant /opt/backup && sudo chown -R 10001:10001 /opt/hiphant/logs'
scp -i ~/LightsailDefaultKey.pem docker-compose.yml Caddyfile .env.example backup.sh ubuntu@43.202.59.44:/opt/hiphant/
```

**[서버]**

```bash
cd /opt/hiphant
cp .env.example .env && chmod 600 .env

# 비밀번호/시크릿 생성 — 출력값을 .env 에 붙여넣는다
openssl rand -base64 24    # DB_PASSWORD
openssl rand -base64 48    # ADMIN_JWT_SECRET

nano .env
```

> 위 명령의 `chown 10001` 은 컨테이너 사용자에게 로그 디렉터리 쓰기 권한을 주는 것이다.
> 자세한 내용은 아래 "로그와 데이터가 어디에 남는가" 참고.

`.env`에서 최소한 채워야 하는 것: `API_DOMAIN`, `DB_PASSWORD`, `ADMIN_JWT_SECRET`,
`ADMIN_USERNAME`/`ADMIN_PASSWORD`(최초 계정), `API_IMAGE`.

> 값에 따옴표나 공백을 넣지 않는다. `backup.sh` 가 이 파일을 그대로 source 하므로
> 공백이 들어가면 깨진다. `openssl rand -base64` 출력은 안전하다.

---

## 4. DB 먼저 띄우고 덤프 복원 [서버 + 내 PC]

여기가 다운타임을 결정하는 단계다. **지금 한 번 리허설로 해보고 걸린 시간을 재 둔다.**

**[내 PC]** 덤프 전송:

```bash
scp -i ~/LightsailDefaultKey.pem ~/hiphant-backup/hiphant-*.dump ubuntu@43.202.59.44:/opt/backup/
```

**[서버]** 복원:

```bash
cd /opt/hiphant
docker compose up -d postgres
docker compose ps          # postgres 가 healthy 가 될 때까지 대기

# 1번에서 확인한 스키마에 확장을 만든다 (보통 public)
docker compose exec -T postgres psql -U highlight -d highlight -c \
  "CREATE EXTENSION IF NOT EXISTS pgcrypto; CREATE EXTENSION IF NOT EXISTS pg_trgm;"

# 만약 1번 결과가 extensions 스키마였다면 대신 이렇게:
#   CREATE SCHEMA IF NOT EXISTS extensions;
#   CREATE EXTENSION IF NOT EXISTS pg_trgm SCHEMA extensions;
#   CREATE EXTENSION IF NOT EXISTS pgcrypto SCHEMA extensions;
#   ALTER DATABASE highlight SET search_path TO "$user", public, extensions;

# public 스키마만 복원한다 (auth/storage 등 Supabase 전용 스키마는 제외)
time docker compose exec -T postgres pg_restore \
  -U highlight -d highlight \
  --schema=public --no-owner --no-privileges -j 2 \
  < /opt/backup/hiphant-*.dump

# 복원 직후 통계 갱신 — 빼먹으면 채팅 검색이 몇 배 느려진다
docker compose exec -T postgres psql -U highlight -d highlight -c "ANALYZE;"
```

> `pg_restore`가 몇 줄의 에러를 뱉는 것은 정상이다(이미 있는 확장, Supabase 롤 관련).
> 마지막의 `errors ignored on restore: N` 숫자만 보고 판단하지 말고, 아래 검증을 돌린다.

**검증 [서버]**

```bash
docker compose exec -T postgres psql -U highlight -d highlight -c "
SELECT 'chats' t, count(*) FROM chats
UNION ALL SELECT 'highlights', count(*) FROM highlights
UNION ALL SELECT 'banners', count(*) FROM banners
UNION ALL SELECT 'announcements', count(*) FROM announcements
UNION ALL SELECT 'admin_users', count(*) FROM admin_users;"

# 인덱스가 다 살아 있는지 — chats 에 6개(pkey 포함), highlights 에 3개가 나와야 한다.
#   주의: \di chats* 는 "이름이 chats 로 시작하는 인덱스"만 찾는다. 인덱스 이름이 idx_ 로
#   시작하므로 그 명령으로는 chats_pkey 하나만 보인다. 테이블 기준으로 조회해야 한다.
docker compose exec -T postgres psql -U highlight -d highlight -c \
  "SELECT tablename, indexname FROM pg_indexes
   WHERE tablename IN ('chats','highlights') ORDER BY tablename, indexname;"

# Flyway 이력 — V4 까지 있어야 정상이다. V5 는 새 API 가 뜰 때 자동 적용된다.
docker compose exec -T postgres psql -U highlight -d highlight -c \
  "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

행 수가 Supabase 쪽과 같으면 복원 성공이다.

---

## 5. API 이미지 만들기

**서버에서 빌드하지 않는다.** 2GB에서 Gradle 빌드는 메모리도 시간도 위험하다.
둘 중 하나를 고른다.

### 방법 A — 로컬 빌드 후 전송 (지금 바로 되는 방법) [내 PC]

```bash
cd ~/hiphant/hiphant-api
TAG=$(git rev-parse --short HEAD)
docker build -t hiphant-api:$TAG .

# 이미지를 그대로 서버로 밀어 넣는다 (레지스트리 불필요, 압축 전송)
docker save hiphant-api:$TAG | gzip | \
  ssh -i ~/LightsailDefaultKey.pem ubuntu@43.202.59.44 'gunzip | docker load'

echo "API_IMAGE=hiphant-api:$TAG"   # 이 값을 서버 .env 의 API_IMAGE 에 넣는다
```

### 방법 B — GitHub Actions (이후 상시 배포용)

워크플로 두 개가 들어 있다. **워크플로 = `.github/workflows/*.yml` 파일 하나**이고,
GitHub 가 정해진 시점에 자기네 서버(runner)에서 실행한다. 결과는 저장소 **Actions 탭**에서 본다.

| 파일 | 언제 도는가 | 무엇을 하는가 |
| --- | --- | --- |
| `publish-image.yml` | master 에 push 될 때 자동 | 이미지를 빌드해 GHCR 에 올린다 (`latest`, `sha-xxxxxxx` 두 태그) |
| `deploy.yml` | **Actions 탭에서 버튼을 눌러 수동** | 서버에 SSH 로 붙어 이미지를 교체하고 기동을 확인한다 |

빌드는 자동, 배포는 수동으로 시작하는 구성이다. 익숙해지면 `deploy.yml` 의 `push:` 주석을
풀어 자동 배포로 바꾸면 된다.

**한 번만 하는 준비**

1. **서버에서 GHCR 로그인** — 패키지가 private 인 경우. GitHub → Settings → Developer settings →
   Personal access tokens (classic) → `read:packages` 권한으로 발급.
   ```bash
   echo <PAT> | docker login ghcr.io -u pachylover --password-stdin
   ```
   (또는 저장소 Packages 설정에서 이미지를 public 으로 바꾸면 로그인 자체가 불필요하다.)

2. **저장소에 시크릿 등록** — 저장소 → Settings → Secrets and variables → Actions →
   **New repository secret**. 두 개를 만든다.

   | 이름 | 값 |
   | --- | --- |
   | `LIGHTSAIL_HOST` | 서버 고정 IP |
   | `LIGHTSAIL_SSH_KEY` | SSH 개인키 **전문** (`-----BEGIN`부터 `-----END OPENSSH PRIVATE KEY-----`까지, 줄바꿈 포함) |

   Lightsail 기본 키는 콘솔 → 계정 → SSH 키에서 받은 `.pem` 파일이다.
   `cat ~/LightsailDefaultKey.pem` 결과를 통째로 붙여넣는다.

3. **22번 포트가 열려 있는지 확인** — 2번 항목의 경고 참고.

**배포하는 법**

1. 저장소 → **Actions** 탭 → 왼쪽에서 **deploy** 선택 → 우측 **Run workflow** 버튼
2. `image_tag` 에 `latest` (또는 특정 커밋의 `sha-abc1234`) 입력 → 초록 버튼
3. 실행 로그가 실시간으로 뜬다. 각 단계를 펼치면 서버에서 나온 출력이 그대로 보인다.
4. 마지막 "기동 확인" 단계가 초록이면 배포 완료다. 실패하면 서버 로그 60줄을 자동으로 찍어준다.

**롤백**: 같은 방식으로 이전 `sha-xxxxxxx` 태그를 넣고 다시 실행하면 된다.
`deploy.yml` 이 서버 `.env` 의 `API_IMAGE` 를 그 태그로 바꾸고 컨테이너를 갈아끼운다.

**Actions 가 처음이라면 알아둘 것**
- 워크플로 파일은 **기본 브랜치(master)에 있어야** Actions 탭에 나타난다. PR 상태로는 수동 실행 버튼이 안 보인다.
- 시크릿은 등록 후 값을 다시 볼 수 없다. 로그에도 `***` 로 가려진다.
- 실행이 실패하면 빨간 X 를 눌러 어느 단계에서 멈췄는지 본다. 다시 실행은 우측 **Re-run jobs**.

---

## 6. DNS 연결하고 전체 기동

1. DNS에 **A 레코드** 추가: `hiphant-backend.pachylover.com` → `43.202.59.44` (TTL 300)
   — 프론트가 쓰는 `hiphant.pachylover.com`(Vercel)은 **건드리지 않는다**.
2. 전파 확인: **[내 PC]** `getent ahosts hiphant-backend.pachylover.com`

**[서버]**

```bash
cd /opt/hiphant
docker compose up -d
docker compose logs -f caddy    # "certificate obtained successfully" 가 보이면 TLS 발급 완료
docker compose logs -f api      # Flyway 가 V5 를 적용하는 로그 확인
```

> 첫 기동 시 `VideoMetaBackfillRunner`가 기존 하이라이트의 채널 정보를 치지직 API로 채운다.
> 영상 수만큼 0.3초 간격으로 호출하므로 로그에 `영상 정보 백필 시작 - 대상 N건` 이 뜨고
> 잠시 뒤 `백필 완료`가 나온다. 서비스 동작에는 영향이 없다.

---

## 7. 스모크 테스트 [내 PC]

```bash
BASE=https://hiphant-backend.pachylover.com/api

curl -s https://hiphant-backend.pachylover.com/actuator/health    # {"status":"UP"}
curl -s $BASE/v1/banners | head -c 200                # DB 연결 + 복원 데이터
curl -s $BASE/v1/highlights/recent?limit=3 | head -c 300
curl -s $BASE/v1/videos/<실제videoId> | head -c 200   # 치지직 아웃바운드
curl -s "$BASE/v1/chats/<실제videoId>?keyword=ㅋㅋ&page=0&size=5" | head -c 200   # trgm 인덱스
curl -s -X POST $BASE/v1/admin/auth/login \
  -H 'Content-Type: application/json' -d '{"username":"...","password":"..."}'

# SSE 가 버퍼링 없이 흐르는지 (Caddy flush_interval 확인)
curl -N $BASE/v1/highlights/<실제videoId>/stream
```

`recent`와 `banners`가 **복원한 데이터**를 돌려주면 DB·API·프록시·TLS가 모두 정상이다.

---

## 8. 컷오버 (실제 전환) [혼합]

여기서부터가 되돌리기 어려운 구간이다. 예상 소요는 4번에서 잰 복원 시간 + 10분.

1. **[관리자 페이지]** 안내문구에 점검 공지 등록.
2. **[Railway]** API 정지 — **쓰기를 멈춘다.** 수집/생성이 도는 중에 덤프하면 데이터가 갈린다.
3. **[내 PC]** 최종 덤프 → 전송:
   ```bash
   pg_dump "$SUPA" -Fc --no-owner --no-privileges -f final-$(date +%Y%m%d-%H%M).dump
   scp -i ~/LightsailDefaultKey.pem final-*.dump ubuntu@43.202.59.44:/opt/backup/
   ```
4. **[서버]** 리허설로 채운 DB를 비우고 최종 덤프로 다시 복원:
   ```bash
   docker compose stop api
   docker compose exec -T postgres psql -U highlight -d postgres \
     -c "DROP DATABASE highlight;" -c "CREATE DATABASE highlight OWNER highlight;"
   # 4번의 확장 생성 → pg_restore → ANALYZE 를 그대로 반복
   docker compose up -d api
   ```
5. **[내 PC]** 4번의 검증 쿼리 + 7번 스모크 테스트 재실행.
6. **[Vercel]** 환경변수 `NEXT_PUBLIC_API_URL` → `https://hiphant-backend.pachylover.com/api`
   변경 후 **재배포**. (`NEXT_PUBLIC_*`는 빌드 시점에 인라인되므로 재배포하지 않으면 반영되지 않는다.)
7. 브라우저로 홈 → 하이라이트 생성 → 채팅 검색 → 관리자 페이지 확인. 안내문구 내리기.
8. **Railway와 Supabase는 48시간 동안 지우지 않는다.**

**롤백**: Vercel 환경변수를 원래대로 되돌리고 재배포 + Railway 재기동.
컷오버 후 새로 쌓인 데이터는 유실되므로 **판단은 30분 안에** 한다.

---

## 9. 안정화 후 (당일 안에)

```bash
chmod +x /opt/hiphant/backup.sh
/opt/hiphant/backup.sh               # sudo 없이. 손으로 한 번 성공시킨다

crontab -e
#   10 4 * * * /opt/hiphant/backup.sh >> /opt/backup/backup.log 2>&1

# 다음 날 실행됐는지 확인
tail /opt/backup/backup.log
```

> cron 은 `ubuntu` 사용자로 돌린다(`sudo crontab` 아님). `/var/log` 는 쓸 수 없으므로
> 로그도 `/opt/backup` 에 남긴다. `ubuntu` 가 docker 그룹에 있어야 하므로 2번의 `usermod` 후
> 재접속했는지 확인한다.

- **Lightsail 콘솔** → 스냅샷 → 자동 스냅샷 켜기 (일일, 7일 보관)
- **Lightsail 콘솔** → 지표 → 알람: CPU 사용률, **버스트 용량**, 디스크 사용률(70%)
- **UptimeRobot 등**: `https://hiphant-backend.pachylover.com/actuator/health` 5분 간격
- **healthchecks.io**: `.env`의 `BACKUP_PING_URL` 채우기 (백업이 조용히 실패하는 것을 막는다)

첫 주에는 수집이 몰리는 시간대의 **버스트 용량 그래프**를 반드시 확인한다.
매일 소진되거나 스왑이 상시 500MB 이상이면 4GB($24) 플랜으로 올린다.

---

## 로그와 데이터가 어디에 남는가

배포로 컨테이너를 갈아끼워도 남아야 하는 것들이다.

| 무엇 | 호스트 위치 | 종류 | 사라지는 조건 |
| --- | --- | --- | --- |
| **API 로그** | `/opt/hiphant/logs/hiphant.log` | 바인드 마운트 | 직접 지울 때만 |
| Caddy 액세스 로그 | `/opt/hiphant/logs/caddy/access.log` | 바인드 마운트 | 직접 지울 때만 |
| **DB 데이터** | `hiphant_pgdata` 볼륨 | 이름 있는 볼륨 | `docker compose down -v` |
| TLS 인증서 | `hiphant_caddydata` 볼륨 | 이름 있는 볼륨 | `docker compose down -v` |
| DB 백업 덤프 | `/opt/backup/*.dump` | 호스트 디렉터리 | `backup.sh` 의 보관 기간(14일) |
| 컨테이너 stdout | `docker compose logs api` | Docker json-file | **컨테이너를 새로 만들면 사라짐** |

마지막 줄이 파일 로깅을 넣은 이유다. `docker compose logs` 는 배포할 때마다 초기화되므로,
장애 직후에 배포를 한 번 하면 원인을 볼 수 없다.

### 준비 — 로그 디렉터리 소유자 맞추기 (한 번만) [서버]

컨테이너는 비루트 사용자(**uid 10001**)로 돌기 때문에, 바인드 마운트할 호스트 디렉터리의
소유자를 맞춰 주지 않으면 로그 파일을 만들지 못한다.

```bash
mkdir -p /opt/hiphant/logs/caddy
sudo chown -R 10001:10001 /opt/hiphant/logs
```

이 단계를 빼먹으면 앱은 정상 기동하지만 로그 파일만 안 생긴다(콘솔에 logback 오류가 찍힌다).

### 로그 보는 법 [서버]

```bash
tail -f /opt/hiphant/logs/hiphant.log              # 실시간
grep -i error /opt/hiphant/logs/hiphant.log        # 에러만
grep "하이라이트 생성" /opt/hiphant/logs/hiphant.log  # 특정 작업 추적
ls -lh /opt/hiphant/logs/                          # 말려 있는 이전 파일들

tail -f /opt/hiphant/logs/caddy/access.log         # 어떤 요청이 들어왔는지
docker compose logs -f api                         # 현재 컨테이너의 stdout (기동 직후 확인용)
```

로그는 10MB 마다 말리고 **14일 / 총 500MB** 를 넘지 않게 자동 정리된다
(`application.yaml` 의 `logging.logback.rollingpolicy`). 60GB 디스크에서 로그가 문제될 일은 없다.

### 로그 레벨을 잠깐 올리고 싶을 때

`.env` 에 한 줄 추가하고 `docker compose up -d api` 하면 된다. 원인을 찾은 뒤에는 지운다.

```bash
LOGGING_LEVEL_COM_PACHY_HIGHLIGHT=DEBUG
```

> `.env` 에 넣은 값은 compose 의 `environment` 에 나열된 것만 컨테이너로 전달된다.
> 임의의 변수를 넘기려면 `docker-compose.yml` 의 api `environment` 에도 그 줄을 추가해야 한다.

---

## 문제 해결

실제로 이 구성을 로컬에서 띄워 보며 걸렸던 것들이다.

**`password authentication failed for user "highlight"` 로 API 가 재시작을 반복한다**
`POSTGRES_PASSWORD` 는 **데이터 볼륨이 처음 만들어질 때만** 적용된다. 이미 `pgdata` 볼륨이 있는 상태에서
`.env` 의 `DB_PASSWORD` 를 바꾸면 Postgres 안의 비밀번호는 예전 값 그대로라 인증이 실패한다.
DB 안에서 바꾸거나, 데이터가 없다면 볼륨을 지우고 다시 만든다.

```bash
docker compose exec -T postgres psql -U highlight -d postgres \
  -c "ALTER USER highlight WITH PASSWORD '새비밀번호';"   # 데이터가 있을 때

docker compose down -v && docker compose up -d            # 데이터가 없을 때 (볼륨까지 삭제 — 주의)
```

**curl 이 빈 응답을 준다 (본문 없이 502)**
Caddy 는 살아 있고 뒤의 API 가 죽어 있을 때 나오는 모습이다. `docker compose logs api` 를 본다.

**API 가 처음 몇 번 재시작한다**
Postgres 가 완전히 뜨기 전에 붙으면 실패하고, `restart: unless-stopped` 로 다시 뜬다. 1~2회는 정상이다.
계속 반복되면 로그의 첫 `Caused by` 를 본다.

**Caddy 가 인증서를 못 받는다**
`docker compose logs caddy` 에 `no such host` 나 challenge 실패가 보이면 (1) A 레코드가 아직 전파되지 않았거나
(2) Lightsail 방화벽에 80 번이 닫혀 있다. Let's Encrypt 는 80 번으로 도메인 소유를 확인한다.
실패를 반복하면 발급 한도에 걸리므로, DNS 가 확실히 잡힌 뒤에 `docker compose up -d caddy` 를 한다.

**메모리 확인**
띄운 직후 기준 API 450MB / Postgres 60MB / Caddy 13MB 정도다. Postgres 는 쓰면서
`shared_buffers`(384MB)까지 올라간다. `docker stats` 와 `free -h` 로 스왑 사용량을 함께 본다.

---

## 자주 쓰는 명령 [서버]

```bash
cd /opt/hiphant
docker compose ps                       # 상태
docker compose logs -f api              # API 로그
docker compose restart api              # 재시작
docker compose exec postgres psql -U highlight -d highlight   # DB 접속
docker stats --no-stream                # 메모리 사용량
free -h                                 # 스왑 사용량
```
