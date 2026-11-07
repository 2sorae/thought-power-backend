package com.thoughtpower.domain.service;

import com.thoughtpower.domain.dto.QuestionGenerateRequest;
import com.thoughtpower.domain.dto.QuestionGenerateResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QuestionService {

    public QuestionGenerateResponse generateQuestion(
            QuestionGenerateRequest request
    ) {
        Integer order = request.getOrder();

        if (order == null || order < 1 || order > 3) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "질문 순서는 1, 2, 3 중 하나여야 합니다."
            );
        }

        String type;
        String content;

        switch (order) {
            case 1:
                type = "EMOTION";
                content = "그때 어떤 기분이 들었나요?";
                break;
            case 2:
                type = "REASON";
                content = "왜 그런 기분이 들었다고 생각하나요?";
                break;
            default:
                type = "PERSPECTIVE";
                content = "친구는 그때 어떤 기분이었을까요?";
                break;
        }

        if (!type.equals(request.getType())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "질문 순서와 질문 유형이 맞지 않습니다."
            );
        }

        var previousAnswers = request.getPreviousAnswers();
        if (previousAnswers == null || previousAnswers.size() != order - 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "질문 순서에 맞는 이전 질문과 답변을 모두 보내주세요.");
        }
        String[] stages = {"EMOTION", "REASON", "PERSPECTIVE"};
        for (int i = 0; i < previousAnswers.size(); i++) {
            var previous = previousAnswers.get(i);
            if (previous == null || !Integer.valueOf(i + 1).equals(previous.getOrder())
                    || !stages[i].equals(previous.getType())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "이전 질문과 답변의 순서 및 유형을 확인해주세요.");
            }
        }

        return new QuestionGenerateResponse(
                order,
                type,
                content,
                true
        );
    }
}
