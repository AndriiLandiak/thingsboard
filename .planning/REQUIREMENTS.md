# Requirements: X.509 Certificate Revocation

**Defined:** 2026-04-01
**Core Value:** A revoked device certificate must be permanently blocked from authenticating or re-provisioning, regardless of the certificate chain being valid.

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

### Data Model

- [x] **DATA-01**: System stores revoked certificates in a dedicated DB table with SHA3-256 fingerprint, tenant_id, device_profile_id, revocation_reason (enum + free-text notes), revoked_timestamp, and certificate_expiry_date
- [x] **DATA-02**: Revocation entries are tenant-isolated (tenant_id column with proper indexing)
- [x] **DATA-03**: SHA3-256 fingerprint has unique constraint for fast indexed lookups

### Revocation Core

- [x] **REVK-01**: Tenant admin can revoke a specific device's X.509 certificate from the device credentials tab
- [x] **REVK-02**: Tenant admin can revoke a certificate by pasting PEM content directly (system computes SHA3-256 hash, extracts CN and expiry)
- [ ] **REVK-03**: Revocation stores the device name, free-text reason/notes (admin-provided description), and certificate metadata
- [x] **REVK-04**: Revoked certificates are checked in MQTT transport auth flow (in DefaultTransportApiService.validateOrCreateDeviceX509Certificate)
- [x] **REVK-05**: Revocation check occurs before both credential lookup AND chain provisioning branches
- [x] **REVK-06**: A revoked certificate cannot re-provision a device through the X.509 chain flow
- [ ] **REVK-07**: Revoking a certificate does not affect the device profile's intermediate CA or other devices

### Lifecycle

- [ ] **LIFE-01**: Expired revocation entries (cert_expiry + 1 month) are automatically cleaned up
- [ ] **LIFE-02**: Tenant admin can remove an entry from the revoked certificates list (un-revoke)

### REST API

- [x] **API-01**: POST endpoint to revoke a device certificate (by device ID)
- [x] **API-02**: POST endpoint to revoke a certificate by PEM content (extracts hash, CN, expiry from cert)
- [x] **API-03**: DELETE endpoint to remove a revocation entry (un-revoke)
- [x] **API-04**: GET endpoint to list revoked certificates for a device profile (paginated)

### UI

- [ ] **UI-01**: "Revoke Certificate" button on device credentials tab (visible when device has X.509 credentials)
- [ ] **UI-02**: Revocation dialog with free-text reason/notes field (admin describes why cert is revoked)
- [ ] **UI-03**: Revoked certificates list/tab on device profile page showing device name, reason, revoked date, cert expiry, and "Remove" action
- [ ] **UI-04**: "Add to Revoked List" button on device profile revoked certs tab with PEM paste dialog
- [ ] **UI-05**: Confirmation dialog for un-revoke (remove from revoked list) action

## v2 Requirements

### Performance

- **PERF-01**: Cache layer (Caffeine/Redis) for revocation lookups on transport hot path
- **PERF-02**: Configurable auto-cleanup schedule interval

### Audit

- **AUDT-01**: Audit log entries for revoke/un-revoke actions
- **AUDT-02**: Revocation event notifications via rule engine

### Extended Features

- **EXTD-01**: Bulk revocation operations
- **EXTD-02**: Soft-revoke / temporary suspension with auto-restore
- **EXTD-03**: Certificate detail display (full subject, issuer, validity dates)

## Out of Scope

| Feature | Reason |
|---------|--------|
| CRL/OCSP integration | ThingsBoard is the relying party, not an external CA; IoT devices rarely check CRLs |
| CoAP/LwM2M transport check | X.509 chain auto-provisioning is MQTT-only |
| Device profile level revocation | Deleting the CA cert from profile already stops trust |
| TLS handshake rejection | After-chain-validation check is sufficient and simpler |
| Certificate renewal/rotation | Already handled by existing X.509 chain flow |
| Edge deployment sync | Complex distributed system concern, defer to future |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| DATA-01 | Phase 1 | Complete |
| DATA-02 | Phase 1 | Complete |
| DATA-03 | Phase 1 | Complete |
| REVK-01 | Phase 4 | Complete |
| REVK-02 | Phase 4 | Complete |
| REVK-03 | Phase 2 | Pending |
| REVK-04 | Phase 3 | Complete |
| REVK-05 | Phase 3 | Complete |
| REVK-06 | Phase 3 | Complete |
| REVK-07 | Phase 2 | Pending |
| LIFE-01 | Phase 2 | Pending |
| LIFE-02 | Phase 2 | Pending |
| API-01 | Phase 4 | Complete |
| API-02 | Phase 4 | Complete |
| API-03 | Phase 4 | Complete |
| API-04 | Phase 4 | Complete |
| UI-01 | Phase 5 | Pending |
| UI-02 | Phase 5 | Pending |
| UI-03 | Phase 5 | Pending |
| UI-04 | Phase 5 | Pending |
| UI-05 | Phase 5 | Pending |

**Coverage:**
- v1 requirements: 21 total
- Mapped to phases: 21
- Unmapped: 0

---
*Requirements defined: 2026-04-01*
*Last updated: 2026-04-01 after roadmap creation*
