package com.twistlock.v2.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TwistLockAPI {
    private String environment;
    private String gcpProjectId;
    private String subExperience;
    private String subExperienceOwner;
    private String cloudProvider;
    private String address;
    private String cluster;
    private String cri;
    private String twistLockVersion;
}