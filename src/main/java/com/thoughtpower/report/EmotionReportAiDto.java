package com.thoughtpower.report;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.util.StringUtils.hasText;

public final class EmotionReportAiDto {

    private EmotionReportAiDto() {}

    //BE2 -> AI
    public record Request(
            LocalDate startDate,
            LocalDate endDate,
            int totalDiaryCount,
            List<Distribution> distribution,
            List<Day> days
    ) {}

    public record Distribution(
            String emotion,
            int count,
            BigDecimal ratio
    ) {}

    public record Day(
            LocalDate date,
            List<String> emotions
    ) {}

    //AI -> BE2
    public record Response(
            @JsonProperty("most_emotion_reason")
            String mostEmotionReason,

            @JsonProperty("least_emotion_reason")
            String leastEmotionReason,

            @JsonProperty("parent_advice")
            String parentAdvice,

            @JsonProperty("trend_interpretation")
            String trendInterpretation
    ) {
        public boolean hasValidTexts() {
            return hasText(mostEmotionReason)
                    && hasText(leastEmotionReason)
                    && hasText(parentAdvice)
                    && hasText(trendInterpretation);
        }

        public static Response empty() {
            return new Response(
                    "선택한 기간에 작성된 일기가 없어요.",
                    "선택한 기간에 작성된 일기가 없어요.",
                    "아이와 오늘의 기분을 이야기하며 첫 일기를 남겨 보세요.",
                    "감정 기록이 없어 변화의 추이를 확인할 수 없어요."
            );
        }

        public static Response fallback() {
            return new Response(
                    "가장 자주 기록한 감정을 확인해 주세요.",
                    "가장 적게 기록한 감정을 확인해 주세요.",
                    "아이의 감정 기록을 보며 이야기를 들어 주세요.",
                    "날짜별 감정 기록은 그래프에서 확인할 수 있어요."
            );
        }
    }
    //기존 통계 응답에서 AI에 필요한 데이터 추출
    public static Request from(
            EmotionReportService.ReportResponse report
    ) {
        return new Request(
                report.startDate(),
                report.endDate(),
                report.totalDiaryCount(),
                report.distribution().stream()
                        .map(item -> new Distribution(
                                item.emotion(),
                                item.count(),
                                item.ratio()
                        ))
                        .toList(),
                report.days().stream()
                        .map(day -> new Day(
                                day.date(),
                                day.diaries().stream()
                                        .map(diary -> diary.emotion())
                                        .toList()
                        ))
                        .toList()
            );
        }
    }

