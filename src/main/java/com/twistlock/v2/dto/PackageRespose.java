package com.twistlock.v2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.twistlock.v2.entity.Package;
import com.twistlock.v2.entity.PackageId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PackageRespose {
    private PackageId id;
    private String environment;
    private String project;
    private String address;
    private boolean cri;
    private String experience;
    private String owner;
    private String helmChartUrl;
    @JsonProperty("ADMISSION_CERT")
    private String admissionCert;
    @JsonProperty("ADMISSION_KEY")
    private String admissionKey;
    @JsonProperty("DEFENDER_CLUSTER_ID")
    private String clusterId;
    @JsonProperty("CA_CERT")
    private String caCert;
    @JsonProperty("CLIENT_CERT")
    private String clientCert;
    @JsonProperty("CLIENT_KEY")
    private String clientKey;
    @JsonProperty("WS_ADDRESS")
    private String wssAddress;
    @JsonProperty("DEFENDER_INSTALL_BUNDLE")
    private String installBundle;
    @JsonProperty("SERVICE_PARAMETER")
    private String serviceParameter;
    public PackageRespose(Package aPackage) {
        this.id = aPackage.getId();
        this.environment = aPackage.getEnvironment();
        this.project = aPackage.getProject();
        this.cri = aPackage.isCri();
        this.experience = aPackage.getExperience();
        this.owner = aPackage.getOwner();
        this.admissionCert = aPackage.getAdmissionCert();
        this.admissionKey = aPackage.getAdmissionKey();
        this.clientCert = aPackage.getClientCert();
        this.clientKey = aPackage.getClientKey();
        this.helmChartUrl = aPackage.getHelmChartUrl();
        this.caCert = aPackage.getCaCert();
        this.clusterId = aPackage.getClusterId();
        this.wssAddress = aPackage.getWssAddress();
        this.installBundle = aPackage.getInstallBundle();
        this.serviceParameter = aPackage.getServiceParameter();
    }
}
