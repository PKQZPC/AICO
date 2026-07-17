package com.project.smart_intervention.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.TimeZone;

/**
 * @ClassName: JacksonConfig
 * @Description:
 * @Date: 2025/4/22
 * @Version: 1.0
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 娉ㄥ唽Java 8鏃堕棿妯″潡
        mapper.registerModule(new JavaTimeModule());
        // 绂佺敤鏃ユ湡鏃堕棿鎴虫牸寮?
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 璁剧疆鏃跺尯
        mapper.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        return mapper;
    }
}
