package com.example.requestdemo.config;

import com.example.requestdemo.entity.ProjectProperties;
import com.example.requestdemo.entity.RequestProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
@Slf4j
public class ProjectConfig {

    @Bean
    RequestProperties requestProperties(ProjectProperties properties){
        try {
            File file = new File(properties.getJsonPath());
            String json = FileUtils.readFileToString(file, "UTF-8");
            return new ObjectMapper().readValue(json, RequestProperties.class);
        } catch (IOException e) {
            log.info("requestProperties初始化错误");
            e.printStackTrace();
            return null;
        }
    }
}
