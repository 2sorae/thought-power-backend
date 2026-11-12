package com.thoughtpower;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class BrowserIdentityIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    private MockMvc mvc;
    private final ObjectMapper json = new ObjectMapper();
    private String a;
    private String b;
    private final LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
    private static final String PROFILE = """
            {"name":"Test child","age":8,"helperType":"PARENT","agreements":[
              {"type":"GUARDIAN","version":"1.0","agreed":true},
              {"type":"SERVICE","version":"1.0","agreed":true},
              {"type":"PRIVACY","version":"1.0","agreed":true}]}
            """;
    private static final String DIARY = """
            {"emotion":"JOY","inputType":"TEXT","content":"Test diary","questions":[
              {"order":1,"type":"EMOTION","content":"Question 1?","answer":""},
              {"order":2,"type":"REASON","content":"Question 2?","answer":"Answer 2"},
              {"order":3,"type":"PERSPECTIVE","content":"Question 3?","answer":"Answer 3"}]}
            """;
    private static final String FLOW = """
            {"emotion":"JOY","content":"Test diary","order":1,"type":"EMOTION","previousAnswers":[]}
            """;
    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        a = UUID.randomUUID().toString(); b = UUID.randomUUID().toString();
        jdbc.update("DELETE FROM diary_questions");
        jdbc.update("DELETE FROM diaries");
        jdbc.update("DELETE FROM emotion_reports");
        jdbc.update("DELETE FROM children");
    }
    private long onboard(String client) throws Exception {
        String body = mvc.perform(post("/api/users/me/onboarding").header("X-Client-Id", client)
                        .contentType("application/json").content(PROFILE))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("userId").asLong();
    }
    private long save(String client) throws Exception {
        String body = mvc.perform(post("/api/diaries").header("X-Client-Id", client)
                        .contentType("application/json").content(DIARY))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("diaryId").asLong();
    }
    @Test
    void onboardingIsPerBrowserAndLegacyDataIsNotExposed() throws Exception {
        jdbc.update("INSERT INTO children(id,name,age,age_recorded_at,guardian_type) VALUES(1,'Legacy',8,CURRENT_DATE,'PARENT')");
        mvc.perform(get("/api/users/me").header("X-Client-Id",a)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_FOUND"));
        long first = onboard(a);
        assertNotEquals(1, first);
        assertEquals(first, onboard(a.toUpperCase()));
        mvc.perform(get("/api/users/me").header("X-Client-Id",b)).andExpect(status().isNotFound());
        assertNotEquals(first, onboard(b));
        mvc.perform(patch("/api/users/me").header("X-Client-Id",a).contentType("application/json")
                .content("{\"name\":\"Changed\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/users/me").header("X-Client-Id",b)).andExpect(jsonPath("$.name").value("Test child"));
        assertEquals("Legacy",jdbc.queryForObject("SELECT name FROM children WHERE id=1",String.class));
    }
    @Test
    void dailyLimitListsCalendarReportsAndDeletionAreIsolated() throws Exception {
        onboard(a); onboard(b);
        long diary = save(a);
        mvc.perform(post("/api/diaries").header("X-Client-Id",a).contentType("application/json").content(DIARY))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DIARY_ALREADY_EXISTS"));
        mvc.perform(get("/api/diaries").param("date",today.toString()).header("X-Client-Id",b))
                .andExpect(status().isOk()).andExpect(jsonPath("$.diaries.length()").value(0));
        mvc.perform(get("/api/diaries/calendar").param("year",""+today.getYear()).param("month",""+today.getMonthValue())
                .header("X-Client-Id",b)).andExpect(status().isOk()).andExpect(jsonPath("$.days.length()").value(0));
        mvc.perform(get("/api/reports/emotions").param("startDate",today.toString()).param("endDate",today.toString())
                .header("X-Client-Id",b)).andExpect(status().isOk()).andExpect(jsonPath("$.totalDiaryCount").value(0));
        mvc.perform(get("/api/diaries/"+diary).header("X-Client-Id",b)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/diaries/"+diary).header("X-Client-Id",b)).andExpect(status().isNotFound());
        mvc.perform(get("/api/diaries/"+diary).header("X-Client-Id",a)).andExpect(status().isOk());
        long other = save(b);
        assertNotEquals(diary,other);
        mvc.perform(get("/api/reports/emotions").param("startDate",today.toString()).param("endDate",today.toString())
                .header("X-Client-Id",b)).andExpect(jsonPath("$.totalDiaryCount").value(1));
        mvc.perform(delete("/api/diaries/"+diary).header("X-Client-Id",a)).andExpect(status().isNoContent());
        mvc.perform(get("/api/diaries/"+other).header("X-Client-Id",b)).andExpect(status().isOk());
    }
    @Test
    void missingMalformedAndDuplicateHeadersNeverUseSharedAccount() throws Exception {
        mvc.perform(get("/api/users/me")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_CLIENT_ID"));
        for (String invalid : List.of("", "1", "not-a-uuid", " "+a)) {
            mvc.perform(get("/api/users/me").header("X-Client-Id",invalid)).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/users/me").header("X-Client-Id",a,b)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/terms")).andExpect(status().isOk());
        mvc.perform(post("/api/diaries").header("X-Client-Id",a).contentType("application/json").content(DIARY))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PROFILE_NOT_FOUND"));
    }
    @Test
    void flowOwnershipCheckedBeforeAnswerReplay() throws Exception {
        String body=mvc.perform(post("/api/diaries/question-flows").header("X-Client-Id",a)
                .contentType("application/json").content(FLOW)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String url="/api/diaries/question-flows/"+json.readTree(body).get("flowId").asText()+"/answers";
        String answer="{\"questionVersion\":0,\"answer\":\"\"}";
        mvc.perform(post(url).header("X-Client-Id",a).contentType("application/json").content(answer)).andExpect(status().isOk());
        mvc.perform(post(url).header("X-Client-Id",b).contentType("application/json").content(answer))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("QUESTION_FLOW_NOT_FOUND"));
        mvc.perform(post(url).contentType("application/json").content(answer))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_CLIENT_ID"));
    }
    @Test
    void concurrentOnboardingCreatesOneProfileAndConcurrentSaveAllowsOneDiary() throws Exception {
        var pool=Executors.newFixedThreadPool(2);
        try {
            var first=pool.submit(()->onboard(a)); var second=pool.submit(()->onboard(a));
            assertEquals(first.get(15,TimeUnit.SECONDS),second.get(15,TimeUnit.SECONDS));
            java.util.concurrent.Callable<Integer> save=()->mvc.perform(post("/api/diaries").header("X-Client-Id",a)
                    .contentType("application/json").content(DIARY)).andReturn().getResponse().getStatus();
            var one=pool.submit(save); var two=pool.submit(save);
            var statuses=new java.util.ArrayList<>(List.of(one.get(15,TimeUnit.SECONDS),two.get(15,TimeUnit.SECONDS)));
            statuses.sort(Integer::compareTo); assertEquals(List.of(201,409),statuses);
        } finally { pool.shutdownNow(); }
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM children",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM diaries",Integer.class));
    }
    @Test
    void corsAcceptsClientHeaderAndSwaggerDescribesIt() throws Exception {
        mvc.perform(options("/api/users/me").header("Origin","https://thought-power-frontend.vercel.app")
                .header("Access-Control-Request-Method","GET").header("Access-Control-Request-Headers","X-Client-Id"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Headers","X-Client-Id"));
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.ClientId.name").value("X-Client-Id"));
    }
}
