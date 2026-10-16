package com.chuseok22.whoim.server.setting.infrastructure.repository;

import com.chuseok22.whoim.server.setting.infrastructure.entity.SettingConfig;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingConfigRepository extends JpaRepository<SettingConfig, UUID> {

  Optional<SettingConfig> findByConfigKey(String configKey);
}
