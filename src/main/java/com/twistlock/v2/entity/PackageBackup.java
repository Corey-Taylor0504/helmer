package com.twistlock.v2.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document
@Getter
@Setter
@NoArgsConstructor
public class PackageBackup {
    @Id
    private PackageId id;
    private String environment;
    private String project;
    private String address;
    private boolean cri;
    private String experience;
    private String owner;
    private String admissionCert;
    private String admissionKey;
    private String clientCert;
    private String clientKey;
    private String helmChartUrl;
    private String fileVersion;
    private String caCert;
    private String clusterId;
    private String wssAddress;
    private String installBundle;
    private String serviceParameter;
    private String cloudProvider;
    private LocalDateTime createdDateTime;
    public PackageBackup(Package aPackage) {
        this.id = aPackage.getId();
        this.environment = aPackage.getEnvironment();
        this.project = aPackage.getProject();
        this.address = aPackage.getAddress();
        this.cri = aPackage.isCri();
        this.experience = aPackage.getExperience();
        this.owner = aPackage.getOwner();
        this.admissionCert = aPackage.getAdmissionCert();
        this.admissionKey = aPackage.getAdmissionKey();
        this.clientCert = aPackage.getClientCert();
        this.clientKey = aPackage.getClientKey();
        this.helmChartUrl = aPackage.getHelmChartUrl();
        this.fileVersion = aPackage.getFileVersion();
        this.caCert = aPackage.getCaCert();
        this.clusterId = aPackage.getClusterId();
        this.wssAddress = aPackage.getWssAddress();
        this.installBundle = aPackage.getInstallBundle();
        this.serviceParameter = aPackage.getServiceParameter();
        this.createdDateTime = LocalDateTime.now();
    }
}