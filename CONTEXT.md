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

**Primary Signal**:
The primary cobalt blue functional color token (`KasirPrimary`) representing user actions, primary buttons, and active indicators.
_Avoid_: KasirGreen, brand color

**Paper Counter Theme**:
The default high-contrast light theme surface mood designed for readability under direct daylight counter conditions.
_Avoid_: Light mode, white theme

**Night Counter Theme**:
The low-glare dark theme surface mood designed for evening or dim rental counter environments.
_Avoid_: Dark mode, black theme

**Revenue**:
The total monetary intake across all completed transactions within a given filter period or shift.
_Avoid_: Omset, sales volume

**Cash**:
Physical currency payments collected in the register drawer.
_Avoid_: Cash fisik, uang tunai

**Receipt Format**:
The layout, typography, visibility rules, and hardware parameters governing how transaction receipts render on thermal paper.
_Avoid_: Printer template, struk layout, print config

**Receipt Preview**:
A live, monospaced on-screen facsimile of the physical thermal receipt rendered before sending commands to a printer.
_Avoid_: Struk mock, print simulation

**Shift Settlement**:
The formal closing procedure of an operator's shift that validates financial totals (revenue, cash, QRIS), blocks closing if active sessions remain, records audit history to the server, and prints a thermal settlement receipt.
_Avoid_: Daily settlement, end of day, simple logout

**Active Session Guard**:
The operational constraint that strictly forbids a cashier from closing their shift while one or more rental sessions remain active in the field.
_Avoid_: Force handover, oper sesi, closing paksa

**Settlement Tab**:
A top-level navigation destination beside History that displays real-time shift metrics, rental item breakdown, and provides the Shift Settlement trigger.
_Avoid_: Closing tab, ringkasan tab, admin menu

**Item Rental Breakdown**:
The aggregated quantity and gross rental intake grouped by equipment name across all completed transactions within the current cashier shift.
_Avoid_: Sales report, rekap produk, inventory log
