package com.twistlock.v2.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TwistLockDTO {
    private String cluster;
    private boolean collectPodLabels = true;
    private String consoleAddr;
    private boolean cri = true;
    private String dockerSocketPath = "/var/run/docker.sock";
    private String image;
    private boolean istio;
    private String namespace = "twistlock";
    private String nodeSelector = "";
    private String orchestration = "Kubernetes";
    private boolean privileged;
    private String projectID = "";
    private String region = "";
    private String secretsname = "";
    private boolean selinux;
    private boolean serviceaccounts = true;
    private boolean uniqueHostname = true;
    private String twistLockVersion;
    private boolean gkeAutopilot = true;

}
