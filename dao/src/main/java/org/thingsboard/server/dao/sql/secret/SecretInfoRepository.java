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
package org.thingsboard.server.dao.sql.secret;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.SecretInfoEntity;

import java.util.List;
import java.util.UUID;

public interface SecretInfoRepository extends JpaRepository<SecretInfoEntity, UUID> {

    @Query("SELECT d FROM SecretInfoEntity d WHERE d.tenantId = :tenantId AND " +
            "(:searchText is NULL OR ilike(d.name, concat('%', :searchText, '%')) = true)")
    Page<SecretInfoEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                          @Param("searchText") String searchText,
                                          Pageable pageable);

    @Query("SELECT d.key FROM SecretInfoEntity d WHERE d.tenantId = :tenantId")
    List<String> findAllKeysByTenantId(@Param("tenantId") UUID tenantId);

}
