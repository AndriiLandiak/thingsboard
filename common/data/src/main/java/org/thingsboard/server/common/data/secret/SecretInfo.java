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
package org.thingsboard.server.common.data.secret;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serial;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class SecretInfo extends BaseData<SecretId> implements HasTenantId, HasName {

    @Serial
    private static final long serialVersionUID = 4356095580465337566L;

    @Schema(description = "JSON object with Tenant Id. Tenant Id of the secret cannot be changed.", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;

    @NoXss
    @Length(fieldName = "name")
    @Schema(description = "Secret name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NoXss
    @Length(fieldName = "key")
    @Schema(description = "Secret key", requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.READ_ONLY)
    private String key;

    public SecretInfo() {
        super();
    }

    public SecretInfo(SecretId id) {
        super(id);
    }

    public SecretInfo(SecretInfo secretInfo) {
        super(secretInfo);
        this.tenantId = secretInfo.getTenantId();
        this.name = secretInfo.getName();
        this.key = secretInfo.getKey();
    }

}
