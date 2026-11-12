package com.thoughtpower.identity;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Booth-only browser identity, not a replacement for authenticated accounts. */
@Component
public class CurrentClient {
    private final HttpServletRequest request;
    private final JdbcTemplate jdbc;

    public CurrentClient(HttpServletRequest request, JdbcTemplate jdbc) {
        this.request = request;
        this.jdbc = jdbc;
    }

    public String clientId() {
        var values = Collections.list(request.getHeaders("X-Client-Id"));
        if (values.size() != 1 || !values.get(0).matches(
                "(?i)[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")) {
            throw new ClientIdentityException(HttpStatus.BAD_REQUEST, "INVALID_CLIENT_ID",
                    "X-Client-Id 헤더에 브라우저에서 생성한 UUID v4를 보내주세요.");
        }
        return UUID.fromString(values.get(0)).toString();
    }

    public long requireChildId() {
        var ids = jdbc.query("SELECT id FROM children WHERE client_id = ?",
                (rs, row) -> rs.getLong("id"), clientId());
        if (ids.isEmpty()) {
            throw new ClientIdentityException(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND",
                    "아이 프로필을 먼저 등록해주세요.");
        }
        return ids.get(0);
    }
}
