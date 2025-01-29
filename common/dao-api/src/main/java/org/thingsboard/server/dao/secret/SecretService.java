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
package org.thingsboard.server.dao.secret;

import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;

public interface SecretService extends EntityDaoService {

    Secret saveSecret(TenantId tenantId, Secret secret);

    void deleteSecret(TenantId tenantId, SecretId secretId);

    Secret findSecretById(TenantId tenantId, SecretId secretId);

    SecretInfo findSecretInfoById(TenantId tenantId, SecretId secretId);

    Secret findSecretByKey(TenantId tenantId, String name);

    PageData<SecretInfo> findSecretInfosByTenantId(TenantId tenantId, PageLink pageLink);

    List<String> findSecretKeysByTenantId(TenantId tenantId);

}
