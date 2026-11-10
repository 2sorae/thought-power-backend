# AI 질문 연동

프론트는 기존 `POST /api/diaries/questions`를 호출한다. BE1이 검증 후 별도 AI 서버를 호출한다.
AI 호출은 Spring RestClient를 사용한다: https://docs.spring.io/spring-framework/reference/integration/rest-clients.html

## 설정

- `AI_QUESTIONS_URL`: AI 담당과 확정한 전체 질문 생성 URL. 아직 미정이므로 기본값은 빈 값(로컬 기본 질문 사용).
- `AI_CONNECT_TIMEOUT`: 연결 제한, 임시 기본값 3s.
- `AI_READ_TIMEOUT`: 응답 제한, 임시 기본값 30s. AI 내부 재시도 시간을 고려해 담당과 조정한다.
- `AI_AUTH_HEADER`, `AI_AUTH_VALUE`: 인증을 사용하면 두 값 모두 설정. 예: 담당과 합의한 헤더 이름과 키. Bearer 방식이라면 값에 `Bearer ` 접두어 포함.
- 인증값은 환경변수 또는 Git에서 제외한 application-local.properties에만 저장한다.
- 기본 질문 3개는 현재 QuestionService 문구를 유지한다. AI 팀 문구를 받으면 맞춘다.

## 요청·응답

요청은 emotion, content, order, type, previousAnswers를 그대로 전달한다.
이전 답변은 순서대로 0/1/2개여야 하며 누락, 중복, 역순, 잘못된 유형, 빈 답변은 BE1에서 400으로 거절한다. 임의로 정렬하지 않는다.
`몰라요`는 비어 있지 않은 답변이므로 허용한다. AI가 빈 답변을 지원하더라도 현재 프론트 계약은 빈 답변을 허용하지 않는다.
age, guardianType은 아직 합의된 요청 필드가 아니므로 추가하지 않는다.

AI 성공 응답의 order, type, content, isFallback은 필수다. isFallback은 실제 JSON boolean이어야 한다.
AI가 true/false를 보내면 그대로 프론트에 전달한다. 질문 생성 단계에서는 DB 저장이 없다.
isFallback은 현재 최종 일기 저장 대상이 아니므로 DB 컬럼을 추가하지 않는다.

| 상황 | BE1 처리 |
| --- | --- |
| AI 정상 응답 | 질문과 isFallback 그대로 전달 |
| 연결 실패·시간 초과, HTTP 408/429/5xx | 로컬 기본 질문, isFallback=true, 200 |
| 응답 JSON 또는 필수 필드 오류·order/type 불일치 | 로컬 기본 질문, isFallback=true, 200 |
| AI 400/422 + error.code/message | 기본 질문으로 숨기지 않고 프론트에 400 + 기존 code=INVALID_REQUEST 및 이유 메시지 반환 |
| 인증·경로 오류 등 그 밖의 HTTP 오류 | 502 + AI_UPSTREAM_ERROR |

AI 오류의 code는 AI 내부 규격이며 프론트용 code로 변환한다. AI 오류 body가 깨져 있어도 400/422는 성공으로 바꾸지 않는다.
BE1은 자동 재시도하지 않는다(AI 내부 재시도와 중복 방지). 로그에 일기·답변·인증값을 남기지 않는다.

## 연동 전 확정할 사항

실제 URL, 인증 헤더/값, 응답 제한 시간, AI 기본 질문 문구를 공유받아 설정한다.
AI 서버와 실제 통신 검증은 배포/로컬 실행 주소를 받은 뒤 진행한다. OpenAI 키는 AI 서버에서 관리한다.
위험 내용 처리, 빈 답변 허용, 나이 전달은 별도 정책/계약 합의가 필요하다.
