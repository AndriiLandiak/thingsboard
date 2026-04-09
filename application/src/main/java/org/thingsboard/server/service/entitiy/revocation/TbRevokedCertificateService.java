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

import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.RevokedCertificateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.RevocationReason;
import org.thingsboard.server.common.data.security.RevokedCertificate;

public interface TbRevokedCertificateService {

    RevokedCertificate revokeByDevice(TenantId tenantId, DeviceId deviceId, RevocationReason reason, String notes) throws ThingsboardException;

    RevokedCertificate revokeByPem(TenantId tenantId, DeviceProfileId deviceProfileId, String pem, RevocationReason reason, String notes);

    void unRevoke(TenantId tenantId, RevokedCertificateId revokedCertificateId) throws ThingsboardException;

    PageData<RevokedCertificate> findRevokedCertificates(TenantId tenantId, DeviceProfileId deviceProfileId, PageLink pageLink);

}
