package com.twistlock.v2.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "role.auth")
@Getter
@Setter
public class UserConfig {
	private String userName;
	private String password;
}
