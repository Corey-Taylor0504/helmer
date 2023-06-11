package com.twistlock.v2.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ResponseDTO {
    @JsonIgnore
    private String uploadLocation;
    @JsonIgnore
    private String outputLocation;
    @JsonIgnore
    private String inputLocation;
    @JsonIgnore
    private String gcpProjectId;
    @JsonIgnore
    private String subExperience;
    @JsonIgnore
    private String subExperienceOwner;
    @JsonIgnore
    private String helmChartUrl;
    @JsonIgnore
    private String clusterName;
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
}
