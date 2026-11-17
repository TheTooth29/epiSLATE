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
