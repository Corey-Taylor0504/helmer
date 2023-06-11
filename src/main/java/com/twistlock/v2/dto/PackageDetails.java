package com.twistlock.v2.dto;

import com.twistlock.v2.entity.Package;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PackageDetails {
    private String projectId;
    private String clusterId;
    private String project;
    private String subExperience;
    private String subExperienceOwner;
    public PackageDetails(Package aPackage) {
        this.projectId = aPackage.getId().getGcpId();
        this.clusterId = aPackage.getId().getCluster();
        this.project = aPackage.getProject();
        this.subExperience = aPackage.getExperience();
        this.subExperienceOwner = aPackage.getOwner();
    }
}
