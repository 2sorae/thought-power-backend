package com.thoughtpower.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI thoughtPowerOpenAPI() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("ClientId", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("X-Client-Id")
                        .description("부스용 브라우저 UUID v4. 사용자·일기·리포트·질문 흐름 API에 필요합니다. 정식 인증이 아닙니다.")))
                .addSecurityItem(new SecurityRequirement().addList("ClientId"))
                .info(new Info().title("Thought Power Backend API").version("v1")
                        .description("생각의 힘 백엔드 API. 질문 흐름은 question-flows로 시작하며, COMPLETE 이후 POST /api/diaries로 최종 저장합니다."))
                .addServersItem(new Server().url("/").description("현재 접속한 서버"));
    }
}
