# Timmy's fork: customizations and updates

The integration branch is `dev`. Upstream is `homelessbrian/NuvioPlusIPTV`, which already incorporates official Nuvio changes plus IPTV and Sports.

## Branch policy

- `dev` is the stable baseline. Leave it untouched unless the owner explicitly authorizes a merge.
- `backup/dev-checkpoint-2026-11-12` and tag `known-good-2026-11-12` are frozen references.
- Create all new feature or fix branches under `work/`, with one clear feature or fix per branch.
- Commit in small, reviewable chunks with short, specific messages.
- If work goes wrong, switch to `dev` or the frozen backup and start a new `recovery/` branch. Do not reset or overwrite the baseline.
- `backup/*` are frozen checkpoints; `recovery/*` are emergency recovery branches; tags name milestones.

## Keep these customizations

- Full app launcher branding is epiSLATE; test builds append Test. Package IDs, signing and updater repository stay unchanged.
- Root Modern homepage uses rowsOnlyHomepage: the approved collection headings, spacing and normal scrolling, no homepage hero media/metadata, blue-charcoal gradient, pinned epiSLATE wordmark and collection focus styling. Collection drill-down layouts retain original defaults. Leaving a homepage row resets its horizontal position.

- Modern home collection tiles with blank Hero Backdrop and Hero Video fields do not render hero media. Tile covers and focus GIFs remain. Explicit collection hero media is allowed.
- English top-row labels remain `CONTINUE WATCHING` and `UPCOMING`.
- The in-app updater uses `TheTooth29/epiSLATE`, not the upstream repositories.
- Build workflows load this repository's saved Nuvio backend and TMDB settings. Debug builds require the login backend settings, preventing another APK with missing QR configuration.
- Keep this repository's signing configuration and version-code progression when publishing regular releases. A debug APK passing is not release-signing verification.

`scripts/check-personal-customizations.py` checks known source markers and configuration wiring. It stops on missing markers; it does not prove runtime behavior or silently reapply changes after upstream refactors.

## Prepare a future update

1. Open Actions -> **Prepare IPTV fork update**.
2. Run the reviewed workflow from its `work/` branch; it reads the stable `dev` baseline. Leave the tag blank for the newest published IPTV release, including betas. Enter a specific upstream release tag to choose a version.
3. Leave **dry_run** checked first. This checks the merge and customization markers without pushing a branch or publishing anything.
4. If it passes and you want that update, run again with **dry_run** unchecked.
5. The workflow creates a separate `work/iptv-...` branch and opens a draft pull request. It builds a debug APK in the same workflow, even when the push uses GitHub's default token. If repository settings prevent automated PR creation, inspect the error and open a PR from the prepared branch.
6. Download the new debug APK from that workflow's Artifacts. Test QR login, Streaming Platforms covers and GIFs without unwanted hero images, movie/show artwork, uppercase headings, and update settings.
7. Review and test the update PR. Merge into `dev` only when the repository owner explicitly instructs it.
8. Build a regular APK using **APK · epiSLATE manual or tagged build** on the reviewed `work/` branch. Release publishing remains a separate deliberate step; update preparation never publishes a release or replaces the Downloader APK.

The previous hourly direct-official-Nuvio merge-and-publish workflow has been replaced. Updates happen when requested.

## If preparation fails

Merge conflicts stop the workflow without changing `dev`. Resolve them on a separate update branch, keeping both upstream improvements and the custom behavior. Do not use a blanket force sync, discard local changes, or keep every conflicting local line automatically.

The old **Merge Nuvio release (manual)** workflow is a legacy upstream-kit recovery tool, not this personal update process. Do not use it for routine IPTV updates.

Saved Actions secrets remain in this repository. Upstream merges do not copy secrets from someone else's repository. Never put private keys or tokens in source files.

## Manual verification before publishing

- Login QR works.
- Netflix and other collection tiles retain their covers and focus GIFs; no implicit cover appears in the hero area.
- Explicit collection hero media still works if configured.
- Movie/show hero artwork still works after navigating between rows.
- Top-row headings remain uppercase.
- Updates point to this repository; the regular release installs over the previous regular release and has a higher version code.
