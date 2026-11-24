"""Check actual APK identities and signatures before distributing an in-place update."""
import argparse
import json
import re
import subprocess


def apk_info(aapt, apksigner, path):
    badging = subprocess.check_output([aapt, "dump", "badging", path], text=True)
    package_line = next(line for line in badging.splitlines() if line.startswith("package:"))
    fields = dict(re.findall(r"(\w+)='([^']*)'", package_line))
    label = re.search(r"^application-label:'([^']*)'", badging, re.MULTILINE)
    certificates = subprocess.check_output([apksigner, "verify", "--print-certs", path], text=True)
    # Build-tools can label signers by number, SDK range, or signature scheme.
    signers = sorted(set(signer.lower() for signer in re.findall(
        r"^(?:Signer (?:#\d+|\(minSdkVersion=\d+, maxSdkVersion=\d+\))|V[1234](?:\.\d+)? Signer:) certificate SHA-256 digest:[ \t]*([0-9a-fA-F]{64})[ \t]*$",
        certificates, re.MULTILINE
    )))
    if not signers:
        raise ValueError("No signing certificate recognized in apksigner output:\n" + certificates)
    return {
        "package": fields["name"], "version_code": int(fields["versionCode"]),
        "version_name": fields["versionName"], "label": label.group(1) if label else None,
        "signers": [signer.lower() for signer in signers]
    }


def upgrade_checks(old, new, previous_version, previous_code, candidate_version, candidate_code):
    return {
        "regular_app_identity": old["package"] == new["package"] == "com.nuvio.livetv",
        "expected_previous_version": old["version_name"] == previous_version and old["version_code"] == previous_code,
        "expected_candidate_version": new["version_name"] == candidate_version and new["version_code"] == candidate_code,
        "higher_version_code": new["version_code"] > old["version_code"],
        "regular_app_label": new["label"] == "epiSLATE",
        "same_signing_certificates": new["signers"] == old["signers"],
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--aapt", required=True)
    parser.add_argument("--apksigner", required=True)
    parser.add_argument("--previous", required=True)
    parser.add_argument("--candidate", required=True)
    parser.add_argument("--previous-version", required=True)
    parser.add_argument("--previous-code", type=int, required=True)
    parser.add_argument("--candidate-version", required=True)
    parser.add_argument("--candidate-code", type=int, required=True)
    args = parser.parse_args()
    old = apk_info(args.aapt, args.apksigner, args.previous)
    new = apk_info(args.aapt, args.apksigner, args.candidate)
    checks = upgrade_checks(old, new, args.previous_version, args.previous_code,
                            args.candidate_version, args.candidate_code)
    print(json.dumps({"previous": old, "candidate": new, "checks": checks}, indent=2))
    failed = [name for name, passed in checks.items() if not passed]
    if failed:
        raise SystemExit("APK upgrade checks failed: " + ", ".join(failed))
    print("Regular APK identity, expected versions, increasing code and signing continuity verified.")


if __name__ == "__main__":
    main()
