package com.thoughtpower.archive;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class CalendarService {

    private static final long DEMO_USER_ID = 1L;

    private final JdbcTemplate jdbc;

    public CalendarService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public CalendarResponse getCalendar(int year, int month) {
        if (year < 1000 || year > 9999) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "연도는 1000~9999 사이여야 합니다.");
        }

        if (month <1 || month > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "월은 1~12 사이여야 합니다.");
        }

        YearMonth selectedMonth = YearMonth.of(year,month);
        LocalDate startDate = selectedMonth.atDay(1);
        LocalDate endDate = selectedMonth.atEndOfMonth();

        List<CalendarDay> days = jdbc.query("""
                SELECT d.diary_date, e.code AS emotion_code
                                FROM diaries d
                                JOIN emotions e ON e.id = d.emotion_id
                                WHERE d.child_id = ?
                                  AND d.diary_date BETWEEN ? AND ?
                                ORDER BY d.diary_date ASC
                """,
                (resultSet, rowNum) -> new CalendarDay(
                        resultSet.getDate("diary_date").toLocalDate(), 1,
                        List.of(resultSet.getString("emotion_code"))
                ),
                DEMO_USER_ID,
                Date.valueOf(startDate),
                Date.valueOf(endDate)
                );
        return new CalendarResponse(year, month, days);
    }

    public record CalendarResponse(
            int year, int month, List<CalendarDay> days
    ) {}

    public record CalendarDay(
            LocalDate date, int diaryCount, List<String> emotions
    ) {}

}
