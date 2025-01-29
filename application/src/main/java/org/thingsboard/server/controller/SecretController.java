/**
 * Copyright © 2016-2024 The Thingsboard Authors
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

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.secret.TbSecretService;
import org.thingsboard.server.service.security.permission.Operation;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SECRET_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;
import static org.thingsboard.server.service.security.permission.Resource.SECRET;

@Slf4j
@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
public class SecretController extends BaseController {

    private final TbSecretService tbSecretService;

    @ApiOperation(value = "Save or Update Secret (saveSecret)",
            notes = "Create or update the Secret. When creating secret, platform generates Secret Id as " + UUID_WIKI_LINK +
                    "The newly created Secret Id will be present in the response. " +
                    "Specify existing Secret Id to update the secret. Secret name is not updatable, only value could be changed. " +
                    "Referencing non-existing Secret Id will cause 'Not Found' error." + NEW_LINE +
                    "Secret name is unique in the scope of tenant.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/secret")
    public SecretInfo saveSecret(
            @Parameter(description = "A JSON value representing the Secret.", required = true)
            @RequestBody @Valid Secret secret) throws Exception {
        secret.setTenantId(getTenantId());
        checkEntity(secret.getId(), secret, SECRET);
        return new SecretInfo(tbSecretService.save(secret, getCurrentUser()));
    }

    @ApiOperation(value = "Delete secret by ID (deleteEdge)",
            notes = "Deletes the secret. Referencing non-existing Secret Id will cause an error." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping(value = "/secret/{id}")
    public void deleteSecret(@PathVariable UUID id) throws ThingsboardException {
        SecretId secretId = new SecretId(id);
        Secret secret = checkSecretId(secretId, Operation.DELETE);
        tbSecretService.delete(secret, getCurrentUser());
    }

    @ApiOperation(value = "Get Tenant Secret infos (getSecretInfos)",
            notes = "Returns a page of secret infos owned by tenant. " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/secret/infos", params = {"pageSize", "page"})
    public PageData<SecretInfo> getSecretInfos(@Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
                                               @RequestParam int pageSize,
                                               @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
                                               @RequestParam int page,
                                               @Parameter(description = SECRET_TEXT_SEARCH_DESCRIPTION)
                                               @RequestParam(required = false) String textSearch,
                                               @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"name", "key"}))
                                               @RequestParam(required = false) String sortProperty,
                                               @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
                                               @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        TenantId tenantId = getCurrentUser().getTenantId();
        return checkNotNull(secretService.findSecretInfosByTenantId(tenantId, pageLink));
    }

    @ApiOperation(value = "Get Tenant Secret keys (getSecretKeys)",
            notes = "Returns a page of secret keys owned by tenant. " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/secret/keys")
    public List<String> getSecretKeys() throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        return checkNotNull(secretService.findSecretKeysByTenantId(tenantId));
    }

    @ApiOperation(value = "Get Secret info by Id (getSecretInfoById)", notes = TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/secret/info/{id}")
    public SecretInfo getSecretInfoById(@PathVariable UUID id) throws ThingsboardException {
        SecretId secretId = new SecretId(id);
        return checkEntityId(secretId, secretService::findSecretInfoById, Operation.READ);
    }

}
