# Trial profile update feed

Only Trial full-flavor APKs check this feed, on entering the profile picker. Regular updater startup, jobs, release selection, and Beta channels remain unchanged. Store flavor has a no-op host. Network/missing-feed/invalid-feed errors are silent. No update is offered for equal or older versions.

The approved release `trial-download` can carry an additional asset `trial-update.json`:

```json
{
  "approved": false,
  "packageName": "com.nuvio.livetv.trial",
  "versionCode": 107116,
  "apkUrl": "https://github.com/TheTooth29/epiSLATE/releases/download/trial-download/epiSLATE-Trial.apk",
  "sha256": "REPLACE_WITH_EXACT_VERIFIED_APK_SHA256"
}
```

This example is deliberately ineligible. Publishing an approved feed and replacing the public APK require owner release approval. First verify the exact signed, device-tested APK, then replace the Trial APK asset, then publish the feed with that APK's actual versionCode and SHA-256 and `approved: true`. Regular Beta assets/Downloader must remain unchanged. A transient mismatch fails the download hash check rather than installing a different candidate.

The UI offers Install update or Later. Android unknown-source permission is handled within the prompt without unlocking epiSLATE Settings; after permission is granted return and select Install update again. Only explicit foreground Android installation is used, never automatic background install or data reset. The downloaded APK must match the feed digest, current Trial package, a newer installed version, and exact approved version. Android enforces signing compatibility on install. Dismissal/Back returns focus to the profile page. Checks occur only while that selection page is composed; no homepage/startup jobs are added.

Existing installations without this feature need an initial manual in-place update before they can display future profile update prompts. The compatible seven-day backend minimum version must be coordinated with the exact owner-approved replacement APK; do not enable it from this documentation or source change.

Device checks: no prompt with missing/current/older/unapproved feed; newer approved Trial prompt; TV directional focus and Later/Back restore; unknown-source permission round trip; download failure/hash mismatch; foreground installer; in-place activation/profile/history retention; no regular Beta prompt or downgrade. Do not publish fake newer feed entries to test on live prospects.
