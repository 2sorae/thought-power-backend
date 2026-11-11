package com.thoughtpower.onboarding;

import com.thoughtpower.identity.CurrentClient;
import com.thoughtpower.onboarding.OnboardingController.AgreementRequest;
import com.thoughtpower.onboarding.OnboardingController.OnboardingRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OnboardingService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final JdbcTemplate jdbc;
    private final CurrentClient currentClient;
    private final List<Term> terms;

    public OnboardingService(JdbcTemplate jdbc, CurrentClient currentClient) throws IOException {
        this.jdbc = jdbc;
        this.currentClient = currentClient;
        this.terms = List.of(
                loadTerm("GUARDIAN", "1.0", "guardian.txt"),
                loadTerm("SERVICE", "1.0", "service.txt"),
                loadTerm("PRIVACY", "1.0", "privacy.txt")
        );
    }
    public List<Term> getTerms() {
        return terms;
    }

    @org.springframework.transaction.annotation.Transactional
    public long complete(OnboardingRequest request){
        validateAgreements(request.agreements());

        String name = request.name().strip();
        LocalDate ageRecordedAt = LocalDate.now(SEOUL);
        String guardianType = request.helperType().name();

        jdbc.update("""
                    INSERT INTO children (client_id, name, age, age_recorded_at, guardian_type)
                                    VALUES (?, ?, ?, ?, ?)
                                    ON DUPLICATE KEY UPDATE
                                        name = ?,
                                        age = ?,
                                        age_recorded_at = ?,
                                        guardian_type = ?  
                """,
                currentClient.clientId(), name, request.age(), ageRecordedAt, guardianType, name, request.age(), ageRecordedAt, guardianType);

        return currentClient.requireChildId();
    }

    private void validateAgreements(List<AgreementRequest> submitted) {
        Map<String, AgreementRequest> byType = new HashMap<>();

        for (AgreementRequest agreement : submitted) {
            if (byType.putIfAbsent(agreement.type(), agreement) != null) {
                throw badRequest("같은 약관을 두 번 제출했습니다.");
            }
        }

        for (Term term : terms) {
            AgreementRequest agreement = byType.remove(term.type());

            if (agreement == null || !agreement.agreed()) {
                throw badRequest("필수 약관에 동의해 주세요." + term.type());
            }

            if (!term.version().equals(agreement.version())){
                throw badRequest("최신 약관을 다시 확인해 주세요." + term.type());
            }
        }

        if(!byType.isEmpty()){
            throw badRequest("알 수 없는 약관 유형입니다.");
        }
    }

    private static Term loadTerm(String type, String version, String file)
        throws IOException {
        var resource = new ClassPathResource("terms/" + file);
        String content = resource.getContentAsString(StandardCharsets.UTF_8).strip();

        if(content.isBlank()){
            throw new IllegalStateException("약관 본문이 비어 있습니다." +file);
        }

        return new Term(type, version, true, content);
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    public record Term(String type, String version, boolean required, String content){}
}
