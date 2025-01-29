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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.SecretId;

import java.io.Serial;
import java.util.Base64;
import java.util.Optional;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class Secret extends SecretInfo {

    @Serial
    private static final long serialVersionUID = 3671364019778017637L;

    private byte[] value;

    @Schema(description = "Encrypted secret value", requiredMode = RequiredMode.REQUIRED)
    @JsonGetter
    public String getEncodedValue() {
        return Optional.ofNullable(value)
                .map(Base64.getEncoder()::encodeToString)
                .orElse(null);
    }

    @JsonSetter("value")
    public void setEncodedValue(String value) {
        this.value = Optional.ofNullable(value)
                .map(Base64.getDecoder()::decode)
                .orElse(null);
    }

    public Secret() {
        super();
    }

    public Secret(SecretId id) {
        super(id);
    }

    public Secret(Secret secret) {
        super(secret);
        this.value = secret.getValue();
    }

    @Override
    public String toString() {
        return super.toString();
    }

}
