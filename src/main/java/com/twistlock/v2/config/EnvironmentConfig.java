package com.twistlock.v2.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "twistlock")
@Getter
@Setter
public class EnvironmentConfig {

    private Map<String, Config> config = new HashMap<>();
    private Map<String, String> projectAlias = new HashMap<>();
    private List<String> adminUsers = new ArrayList<>();
    private List<String> skipList = new ArrayList<>();
    private String apiUserName;
    private String apiPassword;
    private boolean deleteChartEnabled;
    private String vaultUri;
    private String vaultToken;
    private String imageUrl;

    @Getter
    @Setter
    public static class Config {
        String authUrl;
        String renewUrl;
        String cloudUrl;
        String cloudConsoleAddress;
        String internalConsoleAddress;
        Map<String, UploadConfig> upload;
    }

    @Getter
    @Setter
    public static class UploadConfig {
        private String url;
        private String userName;
        private String password;
    }

}
