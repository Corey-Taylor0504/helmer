package com.twistlock.v2.entity;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PackageId {
    private String gcpId;
    private String cluster;
}
