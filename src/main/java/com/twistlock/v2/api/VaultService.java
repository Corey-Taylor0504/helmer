package com.twistlock.v2.api;

import com.twistlock.v2.config.EnvironmentConfig;
import com.twistlock.v2.dto.VaultDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class VaultService {
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private EnvironmentConfig environmentConfig;

    public void saveVault(String path, String url) {
        VaultDTO vaultDTO = new VaultDTO();
        vaultDTO.setUrl(url);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("data", vaultDTO);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Vault-Token", environmentConfig.getVaultToken());
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity(requestMap, headers);
        restTemplate.exchange(String.join("/", environmentConfig.getVaultUri(), path), HttpMethod.POST, requestEntity,
                String.class);
    }
}