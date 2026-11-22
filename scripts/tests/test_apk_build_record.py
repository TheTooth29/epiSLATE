import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch
import tempfile
import json
import hashlib

spec = importlib.util.spec_from_file_location('apk_record', Path(__file__).parents[1] / 'record-apk-build.py')
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

class ApkBuildRecordTest(unittest.TestCase):
    def test_manifest_channel_is_enforced(self):
        sample = "package: name='com.nuvio.livetv.test' versionCode='107120' versionName='1.0.0-beta.4'"
        self.assertEqual(module.identity(sample, 'test'), ('com.nuvio.livetv.test', 107120, '1.0.0-beta.4'))
        for channel in ('regular', 'trial'):
            with self.assertRaises(ValueError): module.identity(sample, channel)

    def test_trial_suffix_is_enforced(self):
        with self.assertRaises(ValueError):
            module.identity("package: name='com.nuvio.livetv.trial' versionCode='107117' versionName='1.0.0-beta.4'", 'trial')

    def test_unsafe_version_and_missing_manifest_are_rejected(self):
        for sample in ('', "package: name='com.nuvio.livetv.test' versionCode='1' versionName='../../bad'"):
            with self.assertRaises(ValueError): module.identity(sample, 'test')

    def test_channels_and_sequences_cannot_be_confused(self):
        self.assertEqual(module.filename('trial', '1.0.0-beta.4-trial', 107117, 14),
            'epiSLATE-Trial-1.0.0-beta.4-trial-vc107117-build14-universal.apk')
        self.assertEqual(module.filename('test', '1.0.0-beta.4', 107120, 85),
            'epiSLATE-Test-1.0.0-beta.4-vc107120-run85-universal.apk')
        with self.assertRaises(ValueError): module.filename('test', '1', 1, '../bad')

    def test_record_uses_actual_bytes_and_checkout_not_workflow_sha(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            apk = root / 'candidate.apk'; apk.write_bytes(b'controlled-fixture-bytes')
            notes = root / 'notes.md'; notes.write_text('# epiSLATE Test — 107120', encoding='utf-8')
            signer = root / 'signing.txt'; signer.write_text('Signer #1 certificate SHA-256 digest: ' + 'a'*64)
            output = root / 'out'
            argv = ['record', '--apk', str(apk), '--aapt', 'aapt', '--channel', 'test', '--notes', str(notes), '--signing-report', str(signer), '--output', str(output)]
            manifest = "package: name='com.nuvio.livetv.test' versionCode='107120' versionName='1.0.0-beta.4'"
            env = {'GITHUB_RUN_NUMBER':'85','GITHUB_RUN_ID':'123','GITHUB_REPOSITORY':'owner/repo','GITHUB_SHA':'wrong-workflow-commit'}
            with patch('sys.argv', argv), patch.dict('os.environ', env, clear=True), patch.object(module.subprocess, 'check_output', side_effect=[manifest, 'b'*40]):
                module.main()
            record = json.loads((output / 'BUILD-RECORD.json').read_text())
            self.assertEqual(record['source_commit'], 'b'*40)
            self.assertEqual(record['sha256'], hashlib.sha256(apk.read_bytes()).hexdigest())
            self.assertEqual(record['size_bytes'], len(apk.read_bytes()))
            self.assertEqual(record['version_code'], 107120)
            self.assertEqual(record['device_testing'], 'not established by this build')
            self.assertTrue((output / record['filename']).exists())

class SigningReportTest(unittest.TestCase):
    def test_numbered_and_sdk_range_signers(self):
        digest = 'a'*64
        report = '\n'.join([
            'Signer #1 certificate SHA-256 digest: ' + digest,
            'Signer (minSdkVersion=33, maxSdkVersion=2147483647) certificate SHA-256 digest: ' + digest.upper(),
            'Signer #2 (minSdkVersion=28, maxSdkVersion=32) certificate SHA-256 digest: ' + 'b'*64,
        ])
        self.assertEqual(module.certificate_digests(report), [digest, 'b'*64])
    def test_only_real_signer_certificate_digests_are_accepted(self):
        report = '\n'.join([
            'Source Stamp Signer certificate SHA-256 digest: ' + 'a'*64,
            'Signer #1 public key SHA-256 digest: ' + 'b'*64,
            'Signer #1 certificate SHA-256 digest: too-short',
        ])
        self.assertEqual(module.certificate_digests(report), [])

class ActualCiSigningReportTest(unittest.TestCase):
    def test_exact_trial16_v2_signer_line(self):
        digest = 'e2c9a67239c6297fb28a28b72a088ad32cff8aeb94e0157b02d22ee7aeb5cbd8'
        report = 'V2 Signer: certificate SHA-256 digest: ' + digest
        self.assertEqual(module.certificate_digests(report), [digest])
    def test_scheme_labels_do_not_admit_stamp_or_public_key(self):
        for version in ['1','2','3','3.1','4']:
            self.assertEqual(module.certificate_digests('V' + version + ' Signer: certificate SHA-256 digest: ' + 'a'*64), ['a'*64])
        self.assertEqual(module.certificate_digests('V2 Signer: public key SHA-256 digest: ' + 'a'*64), [])

if __name__ == '__main__': unittest.main()
