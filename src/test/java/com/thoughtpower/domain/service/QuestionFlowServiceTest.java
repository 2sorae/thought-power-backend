package com.thoughtpower.domain.service;

import com.thoughtpower.domain.dto.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuestionFlowServiceTest {
    private QuestionService generator;
    private AiAnswerCheckClient checker;
    private QuestionFlowService flows;
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-11T00:00:00Z"));
    private final Clock clock = new Clock() {
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now.get(); }
    };
    @BeforeEach
    void setup() {
        generator = mock(QuestionService.class);
        checker = mock(AiAnswerCheckClient.class);
        when(generator.generateQuestion(any())).thenAnswer(call -> {
            QuestionGenerateRequest request = call.getArgument(0);
            return new QuestionGenerateResponse(request.getOrder(), request.getType(),
                    (request.getRetry() == null ? "Question " : "Easy ") + request.getOrder(), false);
        });
        flows = new QuestionFlowService(generator, checker, Duration.ofMinutes(30), 10, clock);
    }
    private QuestionGenerateRequest startRequest() {
        var request = new QuestionGenerateRequest();
        request.setEmotion("SHAME"); request.setContent("Diary"); request.setOrder(1);
        request.setType("EMOTION"); request.setPreviousAnswers(List.of());
        return request;
    }
    private QuestionAnswerSubmitRequest answer(int version, String text) {
        var request = new QuestionAnswerSubmitRequest();
        request.setQuestionVersion(version); request.setAnswer(text); return request;
    }
    @Test
    void retriesOncePerOrderAndReturnsOnlyLastAnswers() {
        when(checker.isInformative(anyString())).thenReturn(false);
        var response = flows.start(startRequest());
        var id = response.getFlowId();
        for (int order = 1; order <= 3; order++) {
            response = flows.submit(id, answer(response.getQuestionVersion(), "몰라요"));
            assertEquals("RETRY", response.getAction());
            assertEquals(order, response.getQuestion().getOrder());
            assertEquals(1, response.getRetryCount());
            response = flows.submit(id, answer(response.getQuestionVersion(), ""));
            assertEquals(order == 3 ? "COMPLETE" : "NEXT", response.getAction());
        }
        assertNull(response.getQuestion());
        assertEquals(3, response.getQuestions().size());
        for (int i = 0; i < 3; i++) {
            assertEquals("Easy " + (i + 1), response.getQuestions().get(i).getContent());
            assertEquals("", response.getQuestions().get(i).getAnswer());
        }
        ArgumentCaptor<QuestionGenerateRequest> captor = ArgumentCaptor.forClass(QuestionGenerateRequest.class);
        verify(generator, times(6)).generateQuestion(captor.capture());
        var requests = captor.getAllValues();
        assertEquals("Question 1", requests.get(1).getRetry().getQuestion());
        assertEquals("몰라요", requests.get(1).getRetry().getAnswer());
        assertTrue(requests.get(1).getPreviousAnswers().isEmpty());
        assertEquals("Easy 1", requests.get(2).getPreviousAnswers().get(0).getQuestion());
        assertEquals("", requests.get(2).getPreviousAnswers().get(0).getAnswer());
        assertEquals(2, requests.get(5).getPreviousAnswers().size());
        verify(checker, times(6)).isInformative(anyString());
    }
    @Test
    void informativeAnswerMovesOnWithoutRetry() {
        when(checker.isInformative(anyString())).thenReturn(true);
        var initial = flows.start(startRequest());
        var next = flows.submit(initial.getFlowId(), answer(0, "Happy"));
        assertEquals("NEXT", next.getAction());
        assertEquals(2, next.getQuestion().getOrder());
        assertEquals("Question 1", next.getQuestions().get(0).getContent());
    }
    @Test
    void duplicateSubmissionIsIdempotentAndDifferentAnswerConflicts() {
        var initial = flows.start(startRequest());
        var first = flows.submit(initial.getFlowId(), answer(0, ""));
        assertSame(first, flows.submit(initial.getFlowId(), answer(0, "")));
        var ex = assertThrows(AiQuestionException.class, () -> flows.submit(initial.getFlowId(), answer(0, "changed")));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(checker, times(1)).isInformative(anyString());
        verify(generator, times(2)).generateQuestion(any());
    }
    @Test
    void simultaneousDuplicateCallsDoNotConsumeTwoAttempts() throws Exception {
        var initial = flows.start(startRequest());
        var executor = Executors.newFixedThreadPool(2);
        try {
            var a = executor.submit(() -> flows.submit(initial.getFlowId(), answer(0, "")));
            var b = executor.submit(() -> flows.submit(initial.getFlowId(), answer(0, "")));
            assertSame(a.get(5, TimeUnit.SECONDS), b.get(5, TimeUnit.SECONDS));
        } finally { executor.shutdownNow(); }
        verify(checker, times(1)).isInformative(anyString());
    }
    @Test
    void rejectedAiGenerationDoesNotConsumeAttempt() {
        var initial = flows.start(startRequest());
        doThrow(new AiQuestionException(HttpStatus.BAD_REQUEST,"INVALID_REQUEST","error"))
                .doReturn(new QuestionGenerateResponse(1,"EMOTION","Easy",true)).when(generator).generateQuestion(any());
        assertThrows(AiQuestionException.class, () -> flows.submit(initial.getFlowId(), answer(0,"")));
        var retry = flows.submit(initial.getFlowId(), answer(0,""));
        assertEquals("RETRY",retry.getAction());
        assertEquals(1,retry.getQuestionVersion());
    }
    @Test
    void expiryAndUnknownIdRequireRestart() {
        var initial = flows.start(startRequest());
        now.set(now.get().plus(Duration.ofMinutes(31)));
        assertEquals(HttpStatus.GONE, assertThrows(AiQuestionException.class,
                () -> flows.submit(initial.getFlowId(),answer(0,""))).getStatus());
        assertEquals(HttpStatus.GONE, assertThrows(AiQuestionException.class,
                () -> flows.submit(UUID.randomUUID(),answer(0,""))).getStatus());
    }
    @Test
    void completedFlowAllowsReplayButNotMoreQuestions() {
        when(checker.isInformative(anyString())).thenReturn(true);
        var response = flows.start(startRequest());
        var id=response.getFlowId();
        for(int i=0;i<3;i++) response=flows.submit(id,answer(i,"ok"));
        assertSame(response,flows.submit(id,answer(2,"ok")));
        assertThrows(AiQuestionException.class,()->flows.submit(id,answer(3,"ok")));
    }
    @Test
    void independentFlowsHaveIndependentRetryCounts() {
        var a=flows.start(startRequest()); var b=flows.start(startRequest());
        assertEquals("RETRY",flows.submit(a.getFlowId(),answer(0,"" )).getAction());
        assertEquals("RETRY",flows.submit(b.getFlowId(),answer(0,"" )).getAction());
    }
}
