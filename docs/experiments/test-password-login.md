# Test-only official account password login experiment

**Current:** Test107120, approved Trial-style preview; CI passed at550cf2f/run85, revised-screen TV tests pending. Test107118 proved existing-account email/password, sync and reopened-session behavior on a fresh install;107119 is visually rejected/superseded despite passing CI. Earlier “unverified/pending” statements below are historical stages. Current candidate notes: [TEST-107120.md](../releases/TEST-107120.md).

## Owner-approved revised sign-in UI

After successful account/sync/session-persistence device testing on107118, owner requested email/password-only Test UI, hiding QR and its switch. Test107119 uses “Welcome to epiSLATE” / exact “Make yourself at home.”, email/password fields, Show/Hide control and pale-blue Sign in. No provider name, remote instruction, registration/forgot-password copy or raw server error text is displayed. Underlying QR implementation and regular/Trial/custom-server screens remain intact; this supersedes the original QR-visible Test UI below.

Logo uses the actual EpiSlateProfileWordmark(lowerForPicker=true) component inside the same full-screen Column and profile-picker horizontalhuge/verticalxxxl padding; shared component supplies identical120dp height+16dp offset and asset. Profile screen is not redesigned. Password remains in-memory and clears on leaving/success. Server/session/sync methods unchanged; owner must check actual visual alignment, keyboard/focus and sign-in after build.

Owner authorized an isolated experiment with an existing Nuvio-hosted email/password account. Not released or proven server-compatible, and not a statement of commercial hosted-service permission.

Branch: work/test-email-password-login, based on owner-tested Beta4 adoption source26f3959. Planned Test107118, version1.0.0-beta.4, packagecom.nuvio.livetv.test. No merge, regular/Trial release, backend or Downloader changes.

QR remains the default. Signed-out official-server Test installs can select **Use email and password**, using the existing password-token endpoint and normal session import/profile/addon/history sync. **Use QR code instead** cancels QR polling state/clears errors and restores QR availability. No server URL/key/capability overrides, signup, customer provisioning or auth bypass. Exact package gate excludes regular, Trial and store packages; custom-server behavior remains as before. Password UI state stays in memory rather than saved instance state.

Owner enters credentials privately on TV. Never ask for or record the password, copy tokens, or include credential screenshots/logs. Existing transport diagnostics redact credential fields; request payload content is not newly logged. Only normal existing server authentication is used.

Device checks, one at a time after CI:
1. Update existing Test in place107117->107118 and confirm existing session/history remain.
2. To test login, use a spare signed-out Test install if available. Otherwise deliberately sign out **Test only** after explaining that sign-out clears local account state. Do not sign out regular epiSLATE or clear its data.
3. Confirm QR still appears, switch to email/password, type privately, and submit once. Report success or visible error without password/token.
4. If accepted, verify normal profiles, addons, watched checkmarks and Continue Watching sync, then restart and confirm session persists.
5. If rejected, return to QR and verify normal sign-in remains possible. Do not repeatedly retry invalid credentials.

Automated isolation policy tests cover official Test only, regular/Trial/store/lookalike exclusion, and custom-server exclusion. Full UI/VM compilation and existing auth/regression checks run in signed Test CI. Server acceptance and TV focus/keyboard behavior remain pending device testing.


## Approved Trial-style controls revision, Test107120
Owner rejected107119visuals and approved the revised preview before explicitly requesting build. Uses actual Trial activation Material3OutlinedTextField (320dp, default outlined shape and dark scheme primary#8AB8ED), solid#101923background,26spheading/17sp24spbody,12dpheadinggap/20dpbodygap and44dpfield-to-actionclearance. Sign-in button matches Trial idle/focus/pressed/disabled palette; standard visibility icon is inside password field. Profile wordmark component/insets unchanged. No Trial runtime, licensing, backend or preset code is adopted. Preserve107119source4ed5011 as visual revert point. Local source/customization/hash checks pass; full Compose build and new TV approval pending. Preview is a layout rendering, not a native APK screenshot.


## Test107121 integration — 24 November 2026

Owner authorized one combined Test build. Includes local correction0088726: password IME Done hides the keyboard and focuses the independently focusable TV eye, Back-dismiss focus recovery and directional routing, and explicit Sign in submission. Styling/wordmark/backend/session behavior retained; package gate remains Test-only. Combined with Trial-derived collection navigation restoration in updated layouts. New native compilation/CI and TV keyboard/eye/sign-in checks remain pending; previous failed107120 device result is not reclassified as passed. See docs/releases/TEST-107121.md.
