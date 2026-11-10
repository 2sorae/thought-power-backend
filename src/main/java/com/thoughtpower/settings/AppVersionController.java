package com.thoughtpower.settings;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/version")
public class AppVersionController {

    @GetMapping
    public AppVersionResponse getVersion() {
        return new AppVersionResponse("1.0.0", null);
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record AppVersionResponse(
            String version,
            String logoUrl
    ) {}
}
