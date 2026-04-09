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

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.RevokedCertificateId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.RevocationReason;
import org.thingsboard.server.common.data.security.RevokedCertificate;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.revocation.TbRevokedCertificateService;
import org.thingsboard.server.service.security.permission.Operation;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class RevokedCertificateController extends BaseController {

    private final TbRevokedCertificateService tbRevokedCertificateService;

    @Data
    public static class RevokeDeviceRequest {
        private RevocationReason reason;
        private String notes;
    }

    @Data
    public static class RevokePemRequest {
        private String deviceProfileId;
        private String pem;
        private RevocationReason reason;
        private String notes;
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/revokedCertificate/device/{deviceId}", method = RequestMethod.POST)
    @ResponseBody
    public RevokedCertificate revokeDeviceCertificate(
            @PathVariable("deviceId") String strDeviceId,
            @RequestBody RevokeDeviceRequest request) throws ThingsboardException {
        checkParameter("deviceId", strDeviceId);
        DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        checkDeviceId(deviceId, Operation.READ_CREDENTIALS);
        return tbRevokedCertificateService.revokeByDevice(
                getTenantId(), deviceId, request.getReason(), request.getNotes());
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/revokedCertificate/pem", method = RequestMethod.POST)
    @ResponseBody
    public RevokedCertificate revokeByPem(
            @RequestBody RevokePemRequest request) throws ThingsboardException {
        checkParameter("deviceProfileId", request.getDeviceProfileId());
        checkParameter("pem", request.getPem());
        DeviceProfileId deviceProfileId = new DeviceProfileId(toUUID(request.getDeviceProfileId()));
        checkDeviceProfileId(deviceProfileId, Operation.READ);
        return tbRevokedCertificateService.revokeByPem(
                getTenantId(), deviceProfileId, request.getPem(), request.getReason(), request.getNotes());
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/revokedCertificate/{revokedCertificateId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void unrevokeRevokedCertificate(
            @PathVariable("revokedCertificateId") String strRevokedCertificateId) throws ThingsboardException {
        checkParameter("revokedCertificateId", strRevokedCertificateId);
        RevokedCertificateId revokedCertificateId = new RevokedCertificateId(toUUID(strRevokedCertificateId));
        tbRevokedCertificateService.unRevoke(getTenantId(), revokedCertificateId);
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/deviceProfile/{deviceProfileId}/revokedCertificates")
    @ResponseBody
    public PageData<RevokedCertificate> getRevokedCertificates(
            @PathVariable("deviceProfileId") String strDeviceProfileId,
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("deviceProfileId", strDeviceProfileId);
        DeviceProfileId deviceProfileId = new DeviceProfileId(toUUID(strDeviceProfileId));
        checkDeviceProfileId(deviceProfileId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(tbRevokedCertificateService.findRevokedCertificates(
                getTenantId(), deviceProfileId, pageLink));
    }
}
