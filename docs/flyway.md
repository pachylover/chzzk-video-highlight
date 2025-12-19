# Flyway 사용 가이드 ✅

간단하고 실무에서 바로 쓸 수 있는 Flyway 정리입니다. 이 프로젝트에서는 `src/main/resources/db/migration` 폴더를 사용하고 있으므로 그 관례를 기준으로 설명합니다.

---

## 1. Flyway란? 💡
- 데이터베이스 버전 관리를 위한 마이그레이션 도구입니다.
- SQL 파일(또는 Java 기반 마이그레이션)을 버전 관리하여 순차적으로 적용합니다.

## 2. 파일 규칙 🔧
- 버전 파일: `V<version>__<description>.sql` (예: `V1__create_tables.sql`)
  - 버전은 숫자(또는 점 포함 가능). 중복/충돌에 주의.
- 반복(Repeatable) 파일: `R__<description>.sql` (항상 재적용됨)
- (Undo/자동 롤백은 Flyway Teams에서 제공됩니다.)

## 3. 프로젝트 구조 (권장)
- `src/main/resources/db/migration`에 SQL 파일 배치
- 예: `src/main/resources/db/migration/V1__create_chats_highlights.sql`

## 4. Gradle 연동 예시
build.gradle에 플러그인 추가:

```groovy
plugins {
  id 'org.flywaydb.flyway' version '9.16.0'
}

flyway {
  url = 'jdbc:postgresql://localhost:5432/highlight'
  user = 'dbuser'
  password = 'secret'
  locations = ['filesystem:src/main/resources/db/migration']
}
```

- 명령: `./gradlew flywayMigrate`, `./gradlew flywayInfo`, `./gradlew flywayRepair` 등

## 5. Spring Boot 사용 시
- Spring Boot는 `spring-boot-starter-jdbc`/`spring-boot-starter-data-jpa`와 함께 자동으로 Flyway를 실행합니다.
- application.yaml 예시:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/highlight
    username: dbuser
    password: secret
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true   # 기존 DB가 있을 때 사용
```

- 주의: 프로덕션에서는 앱 시작 시 자동 마이그레이션 대신 배포 파이프라인에서 별도 실행을 권장합니다.

## 6. 주요 명령/기능
- migrate: 최신 마이그레이션 적용
- info: 현재 마이그레이션 상태 확인
- validate: 마이그레이션 무결성 검사
- repair: 실패나 체크섬 충돌 복구 (주의해서 사용)
- baseline: 기존 DB를 기준으로 Flyway 적용 시작
- clean: DB 전체 초기화 (절대 프로덕션에서 사용하지 말 것)

CLI 예시:
```
flyway -url=jdbc:postgresql://localhost:5432/highlight -user=dbuser -password=secret migrate
```

## 7. 롤백 전략
- Flyway Community는 자동 Undo를 제공하지 않습니다.
- 방법:
  - 수동으로 반대 SQL(rollback script) 작성 후 새 버전으로 적용
  - Flyway Teams의 `UNDO` 스크립트 사용
  - 백업/스냅샷을 이용한 복원

## 8. 권장 관행 ✅
- 마이그레이션 파일은 작고 단일 책임(한 변경만)로 작성
- 로컬에서 먼저 마이그레이션 테스트
- CI/CD에서 배포 전 `flyway migrate` 실행 및 실패 시 배포 중단
- `baseline-on-migrate`는 기존 DB 도입 시 신중히 사용
- `clean`은 테스트 환경에서만 사용
- 마이그레이션은 배포 전/배포 시점에 한 번만 실행되도록 락/조정

## 9. 트러블슈팅 ⚠️
- 체크섬 충돌: `flyway repair`로 해결(파일을 수정한 경우 주의)
- 부분 적용 실패: `flyway info`로 상태 확인 후 `repair` 또는 수동 조치
- 권한 문제: 마이그레이션 전 DB 유저 권한(스키마 생성/변경 권한) 확인

## 10. CI/CD 적용 예시 (간단)
- 파이프라인 스텝:
  1. DB 접속 정보/시크릿 주입
  2. `./gradlew flywayMigrate` 실행
  3. 성공 시 다음 배포 단계 진행

---

## 참고 자료
- Flyway 공식 문서: https://flywaydb.org/documentation/

---

필요하면 이 파일에 다음 항목을 더 추가해드릴게요:
- 프로젝트에 맞춘 `build.gradle` 정확한 설정 코드
- CI 도구(Azure DevOps / GitHub Actions / Jenkins) 예제
- 기존 DB를 Flyway로 마이그레이션 하는 단계별 가이드

