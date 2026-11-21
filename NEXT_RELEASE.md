# Next-release feature set

Base: tested Beta 3, `c5b155f9ca54b23116742caf2dd92878b7cd9872`.
The regular Beta 3 branch/APK and protected dev baseline are not replaced by this work.

## Changes

- Full exit on close: default on only when `confirm_exit_enabled` is absent. Explicit saved true/false preferences are retained. Existing double-Back/two-second exit behavior remains; Home is separate.
- Home header: move logo and clock together and center the logo box above the first row artwork using screen height and heading height. TV visual confirmation is required.
- Modern collection bottom row: match the existing 25% tile opacity on the lowest visible row. The final row dims while it is the bottom visible row and another row has focus. Focusing the final row restores full opacity; moving back up reapplies its fade. Always keep the focused row fully opaque. Only collection Modern layouts opt in; Classic/Grid and On Demand retain their behavior.
- Settings > Appearance > Home screen dimming: Off (default), Always, Scheduled; adjustable 10–70% dim level; default strength 50%; start/end times in half-hour increments (default 9 PM–7 AM). Schedules use the TV's local time, support midnight crossing, and matching start/end means all day. Settings follow the active profile. The home screen alone is dimmed, with no additional layer when the sidebar is expanded. No display/backlight or playback brightness changes.
- Home-button playback cleanup: on a genuine host activity background stop, save position and release ExoPlayer/MPV output and decoder resources. In-app navigation, activity finishing and configuration changes keep existing cleanup. Late initialization/retry paths defer while backgrounded. Returning rebuilds paused; MPV initial loading also honors pause before loading media. Live TV stops its renderers/audio output immediately on activity stop, abandons automatic focus, preserves the delayed provider connection cleanup, and prepares paused on return. Scheduled recording services are not stopped.

## Validation and limits

The audio root cause is still a device-level hypothesis, not proven by source inspection or compilation. The narrowly adapted reference is https://github.com/skoruppa/NuvioTV/commit/0b9269276e20b1bbe3997703f438e55c34df274d . Audio focus guidance: https://developer.android.com/media/optimize/audio-focus . No blanket upstream merge.

Focused automated coverage: overnight/daytime schedule boundaries, same-time full-day schedule, Off/Always modes, strength limits, and actual host background vs in-app navigation/finishing/recreation.

Device verification must cover repeated switches to SmartTube from both playing and paused Heartland, foreground return at the saved position without autoplay, Home during loading/retry, fast Home/return, Back-to-homepage then Home, ExoPlayer and MPV, Live TV/catch-up/local-recording playback, and the owner's relevant surround formats. One successful switch is insufficient.

First device test, when an APK is requested and available: play Heartland for about one minute, press Home while it is playing, then play the same SmartTube video used in the original reproduction. Ask for this result before assigning the next device test.

Visual verification: header balance at TV resolution; Modern collection navigation including the last row; home dimming Off/Always/Scheduled across midnight; profile switching; sidebar transitions; text/focus readability; playback unchanged.

## Release boundaries

No merge into dev, package rename, signing change, Beta 3 publishing, tag, Downloader replacement, or trial/activation work is authorized by this implementation request. A distributable test build is a separate step after source checks. Build/install/device-test coordination belongs in the Builds, installs & testing chat with an exact reviewed commit and this checklist carried over.

## Automated validation result

Validated source commit: `ed54dfa` on `work/next-release-features`. Full Debug app compilation and the five selected JUnit classes passed: 16 tests, zero failures/errors/skips. Coverage includes the new scheduling/lifecycle policies and existing startup, first-frame codec recovery, and audio fallback policies. The personal customization checker and diff whitespace check also passed. Android/TV visual checks, repeated SmartTube switches, surround-format checks, and an actual install/upgrade of a newly packaged APK remain pending. No APK was packaged, merged, tagged, or published by this work.

## Post-build-76 corrections

- Owner authorized the focus-dependent final-row fade correction. Acceptance sequence: second-last focused with final row visible -> final row at 25% opacity; final row focused -> 100%; move focus back up -> final row returns to 25%. Modern collections only; existing strength and focused-row readability remain unchanged.
- Blue play-button launcher tiles now reuse the approved normal epiSLATE logo directly, including the off-white wordmark, gold accent, and navy background. The normal logo source is unchanged. Android resource compilation/linking checks passed.
- Projectivy's delayed removal of an uninstalled icon cleared itself; no app change is needed for that observation.
- Owner reports for installed Test build 76: playing and paused Heartland -> Home -> SmartTube audio passed; paused saved-position return with working picture/sound passed; five repeated switches passed; double-Back exit passed; logo/clock looked perfect; home dimming worked well. The initial audio failure was withdrawn because Test had not yet been installed. Scheduled dimming, MPV, Live TV, and surround-format verification have not been reported.
- These corrections require a new test APK for TV confirmation. No merge, publishing, package migration, or Downloader change is part of this update.

Validation of these corrections: six focused checks compiled and executed against the actual Kotlin fade expression passed, including the requested second-last -> last -> second-last focus sequence, Modern-only scope, non-bottom rows, and a focused single row. The four normal-logo launcher resources compiled and linked successfully against Android's resource framework; canonical logo sources are unchanged. Personal customization and whitespace checks passed. No new APK has been produced for these two corrections yet.

## Android Settings application icon correction

Owner authorized this fix after Test build 77. The signed APK from run 43966487784 (artifact 14280625370, package com.nuvio.livetv.test, versionCode 107110) was inspected directly. Its application icon resolves to resource 0x7f080060, packaged as res/ft.xml, the epiSLATE vector; no application roundIcon was declared. The passed home-row activity icon is a separate inset resource pointing to that artwork. This identifies the packaged paths; the precise OEM fallback behavior has not been proven from device diagnostics.

The full-flavor application now uses dedicated square and round PNG icons rendered from the unchanged approved epiSLATE artwork at mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi (48/72/96/144/192 pixels). Launcher activity resources, leanback banners, logo source, focus fade, and playback code remain unchanged. Source customization checks now verify both manifest references and all ten PNG signatures/dimensions.

Validation: all ten images passed size, navy-background/transparency, and nonempty-wordmark checks. Android resource compilation/linking passed. Gradle processFullReleaseResources with livetvTestBuild=true passed; inspecting its actual linked resource package confirmed the application icon resolves to the PNG at each density, the roundIcon attribute is present, and all ten PNGs are packaged. This is resource validation, not a newly signed/installable APK or device confirmation. Next test: after installing the next Test APK, open Android Settings > Apps > Recently opened apps and confirm epiSLATE Test shows the wordmark instead of the generic Android robot. Keep the already-passed home-row appearance intact. No package identity, signing, stable dev, or permanent Beta3 Downloader change was made.

## Remove unused Launcher artwork setting

Owner reports Settings > Appearance > Launcher artwork exits the app and does not need the section. Removed the Launcher artwork card, picker state, and picker/change-confirmation handlers from ThemeSettingsContent. This removes the UI entry into that path across Appearance styles. Other Appearance controls (themes, settings style, home dimming, ambient mode, font/language) remain present. Launcher and Settings icon assets, activity/application manifests, stored icon choices, and AppIconManager initialization/normalization are preserved. No change to playback, trial/activation, package identity/signing, stable dev, or Downloader distribution.

Validation: reviewed the scoped deletion, confirmed no remaining Appearance entry/call into AppIconPickerDialog, confirmed other Appearance controls remain, and passed personal customization and whitespace checks. No new APK or on-device validation for this removal yet. First next-build check: open Appearance, confirm Launcher artwork is absent and the adjacent Home screen dimming controls still open normally.

## Test-only Tabbed Grid comparison

Owner authorized a reversible comparison after reviewing the scrolling video. Frozen starting point: `backup/tabbed-grid-original-2026-11-17` at `0592e9e6b1ab4937956118b70353135f882f204c`. Implementation branch: `work/tabbed-grid-layout-preview`.

Settings > Appearance > Tabbed Grid preview is available only to package `com.nuvio.livetv.test`. Choices: Original (default), Two full rows, Three full rows, Page by page, Centered preview. Layout choice, selected tab, and per-choice collection positions are stored separately from production preferences. Original retains the complete original renderer; `scripts/check-tabbed-grid-original.py` guards its exact checkpoint hash. The regular app and Follow Layout always use that original renderer. See `TABBED_GRID_PREVIEW.md` for recovery and comparison details.

Validation: Full Debug app and test compilation passed; all five focused JUnit tests passed with zero failures/errors/skips (package restriction, two-row and three-row edge scrolling, two-row page movement, and centered scrolling). Original-renderer hash, personal customization and whitespace checks passed. TV appearance and D-pad behavior remain unverified. No APK, device result, stable merge, release publishing or Downloader replacement is included. First device check: select Two full rows and confirm the initial collection view fits two full rows, then move down past the second row and report whether it advances cleanly by one row. Continue comparisons one test at a time in Builds, installs & testing.

## Tabbed Grid refinements after Test 79

Owner authorized implementation and a new Test APK after reviewing measured proposals. Removed Three full rows; an existing saved selection falls back to Original. Two full rows and Page by page retain six columns and identical geometry, using the actual date-line height plus 10dp padding instead of the former 48dp reserve. Keep existing 48dp horizontal margins, 12dp vertical insets and 16dp spacing. Centered uses five columns and the largest proportional cards fitting the width/height limits; its grid viewport shows one full row, with faint directional arrows outside it and no neighbouring rows. Dates are lowered only in experimental layouts; Original and other GridContentCard callers retain the original 2dp default. Selection and per-choice positions remain separate; no saved-position clearing. Frozen checkpoints for original source and tested build79 are retained.

Owner's Test79 results: Two full rows initial display and one-row edge scrolling passed; detail -> Back restored the selected tab/focused item and two complete rows; Three full rows initial/edge scrolling passed before its removal request; Page by page forward/back two-row movement passed. No Centered result was reported. These results do not validate the new sizing or arrows. Updated validation: Full Debug app/test compilation and all seven focused JUnit tests passed, with zero failures/errors/skips. Coverage includes removed-choice fallback, equal Two full rows/Page geometry, centered portrait/landscape sizing and arrow clearance, package gating and scrolling policies. Original-renderer hash, personal customization and whitespace checks passed. New TV sizing, date clearance and arrows await the requested Test APK/device check. Build/install/device checks continue in Builds, installs & testing, one check at a time. No stable merge, regular release, package/signing migration, trial change or Downloader replacement is authorized.

## Regular Beta 4 follow-up — approved selection, not published

User-facing draft: [Unreleased notes](docs/releases/UNRELEASED.md). Base `f93a8e8d1450636afafb069a787602ecbb7d5ed9`, checkpoint `backup/beta4-before-trial-adoption-2026-11-21`, work branch `work/beta4-trial-ui-adoption`. Original renderer/checkpoints remain intact. Selected neutral focus/return policies were adapted from Trial; no Trial engine, preset, credentials, restrictions or exit screen were imported. Current full-version exit strategy is retained. Defaults change only where no preference is saved; explicit settings survive.

Owner additionally selected numbered stream links; the visible-list numbers retain original addon identity and sorting. Repeated regular Beta4 report: an episode marked watched in Details remained in Continue Watching as New episode, then disappeared after full exit/reentry. Nocturne episode5 is the first known reproduction. A separate completion-display patch filters known-watched NextUp episode identities at initial/partial/final/enriched publications and cache-protected state branches; unfinished InProgress rewatches stay. This is a targeted source fix, not a claimed device pass or proof of the original root cause.

Validation: Full Debug app/test compilation passed; 37 tests across five selected JUnit classes passed with zero failures/errors/skips. Original-renderer hash, personal customizations and whitespace checks passed. Local log: `../output/next-release-checks/beta4-adoption-tests.log`. No APK or new TV result is claimed by this source validation. Required TV comparison: new layout defaults/Original choice, grey/single headings, Down/Back/Right with slow-loading tabs, Up from top posters, entry at first heading/row, outlines/date clearance, playback versus ordinary catalog return, sidebar branding and numbered stream identity/order. Separately finish an episode and return without restarting: it must not remain as New episode; resume and legitimate unwatched next episodes must still work. Never clear app data or watched history to test this fix.

Owner authorized implementation and handoff to Builds for Test APK first. No regular publication, protected merge, Trial change, package/signing migration or permanent Downloader update is authorized here.

## Full-version adoption — owner Test sequence completed, 21 November 2026

Signed Test107117 / build82, exact source `a40290708626526833d575e332e5e7de06580e0e`, run https://github.com/TheTooth29/epiSLATE/actions/runs/44888590572 succeeded. Artifact14701383339, SHA256 `99dffcece57d46414f2f6c2bdd95af755167d9391c3602a0a243cadb3eeb3c43`. Build/signature/package/icon/regression checks passed. Owner results are recorded in `../output/beta4-adoption-test-build-handoff.md` and summarized in `docs/releases/UNRELEASED.md`; later owner results supersede that handoff file’s older pending-test wording.

Owner passed Two full rows, heading Down/Back/repeated Right, first-poster Down/Up, first-heading/top-row reentry, outlines/date clearance, details Back to collection heading and further Back to originating homepage tile, stream numbering with addon identity/badges/logos retained, playback/Home/SmartTube audio and paused reopen/resume, playback return to Continue Watching, sidebar removal/home branding, and Original selection/navigation followed by Two full rows. Fresh Test install signed in/synced and showed watched checkmarks. Crucially, a completed unwatched episode disappeared from Continue Watching without restart. No new pass is claimed for unreported slow-loading cases or previously saved switches; no regular-package in-place upgrade is claimed from this fresh Test install.

Results recorded for review only. No merge, tag, regular release, push/build, Trial change or Downloader promotion was performed from this result handoff. Await the owner’s promotion decision.
