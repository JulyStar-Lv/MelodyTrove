#!/usr/bin/env bash
set -euo pipefail

sdk_root="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "$sdk_root" ]]; then
  echo "ANDROID_SDK_ROOT or ANDROID_HOME must be set" >&2
  exit 1
fi

package_list="${RUNNER_TEMP:-/tmp}/sdkmanager-packages.txt"
sdkmanager --channel=3 --list > "$package_list"

mapfile -t available_packages < <(
  awk -F '|' '{
    package = $1
    gsub(/^[ \t]+|[ \t]+$/, "", package)
    if (package != "") print package
  }' "$package_list"
)

has_package() {
  local expected="$1"
  printf '%s\n' "${available_packages[@]}" | grep -Fxq "$expected"
}

platform_package=""
if has_package "platforms;android-37"; then
  platform_package="platforms;android-37"
elif has_package "platforms;android-37.0"; then
  platform_package="platforms;android-37.0"
else
  platform_package="$(
    printf '%s\n' "${available_packages[@]}" |
      grep -E '^platforms;android-37\.[0-9]+$' |
      sort -V |
      tail -n 1 || true
  )"
fi

if [[ -z "$platform_package" ]] && has_package "platforms;android-CinnamonBun"; then
  platform_package="platforms;android-CinnamonBun"
fi

if [[ -z "$platform_package" ]]; then
  echo "Android 17 SDK package was not found. Available Android platforms:" >&2
  printf '%s\n' "${available_packages[@]}" | grep '^platforms;android-' | tail -n 30 >&2 || true
  exit 1
fi

build_tools_package="$(
  printf '%s\n' "${available_packages[@]}" |
    grep -E '^build-tools;37\.[0-9]+\.[0-9]+$' |
    sort -V |
    tail -n 1 || true
)"

packages=("platform-tools" "$platform_package")
if [[ -n "$build_tools_package" ]]; then
  packages+=("$build_tools_package")
fi

printf 'Installing Android SDK packages: %s\n' "${packages[*]}"
yes | sdkmanager --licenses >/dev/null 2>&1 || true
sdkmanager --channel=3 "${packages[@]}"

installed_platform="${platform_package#platforms;}"
canonical_platform="$sdk_root/platforms/android-37"
installed_platform_path="$sdk_root/platforms/$installed_platform"
if [[ "$installed_platform" != "android-37" && ! -d "$canonical_platform" ]]; then
  rm -f "$canonical_platform"
  ln -s "$installed_platform_path" "$canonical_platform"
fi

test -f "$canonical_platform/android.jar"
echo "Android SDK ready: $platform_package${build_tools_package:+, $build_tools_package}"
