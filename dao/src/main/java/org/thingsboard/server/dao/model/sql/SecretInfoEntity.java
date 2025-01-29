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
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.dao.model.BaseSqlEntity;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.SECRET_KEY_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.SECRET_NAME_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.SECRET_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.SECRET_TENANT_ID_COLUMN;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = SECRET_TABLE_NAME)
public class SecretInfoEntity extends BaseSqlEntity<SecretInfo> {

    @Column(name = SECRET_TENANT_ID_COLUMN)
    private UUID tenantId;

    @Column(name = SECRET_NAME_COLUMN)
    private String name;

    @Column(name = SECRET_KEY_COLUMN)
    private String key;

    public SecretInfoEntity() {
        super();
    }

    public SecretInfoEntity(SecretInfo secretInfo) {
        super(secretInfo);
        this.tenantId = secretInfo.getTenantId().getId();
        this.name = secretInfo.getName();
        this.key = secretInfo.getKey();
    }

    @Override
    public SecretInfo toData() {
        SecretInfo secretInfo = new SecretInfo(new SecretId(id));
        secretInfo.setTenantId(TenantId.fromUUID(tenantId));
        secretInfo.setCreatedTime(createdTime);
        secretInfo.setName(name);
        secretInfo.setKey(key);
        return secretInfo;
    }

}
