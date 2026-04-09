/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.common.data.security;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.RevokedCertificateId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serial;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class RevokedCertificate extends BaseData<RevokedCertificateId> implements HasTenantId {

    @Serial
    private static final long serialVersionUID = 2807343040519543363L;

    private TenantId tenantId;
    private DeviceProfileId deviceProfileId;
    private String certificateFingerprint;
    private RevocationReason revocationReason;
    private String revocationNotes;
    private long revokedTimestamp;
    private long certificateExpiryDate;
    private String certificateCommonName;

    public RevokedCertificate() {
        super();
    }

    public RevokedCertificate(RevokedCertificateId id) {
        super(id);
    }

    public RevokedCertificate(RevokedCertificate other) {
        super(other);
        this.tenantId = other.getTenantId();
        this.deviceProfileId = other.getDeviceProfileId();
        this.certificateFingerprint = other.getCertificateFingerprint();
        this.revocationReason = other.getRevocationReason();
        this.revocationNotes = other.getRevocationNotes();
        this.revokedTimestamp = other.getRevokedTimestamp();
        this.certificateExpiryDate = other.getCertificateExpiryDate();
        this.certificateCommonName = other.getCertificateCommonName();
    }

    @Override
    public String toString() {
        return "RevokedCertificate [tenantId=" + tenantId + ", deviceProfileId=" + deviceProfileId
                + ", certificateFingerprint=" + certificateFingerprint + ", revocationReason=" + revocationReason
                + ", revokedTimestamp=" + revokedTimestamp + ", certificateExpiryDate=" + certificateExpiryDate
                + ", certificateCommonName=" + certificateCommonName
                + ", createdTime=" + createdTime + ", id=" + id + "]";
    }

}
