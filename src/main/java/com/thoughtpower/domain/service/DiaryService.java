package com.thoughtpower.domain.service;

import com.thoughtpower.identity.CurrentClient;
import com.thoughtpower.domain.dto.DiaryCreateRequest;
import com.thoughtpower.domain.dto.DiaryCreateResponse;
import com.thoughtpower.domain.entity.*;
import com.thoughtpower.domain.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DiaryService {

    private final CurrentClient currentClient;
    private final ChildRepository childRepository;
    private final EmotionRepository emotionRepository;
    private final DiaryRepository diaryRepository;
    private final DiaryQuestionRepository diaryQuestionRepository;
    private final EmotionReportRepository emotionReportRepository;

    public DiaryService(
            CurrentClient currentClient,
            ChildRepository childRepository,
            EmotionRepository emotionRepository,
            DiaryRepository diaryRepository,
            DiaryQuestionRepository diaryQuestionRepository,
            EmotionReportRepository emotionReportRepository
    ) {
        this.currentClient = currentClient;
        this.childRepository = childRepository;
        this.emotionRepository = emotionRepository;
        this.diaryRepository = diaryRepository;
        this.diaryQuestionRepository = diaryQuestionRepository;
        this.emotionReportRepository = emotionReportRepository;
    }

    @Transactional
    public DiaryCreateResponse createDiary(DiaryCreateRequest request) {

        // 글로 작성한 본문은 최대 500자
        String content = request.getContent();

        if (request.getInputType() == InputType.TEXT
                && content.codePointCount(0, content.length()) > 500) {
            throw new DiaryApiException(
                    HttpStatus.BAD_REQUEST,
                    "CONTENT_TOO_LONG",
                    "글로 작성한 일기는 500자 이하여야 합니다."
            );
        }

        validateQuestions(request.getQuestions());

        // 브라우저별 프로필을 잠가 같은 사용자의 동시 저장 요청을 순서대로 처리
        long childId = currentClient.requireChildId();
        Child child = childRepository.findForUpdate(childId)
                .orElseThrow(() -> new DiaryApiException(
                        HttpStatus.NOT_FOUND,
                        "PROFILE_NOT_FOUND",
                        "아이 프로필을 먼저 등록해주세요."
                ));

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        if (diaryRepository.existsByChild_IdAndDiaryDate(childId, today)) {
            throw new DiaryApiException(
                    HttpStatus.CONFLICT,
                    "DIARY_ALREADY_EXISTS",
                    "오늘 작성한 일기가 이미 있습니다."
            );
        }

        Emotion emotion = emotionRepository.findByCode(request.getEmotion())
                .orElseThrow(() -> new DiaryApiException(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_REQUEST",
                        "등록되지 않은 감정입니다."
                ));

        Diary diary = new Diary(
                child,
                emotion,
                today,
                request.getInputType(),
                content
        );

        diaryRepository.save(diary);

        for (DiaryCreateRequest.QuestionAnswer question
                : request.getQuestions()) {

            DiaryQuestion diaryQuestion = new DiaryQuestion(
                    diary,
                    question.getType(),
                    question.getOrder().shortValue(),
                    question.getContent(),
                    question.getAnswer()
            );

            diaryQuestionRepository.save(diaryQuestion);
        }

        // 새 일기가 생겼으므로 기존 리포트는 다시 생성하도록 처리
        emotionReportRepository.deleteReportsByChildId(child.getId());
        child.increaseDiaryVersion();

        return new DiaryCreateResponse(diary.getId(), today);
    }

    private void validateQuestions(
            List<DiaryCreateRequest.QuestionAnswer> questions
    ) {
        Set<Integer> orders = new HashSet<>();

        for (DiaryCreateRequest.QuestionAnswer question : questions) {
            QuestionStage expectedType;

            switch (question.getOrder()) {
                case 1:
                    expectedType = QuestionStage.EMOTION;
                    break;
                case 2:
                    expectedType = QuestionStage.REASON;
                    break;
                case 3:
                    expectedType = QuestionStage.PERSPECTIVE;
                    break;
                default:
                    throw new DiaryApiException(
                            HttpStatus.BAD_REQUEST,
                            "INVALID_REQUEST",
                            "질문 순서는 1, 2, 3이어야 합니다."
                    );
            }

            if (!orders.add(question.getOrder())
                    || question.getType() != expectedType) {
                throw new DiaryApiException(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_REQUEST",
                        "질문 순서가 중복되거나 질문 유형이 맞지 않습니다."
                );
            }
        }
    }

    public static class DiaryApiException extends RuntimeException {

        private final HttpStatus status;
        private final String code;

        public DiaryApiException(
                HttpStatus status,
                String code,
                String message
        ) {
            super(message);
            this.status = status;
            this.code = code;
        }

        public HttpStatus getStatus() {
            return status;
        }

        public String getCode() {
            return code;
        }
    }
}