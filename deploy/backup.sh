#!/usr/bin/env bash
#
# HiPhant DB 일일 백업. /opt/hiphant/backup.sh 로 두고 cron 에서 실행한다.
#
#   chmod +x /opt/hiphant/backup.sh
#   /opt/hiphant/backup.sh               # 먼저 손으로 한 번 성공시킨다 (sudo 없이)
#   crontab -e
#     10 4 * * * /opt/hiphant/backup.sh >> /opt/backup/backup.log 2>&1
#
# 설계 의도:
#  - .part 로 받아 검증에 성공했을 때만 최종 이름으로 옮긴다 (깨진 덤프를 백업으로 착각하지 않도록)
#  - set -e 로 중간에 실패하면 마지막 핑을 보내지 않는다 → healthchecks 가 "안 돌았다"고 알려준다

set -euo pipefail

COMPOSE_DIR=/opt/hiphant
BACKUP_DIR=/opt/backup
KEEP_DAYS=14

cd "$COMPOSE_DIR"
# shellcheck disable=SC1091
set -a; . "$COMPOSE_DIR/.env"; set +a

mkdir -p "$BACKUP_DIR"
STAMP=$(date +%F-%H%M)
NAME="hiphant-$STAMP.dump"
FILE="$BACKUP_DIR/$NAME"

# 1) 덤프. exec 에 -T 가 없으면 TTY 가 붙어 파일이 깨진다.
docker compose exec -T postgres \
  pg_dump -U "$DB_USER" -Fc --no-owner "$DB_NAME" > "$FILE.part"

# 2) 온전한 파일인지 확인 — 목록이 읽히지 않으면 여기서 중단된다
docker run --rm -v "$BACKUP_DIR":/b postgres:15 \
  pg_restore --list "/b/$NAME.part" > /dev/null

mv "$FILE.part" "$FILE"

# 3) 원격 복사 (BACKUP_S3_URI 가 비어 있으면 건너뛴다)
if [ -n "${BACKUP_S3_URI:-}" ]; then
  aws s3 cp "$FILE" "$BACKUP_S3_URI" --storage-class STANDARD_IA
fi

# 4) 오래된 파일 정리
find "$BACKUP_DIR" -name 'hiphant-*.dump' -mtime +$KEEP_DAYS -delete
find "$BACKUP_DIR" -name '*.part' -mtime +1 -delete

echo "$(date +%F' '%T) backup ok: $FILE ($(du -h "$FILE" | cut -f1))"

# 5) 성공 핑. 여기까지 왔다는 건 위 단계가 모두 통과했다는 뜻이다.
if [ -n "${BACKUP_PING_URL:-}" ]; then
  curl -fsS -m 10 --retry 3 "$BACKUP_PING_URL" > /dev/null
fi
