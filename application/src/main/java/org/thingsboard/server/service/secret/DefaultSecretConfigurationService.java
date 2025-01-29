/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.secret;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.dao.secret.SecretConfig;
import org.thingsboard.server.dao.secret.SecretConfigurationService;
import org.thingsboard.server.dao.secret.SecretService;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultSecretConfigurationService implements SecretConfigurationService {

    private static final Pattern SECRET_PATTERN = Pattern.compile("\\$\\{secret:([a-zA-Z0-9_\\-]+)}");

    private final SecretConfig secretConfig;
    @Lazy
    private final SecretService secretService;

    private SecretKey secretKey;
    private boolean isInitialized;

    private void init() {
        if (StringUtils.isEmpty(secretConfig.getKey())) {
            throw new RuntimeException("Secret key is not configured");
        }
        byte[] key = Base64.getDecoder().decode(secretConfig.getKey());
        secretKey = new SecretKeySpec(key, 0, key.length, secretConfig.getKeyAlgorithm());
        isInitialized = true;
    }

    @Override
    public byte[] encrypt(TenantId tenantId, byte[] value) {
        if (!isInitialized) {
            init();
        }
        log.trace("[{}] process encryption for {}", tenantId, value);
        try {
            // Initialization Vector: to encrypt same value multiples time with different result
            byte[] iv = generateRandomBytes();

            Cipher cipher = Cipher.getInstance(secretConfig.getEncryptionMode());
            if (secretConfig.getEncryptionMode().contains("GCM")) {
                // 128 bit size for GCM mode is recommended by NIST
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
            } else {
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
            }
            byte[] encryptedData = cipher.doFinal(value);

            byte[] result = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encryptedData, 0, result, iv.length, encryptedData.length);

            return result;
        } catch (Exception e) {
            log.warn("[{}] Failed to process encryption for value: {}", tenantId, value, e);
            throw new RuntimeException("Failed to process encryption " + e);
        }
    }

    @Override
    public String decrypt(TenantId tenantId, byte[] value) {
        if (!isInitialized) {
            init();
        }
        try {
            int ivSize = secretConfig.getIvSize(secretConfig.getEncryptionMode());
            byte[] iv = new byte[ivSize];
            System.arraycopy(value, 0, iv, 0, ivSize);

            byte[] encryptedValue = new byte[value.length - ivSize];
            System.arraycopy(value, ivSize, encryptedValue, 0, encryptedValue.length);

            Cipher cipher = Cipher.getInstance(secretConfig.getEncryptionMode());
            if (secretConfig.getEncryptionMode().contains("GCM")) {
                cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
            } else {
                cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            }
            byte[] result = cipher.doFinal(encryptedValue);

            return new String(result, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("[{}] Failed to process decryption", tenantId, e);
            throw new RuntimeException("Failed to process decryption " + e);
        }
    }

    @Override
    public void processJsonConfiguration(TenantId tenantId, JsonNode config) {
        JacksonUtil.replaceAll(config, "", (path, value) -> {
            Matcher matcher = SECRET_PATTERN.matcher(value);
            if (matcher.find()) {
                String secretKey = matcher.group(1);
                Secret secret = secretService.findSecretByKey(tenantId, secretKey);
                if (secret == null) {
                    return value;
                }
                return decrypt(tenantId, secret.getValue());
            }
            return value;
        });
    }

    private byte[] generateRandomBytes() {
        // Need to be unique for every encryption operation
        byte[] bytes = new byte[secretConfig.getIvSize(secretConfig.getEncryptionMode())];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

}
