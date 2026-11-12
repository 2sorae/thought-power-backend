# 로컬 실행

Java 17 / Spring Boot / MySQL 8.0.16 이상을 사용합니다.

1. 프로젝트 루트에서 `Copy-Item application-local.properties.example application-local.properties`를 실행한 뒤, 생성된 `application-local.properties`에 MySQL 계정과 비밀번호를 입력합니다. 이 파일은 Git에서 제외됩니다. 환경변수 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`도 사용할 수 있습니다. 로컬 파일의 값이 설정돼 있으면 그 값이 우선합니다.
2. MySQL에 접속하여 `thought_power`의 기존 테이블을 확인합니다. `db/mysql-schema.sql`은 **빈 DB용**이며 기존 테이블이 있는 DB에는 재실행하지 않습니다.
3. 빈 DB라면 MySQL 클라이언트에서 `source 프로젝트의_절대경로/db/mysql-schema.sql`을 실행합니다.
4. IntelliJ Project SDK와 Gradle JVM은 Java 17로 설정합니다.
5. 프로젝트 루트에서 `./gradlew.bat build`로 전체 테스트를 실행합니다.
6. `ThoughtPowerApplication`을 실행하거나 `./gradlew.bat bootRun`을 실행합니다. 작업 디렉터리는 프로젝트 루트여야 로컬 설정이 적용됩니다.

DB 접속 기본값은 `localhost:3306/thought_power`입니다. Hibernate는 테이블을 자동 생성·변경하지 않습니다. 엔티티 5개를 기존 테이블에 매핑하며, 시작 시 `ddl-auto=validate`로 컬럼 매핑을 확인합니다. CHECK 제약과 업무 규칙 전체를 검증하는 기능은 아닙니다.

설계: X-Client-Id로 구분하는 브라우저별 아이 프로필, 핵심 테이블 5개만 사용합니다. 리포트 집계는 백엔드 쿼리로 구현하며 DB 뷰는 사용하지 않습니다. 날짜별 일기는 Asia/Seoul 기준으로 처리하고 DB 연결과 DATETIME 저장은 UTC 기준을 사용합니다. 실제 아이 정보는 최초 프로필 입력 시 저장합니다.

Swagger는 /swagger-ui/index.html에서 확인합니다. 사용자 구분과 기존 DB 마이그레이션은 BROWSER-CLIENTS.md를 참고하세요. 테스트는 별도 H2 DB를 사용하며 로컬 개발 MySQL 데이터를 변경하지 않습니다.
