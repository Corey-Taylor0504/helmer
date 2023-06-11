package com.twistlock.v2.api;

import com.twistlock.v2.entity.Package;
import com.twistlock.v2.entity.PackageBackup;
import com.twistlock.v2.entity.PackageId;
import com.twistlock.v2.repository.PackageBackupRepository;
import com.twistlock.v2.repository.PackageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PackageService {

    @Autowired
    private PackageRepository packageRepository;

    @Autowired
    private PackageBackupRepository packageBackupRepository;

    public void savePackage(Package aPackage) {
        packageRepository.save(aPackage);
    }

    public void savePackage(PackageBackup aPackage) {
        packageBackupRepository.save(aPackage);
    }

    public void deletePackage(PackageId aPackage) {
        packageRepository.deleteById(aPackage);
    }

    public Optional<Package> getPackage(PackageId aPackage) {
        return packageRepository.findById(aPackage);
    }

    public List<Package> getAllPackages() {
        return packageRepository.findAll();
    }

    public List<Package> getPackagesByGcpId(String gcpId) {
        return packageRepository.findByIdGcpId(gcpId);
    }

    public List<Package> getPackagesByCluster(String cluster) {
        return packageRepository.findByIdCluster(cluster);
    }
}
