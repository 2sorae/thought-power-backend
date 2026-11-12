package com.thoughtpower.report;

import com.thoughtpower.identity.CurrentClient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.annotation.JsonProperty;

@Service
public class EmotionReportService {


    private final JdbcTemplate jdbc;
    private final CurrentClient currentClient;
    private final EmotionReportAiClient aiClient;
    private final Map<String, String> legend;

    public EmotionReportService(
            JdbcTemplate jdbc,
            CurrentClient currentClient,
            EmotionReportAiClient aiClient
    ) {
        this.jdbc = jdbc;
        this.currentClient = currentClient;
        this.aiClient = aiClient;

        this.legend = new LinkedHashMap<>();
        legend.put("JOY", "기쁨");
        legend.put("SADNESS", "슬픔");
        legend.put("ANGER", "화남");
        legend.put("FEAR", "무서움");
        legend.put("SURPRISE", "놀람");
        legend.put("SHAME", "부끄러움");
    }

    public ReportResponse getReport(LocalDate startDate, LocalDate endDate) {
        validateDates(startDate, endDate);

        List<DiaryRecord> diaries = jdbc.query("""
                SELECT d.id, d.diary_date, e.code AS emotion_code
                FROM diaries d
                JOIN emotions e ON e.id = d.emotion_id
                WHERE d.child_id = ?
                  AND d.diary_date BETWEEN ? AND ?
                ORDER BY d.diary_date ASC, d.id ASC
                """,
                (resultSet, rowNum) -> new DiaryRecord(
                        resultSet.getLong("id"),
                        resultSet.getDate("diary_date").toLocalDate(),
                        resultSet.getString("emotion_code")
                ),
                currentClient.requireChildId(),
                Date.valueOf(startDate),
                Date.valueOf(endDate)
        );

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String emotion : legend.keySet()) {
            counts.put(emotion, 0);
        }

        Map<LocalDate, List<DayDiary>> diariesByDate = new LinkedHashMap<>();
        for (LocalDate date = startDate;
             !date.isAfter(endDate);
             date = date.plusDays(1)) {
            diariesByDate.put(date, new ArrayList<>());
        }

        for (DiaryRecord diary : diaries) {
            counts.put(diary.emotion(), counts.get(diary.emotion()) + 1);
            diariesByDate.get(diary.date()).add(
                    new DayDiary(diary.diaryId(), diary.emotion())
            );
        }

        int total = diaries.size();

        List<Distribution> distribution = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            distribution.add(new Distribution(
                    entry.getKey(),
                    entry.getValue(),
                    calculateRatio(entry.getValue(), total)
            ));
        }

        List<String> mostFrequent = new ArrayList<>();
        List<String> leastFrequent = new ArrayList<>();

        if (total > 0) {
            int maxCount = counts.values().stream()
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(0);

            int minCount = counts.values().stream()
                    .mapToInt(Integer::intValue)
                    .min()
                    .orElse(0);

            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                if (entry.getValue() == maxCount) {
                    mostFrequent.add(entry.getKey());
                }
                if (entry.getValue() == minCount) {
                    leastFrequent.add(entry.getKey());
                }
            }
        }

        List<ReportDay> days = new ArrayList<>();
        for (Map.Entry<LocalDate, List<DayDiary>> entry : diariesByDate.entrySet()) {
            days.add(new ReportDay(entry.getKey(), entry.getValue()));
        }

        EmotionReportAiDto.Response defaultTexts =
                total == 0
                ? EmotionReportAiDto.Response.empty()
                        : EmotionReportAiDto.Response.fallback();

        ReportResponse report = new ReportResponse(
                startDate,
                endDate,
                total,
                distribution,
                mostFrequent,
                leastFrequent,
                defaultTexts.mostEmotionReason(),
                defaultTexts.leastEmotionReason(),
                defaultTexts.parentAdvice(),
                days,
                new LinkedHashMap<>(legend),
                defaultTexts.trendInterpretation()
        );

        // 일기가 없으면 AI 호출 X 반환
        if (total == 0) {
            return report;
        }

        EmotionReportAiDto.Request aiRequest =
                EmotionReportAiDto.from(report);

        EmotionReportAiDto.Response aiResponse =
                aiClient.generate(aiRequest);

        return withAiTexts(report, aiResponse);

    }

    private ReportResponse withAiTexts(
            ReportResponse report,
            EmotionReportAiDto.Response texts) {
        return new ReportResponse(
                report.startDate(),
                report.endDate(),
                report.totalDiaryCount(),
                report.distribution(),
                report.mostFrequentEmotions(),
                report.leastFrequentEmotions(),
                texts.mostEmotionReason(),
                texts.leastEmotionReason(),
                texts.parentAdvice(),
                report.days(),
                report.legend(),
                texts.trendInterpretation()
        );
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate.getYear() < 1000 || startDate.getYear() > 9999
                || endDate.getYear() < 1000 || endDate.getYear() > 9999) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "연도는 1000~9999 사이여야 합니다."
            );
        }

        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "시작일은 종료일보다 늦을 수 없습니다."
            );
        }
    }

    private BigDecimal calculateRatio(int count, int total) {
        if (total == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(count)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private record DiaryRecord(
            long diaryId,
            LocalDate date,
            String emotion
    ) {}

    public record Distribution(
            String emotion,
            int count,
            BigDecimal ratio
    ) {}

    public record DayDiary(long diaryId, String emotion) {}

    public record ReportDay(LocalDate date, List<DayDiary> diaries) {}

    public record ReportResponse(
            LocalDate startDate,
            LocalDate endDate,
            int totalDiaryCount,
            List<Distribution> distribution,
            List<String> mostFrequentEmotions,
            List<String> leastFrequentEmotions,
            @JsonProperty("most_emotion_reason")
            String mostEmotionReason,
            @JsonProperty("least_emotion_reason")
            String leastEmotionReason,
            @JsonProperty("parent_advice")
            String parentAdvice,
            List<ReportDay> days,
            Map<String, String> legend,
            @JsonProperty("trend_interpretation")
            String trendInterpretation
    ) {}
}