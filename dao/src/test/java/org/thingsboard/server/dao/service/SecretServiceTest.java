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
package org.thingsboard.server.dao.service;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.secret.SecretConfigurationService;
import org.thingsboard.server.dao.secret.SecretService;
import org.thingsboard.server.dao.tenant.TenantProfileService;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DaoSqlTest
public class SecretServiceTest extends AbstractServiceTest {

    @Autowired
    SecretService secretService;

    @Autowired
    TenantProfileService tenantProfileService;

    @MockBean
    SecretConfigurationService secretConfigurationService;

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
        tenantProfileService.deleteTenantProfiles(tenantId);
    }

    @Test
    public void testSaveSecret() {
        Secret secret = constructSecret(tenantId, "Test Secret", "Password");
        Secret savedSecret = secretService.saveSecret(tenantId, secret);

        Secret retrievedSecret = secretService.findSecretById(tenantId, savedSecret.getId());
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        // check secret info
        SecretInfo retrievedInfo = secretService.findSecretInfoById(tenantId, savedSecret.getId());
        assertThat(retrievedInfo).isEqualTo(new SecretInfo(savedSecret));

        // update encrypted value
        savedSecret.setEncodedValue("NewPassword");
        savedSecret = secretService.saveSecret(tenantId, savedSecret);
        retrievedSecret = secretService.findSecretById(tenantId, savedSecret.getId());
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        // delete secret
        secretService.deleteSecret(tenantId, savedSecret.getId());
        assertThat(secretService.findSecretById(tenantId, savedSecret.getId())).isNull();
    }

    @Test
    public void testUpdateSecretName_thenDataValidationException() {
        Secret secret = constructSecret(tenantId, "Test Validation Exception", "Validation");
        Secret savedSecret = secretService.saveSecret(tenantId, secret);

        Secret retrievedSecret = secretService.findSecretById(tenantId, savedSecret.getId());
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        savedSecret.setName("Updated Validation Exception");

        Assertions.assertThrows(DataValidationException.class, () -> secretService.saveSecret(tenantId, savedSecret));
    }

    @Test
    public void testValidateGeneratedKeys() {
        List<Secret> secrets = new ArrayList<>();
        String name = "Test Rest Api Node Password";
        Secret secret = constructSecret(tenantId, name, "test");
        Secret savedSecret = secretService.saveSecret(tenantId, secret);
        assertThat(savedSecret.getKey()).isEqualTo(generateKey(name));
        secrets.add(savedSecret);

        name = "Test Some Node Password";
        secret = constructSecret(tenantId, name, "test");
        savedSecret = secretService.saveSecret(tenantId, secret);
        assertThat(savedSecret.getKey()).isEqualTo(generateKey(name));
        secrets.add(savedSecret);

        name = "Test Mqtt Node Password";
        secret = constructSecret(tenantId, name, "test");
        savedSecret = secretService.saveSecret(tenantId, secret);
        assertThat(savedSecret.getKey()).isEqualTo(generateKey(name));
        secrets.add(savedSecret);

        name = "Test Secret Password";
        secret = constructSecret(tenantId, name, "test");
        savedSecret = secretService.saveSecret(tenantId, secret);
        assertThat(savedSecret.getKey()).isEqualTo(generateKey(name));
        secrets.add(savedSecret);

        secrets.forEach(s -> secretService.deleteSecret(tenantId, s.getId()));
    }

    @Test
    public void testFindSecretByKey() {
        String name = "Test Secret Password";
        String key = generateKey(name);
        Secret secret = constructSecret(tenantId, name, "test");
        Secret savedSecret = secretService.saveSecret(tenantId, secret);
        assertThat(savedSecret.getKey()).isEqualTo(key);

        Secret retrieved = secretService.findSecretByKey(tenantId, key);
        assertThat(savedSecret).isEqualTo(retrieved);

        secretService.deleteSecret(tenantId, savedSecret.getId());
    }

    @Test
    public void testGetTenantSecrets() {
        List<Secret> secrets = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Secret savedSecret = secretService.saveSecret(tenantId, constructSecret(tenantId, "Name_" + i, "Password"));
            secrets.add(savedSecret);
        }
        PageData<SecretInfo> retrieved = secretService.findSecretInfosByTenantId(tenantId, new PageLink(10, 0));
        List<SecretInfo> secretInfos = secrets.stream().map(SecretInfo::new).toList();
        assertThat(retrieved.getData()).containsOnlyOnceElementsOf(secretInfos);

        secrets.forEach(secret -> secretService.deleteSecret(tenantId, secret.getId()));
        retrieved = secretService.findSecretInfosByTenantId(tenantId, new PageLink(10, 0));
        assertThat(retrieved.getData().size()).isEqualTo(0);
    }

    private Secret constructSecret(TenantId tenantId, String name, String value) {
        Secret secret = new Secret();
        secret.setTenantId(tenantId);
        secret.setName(name);
        secret.setEncodedValue(value);
        return secret;
    }

    private String generateKey(String name) {
        return StringUtils.strip(name.toLowerCase()
                .replaceAll("['\"]", "")
                .replaceAll("[^\\pL\\d]+", "_")
                .replaceAll("_+", "_"), "_");
    }

}
