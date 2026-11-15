package com.thoughtpower.onboarding;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 시연용 예시 일기 생성기.
 * 온보딩을 마친 아이에게 일기가 하나도 없으면 지난 2주 동안의 예시 일기 12개를 넣는다.
 * - 오늘 날짜는 비워 둔다 (시연자가 직접 오늘 일기를 써 볼 수 있게)
 * - 이미 일기가 있는 아이(기존 사용자, 재온보딩)에게는 넣지 않는다
 * - app.demo-seed.enabled=false 로 끌 수 있다
 */
@Component
public class DemoDiarySeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoDiarySeeder.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final JdbcTemplate jdbc;
    private final boolean enabled;
    private final TransactionTemplate transaction;

    public DemoDiarySeeder(
            JdbcTemplate jdbc,
            PlatformTransactionManager transactionManager,
            @Value("${app.demo-seed.enabled:true}") boolean enabled
    ) {
        this.jdbc = jdbc;
        this.enabled = enabled;
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public static class ProfileCompleted {
        private final long childId;
        public ProfileCompleted(long childId) { this.childId = childId; }
    }

    // Run only after onboarding commits: a seed failure must never undo the profile.
    @TransactionalEventListener
    public void afterOnboarding(ProfileCompleted event) {
        seedIfEmpty(event.childId);
    }

    public void seedIfEmpty(long childId) {
        if (!enabled) {
            return;
        }
        try {
            transaction.executeWithoutResult(status -> seedAtomically(childId));
        } catch (RuntimeException e) {
            // The separate transaction has already rolled back every sample row.
            log.warn("demo diary seed failed: childId={}", childId, e);
        }
    }

    private void seedAtomically(long childId) {
        // Same lock as diary creation; serializes repeat onboarding and actual diary saves.
        jdbc.queryForObject("SELECT id FROM children WHERE id = ? FOR UPDATE", Long.class, childId);
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM diaries WHERE child_id = ?", Integer.class, childId);
        if (count != null && count > 0) {
            return;
        }

        LocalDate today = LocalDate.now(SEOUL);
        for (Sample s : SAMPLES) {
            Date date = Date.valueOf(today.minusDays(s.daysAgo()));

            int inserted = jdbc.update("""
                    INSERT INTO diaries (child_id, emotion_id, diary_date, input_type, content)
                    SELECT ?, e.id, ?, 'TEXT', ?
                    FROM emotions e
                    WHERE e.code = ?
                    """, childId, date, s.content(), s.emotion());
            if (inserted != 1) throw new IllegalStateException("Missing sample emotion: " + s.emotion());

            Long diaryId = jdbc.queryForObject(
                    "SELECT id FROM diaries WHERE child_id = ? AND diary_date = ?",
                    Long.class, childId, date);

            jdbc.update("""
                    INSERT INTO diary_questions (diary_id, stage, `sequence`, question_text, answer_text)
                    VALUES (?, 'EMOTION', 1, ?, ?),
                           (?, 'REASON', 2, ?, ?),
                           (?, 'PERSPECTIVE', 3, ?, ?)
                    """,
                    diaryId, s.emotionQ(), s.emotionA(),
                    diaryId, s.reasonQ(), s.reasonA(),
                    diaryId, s.perspectiveQ(), s.perspectiveA());
        }

        jdbc.update("UPDATE children SET diary_version = diary_version + 1 WHERE id = ?", childId);
        jdbc.update("DELETE FROM emotion_reports WHERE child_id = ?", childId);
    }

    private record Sample(
            int daysAgo,
            String emotion,
            String content,
            String emotionQ, String emotionA,
            String reasonQ, String reasonA,
            String perspectiveQ, String perspectiveA
    ) {
    }

    // 감정 분포: 기쁨 4, 슬픔 2, 놀람 2, 화남 2, 무서움 1, 부끄러움 1 (5일 전, 10일 전은 비워 둠)
    private static final List<Sample> SAMPLES = List.of(
            new Sample(1, "JOY",
                    "오늘 받아쓰기 시험에서 백 점을 받았다. 선생님이 칭찬해 주셨다.",
                    "백 점을 받았을 때 어떤 기분이 들었나요?", "너무 기뻐서 소리 지를 뻔했어요.",
                    "왜 그렇게 기뻤다고 생각하나요?", "어제 엄마랑 열심히 연습했기 때문이에요.",
                    "같이 연습해 준 엄마는 이 소식을 듣고 어떤 기분이었을까요?", "엄마도 기뻐서 웃었을 것 같아요."),
            new Sample(2, "SADNESS",
                    "아끼던 지우개를 잃어버렸다. 가방을 다 찾아봤는데 없었다.",
                    "지우개가 없어진 걸 알았을 때 어떤 기분이 들었나요?", "속상해서 눈물이 날 것 같았어요.",
                    "왜 그렇게 속상했다고 생각하나요?", "생일 선물로 받은 지우개였거든요.",
                    "선물해 준 친구는 이 이야기를 들으면 어떤 기분일까요?", "아쉬워하겠지만 괜찮다고 해 줄 것 같아요."),
            new Sample(3, "SURPRISE",
                    "학교 가는 길에 무지개를 봤다. 엄청 크고 선명했다.",
                    "무지개를 봤을 때 어떤 기분이 들었나요?", "깜짝 놀라서 입이 벌어졌어요.",
                    "왜 그렇게 놀랐다고 생각하나요?", "아침에 비가 와서 무지개가 뜰 줄 몰랐어요.",
                    "같이 걷던 동생은 무지개를 보고 어떤 생각을 했을까요?", "동생도 신기해서 사진 찍자고 했을 것 같아요."),
            new Sample(4, "ANGER",
                    "동생이 내가 만든 블록 성을 무너뜨렸다.",
                    "블록 성이 무너졌을 때 어떤 기분이 들었나요?", "화가 나서 동생한테 소리쳤어요.",
                    "왜 그렇게 화가 났다고 생각하나요?", "한 시간 동안 열심히 만들었거든요.",
                    "동생은 그때 어떤 마음이었을까요?", "같이 놀고 싶어서 그랬던 것 같아요."),
            new Sample(6, "JOY",
                    "할머니 댁에 가서 강아지랑 같이 놀았다.",
                    "강아지랑 놀 때 어떤 기분이 들었나요?", "행복하고 신났어요.",
                    "왜 그렇게 행복했다고 생각하나요?", "강아지가 나를 기억하고 꼬리를 흔들어 줬어요.",
                    "할머니는 우리가 노는 모습을 보고 어떤 기분이었을까요?", "흐뭇해서 웃으셨을 것 같아요."),
            new Sample(7, "FEAR",
                    "밤에 천둥 번개가 쳐서 잠을 못 잤다.",
                    "천둥소리를 들었을 때 어떤 기분이 들었나요?", "무서워서 이불 속에 숨었어요.",
                    "왜 그렇게 무서웠다고 생각하나요?", "소리가 너무 크고 갑자기 번쩍해서요.",
                    "그때 옆에 있던 아빠는 어떤 마음이었을까요?", "내가 무서워할까 봐 걱정했을 것 같아요."),
            new Sample(8, "JOY",
                    "친구랑 놀이터에서 술래잡기를 했다. 내가 한 번도 안 잡혔다.",
                    "술래잡기를 할 때 어떤 기분이 들었나요?", "신나고 재미있었어요.",
                    "왜 그렇게 신났다고 생각하나요?", "열심히 뛰어서 한 번도 안 잡혔거든요.",
                    "계속 술래였던 친구는 어떤 기분이었을까요?", "조금 힘들었을 것 같아요. 다음엔 제가 술래 해 줄래요."),
            new Sample(9, "SHAME",
                    "수업 시간에 발표하다가 말을 더듬었다. 친구들이 웃었다.",
                    "친구들이 웃었을 때 어떤 기분이 들었나요?", "얼굴이 빨개지고 부끄러웠어요.",
                    "왜 그렇게 부끄러웠다고 생각하나요?", "다들 나만 쳐다보는 것 같았어요.",
                    "웃었던 친구들은 어떤 마음이었을까요?", "놀리려던 게 아니라 그냥 웃겼던 것 같아요."),
            new Sample(11, "SADNESS",
                    "제일 친한 친구가 다음 달에 이사를 간다고 했다.",
                    "친구가 이사 간다는 말을 들었을 때 어떤 기분이 들었나요?", "슬퍼서 아무 말도 못 했어요.",
                    "왜 그렇게 슬펐다고 생각하나요?", "매일 같이 학교에 갔는데 이제 못 하니까요.",
                    "이사 가는 친구는 어떤 마음일까요?", "친구도 나랑 헤어지는 게 슬플 것 같아요."),
            new Sample(12, "SURPRISE",
                    "아빠가 몰래 내가 좋아하는 딸기 케이크를 사 오셨다.",
                    "케이크를 봤을 때 어떤 기분이 들었나요?", "깜짝 놀라고 기분이 좋았어요.",
                    "왜 그렇게 놀랐다고 생각하나요?", "생일도 아닌데 케이크가 있어서요.",
                    "케이크를 사 온 아빠는 어떤 마음이었을까요?", "제가 좋아하는 모습을 보고 싶으셨던 것 같아요."),
            new Sample(13, "ANGER",
                    "급식 줄을 서 있는데 다른 반 친구가 새치기를 했다.",
                    "새치기를 당했을 때 어떤 기분이 들었나요?", "억울하고 화가 났어요.",
                    "왜 그렇게 화가 났다고 생각하나요?", "저는 오래 기다렸는데 순서를 안 지켜서요.",
                    "새치기한 친구는 그때 어떤 마음이었을까요?", "배가 많이 고팠던 것 같아요. 그래도 순서는 지켜야 해요."),
            new Sample(14, "JOY",
                    "미술 시간에 그린 그림이 교실 뒤에 걸렸다.",
                    "그림이 걸린 걸 봤을 때 어떤 기분이 들었나요?", "뿌듯하고 기뻤어요.",
                    "왜 그렇게 뿌듯했다고 생각하나요?", "색칠을 꼼꼼하게 오래 했거든요.",
                    "그림을 본 친구들은 어떤 생각을 했을까요?", "멋있다고 생각했을 것 같아요.")
    );
}