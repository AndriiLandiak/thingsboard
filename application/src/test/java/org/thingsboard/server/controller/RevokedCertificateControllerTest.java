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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.SaveDeviceWithCredentialsRequest;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.data.security.RevocationReason;
import org.thingsboard.server.common.data.security.RevokedCertificate;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class RevokedCertificateControllerTest extends AbstractControllerTest {

    private Tenant savedTenant;
    private User tenantAdmin;

    @Before
    public void beforeTest() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("Revocation Test Tenant");
        savedTenant = saveTenant(tenant);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant-revoke@thingsboard.org");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();
        deleteTenant(savedTenant.getId());
    }

    @Test
    public void testRevokeByDevice() throws Exception {
        String certPem = generateTestCertPem();
        DeviceProfile deviceProfile = createDeviceProfile("Revoke Test Profile");
        DeviceProfile savedProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);

        Device device = createDeviceWithX509Credentials(savedProfile.getId(), certPem);

        Map<String, Object> request = new HashMap<>();
        request.put("reason", "COMPROMISED");
        request.put("notes", "Device was compromised during testing");

        RevokedCertificate result = doPost(
                "/api/revokedCertificate/device/" + device.getId().getId().toString(),
                request, RevokedCertificate.class);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getId());
        Assert.assertEquals(savedProfile.getId(), result.getDeviceProfileId());
        Assert.assertEquals(RevocationReason.COMPROMISED, result.getRevocationReason());
        Assert.assertEquals("Device was compromised during testing", result.getRevocationNotes());
        Assert.assertNotNull(result.getCertificateFingerprint());
        Assert.assertTrue(result.getRevokedTimestamp() > 0);
        Assert.assertNotNull(result.getCertificateCommonName());
        Assert.assertTrue(result.getCertificateCommonName().startsWith("TestDevice"));
    }

    @Test
    public void testRevokeByPem() throws Exception {
        String certPem = generateTestCertPem();
        DeviceProfile deviceProfile = createDeviceProfile("PEM Revoke Profile");
        DeviceProfile savedProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);

        Map<String, Object> request = new HashMap<>();
        request.put("deviceProfileId", savedProfile.getId().getId().toString());
        request.put("pem", certPem);
        request.put("reason", "LOST");
        request.put("notes", "Device physically lost");

        RevokedCertificate result = doPost("/api/revokedCertificate/pem", request, RevokedCertificate.class);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getId());
        Assert.assertEquals(savedProfile.getId(), result.getDeviceProfileId());
        Assert.assertEquals(RevocationReason.LOST, result.getRevocationReason());
        Assert.assertEquals("Device physically lost", result.getRevocationNotes());
        // Fingerprint should be SHA3 hash of the PEM
        String expectedHash = EncryptionUtil.getSha3Hash(certPem);
        Assert.assertEquals(expectedHash, result.getCertificateFingerprint());
        Assert.assertTrue(result.getCertificateExpiryDate() > 0);
        Assert.assertNotNull(result.getCertificateCommonName());
        Assert.assertTrue(result.getCertificateCommonName().startsWith("TestDevice"));
    }

    @Test
    public void testUnrevoke() throws Exception {
        String certPem = generateTestCertPem();
        DeviceProfile deviceProfile = createDeviceProfile("Unrevoke Test Profile");
        DeviceProfile savedProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);

        // First revoke via PEM
        Map<String, Object> revokeRequest = new HashMap<>();
        revokeRequest.put("deviceProfileId", savedProfile.getId().getId().toString());
        revokeRequest.put("pem", certPem);
        revokeRequest.put("reason", "DECOMMISSIONED");
        revokeRequest.put("notes", "Test unrevoke");

        RevokedCertificate revoked = doPost("/api/revokedCertificate/pem", revokeRequest, RevokedCertificate.class);
        Assert.assertNotNull(revoked);

        // Now un-revoke
        doDelete("/api/revokedCertificate/" + revoked.getId().getId().toString())
                .andExpect(status().isOk());

        // Verify it's gone by listing
        PageData<RevokedCertificate> page = doGetTypedWithPageLink(
                "/api/deviceProfile/" + savedProfile.getId().getId().toString() + "/revokedCertificates?",
                new TypeReference<PageData<RevokedCertificate>>() {
                },
                new org.thingsboard.server.common.data.page.PageLink(10));
        Assert.assertEquals(0, page.getTotalElements());
    }

    @Test
    public void testListRevokedCertificates() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("List Test Profile");
        DeviceProfile savedProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);

        // Revoke 3 certificates via PEM
        for (int i = 0; i < 3; i++) {
            String certPem = generateTestCertPem();
            Map<String, Object> request = new HashMap<>();
            request.put("deviceProfileId", savedProfile.getId().getId().toString());
            request.put("pem", certPem);
            request.put("reason", "OTHER");
            request.put("notes", "Test cert " + i);
            doPost("/api/revokedCertificate/pem", request, RevokedCertificate.class);
        }

        PageData<RevokedCertificate> page = doGetTypedWithPageLink(
                "/api/deviceProfile/" + savedProfile.getId().getId().toString() + "/revokedCertificates?",
                new TypeReference<PageData<RevokedCertificate>>() {
                },
                new org.thingsboard.server.common.data.page.PageLink(10));

        Assert.assertNotNull(page);
        Assert.assertEquals(3, page.getTotalElements());
        Assert.assertEquals(3, page.getData().size());
        Assert.assertFalse(page.hasNext());
    }

    @Test
    public void testRevokeByDeviceNonX509Fails() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("Non X509 Profile");
        DeviceProfile savedProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);

        // Create device with default ACCESS_TOKEN credentials
        Device device = new Device();
        device.setName("Non X509 Device");
        device.setType("default");
        device.setDeviceProfileId(savedProfile.getId());
        Device savedDevice = doPost("/api/device", device, Device.class);

        Map<String, Object> request = new HashMap<>();
        request.put("reason", "COMPROMISED");
        request.put("notes", "Should fail");

        doPost("/api/revokedCertificate/device/" + savedDevice.getId().getId().toString(), request)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("X.509 credentials")));
    }

    @Test
    public void testRevokeByPemInvalidPemFails() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("Invalid PEM Profile");
        DeviceProfile savedProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);

        Map<String, Object> request = new HashMap<>();
        request.put("deviceProfileId", savedProfile.getId().getId().toString());
        request.put("pem", "not-a-valid-pem-content");
        request.put("reason", "COMPROMISED");
        request.put("notes", "Invalid PEM test");

        doPost("/api/revokedCertificate/pem", request)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Invalid PEM")));
    }

    @Test
    public void testUnrevokeNotFoundFails() throws Exception {
        String randomId = UUID.randomUUID().toString();
        doDelete("/api/revokedCertificate/" + randomId)
                .andExpect(status().isNotFound());
    }

    private String generateTestCertPem() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        X500Name issuer = new X500Name("CN=TestDevice" + System.nanoTime());
        BigInteger serial = BigInteger.valueOf(System.nanoTime());
        Date notBefore = new Date();
        Date notAfter = new Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000);

        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer, serial, notBefore, notAfter, issuer, keyPair.getPublic());
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA")
                .setProvider("BC")
                .build(keyPair.getPrivate());
        X509CertificateHolder certHolder = certBuilder.build(signer);
        X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certHolder);

        String base64 = Base64.getEncoder().encodeToString(cert.getEncoded());
        return "-----BEGIN CERTIFICATE-----\n" + base64 + "\n-----END CERTIFICATE-----";
    }

    private Device createDeviceWithX509Credentials(DeviceProfileId deviceProfileId, String certPem) throws Exception {
        Device device = new Device();
        device.setName("Test X509 Device " + System.nanoTime());
        device.setType("default");
        device.setDeviceProfileId(deviceProfileId);

        DeviceCredentials credentials = new DeviceCredentials();
        credentials.setCredentialsType(DeviceCredentialsType.X509_CERTIFICATE);
        credentials.setCredentialsValue(certPem);

        SaveDeviceWithCredentialsRequest saveRequest = new SaveDeviceWithCredentialsRequest(device, credentials);
        return doPost("/api/device-with-credentials", saveRequest, Device.class);
    }

}
