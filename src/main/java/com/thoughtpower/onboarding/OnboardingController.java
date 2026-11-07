package com.thoughtpower.onboarding;

import com.thoughtpower.domain.entity.GuardianType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class OnboardingController {

    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @GetMapping("/terms")
    public TermsResponse getTerms() {
        return new TermsResponse(onboardingService.getTerms());
    }

    @PostMapping("/users/me/onboarding")
    public OnboardingResponse complete(@Valid @RequestBody OnboardingRequest request) {
        long userId = onboardingService.complete(request);
        return new OnboardingResponse(userId, true);
    }

    public record TermsResponse(List<OnboardingService.Term> terms) {}

    public record OnboardingRequest(
            @NotBlank @Size(max = 50) String name,
            @NotNull @Min(6) @Max(10) Integer age,
            @NotNull GuardianType helperType,
            @NotEmpty List<@NotNull @Valid AgreementRequest> agreements
    ) {}

    public record AgreementRequest(
            @NotBlank String type,
            @NotBlank String version,
            @NotNull Boolean agreed
    ) {}

    public record OnboardingResponse(long userId, boolean onboardingCompleted) {}

}
