package com.thoughtpower.domain.controller;

import com.thoughtpower.domain.service.QuestionService;
import com.thoughtpower.domain.service.AiQuestionClient;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class QuestionControllerTest {
    private MockMvc mvc;
    private static final String FIRST = "{\"order\":1,\"type\":\"EMOTION\",\"question\":\"How?\",\"answer\":\"Happy\"}";
    private static final String SECOND = "{\"order\":2,\"type\":\"REASON\",\"question\":\"Why?\",\"answer\":\"Friends\"}";

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new QuestionController(new QuestionService(new AiQuestionClient("", Duration.ofSeconds(1), Duration.ofSeconds(1), "", "")))).build();
    }

    private String body(int order, String type, String history) {
        return "{\"emotion\":\"SHAME\",\"content\":\"Today\",\"order\":" + order
                + ",\"type\":\"" + type + "\",\"previousAnswers\":" + history + "}";
    }

    private void invalid(String json) throws Exception {
        mvc.perform(post("/api/diaries/questions").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void acceptsAllThreeStages() throws Exception {
        String[] types = {"EMOTION", "REASON", "PERSPECTIVE"};
        String[] histories = {"[]", "[" + FIRST + "]", "[" + FIRST + "," + SECOND + "]"};
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/diaries/questions").contentType(MediaType.APPLICATION_JSON)
                    .content(body(i + 1, types[i], histories[i])))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.order").value(i + 1))
                    .andExpect(jsonPath("$.isFallback").value(true));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "null", "{", "{\"order\":\"bad\"}"})
    void rejectsMalformedOrMissingFields(String json) throws Exception { invalid(json); }

    @Test
    void rejectsInvalidFields() throws Exception {
        String valid = body(1, "EMOTION", "[]");
        invalid(valid.replace("SHAME", "SHYNESS"));
        invalid(valid.replace("Today", " "));
        invalid(valid.replace("SHAME", ""));
        invalid(body(0, "EMOTION", "[]"));
        invalid(body(4, "PERSPECTIVE", "[]"));
        invalid(body(1, "REASON", "[]"));
        invalid(body(1, "UNKNOWN", "[]"));
        invalid(body(1, "EMOTION", "null"));
    }

    @Test
    void rejectsMissingExtraOrOutOfOrderHistory() throws Exception {
        invalid(body(2, "REASON", "[]"));
        invalid(body(1, "EMOTION", "[" + FIRST + "]"));
        invalid(body(2, "REASON", "[null]"));
        invalid(body(3, "PERSPECTIVE", "[" + SECOND + "," + FIRST + "]"));
        invalid(body(3, "PERSPECTIVE", "[" + FIRST + "," + FIRST + "]"));
        invalid(body(3, "PERSPECTIVE", "[" + FIRST + "," + SECOND + "," + SECOND + "]"));
    }

    @Test
    void rejectsInvalidNestedAnswers() throws Exception {
        for (String item : new String[]{FIRST.replace("How?", " "), FIRST.replace("Happy", " "),
                FIRST.replace("EMOTION", "REASON"), FIRST.replace("1", "0"),
                FIRST.replace("1", "null"), FIRST.replace("\"Happy\"", "null")}) {
            invalid(body(2, "REASON", "[" + item + "]"));
        }
    }
}
