package com.thoughtpower;

import java.time.LocalDate;
import java.time.ZoneId;
import java.sql.Date;
import java.util.*;
import java.util.concurrent.*;
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

@SpringBootTest(properties={"app.demo-seed.enabled=true",
    "spring.datasource.url=jdbc:h2:mem:seed_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"})
@ActiveProfiles("test")
class DemoDiarySeederIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    private MockMvc mvc;
    private final ObjectMapper json=new ObjectMapper();
    private final LocalDate today=LocalDate.now(ZoneId.of("Asia/Seoul"));
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

    @BeforeEach
    void setup() {
        mvc=MockMvcBuilders.webAppContextSetup(context).build();
        jdbc.update("DELETE FROM diary_questions");
        jdbc.update("DELETE FROM diaries");
        jdbc.update("DELETE FROM emotion_reports");
        jdbc.update("DELETE FROM children");
    }
    private long onboard(String client) throws Exception {
        String body=mvc.perform(post("/api/users/me/onboarding").header("X-Client-Id",client)
                .contentType("application/json").content(PROFILE)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("userId").asLong();
    }
    private int count(long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM diaries WHERE child_id=?",Integer.class,id);
    }
    @Test
    void generatesTwelveCompleteDiariesWithRequestedDatesAndEmotionsAndLeavesTodayFree() throws Exception {
        String client=UUID.randomUUID().toString(); long id=onboard(client);
        assertEquals(12,count(id));
        var dates=jdbc.query("SELECT diary_date FROM diaries WHERE child_id=?",(rs,n)->rs.getDate(1).toLocalDate(),id);
        Set<LocalDate> expected=new HashSet<>();
        for(int days=1;days<=14;days++) if(days!=5 && days!=10) expected.add(today.minusDays(days));
        assertEquals(expected,new HashSet<>(dates));
        Map<String,Integer> counts=new HashMap<>();
        jdbc.query("SELECT e.code, COUNT(*) AS total FROM diaries d JOIN emotions e ON d.emotion_id=e.id WHERE child_id=? GROUP BY e.code",
                (rs,n)->counts.put(rs.getString(1),rs.getInt(2)),id);
        assertEquals(Map.of("JOY",4,"SADNESS",2,"SURPRISE",2,"ANGER",2,"FEAR",1,"SHAME",1),counts);
        assertEquals(36,jdbc.queryForObject("SELECT COUNT(*) FROM diary_questions",Integer.class));
        assertEquals(1L,jdbc.queryForObject("SELECT diary_version FROM children WHERE id=?",Long.class,id));
        long diary=jdbc.queryForObject("SELECT MIN(id) FROM diaries WHERE child_id=?",Long.class,id);
        mvc.perform(get("/api/diaries/"+diary).header("X-Client-Id",client))
                .andExpect(status().isOk()).andExpect(jsonPath("$.questions.length()").value(3))
                .andExpect(jsonPath("$.questions[0].type").value("EMOTION"))
                .andExpect(jsonPath("$.questions[1].type").value("REASON"))
                .andExpect(jsonPath("$.questions[2].type").value("PERSPECTIVE"));
        mvc.perform(get("/api/reports/emotions").header("X-Client-Id",client)
                .param("startDate",today.minusDays(14).toString()).param("endDate",today.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalDiaryCount").value(12));
        mvc.perform(post("/api/diaries").header("X-Client-Id",client).contentType("application/json").content(DIARY))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/diaries").header("X-Client-Id",client).contentType("application/json").content(DIARY))
                .andExpect(status().isConflict());
    }
    @Test
    void repeatedOnboardingDoesNotDuplicateAndAnotherBrowserGetsSeparateSamples() throws Exception {
        String client=UUID.randomUUID().toString(); long a=onboard(client);
        assertEquals(a,onboard(client)); assertEquals(12,count(a));
        assertEquals(1L,jdbc.queryForObject("SELECT diary_version FROM children WHERE id=?",Long.class,a));
        long b=onboard(UUID.randomUUID().toString()); assertNotEquals(a,b); assertEquals(12,count(b));
        assertEquals(72,jdbc.queryForObject("SELECT COUNT(*) FROM diary_questions",Integer.class));
    }
    @Test
    void existingRealDiaryIsUntouched() throws Exception {
        String client=UUID.randomUUID().toString();
        jdbc.update("INSERT INTO children(client_id,name,age,age_recorded_at,guardian_type) VALUES(?,'Existing',8,CURRENT_DATE,'PARENT')",client);
        long id=jdbc.queryForObject("SELECT id FROM children WHERE client_id=?",Long.class,client);
        jdbc.update("INSERT INTO diaries(child_id,emotion_id,diary_date,input_type,content) VALUES(?,1,?,'TEXT','Real diary')",id,Date.valueOf(today.minusDays(20)));
        onboard(client);
        assertEquals(1,count(id));
        assertEquals("Real diary",jdbc.queryForObject("SELECT content FROM diaries WHERE child_id=?",String.class,id));
        assertEquals(0L,jdbc.queryForObject("SELECT diary_version FROM children WHERE id=?",Long.class,id));
    }
    @Test
    void failureAfterTwoDiariesRollsBackAllSamplesButOnboardingStillSucceeds() throws Exception {
        jdbc.execute("ALTER TABLE diary_questions ADD CONSTRAINT test_seed_failure CHECK(question_text <> '무지개를 봤을 때 어떤 기분이 들었나요?')");
        String client=UUID.randomUUID().toString(); long id;
        try {
            id=onboard(client);
            assertEquals(0,count(id));
            assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM diary_questions",Integer.class));
            assertEquals(0L,jdbc.queryForObject("SELECT diary_version FROM children WHERE id=?",Long.class,id));
            mvc.perform(get("/api/users/me").header("X-Client-Id",client)).andExpect(status().isOk());
        } finally { jdbc.execute("ALTER TABLE diary_questions DROP CONSTRAINT test_seed_failure"); }
        assertEquals(id,onboard(client)); assertEquals(12,count(id));
    }
    @Test
    void concurrentOnboardingSeedsOnlyOnce() throws Exception {
        String client=UUID.randomUUID().toString(); var pool=Executors.newFixedThreadPool(2);
        try {
            var a=pool.submit(()->onboard(client)); var b=pool.submit(()->onboard(client));
            long id=a.get(20,TimeUnit.SECONDS); assertEquals(id,b.get(20,TimeUnit.SECONDS));
            assertEquals(12,count(id));
            assertEquals(36,jdbc.queryForObject("SELECT COUNT(*) FROM diary_questions",Integer.class));
            assertEquals(1L,jdbc.queryForObject("SELECT diary_version FROM children WHERE id=?",Long.class,id));
        } finally { pool.shutdownNow(); }
    }
    @Test
    void rejectedOnboardingDoesNotSeed() throws Exception {
        mvc.perform(post("/api/users/me/onboarding").header("X-Client-Id",UUID.randomUUID().toString())
                .contentType("application/json").content(PROFILE.replace("true","false"))).andExpect(status().isBadRequest());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM children",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM diaries",Integer.class));
    }
}
