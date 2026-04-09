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

import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.RevokedCertificateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.RevocationReason;
import org.thingsboard.server.common.data.security.RevokedCertificate;

public interface RevokedCertificateService {

    RevokedCertificate revoke(TenantId tenantId, DeviceProfileId deviceProfileId, String fingerprint, RevocationReason reason, String notes, long certificateExpiryDate, String certificateCommonName);

    boolean isRevoked(String fingerprint);

    boolean unRevoke(TenantId tenantId, String fingerprint);

    RevokedCertificate findById(TenantId tenantId, RevokedCertificateId revokedCertificateId);

    PageData<RevokedCertificate> findRevokedCertificates(TenantId tenantId, DeviceProfileId deviceProfileId, PageLink pageLink);

}
