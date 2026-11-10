package com.thoughtpower.archive;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DiaryDeleteService {

    private static final long DEMO_USER_ID = 1L;

    private final JdbcTemplate jdbc;

    public DiaryDeleteService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void deleteDiary(long diaryId) {
        if (diaryId < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "일기 id는 1 이상이어야 합니다.");
        }

        int updateUsers = jdbc.update("""
                   UPDATE children
                   SET diary_version = diary_version + 1
                   WHERE id = ?
           """,
                DEMO_USER_ID);

        if (updateUsers == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자 프로필이 없습니다.");
        }

        int deletedDiaries = jdbc.update("""
                DELETE FROM diaries
                WHERE id = ?
                  AND child_id = ?
                """,
                diaryId,
                DEMO_USER_ID
        );

        if (deletedDiaries == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "일기를 찾을 수 없습니다."
            );
        }

        jdbc.update("""
                DELETE FROM emotion_reports
                WHERE child_id = ?
                """,
                DEMO_USER_ID
        );
    }
}
