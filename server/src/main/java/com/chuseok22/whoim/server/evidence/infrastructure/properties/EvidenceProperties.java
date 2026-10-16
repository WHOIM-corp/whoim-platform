package com.chuseok22.whoim.server.evidence.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// 업로드 크기 상한(바이트). 글자 수가 아니라 바이트라서 한글은 더 적게 들어간다
@ConfigurationProperties(prefix = "evidence")
public record EvidenceProperties(@DefaultValue("131072") long maxFileBytes) {

}
