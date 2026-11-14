# Invitation preview (local prototype)

This branch implements a separate invitation APK, not restrictions on beta 1.

- Opt in locally with `-PepislateTrial=true`. Default builds stay unrestricted.
- Trial application ID: `com.nuvio.livetv.trial`; launcher label: epiSLATE Trial.
- Generic `livetvTestBuild=true` cannot be combined with the trial flag.
- Six-digit codes redeem offline and start 72 hours. The bundled allowlist contains hashes, not an online licensing service.
- Redemption is stored atomically; subsequent codes never extend the installation's deadline.
- Clearing data or reinstalling resets local protection. Codes are reusable on other installations. This preview deliberately does not claim global one-time use.
- Current viewing and recorder jobs can finish after expiry. New episodes, channels, external launches, and recordings require active time. A live channel can continue until stopped/left; no hidden grace timer.
- Trial Settings and protected subpages require the owner's master PIN. Leaving that area or backgrounding relocks it. The PIN does not bypass trial expiry.
- Trial automatic updates are disabled to avoid offering the unrestricted beta. Existing beta updater/signing/package configuration is preserved.
- No subscriptions, purchase links, service accounts, build dispatches, or release publication are introduced.

The owner's 100-code list and manual tracking database are kept outside the repository. The database records distribution and manually reported activation, not automatic usage telemetry. Do not commit recipients or the plaintext list.

Validation: run the trial unit tests with the trial build flag, then device-test code entry, restart persistence, exact expiry, finishing sessions, navigation guards, side-by-side installation, and remote focus before distribution. Do not build or publish an APK without the owner's request.

## Local validation

- 15 focused JVM tests passed: exact expiry, clock rollback/reboot handling, malformed state, actual activation persistence across reconstructed stores, failed commits, repeated codes, and concurrent redemption.
- All 100 owner codes were checked against the actual embedded hash allowlist; invalid-code checks passed.
- Trial engine and screens compiled in isolation with Kotlin 2.3.0 and its Compose compiler plugin using cached Android libraries.
- The repository customization checker and whitespace/diff checks passed.
- Generated trial configuration and merged manifest verified package, version code/name, separate internal upstream version, all 12 launcher labels, provider authority, and disabled updates.
- The local owner ledger has five passing tests; it remains outside this repository.

The complete app compilation was intentionally cancelled after it put excessive memory pressure on the local computer. The isolated compile is not full integration verification. No APK was assembled, no remote build was dispatched, and no emulator/device acceptance test was performed. Full app compilation and Android remote/focus, playback, recording, Settings relock, and side-by-side-installation checks remain required before distribution.

The normal beta configuration and merged manifest were also generated and verified: original package, version, all 12 launcher labels, provider authority, updates enabled, and trial enforcement disabled. No APK was assembled by these configuration checks.
