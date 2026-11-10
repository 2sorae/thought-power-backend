package com.thoughtpower.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {


    public void addCorsMapping(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("https://thought-power-frontend.vercel.app")
                .allowedMethods("GET", "POST", "PUT","PATCH", "DELETE","OPTIONS")
                .allowedHeaders("*");
    }
}
