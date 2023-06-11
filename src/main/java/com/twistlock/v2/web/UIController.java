package com.twistlock.v2.web;

import com.twistlock.v2.api.PackageService;
import com.twistlock.v2.api.TwistLockClient;
import com.twistlock.v2.api.VaultService;
import com.twistlock.v2.config.EmbeddedConfig;
import com.twistlock.v2.config.EnvironmentConfig;
import com.twistlock.v2.dto.ResponseDTO;
import com.twistlock.v2.dto.TwistLockDTO;
import com.twistlock.v2.entity.Package;
import com.twistlock.v2.entity.PackageBackup;
import com.twistlock.v2.entity.PackageId;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Optional;

@Controller
public class UIController {

    private static final Logger LOGGER = Logger.getLogger(UIController.class);
    @Autowired
    private TwistLockClient twistLockClient;
    @Autowired
    private EnvironmentConfig environmentConfig;
    @Autowired
    private EmbeddedConfig embeddedConfig;
    @Autowired
    private PackageService packageService;
    @Autowired
    private VaultService vaultService;

    @RequestMapping(value = "/index", method = {RequestMethod.GET, RequestMethod.POST})
    public String loadIndex() {
        if (!embeddedConfig.isSamlEnabled()) {
            return "cloud";
        }
        return "index";
    }

    @RequestMapping(value = "/cloud", method = {RequestMethod.GET, RequestMethod.POST})
    public String cloud(HttpServletRequest request) {
        request.setAttribute("responseFlag", "");
        return "cloud";
    }

    @RequestMapping(value = "/logout", method = {RequestMethod.GET, RequestMethod.POST})
    public String exit(HttpServletRequest request) {
        request.getSession().invalidate();
        return "index";
    }

    @RequestMapping(value = "/delete", method = {RequestMethod.GET})
    public String deletePackage(@RequestParam("gcpProjectId") String gcpProjectId, @RequestParam("clusterId") String clusterId, HttpServletRequest request) {
        if (!hasRole("ADMIN_USER")) {
            throw new RuntimeException(" You are not authorized to access this url");
        }
        PackageId packageId = new PackageId();
        packageId.setCluster(clusterId);
        packageId.setGcpId(gcpProjectId);
        Optional<Package> packageOptional = packageService.getPackage(packageId);
        if (packageOptional.isPresent()) {
            if (environmentConfig.isDeleteChartEnabled()) {
                EnvironmentConfig.Config config = "prd".equalsIgnoreCase(packageOptional.get().getEnvironment()) ? environmentConfig.getConfig().get("prod") : environmentConfig.getConfig().get("nonprod");
                EnvironmentConfig.UploadConfig uploadConfig = config.getUpload().get(packageOptional.get().getProject().toLowerCase());
                String fileVersion = packageOptional.get().getFileVersion();
                twistLockClient.deleteFile(gcpProjectId, clusterId, fileVersion, uploadConfig);
            }
            packageService.deletePackage(packageId);
            LOGGER.debug("Package Deleted for environment: " + packageOptional.get().getEnvironment() +
                    " GCP ProjectID: " + gcpProjectId +
                    " ClusterName:" + clusterId +
                    " SubExperience: " + packageOptional.get().getExperience() +
                    " SubExperienceOwner: " + packageOptional.get().getOwner());
            request.setAttribute("responseFlag", "Record has been deleted successfully");
        } else {
            request.setAttribute("responseFlag", "Record not found");
        }
        return "cloud";
    }

    @RequestMapping(value = "/upgradeCluster", method = {RequestMethod.GET})
    public String upgradeCluster(@RequestParam("gcpProjectId") String gcpProjectId, @RequestParam("clusterId") String clusterId, HttpServletRequest request) throws URISyntaxException, IOException {
        PackageId packageId = new PackageId();
        packageId.setCluster(clusterId);
        packageId.setGcpId(gcpProjectId);
        Optional<Package> packageOptional = packageService.getPackage(packageId);
        if (packageOptional.isPresent()){
            String chartFileName = String.join("-", "twistlock-defender", packageId.getGcpId(), packageId.getCluster(), embeddedConfig.getTwistLockVersion() + ".tgz");
            String url = StringUtils.isNotEmpty(packageOptional.get().getHelmChartUrl())?packageOptional.get().getHelmChartUrl():"";
            String helmUrl = url.substring(url.indexOf("charts")+"charts/".length());
            if (helmUrl.equals(chartFileName)){
                request.setAttribute("responseFlag", "GCP ProjectId "+gcpProjectId+" Cluster "+clusterId+" not eligible to upgrade");
                return "cloud";
            }
            String environment = packageOptional.get().getEnvironment();
            EnvironmentConfig.Config config = "prd".equalsIgnoreCase(environment) ? environmentConfig.getConfig().get("prod") : environmentConfig.getConfig().get("nonprod");
            boolean cli = packageOptional.get().isCri();
            TwistLockDTO twistLockDTO = new TwistLockDTO();
            twistLockDTO.setCluster(clusterId);
            twistLockDTO.setConsoleAddr(packageOptional.get().getAddress());
            twistLockDTO.setImage(environmentConfig.getImageUrl());
            twistLockDTO.setCri(cli);
            twistLockDTO.setTwistLockVersion(embeddedConfig.getTwistLockVersion());
            ResponseDTO responseDTO = twistLockClient.getCloudData(twistLockDTO, packageOptional.get().getProject(), packageId.getGcpId(), config);
            EnvironmentConfig.UploadConfig uploadConfig = config.getUpload().get(packageOptional.get().getProject().toLowerCase());
            twistLockClient.upload(responseDTO, uploadConfig);
            String helmChartUrl = String.join("/", uploadConfig.getUrl().replaceAll("api/", ""), new File(responseDTO.getUploadLocation()).getName());
            if (embeddedConfig.isMongoDBEnabled()) {
                Package aPackage = packageOptional.isPresent() ? packageOptional.get() : new Package();
                PackageBackup packageBackup = null;
                if (!packageOptional.isPresent()) {
                    aPackage.setId(packageId);
                } else {
                    packageBackup = new PackageBackup(packageOptional.get());
                }
                aPackage.setCri(cli);
                aPackage.setAddress(packageOptional.get().getAddress());
                aPackage.setEnvironment(environment);
                aPackage.setExperience(packageOptional.get().getExperience());
                aPackage.setOwner(packageOptional.get().getOwner());
                aPackage.setProject(packageOptional.get().getProject());
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
                LOGGER.debug("Package Upgraded for environment: " + environment +
                        " GCP ProjectID: " + gcpProjectId +
                        " ClusterName:" + clusterId+
                        " SubExperience: " + packageOptional.get().getExperience() +
                        " SubExperienceOwner: " + packageOptional.get().getOwner());
            }
            String env = "prd".equalsIgnoreCase(environment) ? "prod" : "nonprod";
            String projectId = gcpProjectId;
            String clusterName = clusterId;
            String path = String.join("/", env, projectId, clusterName);
            vaultService.saveVault(path, helmChartUrl);
            request.setAttribute("responseFlag", "Package upgraded successfully for GCP ProjectId "+gcpProjectId+" Cluster "+clusterId);
        }
        return "cloud";
    }

    @PostMapping("/cloudData")
    public String cloudData(HttpServletRequest request)
            throws URISyntaxException, IOException {
        String environment = request.getParameter("env");
        PackageId packageId = new PackageId();
        packageId.setCluster(request.getParameter("cluster"));
        packageId.setGcpId(request.getParameter("projectId"));
        boolean searchFlag = Boolean.parseBoolean(request.getParameter("searchFlag"));
        if (searchFlag) {
            if (embeddedConfig.isMongoDBEnabled()) {
                Optional<Package> searchData = packageService.getPackage(packageId);
                if (searchData.isPresent()) {
                    String chartFileName = String.join("-", "twistlock-defender", packageId.getGcpId(), packageId.getCluster(), embeddedConfig.getTwistLockVersion() + ".tgz");
                    ResponseDTO searchResponse = new ResponseDTO();
                    searchResponse.setSubExperienceOwner(searchData.get().getOwner());
                    searchResponse.setSubExperience(searchData.get().getExperience());
                    searchResponse.setGcpProjectId(packageId.getGcpId());
                    searchResponse.setClusterId(searchData.get().getClusterId());
                    searchResponse.setAdmissionCert(searchData.get().getAdmissionCert());
                    searchResponse.setAdmissionKey(searchData.get().getAdmissionKey());
                    searchResponse.setHelmChartUrl(searchData.get().getHelmChartUrl());
                    searchResponse.setClusterName(packageId.getCluster());
                    if (hasRole("ADMIN_USER")) {
                        request.setAttribute("deleteFlag", "true");
                    }
                    String url = StringUtils.isNotEmpty(searchData.get().getHelmChartUrl())?searchData.get().getHelmChartUrl():"";
                    String helmUrl = url.substring(url.indexOf("charts")+"charts/".length());
                    if (!helmUrl.equals(chartFileName)){
                        request.setAttribute("upgradeFlag", "true");
                    }
                    if (StringUtils.isNotEmpty(searchData.get().getHelmChartUrl())) {
                        request.setAttribute("helmFlag", "true");
                    }
                    request.setAttribute("responseData", searchResponse);
                } else {
                    request.setAttribute("responseFlag", "No record found");
                }
            } else {
                request.setAttribute("responseFlag", "Unsupported operation please enable database");
            }

            return "cloud";
        }
        if (embeddedConfig.isMongoDBEnabled()) {
            if (packageService.getPackage(packageId).isPresent()) {
                request.setAttribute("responseFlag", "Record already exist");
                return "cloud";
            }
        }
        EnvironmentConfig.Config config = "prd".equalsIgnoreCase(environment) ? environmentConfig.getConfig().get("prod") : environmentConfig.getConfig().get("nonprod");
        boolean cli = Boolean.parseBoolean(request.getParameter("cli"));
        String project = environmentConfig.getProjectAlias().get(request.getParameter("project"));
        boolean isHelmEnabled = environmentConfig.getSkipList().contains(request.getParameter("project"));
        TwistLockDTO twistLockDTO = new TwistLockDTO();
        twistLockDTO.setCluster(request.getParameter("cluster"));
        twistLockDTO.setConsoleAddr(request.getParameter("address"));
        twistLockDTO.setImage(environmentConfig.getImageUrl());
        if ("GKE-AUTOPILOT".equals(request.getParameter("project"))){
            twistLockDTO.setCri(true);
        }else{
            twistLockDTO.setCri(cli);
        }
        ResponseDTO responseDTO = twistLockClient.getCloudData(twistLockDTO, project, packageId.getGcpId(), config);
        EnvironmentConfig.UploadConfig uploadConfig = config.getUpload().get(project.toLowerCase());
        twistLockClient.upload(responseDTO, uploadConfig);
        String helmChartUrl = String.join("/", uploadConfig.getUrl().replaceAll("api/", ""), new File(responseDTO.getUploadLocation()).getName());
        if (embeddedConfig.isMongoDBEnabled()) {
            Package aPackage = new Package();
            aPackage.setId(packageId);
            aPackage.setCri(cli);
            aPackage.setAddress(request.getParameter("address"));
            aPackage.setEnvironment(environment);
            aPackage.setExperience(request.getParameter("experience"));
            aPackage.setOwner(request.getParameter("owner"));
            aPackage.setProject(project);
            aPackage.setCaCert(responseDTO.getCaCert());
            aPackage.setAdmissionCert(responseDTO.getAdmissionCert());
            aPackage.setAdmissionKey(responseDTO.getAdmissionKey());
            aPackage.setClientCert(responseDTO.getClientCert());
            aPackage.setClientKey(responseDTO.getClientKey());
            aPackage.setClusterId(responseDTO.getClusterId());
            aPackage.setHelmChartUrl(!isHelmEnabled ? helmChartUrl : StringUtils.EMPTY);
            aPackage.setFileVersion(embeddedConfig.getTwistLockVersion());
            aPackage.setServiceParameter(responseDTO.getServiceParameter());
            aPackage.setWssAddress(responseDTO.getWssAddress());
            aPackage.setInstallBundle(responseDTO.getInstallBundle());
            aPackage.setCreatedDateTime(LocalDateTime.now());
            packageService.savePackage(aPackage);
        }
        responseDTO.setClusterName(packageId.getCluster());
        if (hasRole("ADMIN_USER")) {
            request.setAttribute("deleteFlag", "true");
        }
        responseDTO.setGcpProjectId(request.getParameter("projectId"));
        responseDTO.setSubExperience(request.getParameter("experience"));
        responseDTO.setSubExperienceOwner(request.getParameter("owner"));
        String env = "prd".equalsIgnoreCase(environment) ? "prod" : "nonprod";
        String projectId = request.getParameter("projectId");
        String clusterName = request.getParameter("cluster");
        String path = String.join("/", env, projectId, clusterName);
        if (!isHelmEnabled) {
            request.setAttribute("helmFlag", "true");
            responseDTO.setHelmChartUrl(helmChartUrl);
            vaultService.saveVault(path, helmChartUrl);
        }
        LOGGER.debug("Package Created for environment: " + environment +
                " GCP ProjectID: " + request.getParameter("projectId") +
                " ClusterName:" + request.getParameter("cluster") +
                " SubExperience: " + request.getParameter("experience") +
                " SubExperienceOwner: " + request.getParameter("owner"));
        request.setAttribute("responseFlag", "Package created successfully");
        request.setAttribute("responseData", responseDTO);
        return "cloud";
    }

    private boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        for (GrantedAuthority grantedAuthority : authentication.getAuthorities()) {
            if (role.equals(grantedAuthority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

}