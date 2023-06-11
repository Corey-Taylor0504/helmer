package com.twistlock.v2.web;

import com.twistlock.v2.config.EmbeddedConfig;
import com.twistlock.v2.config.EnvironmentConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping(value = "/api/ui")
public class UIRestController {

    @Autowired
    private EmbeddedConfig embeddedConfig;

    @Autowired
    private EnvironmentConfig environmentConfig;

    @GetMapping(value = "/rest/getAddress")
    public ResponseEntity<List<String>> getAddress(@RequestParam("project") String projectName, @RequestParam("env") String env) {
        EnvironmentConfig.Config config = "prd".equalsIgnoreCase(env) ? environmentConfig.getConfig().get("prod") : environmentConfig.getConfig().get("nonprod");
        String project = environmentConfig.getProjectAlias().get(projectName);
        if ("Cloud".equalsIgnoreCase(project)) {
            return ResponseEntity.ok(Arrays.asList(config.getCloudConsoleAddress().split(",")));
        } else {
            return ResponseEntity.ok(Arrays.asList(config.getInternalConsoleAddress().split(",")));
        }
    }

    @GetMapping(value = "/rest/getProject")
    public ResponseEntity<List<String>> getProject() {
        return ResponseEntity.ok(Arrays.asList(embeddedConfig.getProjects().split(",")));
    }
}