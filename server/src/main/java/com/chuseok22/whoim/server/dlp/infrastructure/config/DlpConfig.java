package com.chuseok22.whoim.server.dlp.infrastructure.config;

import com.chuseok22.whoim.server.dlp.infrastructure.properties.DlpProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DlpProperties.class)
public class DlpConfig {

}
