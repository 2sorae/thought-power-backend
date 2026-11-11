package com.thoughtpower.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI thoughtPowerOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Thought Power Backend API").version("v1")
                        .description("생각의 힘 백엔드 API. 질문 흐름은 question-flows로 시작하며, COMPLETE 이후 POST /api/diaries로 최종 저장합니다."))
                .addServersItem(new Server().url("/").description("현재 접속한 서버"));
    }
}
