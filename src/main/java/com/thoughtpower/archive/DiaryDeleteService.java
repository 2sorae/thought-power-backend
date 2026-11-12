package com.thoughtpower.archive;

import com.thoughtpower.identity.CurrentClient;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DiaryDeleteService {


    private final JdbcTemplate jdbc;
    private final CurrentClient currentClient;

    public DiaryDeleteService(JdbcTemplate jdbc, CurrentClient currentClient) {
        this.jdbc = jdbc;
        this.currentClient = currentClient;
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
                currentClient.requireChildId());

        if (updateUsers == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자 프로필이 없습니다.");
        }

        int deletedDiaries = jdbc.update("""
                DELETE FROM diaries
                WHERE id = ?
                  AND child_id = ?
                """,
                diaryId,
                currentClient.requireChildId()
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
                currentClient.requireChildId()
        );
    }
}
