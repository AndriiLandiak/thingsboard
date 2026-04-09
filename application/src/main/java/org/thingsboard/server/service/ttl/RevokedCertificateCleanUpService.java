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
package org.thingsboard.server.service.ttl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.revocation.RevokedCertificateDao;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Slf4j
@Service
@TbCoreComponent
@ConditionalOnExpression("${sql.ttl.revoked_certificates.enabled:true}")
public class RevokedCertificateCleanUpService extends AbstractCleanUpService {

    public static final String RANDOM_DELAY_INTERVAL_MS_EXPRESSION = "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.revoked_certificates.checking_interval_ms})}";

    private static final long ONE_DAY_MS = 24 * 60 * 60 * 1000L;

    private final RevokedCertificateDao revokedCertificateDao;

    @Value("${sql.ttl.revoked_certificates.expiry_buffer_days:1}")
    private int expiryBufferDays;

    public RevokedCertificateCleanUpService(PartitionService partitionService, RevokedCertificateDao revokedCertificateDao) {
        super(partitionService);
        this.revokedCertificateDao = revokedCertificateDao;
    }

    @Scheduled(
            initialDelayString = RANDOM_DELAY_INTERVAL_MS_EXPRESSION,
            fixedDelayString = "${sql.ttl.revoked_certificates.checking_interval_ms:86400000}"
    )
    public void cleanUp() {
        if (isSystemTenantPartitionMine()) {
            long cutoff = System.currentTimeMillis() - (expiryBufferDays * ONE_DAY_MS);
            revokedCertificateDao.deleteExpiredEntries(cutoff);
            log.info("Revoked certificates cleanup executed (cutoffTs={}, bufferDays={})", cutoff, expiryBufferDays);
        }
    }

}
