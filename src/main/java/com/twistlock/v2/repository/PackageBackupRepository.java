package com.twistlock.v2.repository;

import com.twistlock.v2.entity.PackageBackup;
import com.twistlock.v2.entity.PackageId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PackageBackupRepository extends MongoRepository<PackageBackup, PackageId> {
}