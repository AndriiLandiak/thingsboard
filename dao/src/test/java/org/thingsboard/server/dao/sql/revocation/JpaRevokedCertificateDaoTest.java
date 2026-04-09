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
package org.thingsboard.server.dao.sql.revocation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.RevocationReason;
import org.thingsboard.server.common.data.security.RevokedCertificate;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.device.DeviceProfileDao;
import org.thingsboard.server.dao.revocation.RevokedCertificateDao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class JpaRevokedCertificateDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private RevokedCertificateDao revokedCertificateDao;

    @Autowired
    private DeviceProfileDao deviceProfileDao;

    private final List<RevokedCertificate> savedEntries = new ArrayList<>();
    private DeviceProfile deviceProfile1;
    private DeviceProfile deviceProfile2;

    @Before
    public void setUp() {
        deviceProfile1 = createDeviceProfile("TestProfile1", TenantId.SYS_TENANT_ID);
        deviceProfile2 = createDeviceProfile("TestProfile2", TenantId.SYS_TENANT_ID);
    }

    @After
    public void tearDown() {
        for (RevokedCertificate entry : savedEntries) {
            revokedCertificateDao.removeById(TenantId.SYS_TENANT_ID, entry.getUuidId());
        }
        deviceProfileDao.removeById(TenantId.SYS_TENANT_ID, deviceProfile1.getUuidId());
        deviceProfileDao.removeById(TenantId.SYS_TENANT_ID, deviceProfile2.getUuidId());
    }

    @Test
    public void testSaveAndFindByFingerprint() {
        String fingerprint = "sha3-" + UUID.randomUUID();
        RevokedCertificate saved = saveRevokedCertificate(TenantId.SYS_TENANT_ID, deviceProfile1.getId(), fingerprint);

        RevokedCertificate found = revokedCertificateDao.findByFingerprint(TenantId.SYS_TENANT_ID, fingerprint);

        assertNotNull(found);
        assertEquals(saved.getId(), found.getId());
        assertEquals(saved.getTenantId(), found.getTenantId());
        assertEquals(saved.getDeviceProfileId(), found.getDeviceProfileId());
        assertEquals(saved.getCertificateFingerprint(), found.getCertificateFingerprint());
        assertEquals(saved.getRevocationReason(), found.getRevocationReason());
        assertEquals(saved.getRevocationNotes(), found.getRevocationNotes());
        assertEquals(saved.getRevokedTimestamp(), found.getRevokedTimestamp());
        assertEquals(saved.getCertificateExpiryDate(), found.getCertificateExpiryDate());
    }

    @Test
    public void testTenantIsolation() {
        TenantId tenant2 = TenantId.fromUUID(UUID.randomUUID());

        saveRevokedCertificate(TenantId.SYS_TENANT_ID, deviceProfile1.getId(), "fp-tenant1-" + UUID.randomUUID());
        saveRevokedCertificate(TenantId.SYS_TENANT_ID, deviceProfile1.getId(), "fp-tenant1-" + UUID.randomUUID());
        saveRevokedCertificate(tenant2, deviceProfile1.getId(), "fp-tenant2-" + UUID.randomUUID());

        PageData<RevokedCertificate> tenant1Results = revokedCertificateDao.findByTenantIdAndDeviceProfileId(
                TenantId.SYS_TENANT_ID, deviceProfile1.getId().getId(), new PageLink(10));

        assertEquals(2, tenant1Results.getTotalElements());
        for (RevokedCertificate rc : tenant1Results.getData()) {
            assertEquals(TenantId.SYS_TENANT_ID, rc.getTenantId());
            assertEquals(deviceProfile1.getId(), rc.getDeviceProfileId());
        }

        PageData<RevokedCertificate> tenant2Results = revokedCertificateDao.findByTenantIdAndDeviceProfileId(
                tenant2, deviceProfile1.getId().getId(), new PageLink(10));

        assertEquals(1, tenant2Results.getTotalElements());
        assertEquals(tenant2, tenant2Results.getData().get(0).getTenantId());
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void testUniqueFingerprint() {
        String fingerprint = "sha3-duplicate-" + UUID.randomUUID();
        saveRevokedCertificate(TenantId.SYS_TENANT_ID, deviceProfile1.getId(), fingerprint);
        saveRevokedCertificate(TenantId.SYS_TENANT_ID, deviceProfile2.getId(), fingerprint);
    }

    @Test
    public void testDeleteByFingerprint() {
        String fingerprint = "sha3-delete-" + UUID.randomUUID();
        saveRevokedCertificate(TenantId.SYS_TENANT_ID, deviceProfile1.getId(), fingerprint);

        RevokedCertificate found = revokedCertificateDao.findByFingerprint(TenantId.SYS_TENANT_ID, fingerprint);
        assertNotNull(found);

        revokedCertificateDao.deleteByFingerprint(TenantId.SYS_TENANT_ID, fingerprint);

        RevokedCertificate afterDelete = revokedCertificateDao.findByFingerprint(TenantId.SYS_TENANT_ID, fingerprint);
        assertNull(afterDelete);
    }

    @Test
    public void testDeleteExpiredEntries() {
        long now = System.currentTimeMillis();

        RevokedCertificate expired = createRevokedCertificate(TenantId.SYS_TENANT_ID, deviceProfile1.getId(), "fp-expired-" + UUID.randomUUID());
        expired.setCertificateExpiryDate(now - 100000L);
        expired = revokedCertificateDao.save(TenantId.SYS_TENANT_ID, expired);
        savedEntries.add(expired);

        RevokedCertificate notExpired = createRevokedCertificate(TenantId.SYS_TENANT_ID, deviceProfile1.getId(), "fp-valid-" + UUID.randomUUID());
        notExpired.setCertificateExpiryDate(now + 100000L);
        notExpired = revokedCertificateDao.save(TenantId.SYS_TENANT_ID, notExpired);
        savedEntries.add(notExpired);

        revokedCertificateDao.deleteExpiredEntries(now);

        RevokedCertificate expiredResult = revokedCertificateDao.findByFingerprint(TenantId.SYS_TENANT_ID, expired.getCertificateFingerprint());
        assertNull(expiredResult);

        RevokedCertificate validResult = revokedCertificateDao.findByFingerprint(TenantId.SYS_TENANT_ID, notExpired.getCertificateFingerprint());
        assertNotNull(validResult);
    }

    private RevokedCertificate saveRevokedCertificate(TenantId tenantId, DeviceProfileId profileId, String fingerprint) {
        RevokedCertificate rc = createRevokedCertificate(tenantId, profileId, fingerprint);
        rc = revokedCertificateDao.save(TenantId.SYS_TENANT_ID, rc);
        savedEntries.add(rc);
        return rc;
    }

    private RevokedCertificate createRevokedCertificate(TenantId tenantId, DeviceProfileId profileId, String fingerprint) {
        RevokedCertificate rc = new RevokedCertificate();
        rc.setTenantId(tenantId);
        rc.setDeviceProfileId(profileId);
        rc.setCertificateFingerprint(fingerprint);
        rc.setRevocationReason(RevocationReason.COMPROMISED);
        rc.setRevocationNotes("Test revocation");
        rc.setRevokedTimestamp(System.currentTimeMillis());
        rc.setCertificateExpiryDate(System.currentTimeMillis() + 86400000L);
        return rc;
    }

    private DeviceProfile createDeviceProfile(String name, TenantId tenantId) {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setName(name);
        deviceProfile.setTenantId(tenantId);
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTransportType(DeviceTransportType.DEFAULT);
        deviceProfile.setDescription("Test profile");
        return deviceProfileDao.save(tenantId, deviceProfile);
    }

}
