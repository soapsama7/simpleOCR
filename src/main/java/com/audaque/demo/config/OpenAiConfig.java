package com.audaque.demo.config;

import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * OpenAI API 配置 — 允许自定义 completions-path
 * <p>
 * Spring AI 默认 /v1/chat/completions，火山引擎 Ark 需要 /chat/completions。
 * 通过 spring.ai.openai.chat.completions-path 配置。
 */
@Configuration
public class OpenAiConfig {

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.chat.completions-path:/chat/completions}")
    private String completionsPath;

    @Bean
    OpenAiApi openAiApi(RestClient.Builder restClientBuilder) {
        return OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .completionsPath(completionsPath)
                .restClientBuilder(restClientBuilder)
                .build();
    }
}
