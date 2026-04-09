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
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.RevokedCertificateId;
import org.thingsboard.server.common.data.security.RevocationReason;
import org.thingsboard.server.common.data.security.RevokedCertificate;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.REVOKED_CERTIFICATE_TABLE_NAME)
public final class RevokedCertificateEntity extends BaseSqlEntity<RevokedCertificate> {

    @Column(name = ModelConstants.REVOKED_CERTIFICATE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.REVOKED_CERTIFICATE_DEVICE_PROFILE_ID_PROPERTY)
    private UUID deviceProfileId;

    @Column(name = ModelConstants.REVOKED_CERTIFICATE_FINGERPRINT_PROPERTY)
    private String certificateFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.REVOKED_CERTIFICATE_REASON_PROPERTY)
    private RevocationReason revocationReason;

    @Column(name = ModelConstants.REVOKED_CERTIFICATE_NOTES_PROPERTY)
    private String revocationNotes;

    @Column(name = ModelConstants.REVOKED_CERTIFICATE_REVOKED_TIMESTAMP_PROPERTY)
    private long revokedTimestamp;

    @Column(name = ModelConstants.REVOKED_CERTIFICATE_EXPIRY_DATE_PROPERTY)
    private long certificateExpiryDate;

    @Column(name = ModelConstants.REVOKED_CERTIFICATE_COMMON_NAME_PROPERTY)
    private String certificateCommonName;

    public RevokedCertificateEntity() {
        super();
    }

    public RevokedCertificateEntity(RevokedCertificate domain) {
        super(domain);
        if (domain.getTenantId() != null) {
            this.tenantId = domain.getTenantId().getId();
        }
        if (domain.getDeviceProfileId() != null) {
            this.deviceProfileId = domain.getDeviceProfileId().getId();
        }
        this.certificateFingerprint = domain.getCertificateFingerprint();
        this.revocationReason = domain.getRevocationReason();
        this.revocationNotes = domain.getRevocationNotes();
        this.revokedTimestamp = domain.getRevokedTimestamp();
        this.certificateExpiryDate = domain.getCertificateExpiryDate();
        this.certificateCommonName = domain.getCertificateCommonName();
    }

    @Override
    public RevokedCertificate toData() {
        RevokedCertificate revokedCertificate = new RevokedCertificate(new RevokedCertificateId(this.getUuid()));
        revokedCertificate.setCreatedTime(createdTime);
        if (tenantId != null) {
            revokedCertificate.setTenantId(getTenantId(tenantId));
        }
        if (deviceProfileId != null) {
            revokedCertificate.setDeviceProfileId(new DeviceProfileId(deviceProfileId));
        }
        revokedCertificate.setCertificateFingerprint(certificateFingerprint);
        revokedCertificate.setRevocationReason(revocationReason);
        revokedCertificate.setRevocationNotes(revocationNotes);
        revokedCertificate.setRevokedTimestamp(revokedTimestamp);
        revokedCertificate.setCertificateExpiryDate(certificateExpiryDate);
        revokedCertificate.setCertificateCommonName(certificateCommonName);
        return revokedCertificate;
    }

}
