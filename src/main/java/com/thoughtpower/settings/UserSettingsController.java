package com.thoughtpower.settings;

import com.thoughtpower.domain.entity.GuardianType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    public UserSettingsController(UserSettingsService userSettingsService) {
        this.userSettingsService = userSettingsService;
    }

    @GetMapping
    public UserSettingsService.UserResponse getUser() {
        return userSettingsService.getUser();
    }

    @PatchMapping
    public UserSettingsService.UserResponse updateUser(
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return userSettingsService.updateUser(request);
    }

    public record UserUpdateRequest(
            @Size(max=50) String name,
            @Min(6) @Max(10) Integer age,
            GuardianType helperType
    ) {}
}
