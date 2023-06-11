package com.twistlock.v2.web;

import com.twistlock.v2.api.PackageService;
import com.twistlock.v2.api.TwistLockClient;
import com.twistlock.v2.api.VaultService;
import com.twistlock.v2.config.EmbeddedConfig;
import com.twistlock.v2.config.EnvironmentConfig;
import com.twistlock.v2.dto.*;
import com.twistlock.v2.entity.Package;
import com.twistlock.v2.entity.PackageBackup;
import com.twistlock.v2.entity.PackageId;
import com.twistlock.v2.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping(value = "/api/v2")
public class ApiRestController {

    private static final Logger LOGGER = Logger.getLogger(ApiRestController.class);

    final private TwistLockClient twistLockClient;

    @Autowired
    private EmbeddedConfig embeddedConfig;

    @Autowired
    private EnvironmentConfig environmentConfig;

    @Autowired
    private PackageService packageService;

    @Autowired
    private VaultService vaultService;

    public ApiRestController(TwistLockClient twistLockClient) {
        this.twistLockClient = twistLockClient;
    }

    public static void main(String[] args) {
        System.out.println(9 % 5 != 0 ? (9 / 5) + 1 : 9 / 5);
    }

    public <T> List<T> getPageinatedData(List<T> sourceList, int page, int pageSize) {
        if (pageSize <= 0 || page <= 0) {
            throw new IllegalArgumentException("invalid page size: " + pageSize);
        }
        int fromIndex = (page - 1) * pageSize;
        if (sourceList == null || sourceList.size() <= fromIndex) {
            return Collections.emptyList();
        }
        return sourceList.subList(fromIndex, Math.min(fromIndex + pageSize, sourceList.size()));
    }

    @GetMapping(value = "/rest/packages")
    public ResponseEntity<List<PackageRespose>> getPackagesByGcpId(@RequestParam(value = "gcpId", required = false) String gcpId, @RequestParam(value = "cluster", required = false) String cluster) {
        if (StringUtils.isNotEmpty(gcpId) && StringUtils.isNotEmpty(cluster)) {
            PackageId packageId = new PackageId();
            packageId.setGcpId(gcpId);
            packageId.setCluster(cluster);
            Optional<Package> result = packageService.getPackage(packageId);
            if (result.isPresent()) {
                return new ResponseEntity(Arrays.asList(new PackageRespose(result.get())), HttpStatus.OK);
            } else {
                return new ResponseEntity(Collections.emptyList(), HttpStatus.OK);
            }
        }
        List<Package> packageList = null;
        if (StringUtils.isNotEmpty(cluster)) {
            packageList = packageService.getPackagesByCluster(cluster);
        }
        if (StringUtils.isNotEmpty(gcpId)) {
            packageList = packageService.getPackagesByGcpId(gcpId);
        }
        packageList = packageList == null ? packageService.getAllPackages() : packageList;
        List<PackageRespose> packageResposes = packageList != null ? packageList.stream().map(packageValue -> new PackageRespose(packageValue)).collect(Collectors.toList()) : Collections.emptyList();
        return new ResponseEntity(packageResposes, HttpStatus.OK);
    }

    @GetMapping(value = "/rest/allpackages")
    public ResponseEntity<PaginatedResponse> getAllPackages(@RequestParam(value = "pageNumber", required = false) String pageNumber, @RequestParam(value = "pageSize", required = false) String pageSize) {
        int pageSizeValue = StringUtils.isNotEmpty(pageSize) ? Integer.parseInt(pageSize) : 10;
        int pageNumberValue = StringUtils.isNotEmpty(pageNumber) ? Integer.parseInt(pageNumber) : 1;
        if (pageNumberValue <= 0) {
            throw new ValidationException("Page number should be greater than zero", HttpStatus.BAD_REQUEST);
        }
        List<Package> packageList = packageService.getAllPackages();
        List<PackageDetails> packageResponses = packageList != null ? packageList.stream().map(packageValue -> new PackageDetails(packageValue)).collect(Collectors.toList()) : Collections.emptyList();
        int remainer = packageResponses.size() % pageSizeValue;
        int totalPages = remainer != 0 ? (packageResponses.size() / pageSizeValue) + 1 : packageResponses.size() / pageSizeValue;
        totalPages = totalPages == 0 ? 1 : totalPages;
        if (pageNumberValue > totalPages) {
            throw new ValidationException("Page number exceeds total number of pages", HttpStatus.BAD_REQUEST);
        }
        PaginatedResponse paginatedResponse = new PaginatedResponse();
        paginatedResponse.setPageNumber(pageNumberValue);
        paginatedResponse.setPageSize(pageSizeValue);
        paginatedResponse.setTotalPages(totalPages);
        paginatedResponse.setPackages(getPageinatedData(packageResponses, pageNumberValue, pageSizeValue));
        return new ResponseEntity(paginatedResponse, HttpStatus.OK);
    }

    @PostMapping(value = "/rest/generatePackage")
    public ResponseEntity<ResponseDTO> generatePackage(@RequestBody TwistLockAPI twistLockAPI) throws IOException, URISyntaxException {
        String environment = twistLockAPI.getEnvironment();
        PackageId packageId = new PackageId();
        packageId.setCluster(twistLockAPI.getCluster());
        packageId.setGcpId(twistLockAPI.getGcpProjectId());
        if (embeddedConfig.isMongoDBEnabled()) {
            if (packageService.getPackage(packageId).isPresent()) {
                throw new RuntimeException("Record already exist");
            }
        }
        EnvironmentConfig.Config config = "prd".equalsIgnoreCase(environment) ? environmentConfig.getConfig().get("prod") : environmentConfig.getConfig().get("nonprod");
        boolean cli = Boolean.parseBoolean(twistLockAPI.getCri());
        TwistLockDTO twistLockDTO = new TwistLockDTO();
        twistLockDTO.setCluster(twistLockAPI.getCluster());
        twistLockDTO.setConsoleAddr(twistLockAPI.getAddress());
        twistLockDTO.setImage(environmentConfig.getImageUrl());
        twistLockDTO.setCri(cli);
        ResponseDTO responseDTO = twistLockClient.getCloudData(twistLockDTO, twistLockAPI.getCloudProvider(), packageId.getGcpId(), config);
        EnvironmentConfig.UploadConfig uploadConfig = config.getUpload().get(twistLockAPI.getCloudProvider().toLowerCase());
        twistLockClient.upload(responseDTO, uploadConfig);
        String helmChartUrl = String.join("/", uploadConfig.getUrl().replaceAll("api/", ""), new File(responseDTO.getUploadLocation()).getName());
        if (embeddedConfig.isMongoDBEnabled()) {
            Package aPackage = new Package();
            aPackage.setId(packageId);
            aPackage.setCri(cli);
            aPackage.setAddress(twistLockAPI.getAddress());
            aPackage.setEnvironment(environment);
            aPackage.setExperience(twistLockAPI.getSubExperience());
            aPackage.setOwner(twistLockAPI.getSubExperienceOwner());
            aPackage.setProject(twistLockAPI.getCloudProvider());
            aPackage.setCaCert(responseDTO.getCaCert());
            aPackage.setAdmissionCert(responseDTO.getAdmissionCert());
            aPackage.setInstallBundle(responseDTO.getInstallBundle());
            aPackage.setAdmissionKey(responseDTO.getAdmissionKey());
            aPackage.setClientCert(responseDTO.getClientCert());
            aPackage.setClientKey(responseDTO.getClientKey());
            aPackage.setClusterId(responseDTO.getClusterId());
            aPackage.setHelmChartUrl(helmChartUrl);
            aPackage.setFileVersion(embeddedConfig.getTwistLockVersion());
            aPackage.setServiceParameter(responseDTO.getServiceParameter());
            aPackage.setWssAddress(responseDTO.getWssAddress());
            aPackage.setCreatedDateTime(LocalDateTime.now());
            packageService.savePackage(aPackage);
            LOGGER.debug("Package Created for environment: " + environment +
                    " GCP ProjectID: " + twistLockAPI.getGcpProjectId() +
                    " ClusterName:" + twistLockAPI.getCluster() +
                    " SubExperience: " + twistLockAPI.getSubExperience() +
                    " SubExperienceOwner: " + twistLockAPI.getSubExperienceOwner());
        }
        String env = "prd".equalsIgnoreCase(environment) ? "prod" : "nonprod";
        String projectId = twistLockAPI.getGcpProjectId();
        String clusterName = twistLockAPI.getCluster();
        String path = String.join("/", env, projectId, clusterName);
        vaultService.saveVault(path, helmChartUrl);
        return ResponseEntity.ok(responseDTO);
    }

    @PostMapping(value = "/rest/upgradePackage")
    public ResponseEntity<ResponseDTO> upgradePackage(@RequestBody TwistLockAPI twistLockAPI) throws IOException, URISyntaxException {
        String environment = twistLockAPI.getEnvironment();
        PackageId packageId = new PackageId();
        packageId.setCluster(twistLockAPI.getCluster());
        packageId.setGcpId(twistLockAPI.getGcpProjectId());
        EnvironmentConfig.Config config = "prd".equalsIgnoreCase(environment) ? environmentConfig.getConfig().get("prod") : environmentConfig.getConfig().get("nonprod");
        boolean cli = Boolean.parseBoolean(twistLockAPI.getCri());
        TwistLockDTO twistLockDTO = new TwistLockDTO();
        twistLockDTO.setCluster(twistLockAPI.getCluster());
        twistLockDTO.setConsoleAddr(twistLockAPI.getAddress());
        twistLockDTO.setImage(environmentConfig.getImageUrl());
        twistLockDTO.setCri(cli);
        twistLockDTO.setTwistLockVersion(twistLockAPI.getTwistLockVersion());
        ResponseDTO responseDTO = twistLockClient.getCloudData(twistLockDTO, twistLockAPI.getCloudProvider(), packageId.getGcpId(), config);
        EnvironmentConfig.UploadConfig uploadConfig = config.getUpload().get(twistLockAPI.getCloudProvider().toLowerCase());
        twistLockClient.upload(responseDTO, uploadConfig);
        String helmChartUrl = String.join("/", uploadConfig.getUrl().replaceAll("api/", ""), new File(responseDTO.getUploadLocation()).getName());
        if (embeddedConfig.isMongoDBEnabled()) {
            Optional<Package> packageOptional = packageService.getPackage(packageId);
            Package aPackage = packageOptional.isPresent() ? packageOptional.get() : new Package();
            PackageBackup packageBackup = null;
            if (!packageOptional.isPresent()) {
                aPackage.setId(packageId);
            } else {
                packageBackup = new PackageBackup(packageOptional.get());
            }
            aPackage.setCri(cli);
            aPackage.setAddress(twistLockAPI.getAddress());
            aPackage.setEnvironment(environment);
            aPackage.setExperience(twistLockAPI.getSubExperience());
            aPackage.setOwner(twistLockAPI.getSubExperienceOwner());
            aPackage.setProject(twistLockAPI.getCloudProvider());
            aPackage.setCaCert(responseDTO.getCaCert());
            aPackage.setAdmissionCert(responseDTO.getAdmissionCert());
            aPackage.setInstallBundle(responseDTO.getInstallBundle());
            aPackage.setAdmissionKey(responseDTO.getAdmissionKey());
            aPackage.setClientCert(responseDTO.getClientCert());
            aPackage.setClientKey(responseDTO.getClientKey());
            aPackage.setClusterId(responseDTO.getClusterId());
            aPackage.setHelmChartUrl(helmChartUrl);
            aPackage.setFileVersion(embeddedConfig.getTwistLockVersion());
            aPackage.setServiceParameter(responseDTO.getServiceParameter());
            aPackage.setWssAddress(responseDTO.getWssAddress());
            aPackage.setCreatedDateTime(LocalDateTime.now());
            packageService.savePackage(aPackage);
            if (packageBackup != null) {
                packageService.savePackage(packageBackup);
            }
            LOGGER.debug("Package Created for environment: " + environment +
                    " GCP ProjectID: " + twistLockAPI.getGcpProjectId() +
                    " ClusterName:" + twistLockAPI.getCluster() +
                    " SubExperience: " + twistLockAPI.getSubExperience() +
                    " SubExperienceOwner: " + twistLockAPI.getSubExperienceOwner());
        }
        String env = "prd".equalsIgnoreCase(environment) ? "prod" : "nonprod";
        String projectId = twistLockAPI.getGcpProjectId();
        String clusterName = twistLockAPI.getCluster();
        String path = String.join("/", env, projectId, clusterName);
        vaultService.saveVault(path, helmChartUrl);
        return ResponseEntity.ok(responseDTO);
    }
}