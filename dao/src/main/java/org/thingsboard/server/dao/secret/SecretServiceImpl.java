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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service("SecretDaoService")
public class SecretServiceImpl extends AbstractEntityService implements SecretService {

    @Autowired
    private SecretDao secretDao;

    @Autowired
    private SecretInfoDao secretInfoDao;

    @Autowired
    private DataValidator<Secret> secretValidator;

    @Autowired(required = false)
    private SecretConfigurationService secretConfigurationService;

    @Override
    public Secret saveSecret(TenantId tenantId, Secret secret) {
        log.trace("Executing saveSecret [{}]", secret);
        try {
            Secret oldSecret = secretValidator.validate(secret, Secret::getTenantId);

            if (oldSecret == null) {
                secret.setKey(generateKey(secret.getName()));
            } else {
                secret.setKey(oldSecret.getKey());
            }

            if (secret.getValue() != null) {
                byte[] encrypted = secretConfigurationService.encrypt(tenantId, secret.getValue());
                secret.setValue(encrypted);
            }

            Secret savedSecret = secretDao.save(tenantId, secret);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entityId(savedSecret.getId()).entity(savedSecret).created(secret.getId() == null).build());
            return savedSecret;
        } catch (Exception e) {
            checkConstraintViolation(e,
                    "secret_unq_key", "Secret with such name and scheme already exists!");
            throw e;
        }
    }

    @Override
    public void deleteSecret(TenantId tenantId, SecretId secretId) {
        log.trace("Executing deleteSecretById [{}]", secretId.getId());
        secretDao.removeById(tenantId, secretId.getId());
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(secretId).build());
    }

    @Override
    public Secret findSecretById(TenantId tenantId, SecretId secretId) {
        log.trace("Executing findSecretById [{}] [{}]", tenantId, secretId);
        return secretDao.findById(tenantId, secretId.getId());
    }

    @Override
    public SecretInfo findSecretInfoById(TenantId tenantId, SecretId secretId) {
        log.trace("Executing findSecretInfoById [{}] [{}]", tenantId, secretId);
        return secretInfoDao.findById(tenantId, secretId.getId());
    }

    @Override
    public Secret findSecretByKey(TenantId tenantId, String key) {
        log.trace("Executing findSecretByKey [{}] [{}]", tenantId, key);
        return secretDao.findByKey(tenantId, key);
    }

    @Override
    public PageData<SecretInfo> findSecretInfosByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findSecretInfosByTenantId [{}]", tenantId);
        return secretInfoDao.findByTenantId(tenantId, pageLink);
    }

    @Override
    public List<String> findSecretKeysByTenantId(TenantId tenantId) {
        log.trace("Executing findSecretNamesByTenantId [{}]", tenantId);
        return secretInfoDao.findAllKeysByTenantId(tenantId);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findSecretById(tenantId, new SecretId(entityId.getId())));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SECRET;
    }

    private String generateKey(String name) {
        return StringUtils.strip(name.toLowerCase()
                .replaceAll("['\"]", "")
                .replaceAll("[^\\pL\\d]+", "_") // leaving only letters and numbers
                .replaceAll("_+", "_"), "_");
    }

}
