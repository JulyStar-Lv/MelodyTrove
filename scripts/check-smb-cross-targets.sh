#!/usr/bin/env bash
set -euo pipefail

mode="${1:-all}"
manifest="rust-libs/Cargo.toml"
package="storage-backend"

check_android() {
  local ndk_root="${ANDROID_NDK_HOME:-${ANDROID_NDK:-}}"
  if [[ -z "${ndk_root}" ]]; then
    echo "ANDROID_NDK_HOME or ANDROID_NDK is required for Android checks" >&2
    return 1
  fi

  local host_tag
  case "$(uname -s)" in
    Darwin) host_tag="darwin-x86_64" ;;
    Linux) host_tag="linux-x86_64" ;;
    *)
      echo "Unsupported Android NDK host: $(uname -s)" >&2
      return 1
      ;;
  esac
  local toolchain="${ndk_root}/toolchains/llvm/prebuilt/${host_tag}/bin"

  rustup target add aarch64-linux-android x86_64-linux-android
  export CC_aarch64_linux_android="${toolchain}/aarch64-linux-android24-clang"
  export AR_aarch64_linux_android="${toolchain}/llvm-ar"
  export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="${toolchain}/aarch64-linux-android24-clang"
  cargo check --manifest-path "${manifest}" --package "${package}" --target aarch64-linux-android

  export CC_x86_64_linux_android="${toolchain}/x86_64-linux-android24-clang"
  export AR_x86_64_linux_android="${toolchain}/llvm-ar"
  export CARGO_TARGET_X86_64_LINUX_ANDROID_LINKER="${toolchain}/x86_64-linux-android24-clang"
  cargo check --manifest-path "${manifest}" --package "${package}" --target x86_64-linux-android
}

check_apple() {
  if [[ "$(uname -s)" != "Darwin" ]]; then
    echo "Apple target checks require macOS" >&2
    return 1
  fi
  rustup target add aarch64-apple-ios aarch64-apple-ios-sim
  cargo check --manifest-path "${manifest}" --package "${package}" --target aarch64-apple-ios
  cargo check --manifest-path "${manifest}" --package "${package}" --target aarch64-apple-ios-sim
}

case "${mode}" in
  android) check_android ;;
  apple) check_apple ;;
  all)
    check_android
    check_apple
    ;;
  *)
    echo "Usage: $0 [android|apple|all]" >&2
    exit 2
    ;;
esac
