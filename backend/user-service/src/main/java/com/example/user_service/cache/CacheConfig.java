package com.example.user_service.cache;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

import static com.example.user_service.constant.Constants.EXPIRE_DURATION;

@Configuration
public class CacheConfig {

    @Bean(name = "userLoginCache")
    public CacheStore<String, Integer> cacheStore() {
        return new CacheStore<>(EXPIRE_DURATION, TimeUnit.SECONDS);
    }

}
