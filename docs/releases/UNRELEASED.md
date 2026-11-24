# epiSLATE — Unreleased

Based on regular Beta 4. These changes passed the owner’s Test APK device sequence; they are not a published regular release.

## Collection browsing

- Make Two full rows the default collection layout and offer the layout selector in Appearance. Original remains available with its previous layout.
- Default expanded poster previews and Movies/Series subheadings to off when no preference has been saved. Existing saved choices remain respected, and both settings remain available.
- In the updated collection layouts, show single-catalog headings, including Decades, with softer grey heading focus and white text.
- Add faint outlines around unfocused grid posters and retain date clearance below the focused poster ring in the updated layouts.
- Improve navigation between headings and posters: Down enters the first poster, Up returns to the active heading, and heading changes cancel pending poster-entry requests.
- Reopen updated collection layouts at the top row and first heading. Original retains its previous browsing behavior; existing saved positions are not deleted.

## Home and streams

- On the Modern homepage, return to Continue Watching after catalog playback. Browsing without playback returns to the original catalog tile.
- Remove sidebar wordmarks while retaining homepage branding and profile controls.
- Number visible stream links in their existing order, retaining their original addon labels, descriptions, badges and logos.

## Fixes

- Fix completed episodes remaining labelled “New episode” in Continue Watching until the app is restarted, while retaining unfinished playback and unwatched next episodes.

## Testing status

- Original-renderer integrity and personal customization checks passed.
- Full Debug app/test compilation and 37 focused automated tests passed with no failures, errors or skipped tests. Coverage includes layout defaults/geometry, heading entry/cancellation, catalog return focus, watched-episode display filtering and existing episode-airing rules.
- Owner TV verification passed on Test 107117 (21 November 2026): Two full rows, heading/poster navigation, collection reentry, outlines/date clearance, browsing versus playback return focus, stream numbers with original labels/logos/badges, sidebar/home branding, and selecting Original then returning to Two full rows.
- Most importantly, the owner confirmed a completed episode disappeared from Continue Watching **without restarting**. Home → SmartTube audio and paused playback return/resume also passed; watched checkmarks appeared after sign-in/sync on the fresh Test install.
- Exact tested source: `a40290708626526833d575e332e5e7de06580e0e`; [successful Test build](https://github.com/TheTooth29/epiSLATE/actions/runs/44888590572). Signed packaging, Test identity and artifact checks passed.
- Unreported slow-loading catalog cases and preservation of previously saved default switches were not newly device-verified. A regular-package upgrade remains a separate release check; this was a fresh Test-package installation.

## Scope

Keep the current full-version exit strategy. This update does not add Trial licensing, activation, PIN restrictions, notices, Trial exit/contact information or its button colours. It does not reset profiles, progress or watched history. No regular release, protected-branch merge, signing/package change or Downloader replacement is included in the implementation authorization.


## Beta 5 login promotion — 24 November 2026

Owner explicitly approved enabling the tested email/password screen for regular Beta after the accepted dev merge. Release candidate branch work/release-beta-5 includes the new regular/Test package policy, approved login wording/wordmark/eye styling and explicit-submit behavior. Trial and custom-server handling remain unchanged. See [Beta5 candidate notes](1.0.0-beta.5.md). Previous Test-only restrictions and failed107120 evidence remain historical, not the current promotion decision. Regular-package compile/upgrade/device validation remains separate.

The accepted browsing batch additionally restores exact heading/poster/row on Details→Back in updated layouts. Owner passed second/third-heading/repeated/index0, fresh entry and Original checks in Test107121; login/eye/keyboard/sync/reopen passed Test107122. No Beta5 device pass or publication claimed.
