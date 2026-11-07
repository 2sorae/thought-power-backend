package com.thoughtpower.settings;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscription")
public class SubscriptionController {

    @GetMapping
    public SubscriptionResponse getSubscription() {
        return new SubscriptionResponse(null);
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record SubscriptionResponse(
            Object subscription
    ) {}
}
