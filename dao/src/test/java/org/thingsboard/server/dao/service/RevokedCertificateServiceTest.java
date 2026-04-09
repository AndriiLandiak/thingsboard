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
package org.thingsboard.server.dao.service;

import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.RevocationReason;
import org.thingsboard.server.common.data.security.RevokedCertificate;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.revocation.RevokedCertificateService;

import java.util.UUID;

@DaoSqlTest
public class RevokedCertificateServiceTest extends AbstractServiceTest {

    @Autowired
    private RevokedCertificateService revokedCertificateService;

    @Autowired
    private DeviceProfileService deviceProfileService;

    private DeviceProfile createAndSaveDeviceProfile() {
        DeviceProfile deviceProfile = createDeviceProfile(tenantId, "TestProfile-" + UUID.randomUUID());
        return deviceProfileService.saveDeviceProfile(deviceProfile);
    }

    @Test
    public void testRevoke() {
        DeviceProfile profile = createAndSaveDeviceProfile();
        String fingerprint = UUID.randomUUID().toString();
        long expiryDate = System.currentTimeMillis() + 86400000L;

        RevokedCertificate result = revokedCertificateService.revoke(
                tenantId, profile.getId(), fingerprint, RevocationReason.COMPROMISED, "test notes", expiryDate, null);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getId());
        Assert.assertEquals(tenantId, result.getTenantId());
        Assert.assertEquals(profile.getId(), result.getDeviceProfileId());
        Assert.assertEquals(fingerprint, result.getCertificateFingerprint());
        Assert.assertEquals(RevocationReason.COMPROMISED, result.getRevocationReason());
        Assert.assertEquals("test notes", result.getRevocationNotes());
        Assert.assertTrue(result.getRevokedTimestamp() > 0);
        Assert.assertEquals(expiryDate, result.getCertificateExpiryDate());
    }

    @Test
    public void testIsRevokedTrue() {
        DeviceProfile profile = createAndSaveDeviceProfile();
        String fingerprint = UUID.randomUUID().toString();
        long expiryDate = System.currentTimeMillis() + 86400000L;

        revokedCertificateService.revoke(
                tenantId, profile.getId(), fingerprint, RevocationReason.LOST, "lost device", expiryDate, null);

        boolean revoked = revokedCertificateService.isRevoked(fingerprint);

        Assert.assertTrue(revoked);
    }

    @Test
    public void testIsRevokedFalse() {
        boolean revoked = revokedCertificateService.isRevoked("non-existent-fingerprint-" + UUID.randomUUID());

        Assert.assertFalse(revoked);
    }

    @Test
    public void testUnRevoke() {
        DeviceProfile profile = createAndSaveDeviceProfile();
        String fingerprint = UUID.randomUUID().toString();
        long expiryDate = System.currentTimeMillis() + 86400000L;

        revokedCertificateService.revoke(
                tenantId, profile.getId(), fingerprint, RevocationReason.DECOMMISSIONED, "decommissioned", expiryDate, null);

        boolean unrevokeResult = revokedCertificateService.unRevoke(tenantId, fingerprint);
        Assert.assertTrue(unrevokeResult);

        boolean revokedAfterUnrevoke = revokedCertificateService.isRevoked(fingerprint);
        Assert.assertFalse(revokedAfterUnrevoke);
    }

    @Test
    public void testUnRevokeNonExistent() {
        boolean result = revokedCertificateService.unRevoke(tenantId, "never-revoked-" + UUID.randomUUID());

        Assert.assertFalse(result);
    }

    @Test
    public void testRevokeIsolation() {
        DeviceProfile profile = createAndSaveDeviceProfile();
        String fingerprintA = UUID.randomUUID().toString();
        String fingerprintB = UUID.randomUUID().toString();
        long expiryDate = System.currentTimeMillis() + 86400000L;

        revokedCertificateService.revoke(
                tenantId, profile.getId(), fingerprintA, RevocationReason.COMPROMISED, "cert A revoked", expiryDate, null);

        boolean revokedA = revokedCertificateService.isRevoked(fingerprintA);
        boolean revokedB = revokedCertificateService.isRevoked(fingerprintB);

        Assert.assertTrue(revokedA);
        Assert.assertFalse(revokedB);
    }

    @Test
    public void testFindRevokedCertificates() {
        DeviceProfile profile = createAndSaveDeviceProfile();
        long expiryDate = System.currentTimeMillis() + 86400000L;

        revokedCertificateService.revoke(
                tenantId, profile.getId(), UUID.randomUUID().toString(), RevocationReason.COMPROMISED, "cert 1", expiryDate, null);
        revokedCertificateService.revoke(
                tenantId, profile.getId(), UUID.randomUUID().toString(), RevocationReason.LOST, "cert 2", expiryDate, null);
        revokedCertificateService.revoke(
                tenantId, profile.getId(), UUID.randomUUID().toString(), RevocationReason.OTHER, "cert 3", expiryDate, null);

        PageData<RevokedCertificate> pageData = revokedCertificateService.findRevokedCertificates(
                tenantId, profile.getId(), new PageLink(10));

        Assert.assertNotNull(pageData);
        Assert.assertEquals(3, pageData.getTotalElements());
        Assert.assertEquals(3, pageData.getData().size());
        for (RevokedCertificate rc : pageData.getData()) {
            Assert.assertEquals(tenantId, rc.getTenantId());
            Assert.assertEquals(profile.getId(), rc.getDeviceProfileId());
        }
    }

    @Test
    public void testRevokeValidationRejectsNullTenantId() {
        DeviceProfile profile = createAndSaveDeviceProfile();
        String fingerprint = UUID.randomUUID().toString();
        long expiryDate = System.currentTimeMillis() + 86400000L;

        Assertions.assertThrows(IncorrectParameterException.class, () -> {
            revokedCertificateService.revoke(
                    null, profile.getId(), fingerprint, RevocationReason.COMPROMISED, "notes", expiryDate, null);
        });
    }

    @Test
    public void testRevokeValidationRejectsEmptyFingerprint() {
        DeviceProfile profile = createAndSaveDeviceProfile();
        long expiryDate = System.currentTimeMillis() + 86400000L;

        Assertions.assertThrows(IncorrectParameterException.class, () -> {
            revokedCertificateService.revoke(
                    tenantId, profile.getId(), "", RevocationReason.COMPROMISED, "notes", expiryDate, null);
        });
    }

}
