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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.thingsboard.server.service.ttl.RevokedCertificateCleanUpService.RANDOM_DELAY_INTERVAL_MS_EXPRESSION;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = RevokedCertificateCleanUpServiceTest.class)
@Slf4j
public class RevokedCertificateCleanUpServiceTest {

    @Value(RANDOM_DELAY_INTERVAL_MS_EXPRESSION)
    long randomDelayMs;

    @Value("${sql.ttl.revoked_certificates.checking_interval_ms}")
    long checkingIntervalMs;

    @Test
    public void givenInterval_whenRandomDelay_ThenDelayInInterval() {
        log.info("randomDelay {}", randomDelayMs);
        log.info("checkingIntervalMs {}", checkingIntervalMs);
        assertThat(randomDelayMs, greaterThanOrEqualTo(0L));
        assertThat(randomDelayMs, lessThanOrEqualTo(checkingIntervalMs));
    }

}
