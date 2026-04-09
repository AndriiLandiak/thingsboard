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
package org.thingsboard.server.dao.revocation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.TbCacheValueWrapper;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.RevokedCertificateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.RevocationReason;
import org.thingsboard.server.common.data.security.RevokedCertificate;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Service
@Slf4j
@RequiredArgsConstructor
public class RevokedCertificateServiceImpl implements RevokedCertificateService {

    private final RevokedCertificateDao revokedCertificateDao;
    private final TbTransactionalCache<String, Boolean> cache;

    @Override
    public RevokedCertificate revoke(TenantId tenantId, DeviceProfileId deviceProfileId, String fingerprint, RevocationReason reason,
                                     String notes, long certificateExpiryDate, String certificateCommonName) {
        log.trace("Executing revoke [{}][{}][{}]", tenantId, deviceProfileId, fingerprint);
        validateId(tenantId, id -> "Incorrect tenantId " + id);
        validateId(deviceProfileId, id -> "Incorrect deviceProfileId " + id);
        validateString(fingerprint, f -> "Incorrect fingerprint " + f);

        RevokedCertificate revokedCertificate = new RevokedCertificate();
        revokedCertificate.setTenantId(tenantId);
        revokedCertificate.setDeviceProfileId(deviceProfileId);
        revokedCertificate.setCertificateFingerprint(fingerprint);
        revokedCertificate.setRevocationReason(reason);
        revokedCertificate.setRevocationNotes(notes);
        revokedCertificate.setRevokedTimestamp(System.currentTimeMillis());
        revokedCertificate.setCertificateExpiryDate(certificateExpiryDate);
        revokedCertificate.setCertificateCommonName(certificateCommonName);

        RevokedCertificate saved = revokedCertificateDao.save(tenantId, revokedCertificate);
        cache.evict(fingerprint);
        return saved;
    }

    @Override
    public boolean isRevoked(String fingerprint) {
        log.trace("Executing isRevoked [{}]", fingerprint);
        validateString(fingerprint, f -> "Incorrect fingerprint " + f);
        return cache.getAndPutInTransaction(fingerprint,
                () -> revokedCertificateDao.findByFingerprint(TenantId.SYS_TENANT_ID, fingerprint) != null,
                boolVal -> boolVal,
                boolVal -> boolVal,
                true);
    }

    @Override
    public boolean unRevoke(TenantId tenantId, String fingerprint) {
        log.trace("Executing unrevoke [{}][{}]", tenantId, fingerprint);
        validateId(tenantId, id -> "Incorrect tenantId " + id);
        validateString(fingerprint, f -> "Incorrect fingerprint " + f);

        RevokedCertificate existing = revokedCertificateDao.findByFingerprint(tenantId, fingerprint);
        if (existing != null) {
            revokedCertificateDao.deleteByFingerprint(tenantId, fingerprint);
            cache.evict(fingerprint);
            return true;
        }
        return false;
    }

    @Override
    public RevokedCertificate findById(TenantId tenantId, RevokedCertificateId revokedCertificateId) {
        log.trace("Executing findById [{}][{}]", tenantId, revokedCertificateId);
        validateId(tenantId, id -> "Incorrect tenantId " + id);
        validateId(revokedCertificateId, id -> "Incorrect revokedCertificateId " + id);
        return revokedCertificateDao.findById(tenantId, revokedCertificateId.getId());
    }

    @Override
    public PageData<RevokedCertificate> findRevokedCertificates(TenantId tenantId, DeviceProfileId deviceProfileId, PageLink pageLink) {
        log.trace("Executing findRevokedCertificates [{}][{}]", tenantId, deviceProfileId);
        validateId(tenantId, id -> "Incorrect tenantId " + id);
        validateId(deviceProfileId, id -> "Incorrect deviceProfileId " + id);

        return revokedCertificateDao.findByTenantIdAndDeviceProfileId(tenantId, deviceProfileId.getId(), pageLink);
    }

}
