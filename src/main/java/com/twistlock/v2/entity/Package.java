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
public class Package {
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
    private LocalDateTime createdDateTime;
}