#!/usr/bin/env bash
#
# Publish the prebuilt Messenger APK as a GitHub release.
#
#   ./release-apk.sh <version> [release notes]
#   ./release-apk.sh 2.12.4
#   ./release-apk.sh 2.12.4 "Fixes call reconnect"
#
# 0. checks the built APK's versionName == <version>; otherwise stops (recompile first)
# 1. copies the release build to release/mesibo-messenger.apk (git-ignored)
# 2. creates GitHub release v<version> on mesibo/messenger-app-android with the
#    APK attached (replacing an asset of the same name if the release exists)
#
# Stable download link (always the newest release, used by console.mesibo.com):
#   https://github.com/mesibo/messenger-app-android/releases/latest/download/mesibo-messenger.apk
#
# Uses the GitHub CLI (gh) if installed and logged in; otherwise needs
# GITHUB_TOKEN in the environment (token with "contents: write" on the repo).

set -euo pipefail

REPO="mesibo/messenger-app-android-kotlin"
SRC_DIR="/code/out/AndroidBuild/MesiboApplication/app/outputs/apk/release"
SRC_APK="$SRC_DIR/app-release.apk"
ASSET_NAME="mesibo-messenger.apk"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT_DIR="$SCRIPT_DIR/release"
OUT_APK="$OUT_DIR/$ASSET_NAME"

die() { echo "error: $*" >&2; exit 1; }

# ---- arguments --------------------------------------------------------------
VERSION="${1:-}"
[ -n "$VERSION" ] || die "usage: $0 <version> [release notes]   e.g. $0 2.12.4"
VERSION="${VERSION#v}"                       # accept 2.12.4 or v2.12.4
[[ "$VERSION" =~ ^[0-9]+(\.[0-9]+)*$ ]] || die "version must look like 2.12.4 (got '$1')"
TAG="v$VERSION"
NOTES="${2:-Prebuilt mesibo Messenger APK $TAG}"

# ---- check the build ----------------------------------------------------------
[ -f "$SRC_APK" ] || die "APK not found: $SRC_APK (build the release variant first)"

# Read versionName from the APK itself (aapt2/aapt from the newest SDK build-tools);
# fall back to Gradle's output-metadata.json only if no aapt is available.
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/data/opt/android-sdk-linux}}"
AAPT="$(ls -1 "$SDK"/build-tools/*/aapt2 "$SDK"/build-tools/*/aapt 2>/dev/null | sort -V | tail -1 || true)"
if [ -n "$AAPT" ]; then
    BUILT_VERSION="$("$AAPT" dump badging "$SRC_APK" 2>/dev/null | sed -n "s/.*versionName='\([^']*\)'.*/\1/p" | head -1)"
    FROM="APK ($(basename "$AAPT"))"
else
    BUILT_VERSION="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["elements"][0]["versionName"])' \
                     "$SRC_DIR/output-metadata.json" 2>/dev/null || true)"
    FROM="output-metadata.json"
fi
[ -n "$BUILT_VERSION" ] || die "could not read versionName from the built APK; rebuild the release APK"

if [ "$BUILT_VERSION" != "$VERSION" ]; then
    die "built APK is version $BUILT_VERSION (from $FROM), not $VERSION.
       Set versionName \"$VERSION\" in app/build.gradle, rebuild the release APK
       (Build > Generate Signed Bundle / APK > APK > release), then run this again."
fi
echo "checked APK versionName $BUILT_VERSION (from $FROM)"

# ---- copy ---------------------------------------------------------------------
mkdir -p "$OUT_DIR"
cp -f "$SRC_APK" "$OUT_APK"
echo "copied  $SRC_APK"
echo "     -> $OUT_APK ($(du -h "$OUT_APK" | cut -f1))"

# keep the 50+ MB APK out of git
if ! git -C "$SCRIPT_DIR" check-ignore -q "$OUT_APK" 2>/dev/null; then
    echo "release/" >> "$SCRIPT_DIR/.git/info/exclude"
    echo "added release/ to .git/info/exclude"
fi

# ---- release: gh ----------------------------------------------------------------
if command -v gh >/dev/null 2>&1 && gh auth status >/dev/null 2>&1; then
    if gh release view "$TAG" --repo "$REPO" >/dev/null 2>&1; then
        echo "release $TAG exists, replacing $ASSET_NAME"
        gh release upload "$TAG" "$OUT_APK" --repo "$REPO" --clobber
    else
        gh release create "$TAG" "$OUT_APK" --repo "$REPO" \
            --title "Messenger $TAG" --notes "$NOTES"
    fi
    echo "done: https://github.com/$REPO/releases/tag/$TAG"
    echo "link: https://github.com/$REPO/releases/latest/download/$ASSET_NAME"
    exit 0
fi

# ---- release: GitHub REST API (curl) -------------------------------------------
[ -n "${GITHUB_TOKEN:-}" ] || die "install and log in to gh, or set GITHUB_TOKEN"

API="https://api.github.com/repos/$REPO"
AUTH=(-H "Authorization: Bearer $GITHUB_TOKEN" -H "Accept: application/vnd.github+json" -H "X-GitHub-Api-Version: 2022-11-28")
json() { python3 -c 'import json,sys; d=json.load(sys.stdin); print(eval(sys.argv[1]))' "$1"; }

# existing release for this tag?
HTTP=$(curl -sS -o /tmp/release-apk.$$ -w '%{http_code}' "${AUTH[@]}" "$API/releases/tags/$TAG")
if [ "$HTTP" = "200" ]; then
    RELEASE_ID=$(json 'd["id"]' < /tmp/release-apk.$$)
    echo "release $TAG exists (id $RELEASE_ID), replacing $ASSET_NAME"
    OLD_ASSET=$(json '" ".join(str(a["id"]) for a in d["assets"] if a["name"]=="'"$ASSET_NAME"'")' < /tmp/release-apk.$$)
    for id in $OLD_ASSET; do
        curl -sS -f -X DELETE "${AUTH[@]}" "$API/releases/assets/$id" >/dev/null
    done
elif [ "$HTTP" = "404" ]; then
    BODY=$(python3 -c 'import json,sys; print(json.dumps({"tag_name":sys.argv[1],"name":"Messenger "+sys.argv[1],"body":sys.argv[2]}))' "$TAG" "$NOTES")
    curl -sS -f "${AUTH[@]}" -X POST "$API/releases" -d "$BODY" > /tmp/release-apk.$$ \
        || die "could not create release $TAG"
    RELEASE_ID=$(json 'd["id"]' < /tmp/release-apk.$$)
    echo "created release $TAG (id $RELEASE_ID)"
else
    cat /tmp/release-apk.$$ >&2; rm -f /tmp/release-apk.$$
    die "GitHub API returned HTTP $HTTP"
fi
rm -f /tmp/release-apk.$$

curl -sS -f "${AUTH[@]}" -H "Content-Type: application/vnd.android.package-archive" \
    --data-binary @"$OUT_APK" \
    "https://uploads.github.com/repos/$REPO/releases/$RELEASE_ID/assets?name=$ASSET_NAME" >/dev/null \
    || die "upload failed"

echo "done: https://github.com/$REPO/releases/tag/$TAG"
echo "link: https://github.com/$REPO/releases/latest/download/$ASSET_NAME"
