package com.example.ai_service.provider;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ai.provider.metadata")
public class AiProviderMetadataProperties {

    private String name = "temporary-noop";
    private String model = "not-configured";
}
