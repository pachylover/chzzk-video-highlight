# Postgres 백업 운영 가이드

어느 기계에서 어떤 명령을 치는지까지 적어 둔 실행용 문서다.
각 코드 블록 앞의 **[대괄호]** 가 그 명령을 입력하는 위치다.

- **[내 PC]** — WSL(Ubuntu) 터미널. 지금 Supabase DB를 백업할 때.
- **[Lightsail]** — 이전 후 서버에 SSH로 접속한 상태. `cd /opt/hiphant` 기준.

---

## 0. 백업 종류와 역할

세 가지는 서로를 대체하지 않는다. 최소한 논리 덤프와 스냅샷 둘 다 있어야 한다.

| 종류 | 복구 단위 | 이 프로젝트에서의 역할 |
| --- | --- | --- |
| **논리 덤프** (`pg_dump`) | 테이블/행 단위, 다른 서버로 이식 가능 | **주력.** 매일 1회. 실수로 지운 테이블 복구, 서버 이전 |
| **디스크 스냅샷** (Lightsail) | 인스턴스 전체 | 서버가 통째로 망가졌을 때. 매일 자동 |
| **PITR** (WAL 아카이빙) | 임의 시점 | 지금 규모에선 과하다. 하루치 손실이 치명적이 되면 그때 도입 |

현재 구성에서 최악의 데이터 손실은 **마지막 덤프 이후 하루치**다.
`chats`는 치지직에서 다시 수집할 수 있고 `highlights`는 재생성이 가능하므로 지금은 이 정도로 충분하다.

---

## 1. 준비 — pg_dump 설치 (한 번만)

`pg_dump` 버전이 서버 버전보다 낮으면 `server version mismatch` 로 실패한다.
서버가 15든 17이든 상관없도록 최신 클라이언트(17)를 깐다.

**[내 PC]**

```bash
sudo apt-get install -y curl ca-certificates
sudo install -d /usr/share/postgresql-common/pgdg
sudo curl -o /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc \
  --fail https://www.postgresql.org/media/keys/ACCC4CF8.asc
echo "deb [signed-by=/usr/share/postgresql-common/pgdg/apt.postgresql.org.asc] \
https://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" \
  | sudo tee /etc/apt/sources.list.d/pgdg.list
sudo apt-get update
sudo apt-get install -y postgresql-client-17

pg_dump --version    # pg_dump (PostgreSQL) 17.x 가 나오면 준비 완료
```

Lightsail 서버에서는 따로 설치할 필요가 없다. Postgres 컨테이너 안의 `pg_dump`를 쓴다.

---

## 2. 접속 정보 — 비밀번호를 명령줄에 쓰지 않기

비밀번호를 `psql "postgresql://...:비번@..."` 형태로 치면 셸 히스토리와 프로세스 목록(`ps aux`)에 남는다.
`~/.pgpass` 에 넣고 파일에서 읽게 한다.

### 어떤 접속 방식을 쓸 것인가

Supabase 는 세 가지를 준다. **앱(JDBC)과 백업(pg_dump)이 같은 것을 써도 된다.**

| 방식 | 호스트 : 포트 | 사용자 | pg_dump | 비고 |
| --- | --- | --- | --- | --- |
| Direct | `db.<ref>.supabase.co:5432` | `postgres` | O | **IPv6 전용** (IPv4 는 유료 애드온) |
| **Session pooler** | `aws-1-<region>.pooler.supabase.com:5432` | `postgres.<ref>` | **O** | IPv4 가능. 현재 앱이 쓰는 방식 |
| Transaction pooler | `aws-1-<region>.pooler.supabase.com:**6543**` | `postgres.<ref>` | **X** | prepared statement 불가. JDBC 로 쓰려면 `prepareThreshold=0` 필요 |

`pg_dump` 가 깨지는 것은 **6543(transaction mode)** 이다. 5432 는 direct 든 pooler 든 세션이 유지되므로 정상 동작한다.

현재 앱의 JDBC URL:

```
jdbc:postgresql://aws-1-ap-southeast-2.pooler.supabase.com:5432/postgres?reWriteBatchedStatements=true
```

→ session pooler(5432). 이 호스트를 백업에도 그대로 쓰면 된다.

**[내 PC]**

```bash
# 형식: 호스트:포트:DB:사용자:비밀번호
# pooler 를 쓸 때 사용자 이름은 postgres 가 아니라 postgres.<project-ref> 다 — 가장 자주 걸리는 부분
echo "aws-1-ap-southeast-2.pooler.supabase.com:5432:postgres:postgres.<project-ref>:<비밀번호>" >> ~/.pgpass
chmod 600 ~/.pgpass          # 600 이 아니면 무시된다

# 이제 비밀번호 없이 접속된다
export SUPA="postgresql://postgres.<project-ref>@aws-1-ap-southeast-2.pooler.supabase.com:5432/postgres"
psql "$SUPA" -c "select version();"
```

> 접속 문자열은 **Project Settings → Database → Connection string** 에서 그대로 복사한다.
> JDBC URL 과 달리 `pg_dump`/`psql` 은 `postgresql://` 형식을 쓴다(`jdbc:` 접두사 없음).

> **pooler 로 덤프할 때 `pg_dump -j`(병렬 덤프)는 쓰지 않는다.** 병렬 덤프는 여러 커넥션이 같은 스냅샷을
> 공유해야 하는데 pooler 뒤에서는 서로 다른 세션이 되어 실패한다. 아래 명령들은 모두 단일 커넥션이라 문제없다.
> (`pg_restore -j` 는 복원 대상이 로컬 Postgres 라 그대로 써도 된다.)

---

## 3. 지금 Supabase DB 백업하기

**[내 PC]**

```bash
mkdir -p ~/hiphant-backup && cd ~/hiphant-backup

pg_dump "$SUPA" \
  --format=custom \
  --no-owner --no-privileges \
  --verbose \
  --file=hiphant-$(date +%Y%m%d-%H%M).dump
```

옵션의 의미:

| 옵션 | 이유 |
| --- | --- |
| `--format=custom` (`-Fc`) | 압축되고, 복원 시 `-j`로 병렬 처리·테이블 선택 복원이 된다. `.sql` 텍스트보다 낫다 |
| `--no-owner` | Supabase의 롤(`postgres`, `supabase_admin`)이 새 서버에 없으므로 소유자 지정을 뺀다 |
| `--no-privileges` | 위와 같은 이유로 GRANT 문을 뺀다 |
| `--verbose` | 어느 테이블에서 멈췄는지 보인다 |

### 큰 테이블만 따로 (선택)

`chats`가 크면 스키마와 데이터를 나눠 받는 편이 다루기 쉽다.

```bash
# 스키마만 (수 초)
pg_dump "$SUPA" -Fc --schema-only --no-owner --no-privileges -f schema.dump

# chats 제외한 데이터 (빠름)
pg_dump "$SUPA" -Fc --data-only --exclude-table=chats --no-owner -f data-nochats.dump

# chats 만 (오래 걸림)
pg_dump "$SUPA" -Fc --data-only --table=chats --no-owner -f data-chats.dump
```

### 백업이 제대로 됐는지 확인 — 반드시

파일 크기만 보고 넘어가면 안 된다. 목록이 읽히는지, 테이블이 다 들어갔는지 확인한다.

```bash
ls -lh hiphant-*.dump

# 덤프 안의 객체 목록 — 에러 없이 출력되면 파일이 온전하다
pg_restore --list hiphant-*.dump | head -40

# 테이블이 빠지지 않았는지
pg_restore --list hiphant-*.dump | grep "TABLE DATA"
# chats, highlights, banners, announcements, admin_users, blocked_users,
# flyway_schema_history 가 모두 보여야 한다
```

### 보관

**한 곳에만 두면 백업이 아니다.** 최소 두 곳:

```bash
# 1) 로컬 (WSL 안이 아니라 윈도우 쪽에도 복사해 두면 WSL을 날려도 남는다)
cp hiphant-*.dump /mnt/c/Users/difed/hiphant-backup/

# 2) 클라우드 (S3 예시 — 5번에서 aws cli 설정)
aws s3 cp hiphant-*.dump s3://<버킷>/hiphant/
```

---

## 4. 이전 후 — Lightsail에서 매일 자동 백업

### 4-1. 수동으로 한 번 돌려 보기

**[Lightsail]**

```bash
cd /opt/hiphant
mkdir -p /opt/backup

# 컨테이너 안의 pg_dump 로 덤프해서 호스트 파일로 저장한다
docker compose exec -T postgres \
  pg_dump -U highlight -Fc --no-owner highlight > /opt/backup/test.dump

ls -lh /opt/backup/test.dump
docker run --rm -v /opt/backup:/b postgres:15 pg_restore --list /b/test.dump | head
```

> `exec` 에 **`-T`** 를 빼먹으면 TTY가 붙어 덤프 파일이 깨진다. cron에서 특히 자주 나는 실수다.

### 4-2. 백업 스크립트

**[Lightsail]**

```bash
sudo tee /opt/hiphant/backup.sh > /dev/null <<'SH'
#!/usr/bin/env bash
set -euo pipefail

BACKUP_DIR=/opt/backup
KEEP_DAYS=14
STAMP=$(date +%F-%H%M)
FILE="$BACKUP_DIR/hiphant-$STAMP.dump"

mkdir -p "$BACKUP_DIR"
cd /opt/hiphant

# 1) 덤프 (임시 이름으로 받고 성공했을 때만 최종 이름으로 옮긴다)
docker compose exec -T postgres \
  pg_dump -U highlight -Fc --no-owner highlight > "$FILE.part"

# 2) 온전한 파일인지 확인 — 깨진 덤프를 성공으로 처리하지 않는다
docker run --rm -v "$BACKUP_DIR":/b postgres:15 \
  pg_restore --list "/b/$(basename "$FILE").part" > /dev/null

mv "$FILE.part" "$FILE"

# 3) 원격 복사 (S3). 실패해도 로컬 백업은 남기고 종료 코드로 알린다
aws s3 cp "$FILE" "s3://<버킷>/hiphant/" --storage-class STANDARD_IA

# 4) 오래된 로컬 백업 정리
find "$BACKUP_DIR" -name 'hiphant-*.dump' -mtime +$KEEP_DAYS -delete
find "$BACKUP_DIR" -name '*.part' -mtime +1 -delete

echo "backup ok: $FILE ($(du -h "$FILE" | cut -f1))"
SH

sudo chmod +x /opt/hiphant/backup.sh
sudo /opt/hiphant/backup.sh          # 먼저 손으로 한 번 성공시킨다
```

### 4-3. cron 등록

**[Lightsail]**

```bash
crontab -e
```

아래 한 줄을 넣는다. **cron은 PATH가 거의 비어 있으므로 절대경로를 쓰고, 출력은 로그로 남긴다.**

```cron
# 매일 04:10 KST 백업 (인스턴스 시간대가 UTC면 19:10 UTC = 04:10 KST)
10 19 * * * /opt/hiphant/backup.sh >> /var/log/hiphant-backup.log 2>&1
```

시간대를 헷갈리지 않으려면 인스턴스를 KST로 맞춘다:

```bash
sudo timedatectl set-timezone Asia/Seoul
date            # KST 확인 후에는 cron 도 KST 기준
```

등록 확인과 로그:

```bash
crontab -l
tail -f /var/log/hiphant-backup.log
```

### 4-4. 실패를 알아채기

백업은 조용히 실패하는 게 가장 위험하다. [healthchecks.io](https://healthchecks.io) 무료 플랜으로
"오늘 백업이 안 돌았다"는 알림을 받는다. 스크립트 마지막 줄에 추가:

```bash
curl -fsS -m 10 --retry 3 https://hc-ping.com/<uuid> > /dev/null
```

`set -e` 때문에 중간에 실패하면 이 줄까지 오지 못하고, healthchecks가 시간 안에 핑을 못 받아 메일을 보낸다.

---

## 5. S3 업로드 설정 (한 번만)

**[Lightsail]**

```bash
# aws cli 설치
sudo apt-get install -y unzip
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o awscliv2.zip
unzip -q awscliv2.zip && sudo ./aws/install && rm -rf aws awscliv2.zip

aws configure
# AWS Access Key ID / Secret / Default region: ap-northeast-2 / output: json
```

IAM 사용자는 **이 버킷의 이 경로에만 쓸 수 있는** 최소 권한으로 만든다.
서버가 털렸을 때 백업까지 삭제되는 것을 막으려면 `s3:DeleteObject`를 주지 않고,
버킷에 **버전 관리 + 수명 주기 규칙(30일 후 삭제)** 을 건다.

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": ["s3:PutObject"],
    "Resource": "arn:aws:s3:::<버킷>/hiphant/*"
  }]
}
```

비용은 무시할 수준이다 (STANDARD_IA 기준 GB당 월 $0.0138).

---

## 6. 복원 — 실제로 해봐야 백업이다

### 6-1. 전체 복원 (새 서버 / 서버가 날아갔을 때)

**[Lightsail]**

```bash
cd /opt/hiphant
docker compose up -d postgres
docker compose exec -T postgres psql -U highlight -d highlight \
  -c "CREATE EXTENSION IF NOT EXISTS pgcrypto; CREATE EXTENSION IF NOT EXISTS pg_trgm;"

cat /opt/backup/hiphant-2026-09-03-0410.dump | \
  docker compose exec -T postgres pg_restore -U highlight -d highlight \
    --no-owner --no-privileges -j 2

docker compose exec -T postgres psql -U highlight -d highlight -c "ANALYZE;"
```

- 확장(`pgcrypto`, `pg_trgm`)은 덤프에 포함되지 않을 수 있어 **먼저 만든다**. 없으면 인덱스 복원이 실패한다.
- 복원 직후 `ANALYZE`를 빼먹으면 통계가 없어 채팅 검색이 몇 배 느려진다.
- `-j 2` 는 vCPU 수에 맞춘 병렬도다.

### 6-2. 테이블 하나만 되돌리기

실수로 `highlights`를 지웠을 때처럼, 전체를 되돌리지 않고 한 테이블만 복원하는 경우:

```bash
docker compose exec -T postgres pg_restore -U highlight -d highlight \
  --data-only --table=highlights --no-owner < /opt/backup/hiphant-2026-09-03-0410.dump
```

### 6-3. 분기 1회 복원 리허설

**해보지 않은 백업은 백업이 아니다.** 운영 DB를 건드리지 않고 임시 컨테이너에 복원해 확인한다.

**[Lightsail]** 또는 **[내 PC]**

```bash
# 임시 Postgres 하나 띄우기
docker run -d --name restore-drill -e POSTGRES_PASSWORD=drill \
  -e POSTGRES_USER=highlight -e POSTGRES_DB=highlight postgres:15
sleep 10

docker exec -i restore-drill psql -U highlight -d highlight \
  -c "CREATE EXTENSION IF NOT EXISTS pgcrypto; CREATE EXTENSION IF NOT EXISTS pg_trgm;"
docker exec -i restore-drill pg_restore -U highlight -d highlight --no-owner -j 2 \
  < /opt/backup/hiphant-최신.dump

# 행 수가 운영과 비슷한지 확인
docker exec -i restore-drill psql -U highlight -d highlight -c "
SELECT 'chats' t, count(*) FROM chats
UNION ALL SELECT 'highlights', count(*) FROM highlights
UNION ALL SELECT 'blocked_users', count(*) FROM blocked_users;"

# 마이그레이션 이력까지 확인 (Flyway 버전이 최신인지)
docker exec -i restore-drill psql -U highlight -d highlight \
  -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"

docker rm -f restore-drill
```

---

## 7. 요약 체크리스트

- [ ] `~/.pgpass` 로 비밀번호를 파일에 두고 명령줄에 쓰지 않는다
- [ ] 지금 Supabase 덤프를 받아 **두 곳**에 보관했다
- [ ] `pg_restore --list` 로 덤프가 온전한지 확인했다
- [ ] Lightsail에서 `backup.sh` 를 손으로 한 번 성공시켰다
- [ ] cron에 등록하고 다음 날 로그로 실행을 확인했다
- [ ] healthchecks.io 로 실패 알림을 걸었다
- [ ] S3 버킷에 버전 관리 + 수명 주기 규칙을 걸고, 서버 키에는 삭제 권한을 주지 않았다
- [ ] Lightsail 자동 스냅샷을 켰다
- [ ] 분기 복원 리허설 일정을 잡았다
