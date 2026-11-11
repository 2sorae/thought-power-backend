package com.thoughtpower.domain.controller;

import com.thoughtpower.domain.service.*;
import com.thoughtpower.domain.dto.*;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class QuestionFlowControllerTest {
    @Test
    void emptyAnswerAcceptedButMissingAnswerAndVersionRejected() throws Exception {
        var service=mock(QuestionFlowService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new QuestionFlowController(service)).build();
        String url="/api/diaries/question-flows/"+UUID.randomUUID()+"/answers";
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content("{\"questionVersion\":0,\"answer\":\"\"}"))
                .andExpect(status().isOk());
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content("{\"questionVersion\":0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content("{\"answer\":\"\"}"))
                .andExpect(status().isBadRequest());
        verify(service,times(1)).submit(any(),any());
    }
    @Test
    void diaryDtoAllowsEmptyFinalAnswerButNotNull() {
        var item=new DiaryCreateRequest.QuestionAnswer();
        item.setOrder(1); item.setType(com.thoughtpower.domain.entity.QuestionStage.EMOTION);
        item.setContent("Question"); item.setAnswer("");
        try(var factory=Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(item).isEmpty());
            item.setAnswer(null);
            assertFalse(factory.getValidator().validate(item).isEmpty());
        }
    }
}
