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
package org.thingsboard.server.service.entitiy.revocation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.RevokedCertificateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.data.security.RevocationReason;
import org.thingsboard.server.common.data.security.RevokedCertificate;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.common.transport.util.SslUtil;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.revocation.RevokedCertificateService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.security.cert.X509Certificate;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultTbRevokedCertificateService implements TbRevokedCertificateService {

    private final DeviceService deviceService;
    private final DeviceCredentialsService deviceCredentialsService;
    private final RevokedCertificateService revokedCertificateService;

    @Override
    public RevokedCertificate revokeByDevice(TenantId tenantId, DeviceId deviceId, RevocationReason reason, String notes) throws ThingsboardException {
        log.trace("Executing revokeByDevice [{}][{}]", tenantId, deviceId);
        Device device = deviceService.findDeviceById(tenantId, deviceId);
        if (device == null) {
            throw new ThingsboardException("Device not found", ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, deviceId);
        if (credentials == null || credentials.getCredentialsType() != DeviceCredentialsType.X509_CERTIFICATE) {
            throw new DataValidationException("Device does not have X.509 credentials");
        }
        String fingerprint = credentials.getCredentialsId();
        DeviceProfileId deviceProfileId = device.getDeviceProfileId();

        // Attempt to extract certificate expiry and CN from credentialsValue (Base64 cert content)
        long expiryDate = 0L;
        String commonName = null;
        String credentialsValue = credentials.getCredentialsValue();
        if (credentialsValue != null && !credentialsValue.isEmpty()) {
            try {
                X509Certificate cert = SslUtil.readCertFile(credentialsValue);
                if (cert != null) {
                    expiryDate = cert.getNotAfter().getTime();
                    commonName = SslUtil.parseCommonName(cert);
                }
            } catch (Exception e) {
                log.debug("Could not parse certificate metadata from credentialsValue for device [{}], using defaults", deviceId, e);
            }
        }

        return revokedCertificateService.revoke(tenantId, deviceProfileId, fingerprint, reason, notes, expiryDate, commonName);
    }

    @Override
    public RevokedCertificate revokeByPem(TenantId tenantId, DeviceProfileId deviceProfileId, String pem, RevocationReason reason, String notes) {
        log.trace("Executing revokeByPem [{}][{}]", tenantId, deviceProfileId);
        if (pem == null || pem.isBlank()) {
            throw new DataValidationException("PEM certificate content is required");
        }
        X509Certificate cert = SslUtil.readCertFile(pem);
        if (cert == null) {
            throw new DataValidationException("Invalid PEM certificate content");
        }
        String sha3Hash = EncryptionUtil.getSha3Hash(pem);
        long expiryDate = cert.getNotAfter().getTime();
        String commonName = SslUtil.parseCommonName(cert);

        return revokedCertificateService.revoke(tenantId, deviceProfileId, sha3Hash, reason, notes, expiryDate, commonName);
    }

    @Override
    public void unRevoke(TenantId tenantId, RevokedCertificateId revokedCertificateId) throws ThingsboardException {
        log.trace("Executing unrevoke [{}][{}]", tenantId, revokedCertificateId);
        RevokedCertificate revokedCertificate = revokedCertificateService.findById(tenantId, revokedCertificateId);
        if (revokedCertificate == null) {
            throw new ThingsboardException("Revoked certificate entry not found", ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        if (!revokedCertificate.getTenantId().equals(tenantId)) {
            throw new ThingsboardException("Revoked certificate entry not found", ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        revokedCertificateService.unRevoke(tenantId, revokedCertificate.getCertificateFingerprint());
    }

    @Override
    public PageData<RevokedCertificate> findRevokedCertificates(TenantId tenantId, DeviceProfileId deviceProfileId, PageLink pageLink) {
        log.trace("Executing findRevokedCertificates [{}][{}]", tenantId, deviceProfileId);
        return revokedCertificateService.findRevokedCertificates(tenantId, deviceProfileId, pageLink);
    }

}
