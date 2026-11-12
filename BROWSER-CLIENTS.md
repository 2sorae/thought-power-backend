# 브라우저별 부스 사용자 연동

기준: main daee795에서 분기. 기존 API 경로와 요청 본문은 유지하고 헤더로 사용자를 구분합니다.
핵심 테이블은 기존 5개 그대로이며 children에 client_id와 자동 번호 발급을 추가합니다.

## 프론트 담당

1. 브라우저에 저장된 ID가 없을 때만 `crypto.randomUUID()`로 UUID v4를 만들고 localStorage에 보관합니다.
2. 사용자 관련 요청에 동일한 `X-Client-Id`를 보냅니다. 숫자 userId를 이 헤더에 넣지 마세요.
3. `GET /api/users/me`가 200이면 기존 프로필을 사용하고, 404 + `PROFILE_NOT_FOUND`이면 온보딩을 보여주세요.
4. `POST /api/users/me/onboarding`도 같은 헤더로 보냅니다. 같은 ID로 다시 보내도 프로필은 하나이고 입력 정보만 갱신됩니다.
5. 새로고침·재접속·다른 탭에서도 같은 ID를 유지합니다. 요청마다 새 ID를 만들면 안 됩니다.
6. 공용 기기에서 다음 사람이 사용할 때만 '새 체험 시작'으로 새 ID를 만들고 프로필 캐시, 일기 초안, flowId, questionVersion, 이전 조회 결과를 비웁니다. 진행 중인 요청도 취소/무시합니다.

```javascript
const key = 'thought-power-client-id';
let clientId = localStorage.getItem(key);
if (!clientId) {
  clientId = crypto.randomUUID();
  localStorage.setItem(key, clientId);
}
// 공통 fetch/axios 설정에서 모든 API 요청에 이 헤더를 추가해도 됩니다.
const response = await fetch(`${BASE_URL}/api/users/me`, {
  headers: { 'X-Client-Id': clientId }
});
```

| API | 헤더 및 동작 |
| --- | --- |
| GET/PATCH /api/users/me | 필수. 해당 브라우저 프로필만 조회/수정 |
| POST /api/users/me/onboarding | 필수. 해당 ID의 프로필 생성/갱신 |
| POST/GET /api/diaries | 필수. 사용자별 저장/목록, 하루 1개 유지 |
| GET/DELETE /api/diaries/{diaryId} | 필수. 다른 사용자의 일기는 404 |
| GET /api/diaries/calendar | 필수. 사용자별 달력 |
| GET /api/reports/emotions | 필수. 사용자별 리포트 |
| POST /api/diaries/question-flows | 필수. 질문 흐름의 소유 ID 저장 |
| POST /api/diaries/question-flows/{flowId}/answers | 필수. 다른 ID로 제출하면 404 QUESTION_FLOW_NOT_FOUND |
| POST /api/diaries/questions | 기존 무상태 질문 생성. DB/진행 상태를 저장하지 않음 |
| 약관·앱 버전·지원·구독 안내 | 기존대로 공개, 헤더 없이 가능 |

- 누락·잘못된 형식·중복 헤더: 400 `INVALID_CLIENT_ID`. 대문자 UUID는 소문자로 정규화합니다.
- 온보딩 전 사용자별 DB API: 404 `PROFILE_NOT_FOUND` (일기 목록/달력/리포트 포함).
- 같은 사용자가 같은 날 다시 저장: 409 `DIARY_ALREADY_EXISTS`. 다른 브라우저는 같은 날 저장 가능.
- 질문 흐름의 RETRY/NEXT/COMPLETE 처리와 최종 일괄 저장은 기존과 같습니다. 처음부터 끝까지 같은 헤더를 보내세요.
- 현재 허용된 프론트 Origin의 CORS에서 X-Client-Id를 사용할 수 있습니다.
- Swagger Authorize에 UUID v4를 입력하면 헤더가 전송됩니다. 공개 API에는 필수가 아닙니다.

## 배포 담당: 프론트와 배포 시점 조율 필요

이 코드는 DB 변경 전에 실행하면 Hibernate 스키마 검증에서 시작 실패할 수 있습니다.
기존 프론트가 헤더 없이 호출하면 사용자 관련 API가 400이므로 프론트도 함께 반영해야 합니다.

1. DB 백업 후 `SHOW CREATE TABLE children`, `diaries`, `emotion_reports`로 제약 이름/컬럼을 확인합니다.
2. 앱의 저장 요청을 잠시 중단합니다.
3. 기존 DB에 `db/migrations/20261111_browser_clients.sql`을 한 번 적용합니다.
   - 기존 id=1 제한을 해제하고 id 자동 발급 및 client_id 유일 인덱스를 추가합니다.
   - id 변경을 위해 외래키 2개를 잠시 제거하고 다시 생성합니다. 완료 후 두 외래키 복구를 확인하세요.
   - MySQL DDL은 자동 커밋됩니다. 도중 실패하면 현재 스키마를 확인하고 남은 단계부터 처리하세요.
4. 이전 빈 답변 허용 마이그레이션 적용 여부도 유지/확인합니다.
5. 백엔드 코드와 프론트 헤더 처리를 배포합니다. 현재 자동배포 workflow는 SQL을 실행하지 않습니다.
6. 서로 다른 UUID A/B로 각각 온보딩 → 같은 날 일기 저장 → 조회를 확인합니다. A의 두 번째 저장은 409여야 합니다.

로컬 개발 DB도 동일하게 마이그레이션이 필요합니다. `db/mysql-schema.sql`은 빈 DB 전용이고 기존 DB에서 재실행하지 않습니다.
기존 공통 프로필/일기는 삭제하지 않습니다. client_id가 NULL인 기존 기록은 새 브라우저에서 보이지 않습니다.
이번 변경에서 기존 설치 SQL의 중복/깨진 CREATE TABLE 구간도 정리했으며, 엔티티에 없는 trend_title은 포함하지 않습니다.

## 범위와 제한

임시 UUID는 로그인 인증이 아닙니다. ID를 알거나 복사하면 같은 사용자로 접근할 수 있으니 공유하지 마세요.
브라우저 데이터 삭제/시크릿 창/다른 브라우저는 새 사용자이며, 하루 제한은 실제 사람의 신원 기준이 아니라 해당 임시 사용자 기준입니다.
공용 기기의 '새 체험 시작'은 기존 데이터를 삭제하지 않고 새 사용자로 전환합니다.
질문 흐름은 기존처럼 서버 메모리에 있으므로 앱 재시작 시 만료되며, 여러 서버 인스턴스 간 공유되지 않습니다.

## 검증

- 전체 Gradle build: 테스트 63개 통과 (외부 DB 없는 H2 MySQL 모드).
- 브라우저별 프로필/일기/달력/리포트/삭제 분리, 헤더 검증, 질문 흐름 소유권, 동시 온보딩/하루 중복 저장, CORS와 Swagger 검증.
- 로컬 실제 MySQL의 별도 임시 DB에서 기존 프로필·일기 보존과 마이그레이션 후 신규 프로필 생성 검증 완료. 임시 검증 DB는 정리했습니다.
- 운영 DB와 기존 로컬 개발 DB에는 마이그레이션을 적용하지 않았습니다.
