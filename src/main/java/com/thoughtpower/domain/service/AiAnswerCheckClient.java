package com.thoughtpower.domain.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

@Component
public class AiAnswerCheckClient {
    private static final Logger log = LoggerFactory.getLogger(AiAnswerCheckClient.class);
    private final RestClient client;
    private final String url;
    private final String authHeader;
    private final String authValue;

    public AiAnswerCheckClient(
            @Value("${ai.answers.check-url:}") String url,
            @Value("${ai.answers.timeout:3s}") Duration timeout,
            @Value("${ai.questions.auth-header:}") String authHeader,
            @Value("${ai.questions.auth-value:}") String authValue) {
        this.url = url.trim();
        if (timeout.isNegative() || timeout.isZero()) throw new IllegalArgumentException("Positive timeout required");
        if (!this.url.isEmpty()) {
            URI uri = URI.create(this.url);
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new IllegalArgumentException("Invalid AI answer check URL");
            }
        }
        if (authHeader.isBlank() != authValue.isBlank()) throw new IllegalArgumentException("Set both authentication fields");
        this.authHeader = authHeader;
        this.authValue = authValue;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(timeout).followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(timeout);
        client = RestClient.builder().requestFactory(factory).build();
    }

    public boolean isInformative(String answer) {
        if (url.isEmpty()) return true;
        try {
            var call = client.post().uri(url).contentType(MediaType.APPLICATION_JSON);
            if (!authHeader.isBlank()) call.header(authHeader, authValue);
            JsonNode body = call.body(Map.of("answer", answer)).retrieve().body(JsonNode.class);
            if (body != null && body.path("informative").isBoolean()) {
                return body.path("informative").booleanValue();
            }
            log.warn("Invalid AI answer check response; proceeding to next question");
        } catch (RestClientException ex) {
            log.warn("AI answer check failed ({}); proceeding to next question", ex.getClass().getSimpleName());
        }
        return true;
    }
}
