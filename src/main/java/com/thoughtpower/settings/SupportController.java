package com.thoughtpower.settings;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/support")
public class SupportController {

    @GetMapping
    public SupportResponse getSupport() {
        return new SupportResponse(List.of(), null);
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record SupportResponse(
            List<Object> fags,
            String inquiryUrl
    ) {}
}
