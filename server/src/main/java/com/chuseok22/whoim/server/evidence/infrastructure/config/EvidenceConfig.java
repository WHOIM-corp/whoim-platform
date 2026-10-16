package com.chuseok22.whoim.server.evidence.infrastructure.config;

import com.chuseok22.whoim.server.evidence.infrastructure.properties.EvidenceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EvidenceProperties.class)
public class EvidenceConfig {

}
