# Roadmap: X.509 Certificate Revocation

## Overview

This roadmap delivers a certificate revocation system for ThingsBoard's X.509 chain authentication flow. The build order follows the natural dependency chain: data model first (everything depends on persistence), then business logic service, then the security-critical transport gate (proven before admin access exists), then the REST API that exposes revocation operations, and finally the UI that wraps it all for tenant admins. Each phase delivers a coherent, testable capability.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [ ] **Phase 1: Data Model and Persistence** - Database table, JPA entity, DAO layer for revoked certificates
- [ ] **Phase 2: Revocation Service** - Business logic for revoke, un-revoke, lookup, and auto-cleanup
- [ ] **Phase 3: Transport Security Gate** - Revocation check in DefaultTransportApiService blocking revoked certs from auth and re-provisioning
- [ ] **Phase 4: REST API** - Controller endpoints for revoke, un-revoke, list, and revoke-by-PEM operations
- [ ] **Phase 5: Admin UI** - Device credentials revoke button, device profile revoked certificates tab, dialogs

## Phase Details

### Phase 1: Data Model and Persistence
**Goal**: A dedicated revoked_certificates table exists with proper schema, tenant isolation, and indexed lookups, and the full JPA persistence layer can store and retrieve revocation entries
**Depends on**: Nothing (first phase)
**Requirements**: DATA-01, DATA-02, DATA-03
**Success Criteria** (what must be TRUE):
  1. A revoked certificate entry can be persisted with SHA3-256 fingerprint, tenant_id, device_name, revocation_reason, revoked_timestamp, and certificate_expiry_date
  2. The SHA3-256 fingerprint column has a unique constraint and lookups by fingerprint return in constant time via index
  3. Revocation entries are tenant-isolated — queries scoped to one tenant never return another tenant's data
  4. Database upgrade migration script runs cleanly against an existing ThingsBoard installation
**Plans:** 2 plans
Plans:
- [x] 01-01-PLAN.md — Domain model classes, JPA entity, ModelConstants, and schema DDL
- [x] 01-02-PLAN.md — DAO layer (Repository, DAO interface, JPA DAO impl) and integration tests

### Phase 2: Revocation Service
**Goal**: A service layer exists that encapsulates all revocation business logic — revoking, checking revocation status, un-revoking, listing, and auto-cleanup of expired entries
**Depends on**: Phase 1
**Requirements**: REVK-03, REVK-07, LIFE-01, LIFE-02
**Success Criteria** (what must be TRUE):
  1. Calling revoke() stores a revocation entry with device name, free-text reason/notes, and certificate metadata
  2. Calling isRevoked() with a SHA3-256 fingerprint returns true for revoked certificates and false for non-revoked ones
  3. Revoking one device's certificate does not affect any other device or the device profile's intermediate CA
  4. Un-revoking (removing from revoked list) makes a previously revoked fingerprint pass isRevoked() checks again
  5. Expired revocation entries (cert_expiry + 1 month) are automatically cleaned up by a scheduled job
**Plans:** 2 plans
Plans:
- [x] 02-01-PLAN.md — Service interface and implementation (revoke, isRevoked, unrevoke, findRevokedCertificates)
- [x] 02-02-PLAN.md — Cleanup service and thingsboard.yml TTL configuration

### Phase 3: Transport Security Gate
**Goal**: Revoked certificates are blocked at the transport authentication layer, preventing both credential lookup and chain re-provisioning
**Depends on**: Phase 2
**Requirements**: REVK-04, REVK-05, REVK-06
**Success Criteria** (what must be TRUE):
  1. A device presenting a revoked certificate to the MQTT transport is rejected with an authentication failure
  2. The revocation check occurs before both the existing-credentials branch and the chain-provisioning branch in DefaultTransportApiService.validateOrCreateDeviceX509Certificate()
  3. A revoked device that has its credentials deleted cannot re-provision itself through the X.509 chain flow
  4. Non-revoked certificates continue to authenticate and provision normally with no behavioral change
**Plans:** 1 plan
Plans:
- [x] 03-01-PLAN.md — Revocation guard clauses at both X.509 auth paths in DefaultTransportApiService with TDD tests

### Phase 4: REST API
**Goal**: Tenant admins can programmatically revoke and un-revoke certificates, and list revoked certificates for a device profile, through secured REST endpoints
**Depends on**: Phase 2
**Requirements**: REVK-01, REVK-02, API-01, API-02, API-03, API-04
**Success Criteria** (what must be TRUE):
  1. A tenant admin can revoke a device's certificate by calling POST on the device revocation endpoint
  2. A tenant admin can revoke a certificate by posting PEM content — the system computes the SHA3-256 hash, extracts CN and expiry automatically
  3. A tenant admin can un-revoke (remove from revoked list) by calling DELETE on the revocation endpoint
  4. A tenant admin can retrieve a paginated list of revoked certificates scoped to a device profile
  5. All endpoints enforce tenant admin authorization and tenant isolation
**Plans:** 3 plans
Plans:
- [x] 04-01-PLAN.md — App-layer service (TbRevokedCertificateService interface and implementation) with findById extension to dao-api service
- [x] 04-02-PLAN.md — RevokedCertificateController REST endpoints and integration tests
- [x] 04-03-PLAN.md — Gap closure: add certificate_common_name field and wire CN extraction in revokeByPem/revokeByDevice

### Phase 5: Admin UI
**Goal**: Tenant admins can revoke and un-revoke certificates and view revoked certificate lists through the ThingsBoard web interface
**Depends on**: Phase 4
**Requirements**: UI-01, UI-02, UI-03, UI-04, UI-05
**Success Criteria** (what must be TRUE):
  1. A "Revoke Certificate" button is visible on the device credentials tab when the device has X.509 credentials
  2. Clicking revoke opens a dialog with a free-text reason/notes field where the admin describes why the cert is revoked
  3. The device profile page shows a revoked certificates list with device name, reason, revoked date, cert expiry, and a "Remove" action per entry
  4. An "Add to Revoked List" button on the device profile revoked certs tab opens a PEM paste dialog for revoking certificates not tied to a current device
  5. Un-revoking (removing from list) requires confirmation through a dialog
**Plans**: TBD
**UI hint**: yes

## Progress

**Execution Order:**
Phases execute in numeric order: 1 -> 2 -> 3 -> 4 -> 5

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Data Model and Persistence | 2/2 | Complete | 2026-04-03 |
| 2. Revocation Service | 2/2 | Complete | 2026-04-06 |
| 3. Transport Security Gate | 1/1 | Complete | 2026-04-07 |
| 4. REST API | 3/3 | Complete | 2026-04-07 |
| 5. Admin UI | 0/0 | Not started | - |
