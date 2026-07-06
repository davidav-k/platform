package com.example.ai_service.provider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"dev", "test"})
public class TemporaryAiProviderConfiguration {

    @Bean
    @ConditionalOnMissingBean(AiProvider.class)
    public AiProvider temporaryNoOpAiProvider() {
        return new TemporaryNoOpAiProvider();
    }
}
