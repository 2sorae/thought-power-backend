package com.thoughtpower.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Set;

import static org.springframework.util.StringUtils.hasText;

@Component
public class EmotionReportAiClient {

    private static final Logger log =
            LoggerFactory.getLogger(EmotionReportAiClient.class);

    private static final String GENERATE_PATH =
            "/api/emotion-reports/generate";

    private static final Set<String> RESPONSE_FIELDS = Set.of(
            "most_emotion_reason",
            "least_emotion_reason",
            "parent_advice",
            "trend_interpretation"
    );

    private final RestClient restClient;
    private final String baseUrl;

    public EmotionReportAiClient(
            @Value("${ai.emotion-report.base-url:}")
            String baseUrl,

            @Value("${ai.emotion-report.connect-timeout-ms:3000}")
            int connectTimeoutMs,

            @Value("${ai.emotion-report.read-timeout-ms:15000}")
            int readTimeoutMs,

            @Value("${AI_API_KEY:}")
            String apiKey
    ) {
        if (connectTimeoutMs <= 0 || readTimeoutMs <= 0) {
            throw new IllegalArgumentException("AI 통신 제한 시간은 0 보다 커야 합니다.");
        }

        this.baseUrl = baseUrl.strip().replaceAll("/+$","");

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);

        var builder = RestClient.builder().requestFactory(factory);
        if (hasText(apiKey)) {
            builder.defaultHeader("X-API-Key", apiKey);
        }
        this.restClient = builder.build();
    }

    public EmotionReportAiDto.Response generate(
            EmotionReportAiDto.Request request
    ) {
        // 0건이면 AI 호출 X
        if (request.totalDiaryCount() == 0){
            return EmotionReportAiDto.Response.empty();
        }

        // AI 연동 대기 + 기본 문구 사용
        if (!hasText(baseUrl)) {
            return EmotionReportAiDto.Response.fallback();
        }

        try {
            ResponseEntity<Map<String, Object>> entity =
                    restClient.post()
                            .uri(baseUrl + GENERATE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .body(request)
                            .retrieve()
                            .toEntity(
                                    new ParameterizedTypeReference<Map<String, Object>>() {}
                            );
            if (entity.getStatusCode().value() != 200) {
                log.warn(
                        "감정 리포트 AI 응답 상태 오류: status={}",
                        entity.getStatusCode().value()
                );
                return EmotionReportAiDto.Response.fallback();
            }

            Map<String, Object> body = entity.getBody();

            //필수 필드가 4개인지 확인
            if (body == null
            || !body.keySet().equals(RESPONSE_FIELDS)) {
                log.warn("감정 리포트 AI 응답 필드 오류");
                return EmotionReportAiDto.Response.fallback();
            }

            EmotionReportAiDto.Response response =
                    new EmotionReportAiDto.Response(
                            readText(body, "most_emotion_reason"),
                            readText(body, "least_emotion_reason"),
                            readText(body, "parent_advice"),
                            readText(body, "trend_interpretation")
                    );

            if (!response.hasValidTexts()) {
                log.warn("감정 리포트 AI 응답 문자열 오류");
                return EmotionReportAiDto.Response.fallback();
            }

            return response;
        } catch (RuntimeException exception) {
            // 연결 실패, 시간 초과, HTTP 오류, JSON 해석 실패 등
            log.warn("감정 리포트 AI 호출 실패: type={}",exception.getClass().getSimpleName());
            return EmotionReportAiDto.Response.fallback();
        }
    }

    private String readText(
            Map<String, Object> body,
            String field
    ) {
        Object value = body.get(field);

        if (!(value instanceof String text)) {
            return null;
        }
        return text.strip();
    }
}
