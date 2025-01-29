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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DaoSqlTest
public class SecretControllerTest extends AbstractControllerTest {

    private static final TypeReference<PageData<SecretInfo>> PAGE_DATA_SECRET_TYPE_REF = new TypeReference<>() {};

    @Before
    public void setUp() throws Exception {
        loginTenantAdmin();
    }

    @After
    public void tearDown() throws Exception {
        PageData<SecretInfo> pageData = doGetTypedWithPageLink("/api/secret/infos?", PAGE_DATA_SECRET_TYPE_REF, new PageLink(100, 0));
        for (SecretInfo secretInfo : pageData.getData()) {
            doDelete("/api/secret/" + secretInfo.getId().getId()).andExpect(status().isOk());
        }
    }

    @Test
    public void testSaveSecret() throws Exception {
        PageData<SecretInfo> pageData = doGetTypedWithPageLink("/api/secret/infos?", PAGE_DATA_SECRET_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData.getData()).isEmpty();

        Secret secret = constructSecret("Test Create Secret", "CreatePassword");
        SecretInfo savedSecret = doPost("/api/secret", secret, SecretInfo.class);

        assertNotNull(savedSecret);
        assertNotNull(savedSecret.getId());

        PageData<SecretInfo> pageData2 = doGetTypedWithPageLink("/api/secret/infos?", PAGE_DATA_SECRET_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData2.getData()).hasSize(1);
        assertThat(pageData2.getData().get(0)).isEqualTo(new SecretInfo(savedSecret));

        SecretInfo retrievedSecret = doGet("/api/secret/info/{id}", SecretInfo.class, savedSecret.getId().getId());
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        doDelete("/api/secret/" + savedSecret.getId().getId()).andExpect(status().isOk());
        doGet("/api/secret/info/{id}", savedSecret.getId().getId()).andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateSecret() throws Exception {
        PageData<SecretInfo> pageData = doGetTypedWithPageLink("/api/secret/infos?", PAGE_DATA_SECRET_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData.getData()).isEmpty();

        Secret secret = constructSecret("Test Update Secret", "UpdatePassword");
        SecretInfo savedSecret = doPost("/api/secret", secret, SecretInfo.class);

        assertNotNull(savedSecret);
        assertNotNull(savedSecret.getId());

        SecretInfo retrievedSecret = doGet("/api/secret/info/{id}", SecretInfo.class, savedSecret.getId().getId());
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        secret = constructSecret(savedSecret);
        secret.setEncodedValue("UpdatedPassword");

        SecretInfo updatedSecret = doPost("/api/secret", secret, SecretInfo.class);
        retrievedSecret = doGet("/api/secret/info/{id}", SecretInfo.class, updatedSecret.getId().getId());
        assertThat(retrievedSecret).isEqualTo(updatedSecret);

        doDelete("/api/secret/" + savedSecret.getId().getId()).andExpect(status().isOk());
        doGet("/api/secret/info/{id}", savedSecret.getId().getId()).andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateSecretNameProhibited() throws Exception {
        PageData<SecretInfo> pageData = doGetTypedWithPageLink("/api/secret/infos?", PAGE_DATA_SECRET_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData.getData()).isEmpty();

        Secret secret = constructSecret("Test Secret", "Prohibited");
        SecretInfo savedSecret = doPost("/api/secret", secret, SecretInfo.class);

        assertNotNull(savedSecret);
        assertNotNull(savedSecret.getId());

        secret = constructSecret(savedSecret);
        secret.setName("Updated Name");

        doPost("/api/secret", secret)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Can't update secret name!")));

        doDelete("/api/secret/" + savedSecret.getId().getId()).andExpect(status().isOk());
        doGet("/api/secret/info/{id}", savedSecret.getId().getId()).andExpect(status().isNotFound());
    }

    @Test
    public void testFindSecretInfos() throws Exception {
        PageData<SecretInfo> pageData = doGetTypedWithPageLink("/api/secret/infos?", PAGE_DATA_SECRET_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData.getData()).isEmpty();

        int expectedSize = 10;
        String namePrefix = "Test Create Secret_";
        for (int i = 0; i < expectedSize; i++) {
            doPost("/api/secret", constructSecret(namePrefix + i, "CreatePassword"), SecretInfo.class);
        }

        PageData<SecretInfo> pageData2 = doGetTypedWithPageLink("/api/secret/infos?", PAGE_DATA_SECRET_TYPE_REF, new PageLink(expectedSize, 0));
        assertThat(pageData2.getData()).hasSize(expectedSize);

        List<UUID> toDelete = new ArrayList<>();

        for (int i = 0; i < expectedSize; i++) {
            SecretInfo secretInfo = pageData2.getData().get(i);
            assertThat(secretInfo.getName()).isEqualTo(namePrefix + i);
            toDelete.add(secretInfo.getUuidId());
        }

        toDelete.forEach(secret -> {
            try {
                doDelete("/api/secret/" + secret).andExpect(status().isOk());
                doGet("/api/secret/info/{id}", secret).andExpect(status().isNotFound());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Secret constructSecret(String name, String value) {
        Secret secret = new Secret();
        secret.setName(name);
        secret.setEncodedValue(value);
        return secret;
    }

    private Secret constructSecret(SecretInfo secretInfo) {
        Secret secret = new Secret();
        secret.setId(secretInfo.getId());
        secret.setName(secretInfo.getName());
        secret.setTenantId(secretInfo.getTenantId());
        secret.setCreatedTime(secretInfo.getCreatedTime());
        return secret;
    }

}
