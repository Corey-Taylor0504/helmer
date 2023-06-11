package com.twistlock.v2.api;

import com.twistlock.v2.config.EmbeddedConfig;
import com.twistlock.v2.config.EnvironmentConfig;
import com.twistlock.v2.config.UserConfig;
import com.twistlock.v2.dto.ResponseDTO;
import com.twistlock.v2.dto.TwistLockDTO;
import com.twistlock.v2.exception.RetryException;
import com.twistlock.v2.exception.ValidationException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

import static java.util.Objects.nonNull;

@Service
public class TwistLockClient {

    final private static HashMap<String, Token> TOKEN_CACHE = new HashMap<>();
    final private static String TOKEN_KEY = "TOKEN_KEY";
    private static final Logger log = Logger.getLogger(TwistLockClient.class);
    final private RestTemplate restTemplate;

    private final EmbeddedConfig embeddedConfig;

    private final UserConfig userConfig;

    @Autowired
    public TwistLockClient(final RestTemplate restTemplate, EmbeddedConfig embeddedConfig, UserConfig userConfig) {
        this.restTemplate = restTemplate;
        this.embeddedConfig = embeddedConfig;
        this.userConfig = userConfig;
    }

    private ResponseEntity<String> uploadFile(String fileLocation, EnvironmentConfig.UploadConfig uploadConfig) {
        try {
            LinkedMultiValueMap<String, Object> linkedMultiValueMap = new LinkedMultiValueMap<>();
            linkedMultiValueMap.add("chart", new FileSystemResource(fileLocation));
            String plainCreds = String.join(":", uploadConfig.getUserName(), uploadConfig.getPassword());
            byte[] plainCredsBytes = plainCreds.getBytes();
            byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
            String base64Creds = new String(base64CredsBytes);
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Basic " + base64Creds);
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(
                    linkedMultiValueMap, headers);
            return restTemplate.exchange(
                    uploadConfig.getUrl(), HttpMethod.POST, requestEntity,
                    String.class);
        } catch (HttpClientErrorException e) {
            log.error("HttpClientErrorException", e);
            return new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
        } catch (HttpServerErrorException e) {
            log.error("HttpServerErrorException", e);
            return new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<String> deleteFile(String gcpId, String cluster, String fileVersion, EnvironmentConfig.UploadConfig uploadConfig) {
        String plainCreds = String.join(":", uploadConfig.getUserName(), uploadConfig.getPassword());
        byte[] plainCredsBytes = plainCreds.getBytes();
        byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
        String base64Creds = new String(base64CredsBytes);
        String file = String.join("-", "twistlock-defender", gcpId, cluster);
        String deleteUrl = String.join("/", uploadConfig.getUrl(), file, fileVersion);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + base64Creds);
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(
                headers);
        return restTemplate.exchange(
                deleteUrl, HttpMethod.DELETE, requestEntity,
                String.class);
    }

    private void unTar(final File inputFile, final File outputDir) throws IOException {
        TarArchiveInputStream debInputStream = new TarArchiveInputStream(
                new GzipCompressorInputStream(
                        new BufferedInputStream(
                                new FileInputStream(inputFile))));
        TarArchiveEntry entry;
        while ((entry = (TarArchiveEntry) debInputStream.getNextEntry()) != null) {
            final File outputFile = new File(outputDir, entry.getName());
            if (entry.isDirectory()) {
                if (!outputFile.exists()) {
                    if (!outputFile.mkdirs()) {
                        throw new IllegalStateException(String.format("Couldn't create directory %s.", outputFile.getAbsolutePath()));
                    }
                }
            } else {
                final OutputStream outputFileStream = new FileOutputStream(outputFile);
                IOUtils.copy(debInputStream, outputFileStream);
                outputFileStream.close();
            }
        }
        debInputStream.close();
    }

    public AuthResponse auth(EnvironmentConfig.Config config) throws URISyntaxException {

        String tokenKey = String.join("_", TOKEN_KEY, "admin");
        if (nonNull(TOKEN_CACHE.get(tokenKey))) {
            if (Instant.now().toEpochMilli()
                    - TOKEN_CACHE.get(tokenKey).getTimestamp() < (embeddedConfig.getTtl() * 1000 - 7000)) {
                return AuthResponse.builder().token(TOKEN_CACHE.get(tokenKey).getToken()).build();
            } else if (embeddedConfig.isRenewEnabled()) {
                return renew(TOKEN_CACHE.get(tokenKey).getToken(), "admin", config);
            }
        }
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(new URI(config.getAuthUrl()),
                AuthRequest.builder().username(userConfig.getUserName()).password(userConfig.getPassword()).build(),
                AuthResponse.class);
        //TOKEN_CACHE.put(tokenKey, Token.builder().timestamp(Instant.now().toEpochMilli())
        //      .token(Objects.requireNonNull(response.getBody()).getToken()).build());
        return response.getBody();
    }

    private AuthResponse renew(String oldToken, String role, EnvironmentConfig.Config config) {
        String tokenKey = String.join("_", TOKEN_KEY, role);
        HttpHeaders httpHeaders = new HttpHeaders();
        HttpEntity<Object> reuestEntity = new HttpEntity<>(httpHeaders);
        httpHeaders.set("Authorization", "Bearer " + oldToken);
        ResponseEntity<AuthResponse> response = restTemplate.exchange(config.getRenewUrl(), HttpMethod.GET,
                reuestEntity, AuthResponse.class);
        //TOKEN_CACHE.put(tokenKey, Token.builder().timestamp(Instant.now().toEpochMilli())
        //      .token(Objects.requireNonNull(response.getBody()).getToken()).build());
        return response.getBody();
    }

    public ResponseDTO getCloudData(TwistLockDTO twistLockDTO, String project, String gcpId, EnvironmentConfig.Config config) throws URISyntaxException, IOException {
        String token = auth(config).getToken();
        HttpHeaders httpHeaders = new HttpHeaders();
        ParameterizedTypeReference<byte[]> parameterizedTypeReference = new ParameterizedTypeReference<byte[]>() {
        };
        HttpEntity<Object> requestEntity = new HttpEntity<>(twistLockDTO, httpHeaders);
        httpHeaders.add("Authorization", "Bearer " + token);
        String url = UriComponentsBuilder.fromHttpUrl(config.getCloudUrl()).queryParam("project", project).toUriString();
        byte[] response = restTemplate.exchange(url, HttpMethod.POST,
                requestEntity, parameterizedTypeReference).getBody();
        File inputFile = File.createTempFile("twistlock-defender-helm.tar.gz", "");
        File outFile = File.createTempFile(UUID.randomUUID().toString(), "twistlock");
        outFile.delete();
        outFile.mkdir();
        FileOutputStream fileOutputStream = new FileOutputStream(inputFile);
        fileOutputStream.write(response);
        fileOutputStream.close();
        unTar(inputFile, outFile);
        File charFile = new File(String.join("/", outFile.getAbsolutePath(), "twistlock-defender", "Chart.yaml"));
        updateChartFile(twistLockDTO, gcpId, charFile);
        File valuesFile = new File(String.join("/", outFile.getAbsolutePath(), "twistlock-defender", "values.yaml"));
        ResponseDTO responseDTO = populateValuesData(valuesFile);
        String finalFolder = String.join("/", outFile.getAbsolutePath(), "twistlock-defender");
        Runtime.getRuntime().exec(String.join(" ", "helm package ", finalFolder), null, new File(outFile.getAbsolutePath()));
        String clusterName = StringUtils.isNotEmpty(twistLockDTO.getCluster()) ? twistLockDTO.getCluster() : "";
        String version = StringUtils.isNotEmpty(twistLockDTO.getTwistLockVersion()) ? twistLockDTO.getTwistLockVersion() : embeddedConfig.getTwistLockVersion();
        String chartFileName = String.join("-", "twistlock-defender", gcpId, clusterName, version + ".tgz");
        responseDTO.setInputLocation(inputFile.getAbsolutePath());
        responseDTO.setOutputLocation(outFile.getAbsolutePath());
        responseDTO.setUploadLocation(String.join("/", outFile.getAbsolutePath(), chartFileName));
        return responseDTO;
    }

    private void updateChartFile(TwistLockDTO twistLockDTO, String gcpId, File charFile) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(charFile));
        StringBuffer stringBuffer = new StringBuffer();
        String data;
        while ((data = bufferedReader.readLine()) != null) {
            if ("name: twistlock-defender".equalsIgnoreCase(data)) {
                stringBuffer.append(String.join("-", data, gcpId, twistLockDTO.getCluster()));
            } else {
                stringBuffer.append(data);
            }
            String version = "version: " + embeddedConfig.getTwistLockVersion();
            if (!version.equalsIgnoreCase(data)) {
                stringBuffer.append("\n");
            }
        }
        bufferedReader.close();
        FileWriter fileWriter = new FileWriter(charFile);
        fileWriter.write(stringBuffer.toString());
        fileWriter.close();
    }

    private ResponseDTO populateValuesData(File valuesFile) throws IOException {
        ResponseDTO responseDTO = new ResponseDTO();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(valuesFile));
        String data;
        while ((data = bufferedReader.readLine()) != null) {
            if (data.startsWith("admission_cert")) {
                responseDTO.setAdmissionCert(data.substring("admission_cert: ".length()));
            }
            if (data.startsWith("admission_key")) {
                responseDTO.setAdmissionKey(data.substring("admission_key: ".length()));
            }
            if (data.startsWith("cluster_id")) {
                responseDTO.setClusterId(data.substring("cluster_id: ".length()));
            }
            if (data.startsWith("ca_cert")) {
                responseDTO.setCaCert(data.substring("ca_cert: ".length()));
            }
            if (data.startsWith("defender_ca_cert")) {
                responseDTO.setCaCert(data.substring("defender_ca_cert: ".length()));
            }
            if (data.startsWith("defender_client_cert")) {
                responseDTO.setClientCert(data.substring("defender_client_cert: ".length()));
            }
            if (data.startsWith("defender_client_key")) {
                responseDTO.setClientKey(data.substring("defender_client_key: ".length()));
            }
            if (data.startsWith("client_cert")) {
                responseDTO.setClientCert(data.substring("client_cert: ".length()));
            }
            if (data.startsWith("client_key")) {
                responseDTO.setClientKey(data.substring("client_key: ".length()));
            }
            if (data.startsWith("ws_address")) {
                responseDTO.setWssAddress(data.substring("ws_address: ".length()));
            }
            if (data.startsWith("install_bundle")) {
                responseDTO.setInstallBundle(data.substring("install_bundle: ".length()));
            }
            if (data.startsWith("service_parameter")) {
                responseDTO.setServiceParameter(data.substring("service_parameter: ".length()));
            }
        }
        bufferedReader.close();
        return responseDTO;
    }

    @Retryable(value = {RetryException.class}, maxAttemptsExpression = "2", backoff = @Backoff(delayExpression = "1000", maxDelayExpression = "3000", multiplierExpression = "2"))
    public void upload(ResponseDTO responseDTO, EnvironmentConfig.UploadConfig uploadConfig) {
        log.debug("retry called");
        ResponseEntity<String> responseEntity = uploadFile(responseDTO.getUploadLocation(), uploadConfig);
        if (HttpStatus.CREATED == responseEntity.getStatusCode()) {
            delete(responseDTO);
        }
        if (HttpStatus.CONFLICT == responseEntity.getStatusCode()) {
            delete(responseDTO);
            throw new ValidationException(responseEntity.getBody(), responseEntity.getStatusCode());
        }
        if (HttpStatus.CREATED != responseEntity.getStatusCode()) {
            throw new RetryException(responseEntity.getBody(), responseEntity.getStatusCode());
        }
    }


    private void delete(ResponseDTO responseDTO) {
        File deleteFile = new File(responseDTO.getOutputLocation());
        if (deleteFile.exists()) {
            delete(deleteFile);
        }
        File inputFile = new File(responseDTO.getInputLocation());
        if (inputFile.exists()) {
            delete(inputFile);
        }
    }

    public void delete(final File file) {
        if (file.isDirectory()) {
            if (file.list().length == 0) {
                file.delete();
            } else {
                String[] files = file.list();
                for (String temp : files) {
                    File fileDelete = new File(file, temp);
                    delete(fileDelete);
                }
                if (file.list().length == 0) {
                    file.delete();
                }
            }
        } else {
            file.delete();
        }
    }
}