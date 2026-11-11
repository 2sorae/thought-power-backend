package com.thoughtpower.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {


    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("https://thought-power-frontend.vercel.app",
                        "http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT","PATCH", "DELETE","OPTIONS")
                .allowedHeaders("*");
    }
}
