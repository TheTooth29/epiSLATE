"""Regression coverage for Android build-tools certificate output formats."""
import importlib.util
from pathlib import Path
import subprocess
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("upgrade", Path(__file__).with_name("verify-android-upgrade.py"))
upgrade = importlib.util.module_from_spec(spec)
spec.loader.exec_module(upgrade)


class CertificateOutputTest(unittest.TestCase):
    def info(self, output):
        badging = "package: name='com.nuvio.livetv' versionCode='107105' versionName='1.0.0-beta.2'\napplication-label:'epiSLATE'"
        with patch.object(upgrade.subprocess, "check_output", side_effect=[badging, output]):
            return upgrade.apk_info("aapt", "apksigner", "app.apk")

    def test_numbered_signer(self):
        self.assertEqual(self.info("Signer #1 certificate SHA-256 digest: " + "AB" * 32)["signers"], ["ab" * 32])

    def test_actual_beta_2_runner_output(self):
        output = """V2 Signer: certificate DN: CN=Nuvio Live TV fork
V2 Signer: certificate SHA-256 digest: e2c9a67239c6297fb28a28b72a088ad32cff8aeb94e0157b02d22ee7aeb5cbd8
V2 Signer: certificate SHA-1 digest: bd1bb0bfef152a4702f67d942534447fe1b80757
V2 Signer: certificate MD5 digest: 346cdaf61533ad6cc11fb12635bd2f5b
"""
        self.assertEqual(self.info(output)["signers"], ["e2c9a67239c6297fb28a28b72a088ad32cff8aeb94e0157b02d22ee7aeb5cbd8"])

    def test_scheme_labels_deduplicate_same_certificate(self):
        output = "\n".join(f"{scheme} Signer: certificate SHA-256 digest: " + "ab" * 32
                           for scheme in ["V1", "V2", "V3", "V3.1", "V4"])
        self.assertEqual(self.info(output)["signers"], ["ab" * 32])

    def test_sdk_range_signers_and_duplicate_certificates(self):
        lines = [f"Signer (minSdkVersion={lo}, maxSdkVersion={hi}) certificate SHA-256 digest: " + "ab" * 32
                 for lo, hi in [(24, 32), (33, 2147483647)]]
        self.assertEqual(self.info("\n".join(lines))["signers"], ["ab" * 32])

    def test_different_signers_are_preserved(self):
        lines = [f"Signer #{i} certificate SHA-256 digest: " + digest * 32
                 for i, digest in [(1, "ab"), (2, "cd")]]
        self.assertEqual(self.info("\n".join(lines))["signers"], ["ab" * 32, "cd" * 32])

    def test_source_stamp_is_not_app_signer(self):
        with self.assertRaises(ValueError):
            self.info("Source Stamp Signer certificate SHA-256 digest: " + "ab" * 32)

    def test_unknown_or_malformed_output_fails_closed(self):
        for output in ["", "Signer #1 certificate SHA-256 digest: abcd"]:
            with self.subTest(output=output), self.assertRaises(ValueError):
                self.info(output)

    def test_signature_verification_failure_propagates(self):
        with patch.object(upgrade.subprocess, "check_output", side_effect=subprocess.CalledProcessError(1, "apksigner")):
            with self.assertRaises(subprocess.CalledProcessError):
                upgrade.apk_info("aapt", "apksigner", "app.apk")


if __name__ == "__main__":
    unittest.main()
