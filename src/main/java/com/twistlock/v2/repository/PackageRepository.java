package com.twistlock.v2.repository;

import com.twistlock.v2.entity.Package;
import com.twistlock.v2.entity.PackageId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PackageRepository extends MongoRepository<Package, PackageId> {

    List<Package> findByIdGcpId(String gcpId);

    List<Package> findByIdCluster(String cluster);

}
