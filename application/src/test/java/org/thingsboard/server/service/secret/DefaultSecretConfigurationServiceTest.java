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

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.secret.SecretConfig;
import org.thingsboard.server.dao.secret.SecretConfigurationService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {DefaultSecretConfigurationService.class, SecretConfig.class})
@EnableConfigurationProperties
@TestPropertySource(properties = {
        "security.secret.key-base=7V9OgYiZFDoeMiYMg623EOPDocwOqfFNffF5Ds8Bmsw=",
        "security.secret.key-algorithm=AES",
        "security.secret.encryption-mode=AES/GCM/NoPadding",
        "security.secret.iv-size[default]=12",
})
public class DefaultSecretConfigurationServiceTest {

    @Autowired
    protected SecretConfigurationService secretConfigurationService;

    @ParameterizedTest
    @MethodSource("encryptionDecryptionProvider")
    public void testEncryptionDecryption(String value) {
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

        // Encrypt
        byte[] encryptedValue = secretConfigurationService.encrypt(tenantId, bytes);
        assertThat(encryptedValue).isNotNull();
        assertThat(new String(encryptedValue, StandardCharsets.UTF_8)).isNotEqualTo(new String(bytes, StandardCharsets.UTF_8));

        // Decrypt
        String decryptedValue = secretConfigurationService.decrypt(tenantId, encryptedValue);
        assertThat(decryptedValue).isNotNull();
        assertThat(decryptedValue).isEqualTo(value);
    }

    @Test
    public void testEncryptionDecryptionSameValue() {
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        String value = "testEncryption";
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

        // encrypt
        byte[] encryptedValue1 = secretConfigurationService.encrypt(tenantId, bytes);
        byte[] encryptedValue2 = secretConfigurationService.encrypt(tenantId, bytes);
        byte[] encryptedValue3 = secretConfigurationService.encrypt(tenantId, bytes);

        assertThat(Base64.getEncoder().encodeToString(encryptedValue1)).isNotEqualTo(Base64.getEncoder().encodeToString(encryptedValue2));
        assertThat(Base64.getEncoder().encodeToString(encryptedValue1)).isNotEqualTo(Base64.getEncoder().encodeToString(encryptedValue3));

        // decrypt
        assertThat(value).isEqualTo(secretConfigurationService.decrypt(tenantId, encryptedValue1));
        assertThat(value).isEqualTo(secretConfigurationService.decrypt(tenantId, encryptedValue2));
        assertThat(value).isEqualTo(secretConfigurationService.decrypt(tenantId, encryptedValue3));
    }

    @Test
    public void testUniqueEncryption() {
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        byte[] value = "testUniqueEncryption".getBytes(StandardCharsets.UTF_8);

        byte[] encryptedValue1 = secretConfigurationService.encrypt(tenantId, value);
        byte[] encryptedValue2 = secretConfigurationService.encrypt(tenantId, value);

        assertThat(Base64.getEncoder().encodeToString(encryptedValue1)).isNotEqualTo(Base64.getEncoder().encodeToString(encryptedValue2));
    }

    @ParameterizedTest
    @MethodSource("invalidValueProvider")
    public void testDecryptionInvalidData() {
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        byte[] invalidEncryptedValue = "invalidEncryptedData".getBytes(StandardCharsets.UTF_8);

        try {
            secretConfigurationService.decrypt(tenantId, invalidEncryptedValue);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("Failed to process decryption");
        }
    }

    private static Stream<Arguments> encryptionDecryptionProvider() {
        return Stream.of(
                Arguments.of("testEncryptionDecryption1"),
                Arguments.of("testEncryptionDecryption2"),
                Arguments.of("testEncryptionDecryption3")
        );
    }

    private static Stream<Arguments> invalidValueProvider() {
        return Stream.of(
                Arguments.of("invalidEncryptedData1"),
                Arguments.of("invalidEncryptedData2"),
                Arguments.of("invalidEncryptedData3")
        );
    }

}
