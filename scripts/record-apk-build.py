"""Name a verified APK from its actual manifest and record reproducible build evidence."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
from datetime import datetime, timezone

PACKAGES = {'regular': 'com.nuvio.livetv', 'test': 'com.nuvio.livetv.test', 'trial': 'com.nuvio.livetv.trial'}

def identity(badging, channel):
    match = re.search(r"^package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", badging, re.M)
    if not match or match[1] != PACKAGES[channel]:
        raise ValueError('APK package does not match the requested release channel')
    package, code, version = match.groups()
    if not re.fullmatch(r'[0-9A-Za-z.+-]+', version):
        raise ValueError('Unsafe or missing APK version name')
    if (channel == 'trial') != version.endswith('-trial'):
        raise ValueError('Trial version suffix does not match APK channel')
    return package, int(code), version

def certificate_digests(report):
    # apksigner uses numbered signers or SDK-range names for v3.1 signatures.
    # Source-stamp and public-key hashes must never be mistaken for APK signer certs.
    pattern = r'^(?:Signer(?: #\d+)?(?: \([^\r\n]+\))?|V[1-4](?:\.1)? Signer:) certificate SHA-256 digest:\s*([0-9a-fA-F]{64})\s*$'
    return list(dict.fromkeys(value.lower() for value in re.findall(pattern, report, re.M)))

def filename(channel, version, code, run):
    if not str(run).isdigit() or int(run) < 1:
        raise ValueError('A positive workflow run number is required')
    label = {'regular': 'epiSLATE', 'test': 'epiSLATE-Test', 'trial': 'epiSLATE-Trial'}[channel]
    sequence = 'build' if channel == 'trial' else 'run'
    return f'{label}-{version}-vc{code}-{sequence}{run}-universal.apk'

def main():
    parser = argparse.ArgumentParser(__doc__)
    parser.add_argument('--apk', type=Path, required=True)
    parser.add_argument('--aapt', required=True)
    parser.add_argument('--channel', choices=PACKAGES, required=True)
    parser.add_argument('--notes', type=Path, required=True)
    parser.add_argument('--signing-report', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    package, code, version = identity(subprocess.check_output([args.aapt, 'dump', 'badging', str(args.apk)], text=True), args.channel)
    certificates = certificate_digests(args.signing_report.read_text())
    if not certificates:
        raise ValueError('No APK signer certificate digest found in verified signing report')
    if not args.notes.is_file():
        raise ValueError('Release notes file missing: ' + str(args.notes))
    if str(code) not in args.notes.read_text(encoding='utf-8').splitlines()[0]:
        raise ValueError('Release-note heading must identify this exact Android version code')
    source = subprocess.check_output(['git', 'rev-parse', 'HEAD'], text=True).strip()
    if not re.fullmatch('[0-9a-f]{40}', source):
        raise ValueError('Exact checked-out source commit is required')
    run = os.environ['GITHUB_RUN_NUMBER']
    name = filename(args.channel, version, code, run)
    args.output.mkdir(parents=True, exist_ok=True)
    target = args.output / name
    shutil.copyfile(args.apk, target)
    notes_target = args.output / 'RELEASE-NOTES.md'
    shutil.copyfile(args.notes, notes_target)
    record = {
        'schema_version': 1, 'channel': args.channel, 'package': package,
        'version_name': version, 'version_code': code, 'architecture': 'universal',
        'filename': name, 'size_bytes': target.stat().st_size,
        'sha256': hashlib.sha256(target.read_bytes()).hexdigest(),
        'signing_certificate_sha256': certificates, 'source_commit': source,
        'workflow_run_number': int(run), 'workflow_run_id': os.environ['GITHUB_RUN_ID'],
        'workflow_run_attempt': int(os.environ.get('GITHUB_RUN_ATTEMPT', '1')),
        'workflow_url': f"https://github.com/{os.environ['GITHUB_REPOSITORY']}/actions/runs/{os.environ['GITHUB_RUN_ID']}",
        'created_at_utc': datetime.now(timezone.utc).isoformat(),
        'notes_source': str(args.notes),
        'notes_sha256': hashlib.sha256(notes_target.read_bytes()).hexdigest(),
        'verification': 'manifest channel and signing checked; required prior CI steps passed',
        'device_testing': 'not established by this build',
        'merge_status': 'not established by this build',
        'publication_status': 'artifact only; publication requires owner approval',
    }
    (args.output / 'BUILD-RECORD.json').write_text(json.dumps(record, indent=2) + '\n')
    summary = f"### {name}\n\nPackage: `{package}` · Android code: **{code}** · workflow run: **{run}**\n\nSource: `{source}`\n\nSHA-256: `{record['sha256']}`\n\nAPK verified; device testing, merge and publication are separate.\n"
    if os.environ.get('GITHUB_STEP_SUMMARY'):
        with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as stream:
            stream.write(summary)
    print(summary)

if __name__ == '__main__':
    main()
