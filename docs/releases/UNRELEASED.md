# epiSLATE — Unreleased

Based on regular Beta 4. These changes are being prepared for a Test APK; they are not a published regular release.

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
- TV verification is pending for the full-version port: collection navigation/reentry, playback versus browsing return focus, stream numbering and completed-episode removal without restarting.

## Scope

Keep the current full-version exit strategy. This update does not add Trial licensing, activation, PIN restrictions, notices, Trial exit/contact information or its button colours. It does not reset profiles, progress or watched history. No regular release, protected-branch merge, signing/package change or Downloader replacement is included in the implementation authorization.
