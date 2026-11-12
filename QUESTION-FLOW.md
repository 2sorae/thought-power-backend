# 프론트 ↔ BE1: 한 번만 다시 묻기

기존 `POST /api/diaries/questions`는 호환용 단건 생성 API다. 재질문 횟수 관리는 아래 새 흐름을 사용한다. 같은 일기 작성에서 두 방식을 섞지 않는다.

## 1. 첫 질문 시작

`POST /api/diaries/question-flows`

```json
{
  "emotion": "JOY",
  "content": "오늘 친구와 놀았다.",
  "order": 1,
  "type": "EMOTION",
  "previousAnswers": []
}
```

200 응답 예시(flowId는 매번 새로 생성):

```json
{
  "flowId": "5a41befd-57e8-4c5c-a93a-0257208e01d8",
  "action": "QUESTION",
  "questionVersion": 0,
  "retryCount": 0,
  "informative": null,
  "question": {"order": 1, "type": "EMOTION", "content": "그때 어떤 기분이 들었나요?", "isFallback": true},
  "questions": []
}
```

`flowId`와 `questionVersion`을 저장한다. 진행률은 `question.order / 3`으로 표시한다. flowId는 진행 정보에 접근하는 식별자이므로 공유하거나 로그에 남기지 않는다.

## 2. 답변 제출

`POST /api/diaries/question-flows/{flowId}/answers`

```json
{"questionVersion": 0, "answer": "몰라요"}
```

빈 답변은 `"answer": ""`로 보낸다. null 또는 필드 누락은 400이다. 재질문 여부·횟수·현재 질문 내용은 프론트가 계산해서 보내지 않고 BE1이 관리한다.

- BE1이 AI의 `POST /api/diary-answers/check`에 `{"answer":"몰라요"}`를 보내 informative를 확인한다.
- 확인 실패·시간 초과·비정상 JSON은 informative=true로 보고 다음 단계로 넘어간다.
- false이고 해당 order의 첫 답변이면 같은 order로 질문을 생성한다. `retry.question`은 실제 표시했던 문장, `retry.answer`는 방금 답변이다. previousAnswers는 앞선 order만 포함한다.
- 다시 물은 뒤의 답변이면 informative가 false여도 다음 단계로 넘어간다. order마다 최대 1회다.

응답 필드는 첫 질문과 같고 action으로 다음 행동을 정한다:

| action | 프론트 처리 |
| --- | --- |
| RETRY | 진행률 그대로. question.content만 새 질문으로 보여 준다. retryCount=1 |
| NEXT | 다음 order의 question을 보여 준다. retryCount=0 |
| COMPLETE | question=null. questions에 최종 질문·답변 3개가 있으므로 완료 저장 단계로 이동 |

매번 응답의 새 questionVersion을 보관하고 다음 답변 제출에 그대로 넣는다. 재질문에도 version은 증가하지만 order는 그대로다.
동일 flowId + questionVersion + answer 재전송은 원래 응답을 반환하며 AI 재호출·횟수 증가가 없다. 처리된 version에 다른 답변을 보내거나 미래 version을 보내면 409다. 본문/감정/이전 답변을 바꿔 질문을 다시 만들려면 새 flow를 시작한다.

## 3. 최종 저장

COMPLETE는 DB 저장 완료를 뜻하지 않는다. 프론트는 기존 `POST /api/diaries`에 감정·작성 방식·본문과 응답의 questions를 보내 저장한다.

```json
{
  "emotion": "JOY",
  "inputType": "TEXT",
  "content": "오늘 친구와 놀았다.",
  "questions": [
    {"order":1,"type":"EMOTION","content":"마지막으로 표시된 1번 질문","answer":""},
    {"order":2,"type":"REASON","content":"마지막으로 표시된 2번 질문","answer":"몰라요"},
    {"order":3,"type":"PERSPECTIVE","content":"마지막으로 표시된 3번 질문","answer":"쉬고 싶어요"}
  ]
}
```

각 order의 마지막 질문과 마지막 답변만 저장한다. 이전 질문의 최초 답변을 재질문 후의 답변으로 덮어써야 한다. date, flowId, questionVersion은 저장 요청에 보내지 않는다. 빈 문자열도 그대로 저장하며 임의의 '무응답' 문구로 바꾸지 않는다.

## 배포 전 필수

1. **DB 담당:** 기존 DB에서 `SHOW CREATE TABLE diary_questions`로 제약을 확인한 뒤 `db/migrations/20261111_allow_empty_diary_answers.sql`을 한 번 적용한다. `answer_text NOT NULL`은 유지하고 빈 문자열 금지 CHECK만 제거한다. 전체 mysql-schema.sql을 기존 DB에 재실행하지 않는다.
2. **설정 담당:** `AI_QUESTIONS_URL`과 `AI_ANSWERS_CHECK_URL`에 각각 전체 주소를 설정한다. 질문 생성 대기 시간은 30초 유지. 확인 API는 기본 3초이며 실패 시 바로 진행한다. 인증 헤더/값은 AI 담당과 확인한다.
3. **프론트:** 새 시작/답변 제출 API, action 분기, 진행률 유지, 최종 questions 저장, 중복 클릭 방지를 반영한다. 공백 답변에 대한 프론트 필수 입력 제한도 해제한다.
4. **실제 연결 테스트:** 빈 답변→RETRY→빈 답변→NEXT, 3번 재질문→COMPLETE→최종 저장을 검증한다.

현재 진행 정보는 DB가 아닌 **단일 서버 메모리**에 보관한다. 생성 후 30분이 지나거나 서버가 재시작되면 410 QUESTION_FLOW_EXPIRED를 반환한다. 프론트는 작성 내용을 유지하고 새 flow로 시작하도록 안내한다. 오래된 항목은 새 시작 요청 시 정리하며 최대 1000개다. 서버 여러 대를 쓰려면 공유 저장소(예: Redis)로 바꾸거나 같은 서버로 라우팅해야 한다. 현재 코드를 다중 인스턴스에서 무작위 라우팅으로 사용하면 안 된다.

## BE1 기본 질문 (AI 담당 공유용)

- EMOTION: 그때 어떤 기분이 들었나요?
- REASON: 왜 그런 기분이 들었다고 생각하나요?
- PERSPECTIVE: 지금 너에게 도움이 되는 것은 무엇일까요?

AI가 연결되지 않을 때도 3번 질문은 가해자의 입장을 추측하도록 요구하지 않는다. BE1 기본 질문은 위험 내용 감지 기능을 대신하지 않는다.

## 브라우저별 사용자

시작과 답변 제출에 같은 `X-Client-Id`(UUID v4)가 필수입니다. 다른 ID로 제출하면 404 `QUESTION_FLOW_NOT_FOUND`입니다. 전체 변경/배포 순서는 `BROWSER-CLIENTS.md`를 참고하세요.
