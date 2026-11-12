package com.thoughtpower.archive;

import com.thoughtpower.identity.CurrentClient;
import java.util.ArrayList;
import java.util.List;

import com.thoughtpower.domain.entity.DiaryQuestion;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Date;
import java.time.LocalDate;

@Service
public class DiaryReadService {

    private static final int PREVIEW_LENGTH = 100;

    private final JdbcTemplate jdbc;
    private final CurrentClient currentClient;

    public DiaryReadService(JdbcTemplate jdbc, CurrentClient currentClient) {
        this.jdbc = jdbc;
        this.currentClient = currentClient;
    }

    public DiaryListResponse getDiaries(LocalDate date) {
        if (date.getYear() < 1000 || date.getYear() > 9999) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "연도는 1000~9999 사이여야 합니다.");
        }

        List<DiarySummary> diaries = jdbc.query("""
                                SELECT d.id,
                                       d.diary_date,
                                       e.code AS emotion_code,
                                       LEFT(d.content, ?) AS content_preview
                                FROM diaries d
                                JOIN emotions e ON e.id = d.emotion_id
                                WHERE d.child_id = ?
                                  AND d.diary_date = ?
                                ORDER BY d.id ASC
                """,
                (resultSet, rowNum) -> new DiarySummary(
                        resultSet.getLong("id"),
                        resultSet.getDate("diary_date").toLocalDate(),
                        resultSet.getString("emotion_code"),
                        resultSet.getString("content_preview")
                ),
                PREVIEW_LENGTH,
                currentClient.requireChildId(),
                Date.valueOf(date)
        );

        return new DiaryListResponse(diaries);
    }

    public record DiaryListResponse(List<DiarySummary> diaries) {}

    public record DiarySummary(
            long diaryId,
            LocalDate date,
            String emotion,
            String contentPreview
    ) {}

    public DiaryDetailResponse getDiary(long diaryId) {
        if (diaryId < 1){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "일기 ID는 1 이상이어야 합니다."
            );
        }

        DiaryDetailResponse diary = jdbc.query("""
                SELECT d.id,
                                   d.diary_date,
                                   e.code AS emotion_code,
                                   d.content,
                                   q.id AS question_id,
                                   q.sequence AS question_order,
                                   q.stage,
                                   q.question_text,
                                   q.answer_text
                            FROM diaries d
                            JOIN emotions e ON e.id = d.emotion_id
                            LEFT JOIN diary_questions q ON q.diary_id = d.id
                            WHERE d.id = ?
                              AND d.child_id = ?
                            ORDER BY q.sequence ASC
                """,
                resultSet -> {
                    if (!resultSet.next()) {
                        return null;
                    }
                    long id = resultSet.getLong("id");
                    LocalDate date = resultSet.getDate("diary_date").toLocalDate();
                    String emotion = resultSet.getString("emotion_code");
                    String content = resultSet.getString("content");

                    List<DiaryQuestionResponse> questions = new ArrayList<>();

                    do {
                        if (resultSet.getObject("question_id") != null) {
                            questions.add(new DiaryQuestionResponse(
                                    resultSet.getInt("question_order"),
                                    resultSet.getString("stage"),
                                    resultSet.getString("question_text"),
                                    resultSet.getString("answer_text")
                            ));
                        }
                    }
                        while (resultSet.next());

                        return new DiaryDetailResponse(
                                id, date, emotion, content, questions
                        );
                    },
                    diaryId,
                    currentClient.requireChildId()
                );

        if (diary == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "일기를 찾을 수 없습니다.");
        }

        return diary;
    }

    public record DiaryDetailResponse(
            long diaryId,
            LocalDate date,
            String emotion,
            String content,
            List<DiaryQuestionResponse> questions
    ) {}

    public record DiaryQuestionResponse(
            int order,
            String type,
            String question,
            String answer
    ) {}


}
