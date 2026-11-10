package com.thoughtpower.domain.service;

import com.thoughtpower.domain.dto.QuestionGenerateRequest;
import com.thoughtpower.domain.dto.QuestionGenerateResponse;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

@Component
public class AiQuestionClient {
    private static final Logger log = LoggerFactory.getLogger(AiQuestionClient.class);
    private final RestClient client;
    private final String url;
    private final String authHeader;
    private final String authValue;

    public AiQuestionClient(
            @Value("${ai.questions.url:}") String url,
            @Value("${ai.questions.connect-timeout:3s}") Duration connectTimeout,
            @Value("${ai.questions.read-timeout:30s}") Duration readTimeout,
            @Value("${ai.questions.auth-header:}") String authHeader,
            @Value("${ai.questions.auth-value:}") String authValue) {
        if (connectTimeout.isZero() || connectTimeout.isNegative()
                || readTimeout.isZero() || readTimeout.isNegative()) {
            throw new IllegalArgumentException("AI timeouts must be positive");
        }
        this.url = url.trim();
        if (!this.url.isEmpty()) {
            URI uri = URI.create(this.url);
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new IllegalArgumentException("AI URL must be an HTTP(S) endpoint without credentials");
            }
        }
        if (authHeader.isBlank() != authValue.isBlank()) {
            throw new IllegalArgumentException("Set both AI auth header and value, or neither");
        }
        this.authHeader = authHeader;
        this.authValue = authValue;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(connectTimeout).followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(readTimeout);
        this.client = RestClient.builder().requestFactory(factory).build();
    }

    public Optional<QuestionGenerateResponse> generate(QuestionGenerateRequest request) {
        // Until a real endpoint is configured, keep the existing local fallback behavior.
        if (url.isEmpty()) return Optional.empty();
        try {
            var call = client.post().uri(url).contentType(MediaType.APPLICATION_JSON);
            if (!authHeader.isBlank()) call.header(authHeader, authValue);
            JsonNode body = call.body(request).retrieve().body(JsonNode.class);
            if (body == null || !body.path("order").isIntegralNumber()
                    || !body.path("order").canConvertToInt()
                    || body.path("order").intValue() != request.getOrder()
                    || !body.path("type").isTextual()
                    || !request.getType().equals(body.path("type").asString())
                    || !body.path("content").isTextual() || body.path("content").asString().isBlank()
                    || !body.path("isFallback").isBoolean()) {
                log.warn("AI question response has invalid fields; using local fallback");
                return Optional.empty();
            }
            return Optional.of(new QuestionGenerateResponse(request.getOrder(), request.getType(),
                    body.path("content").asString(), body.path("isFallback").booleanValue()));
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status == 400 || status == 422) {
                // Never turn a rejected request into a successful fallback question.
                String message = "AI 서버가 요청을 거절했습니다. 질문 순서와 이전 답변을 확인해주세요.";
                try {
                    JsonNode error = ex.getResponseBodyAs(JsonNode.class);
                    if (error != null && error.path("error").path("message").isTextual()
                            && !error.path("error").path("message").asString().isBlank()) {
                        message = error.path("error").path("message").asString();
                    }
                } catch (RuntimeException ignored) {
                    // A malformed error body is still an error, never fallback success.
                }
                throw new AiQuestionException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message);
            }
            if (status == 408 || status == 429 || ex.getStatusCode().is5xxServerError()) {
                log.warn("AI question server unavailable (HTTP {}); using local fallback", status);
                return Optional.empty();
            }
            // Authentication/endpoint mistakes should be visible, not hidden by fallback.
            throw new AiQuestionException(HttpStatus.BAD_GATEWAY, "AI_UPSTREAM_ERROR",
                    "AI 서버 연결 설정을 확인해주세요.");
        } catch (RestClientException ex) {
            // Do not log diary text, upstream response bodies, or authentication values.
            log.warn("AI question transport/response failure ({}); using local fallback",
                    ex.getClass().getSimpleName());
            return Optional.empty();
        }
    }
}
