package com.thoughtpower.settings;

import com.thoughtpower.settings.UserSettingsController.UserUpdateRequest;
import com.thoughtpower.domain.entity.GuardianType;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class UserSettingsService {

    private static final long DEMO_USER_ID = 1L;

    private final JdbcTemplate jdbc;

    public UserSettingsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UserResponse getUser() {
        List<UserResponse> users = jdbc.query("""
               SELECT id, name, age, guardian_type
               FROM children
               WHERE id = ?
                """,
                (resultSet, rowNum) -> new UserResponse(
                        resultSet.getLong("id"),
                        resultSet.getString("name"),
                        resultSet.getInt("age"),
                        GuardianType.valueOf(
                                resultSet.getString("guardian_type")
                        )
                ),
                DEMO_USER_ID
        );

        if(users.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"사용자 정보가 없습니다. 온보딩을 먼저 완료해주세요.");
        }

        return users.get(0);
    }

    @Transactional
    public UserResponse updateUser(UserUpdateRequest request) {
        if (request.name() == null
            && request.age() == null
            && request.helperType() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정할 정보를 하나 이상 입력해주세요.");
        }

        String name = null;
        if (request.name() != null) {
            name = request.name().strip();

            if (name.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"이름은 공백일 수 없습니다.");
            }
        }

        String guardianType = request.helperType() == null
                ? null
                : request.helperType().name();

        Date ageRecordedAt = request.age() == null
                ? null
                : Date.valueOf(LocalDate.now(ZoneId.of("Asia/Seoul")));

        jdbc.update("""
                UPDATE children
                SET name = COALESCE(?, name),
                    age = COALESCE(?, age),
                    age_recorded_at = COALESCE(?, age_recorded_at),
                    guardian_type = COALESCE(?, guardian_type)
                WHERE id = ?
                """,
                name,
                request.age(),
                ageRecordedAt,
                guardianType,
                DEMO_USER_ID
        );

        return getUser();
    }
    public record UserResponse(
            long userId,
            String name,
            int age,
            GuardianType helperType
    ) {}

}
