package com.thoughtpower.domain.controller;

import com.sun.net.httpserver.HttpServer;
import com.thoughtpower.domain.service.AiQuestionClient;
import com.thoughtpower.domain.service.QuestionService;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AiQuestionIntegrationTest {
    private HttpServer server;
    private ExecutorService executor;
    private String url;
    private int responseStatus = 200;
    private String responseBody;
    private long delayMillis;
    private volatile String receivedBody;
    private volatile String receivedAuth;
    private final AtomicInteger calls = new AtomicInteger();
    private static final String REQUEST = """
            {"emotion":"SHAME","content":"오늘 친구와 놀았다.","order":2,"type":"REASON",
             "previousAnswers":[{"order":1,"type":"EMOTION","question":"어땠니?","answer":"몰라요"}]}
            """;

    @BeforeEach
    void setup() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.createContext("/questions", exchange -> {
            calls.incrementAndGet();
            receivedBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            receivedAuth = exchange.getRequestHeaders().getFirst("X-Test-Key");
            try {
                if (delayMillis > 0) Thread.sleep(delayMillis);
                byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(responseStatus, bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
        url = "http://127.0.0.1:" + server.getAddress().getPort() + "/questions";
    }

    @AfterEach
    void cleanup() {
        server.stop(0);
        executor.shutdownNow();
    }

    private MockMvc mvc(Duration timeout) {
        return MockMvcBuilders.standaloneSetup(new QuestionController(new QuestionService(
                new AiQuestionClient(url, Duration.ofSeconds(1), timeout, "X-Test-Key", "test-only")))).build();
    }

    private org.springframework.test.web.servlet.ResultActions send(Duration timeout) throws Exception {
        return mvc(timeout).perform(post("/api/diaries/questions")
                .contentType(MediaType.APPLICATION_JSON).content(REQUEST));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void preservesAiFallbackFlagAndForwardsHistory(boolean fallback) throws Exception {
        responseBody = "{\"order\":2,\"type\":\"REASON\",\"content\":\"왜 그랬니?\",\"isFallback\":" + fallback + "}";
        send(Duration.ofSeconds(2)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("왜 그랬니?"))
                .andExpect(jsonPath("$.isFallback").value(fallback));
        assertTrue(receivedBody.contains("previousAnswers"));
        assertTrue(receivedBody.contains("몰라요"));
        assertTrue(receivedBody.contains("SHAME"));
        assertEquals("test-only", receivedAuth);
        assertEquals(1, calls.get());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 422})
    void rejectedRequestIsNeverFallback(int statusCode) throws Exception {
        responseStatus = statusCode;
        responseBody = """
                {"error":{"code":"ORDER_TYPE_MISMATCH","message":"순서와 유형 불일치"}}
                """;
        send(Duration.ofSeconds(2)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("순서와 유형 불일치"))
                .andExpect(jsonPath("$.isFallback").doesNotExist());
    }

    @Test
    void malformed422BodyIsStillAnError() throws Exception {
        responseStatus = 422;
        responseBody = "not json";
        send(Duration.ofSeconds(2)).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(ints = {408, 429, 500, 503})
    void temporaryFailureUsesLocalFallback(int statusCode) throws Exception {
        responseStatus = statusCode;
        responseBody = "{}";
        send(Duration.ofSeconds(2)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("왜 그런 기분이 들었다고 생각하나요?"))
                .andExpect(jsonPath("$.isFallback").value(true));
        assertEquals(1, calls.get());
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 404})
    void authAndEndpointErrorsAreVisible(int statusCode) throws Exception {
        responseStatus = statusCode;
        responseBody = "{}";
        send(Duration.ofSeconds(2)).andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("AI_UPSTREAM_ERROR"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}", "not json", "null",
        "{\"order\":2,\"type\":\"REASON\",\"content\":\"Q?\"}",
        "{\"order\":2,\"type\":\"REASON\",\"content\":\"Q?\",\"isFallback\":\"false\"}",
        "{\"order\":1,\"type\":\"REASON\",\"content\":\"Q?\",\"isFallback\":false}",
        "{\"order\":2,\"type\":\"EMOTION\",\"content\":\"Q?\",\"isFallback\":false}",
        "{\"order\":2,\"type\":\"REASON\",\"content\":\" \",\"isFallback\":false}"
    })
    void malformedSuccessUsesFallback(String json) throws Exception {
        responseBody = json;
        send(Duration.ofSeconds(2)).andExpect(status().isOk())
                .andExpect(jsonPath("$.isFallback").value(true));
    }

    @Test
    void timeoutUsesFallback() throws Exception {
        delayMillis = 700;
        responseBody = "{}";
        send(Duration.ofMillis(100)).andExpect(status().isOk())
                .andExpect(jsonPath("$.isFallback").value(true));
    }

    @Test
    void stoppedServerUsesFallback() throws Exception {
        server.stop(0);
        send(Duration.ofSeconds(1)).andExpect(status().isOk())
                .andExpect(jsonPath("$.isFallback").value(true));
    }

    @Test
    void validationStopsBadRequestsBeforeAiCall() throws Exception {
        responseBody = "{}";
        mvc(Duration.ofSeconds(2)).perform(post("/api/diaries/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST.replace("\"order\":2", "\"order\":3")))
                .andExpect(status().isBadRequest());
        assertEquals(0, calls.get());
    }
}
