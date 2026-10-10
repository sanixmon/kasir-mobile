# Kasir Mobile

A native Android client for the Evren House POS cashier and rental counter system, communicating with the kasir-backend service.

## Language

**Version Name**:
The human-readable semantic version of the client application formatted as `MAJOR.MINOR.PATCH`.
_Avoid_: Build tag, code version

**Version Code**:
A strictly monotonically increasing integer required by the Android operating system to identify build progression and permit in-place updates.
_Avoid_: Run number, build string

**Release Tag**:
A Git reference matching `v*` (e.g., `v1.0.2`) that triggers the production release pipeline.
_Avoid_: CI build, branch release

**Update Checker**:
A client component that queries the public GitHub Releases API to detect newer application builds and provide in-app update prompts.
_Avoid_: Store updater, OTA service

**Shift**:
A cashier work session tied to an operator username that isolates audit logs and transactions.
_Avoid_: Login session, account

**Session**:
An active customer rental period for scooters or strollers tracking elapsed time and equipment items.
_Avoid_: Order, booking
