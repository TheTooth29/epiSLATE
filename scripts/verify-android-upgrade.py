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


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--aapt", required=True)
    parser.add_argument("--apksigner", required=True)
    parser.add_argument("--previous", required=True)
    parser.add_argument("--candidate", required=True)
    args = parser.parse_args()
    old = apk_info(args.aapt, args.apksigner, args.previous)
    new = apk_info(args.aapt, args.apksigner, args.candidate)
    checks = {
        "regular_app_identity": old["package"] == new["package"] == "com.nuvio.livetv",
        "previous_is_beta_2": old["version_name"] == "1.0.0-beta.2" and old["version_code"] == 107105,
        "candidate_is_beta_3": new["version_name"] == "1.0.0-beta.3" and new["version_code"] == 107106,
        "higher_version_code": new["version_code"] > old["version_code"],
        "regular_app_label": new["label"] == "epiSLATE",
        "same_signing_certificates": new["signers"] == old["signers"],
    }
    print(json.dumps({"previous": old, "candidate": new, "checks": checks}, indent=2))
    failed = [name for name, passed in checks.items() if not passed]
    if failed:
        raise SystemExit("APK upgrade checks failed: " + ", ".join(failed))
    print("Beta 3 APK identity, version progression, and signing match Beta 2.")


if __name__ == "__main__":
    main()
