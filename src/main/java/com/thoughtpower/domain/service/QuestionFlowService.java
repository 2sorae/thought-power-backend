package com.thoughtpower.domain.service;

import com.thoughtpower.domain.dto.*;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class QuestionFlowService {
    private static final String[] TYPES = {"EMOTION", "REASON", "PERSPECTIVE"};
    private final QuestionService questions;
    private final AiAnswerCheckClient checker;
    private final Duration ttl;
    private final int maxFlows;
    private final Clock clock;
    private final Map<UUID, Flow> flows = new ConcurrentHashMap<>();

    @Autowired
    public QuestionFlowService(QuestionService questions, AiAnswerCheckClient checker,
            @Value("${diary.question-flow.ttl:30m}") Duration ttl,
            @Value("${diary.question-flow.max-active:1000}") int maxFlows) {
        this(questions, checker, ttl, maxFlows, Clock.systemUTC());
    }

    QuestionFlowService(QuestionService questions, AiAnswerCheckClient checker,
            Duration ttl, int maxFlows, Clock clock) {
        if (ttl.isZero() || ttl.isNegative() || maxFlows < 1) throw new IllegalArgumentException("Invalid flow limits");
        this.questions = questions;
        this.checker = checker;
        this.ttl = ttl;
        this.maxFlows = maxFlows;
        this.clock = clock;
    }

    public QuestionFlowResponse start(QuestionGenerateRequest request) {
        if (!Integer.valueOf(1).equals(request.getOrder()) || !"EMOTION".equals(request.getType())
                || request.getPreviousAnswers() == null || !request.getPreviousAnswers().isEmpty()
                || request.getRetry() != null) {
            throw error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "첫 질문은 order=1, type=EMOTION, previousAnswers=[]로 시작해주세요.");
        }
        Flow flow = new Flow(request.getEmotion(), request.getContent(), clock.instant().plus(ttl));
        synchronized (flows) {
            flows.entrySet().removeIf(entry -> !entry.getValue().expiresAt.isAfter(clock.instant()));
            if (flows.size() >= maxFlows) throw error(HttpStatus.SERVICE_UNAVAILABLE, "FLOW_CAPACITY_EXCEEDED", "잠시 후 다시 시도해주세요.");
            flows.put(flow.id, flow);
        }
        try {
            flow.current = questions.generateQuestion(buildRequest(flow, 1, List.of(), null));
            return snapshot(flow, "QUESTION", null);
        } catch (RuntimeException ex) {
            flows.remove(flow.id, flow);
            throw ex;
        }
    }

    public QuestionFlowResponse submit(UUID flowId, QuestionAnswerSubmitRequest request) {
        Flow flow = flows.get(flowId);
        if (flow == null) throw expired();
        synchronized (flow) {
            if (!flow.expiresAt.isAfter(clock.instant())) {
                flows.remove(flow.id, flow);
                throw expired();
            }
            int version = request.getQuestionVersion();
            Submission previous = flow.submissions.get(version);
            if (previous != null) {
                if (previous.answer.equals(request.getAnswer())) return previous.response;
                throw error(HttpStatus.CONFLICT, "ANSWER_ALREADY_SUBMITTED", "이미 처리한 질문입니다. 최신 질문을 확인해주세요.");
            }
            if (flow.completed || version != flow.version) {
                throw error(HttpStatus.CONFLICT, "QUESTION_VERSION_MISMATCH", "최신 questionVersion으로 답변을 보내주세요.");
            }
            boolean informative = checker.isInformative(request.getAnswer());
            String action;
            if (!informative && flow.retryCount == 0) {
                QuestionGenerateRequest.Retry retry = new QuestionGenerateRequest.Retry();
                retry.setQuestion(flow.current.getContent());
                retry.setAnswer(request.getAnswer());
                // Commit the transition only after question generation succeeds.
                var generated = questions.generateQuestion(buildRequest(flow, flow.current.getOrder(), flow.answers, retry));
                flow.current = generated;
                flow.retryCount = 1;
                action = "RETRY";
            } else {
                List<DiaryCreateRequest.QuestionAnswer> accepted = new ArrayList<>(flow.answers);
                DiaryCreateRequest.QuestionAnswer answer = new DiaryCreateRequest.QuestionAnswer();
                answer.setOrder(flow.current.getOrder());
                answer.setType(com.thoughtpower.domain.entity.QuestionStage.valueOf(flow.current.getType()));
                answer.setContent(flow.current.getContent());
                answer.setAnswer(request.getAnswer());
                accepted.add(answer);
                if (flow.current.getOrder() == 3) {
                    flow.completed = true;
                    flow.current = null;
                    action = "COMPLETE";
                } else {
                    flow.current = questions.generateQuestion(buildRequest(flow, flow.current.getOrder() + 1, accepted, null));
                    action = "NEXT";
                }
                flow.answers = accepted;
                flow.retryCount = 0;
            }
            flow.version++;
            QuestionFlowResponse response = snapshot(flow, action, informative);
            flow.submissions.put(version, new Submission(request.getAnswer(), response));
            return response;
        }
    }

    private QuestionGenerateRequest buildRequest(Flow flow, int order,
            List<DiaryCreateRequest.QuestionAnswer> answers, QuestionGenerateRequest.Retry retry) {
        QuestionGenerateRequest request = new QuestionGenerateRequest();
        request.setEmotion(flow.emotion);
        request.setContent(flow.content);
        request.setOrder(order);
        request.setType(TYPES[order - 1]);
        List<QuestionGenerateRequest.PreviousAnswer> history = new ArrayList<>();
        for (var item : answers) {
            var previous = new QuestionGenerateRequest.PreviousAnswer();
            previous.setOrder(item.getOrder());
            previous.setType(item.getType().name());
            previous.setQuestion(item.getContent());
            previous.setAnswer(item.getAnswer());
            history.add(previous);
        }
        request.setPreviousAnswers(history);
        request.setRetry(retry);
        return request;
    }

    private QuestionFlowResponse snapshot(Flow flow, String action, Boolean informative) {
        return new QuestionFlowResponse(flow.id, action, flow.version, flow.retryCount,
                informative, flow.current, flow.answers);
    }
    private AiQuestionException expired() {
        return error(HttpStatus.GONE, "QUESTION_FLOW_EXPIRED", "질문 진행 정보가 만료되었습니다. 작성 내용을 보관하고 질문을 다시 시작해주세요.");
    }
    private AiQuestionException error(HttpStatus status, String code, String message) {
        return new AiQuestionException(status, code, message);
    }
    private static class Flow {
        private final UUID id = UUID.randomUUID();
        private final String emotion;
        private final String content;
        private final Instant expiresAt;
        private int version;
        private int retryCount;
        private boolean completed;
        private QuestionGenerateResponse current;
        private List<DiaryCreateRequest.QuestionAnswer> answers = List.of();
        private final Map<Integer, Submission> submissions = new HashMap<>();
        private Flow(String emotion, String content, Instant expiresAt) {
            this.emotion = emotion;
            this.content = content;
            this.expiresAt = expiresAt;
        }
    }
    private static class Submission {
        private final String answer;
        private final QuestionFlowResponse response;
        private Submission(String answer, QuestionFlowResponse response) {
            this.answer = answer;
            this.response = response;
        }
    }
}
