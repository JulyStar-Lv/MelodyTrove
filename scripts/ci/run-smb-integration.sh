#!/usr/bin/env bash
set -euo pipefail

sudo apt-get update
sudo apt-get install --yes --no-install-recommends samba netcat-openbsd
sudo systemctl stop smbd nmbd 2>/dev/null || true

temp_root="$(mktemp -d /tmp/musicapp-smb.XXXXXX)"
fixture="$temp_root/fixture"
smb_config="$temp_root/smb.conf"
samba_log="$temp_root/samba.log"
smb_pid=""

cleanup() {
  if [[ -n "$smb_pid" ]]; then
    sudo kill "$smb_pid" 2>/dev/null || true
  fi
  sudo rm -rf "$temp_root"
}
trap cleanup EXIT

# mktemp creates directories as 0700. Samba serves files as the authenticated
# user (or guest), so every parent directory of the share must be traversable.
chmod 0755 "$temp_root"
mkdir -p "$fixture/音乐" "$fixture/restricted"
printf '0123456789' > "$fixture/range.bin"
printf '0123456789' > "$fixture/mutable.bin"
printf 'unicode fixture' > "$fixture/音乐/大海.flac"
dd if=/dev/zero of="$fixture/large.flac" bs=1M count=4 status=none
chmod -R a+rX "$fixture"
chmod a+rw "$fixture/mutable.bin"
chmod 000 "$fixture/restricted"

test_password="$(openssl rand -hex 24)"
if ! id musicapp-smb >/dev/null 2>&1; then
  sudo useradd --no-create-home --shell /usr/sbin/nologin musicapp-smb
fi
printf '%s\n%s\n' "$test_password" "$test_password" | sudo smbpasswd -a -s musicapp-smb

# Write the configuration without sudo. Ubuntu's protected_regular policy can
# reject root writes to a pre-created, user-owned file directly under /tmp.
cat > "$smb_config" <<EOF_CONFIG
[global]
server min protocol = SMB2
server max protocol = SMB3
map to guest = Bad User
interfaces = 127.0.0.1
bind interfaces only = yes
smb ports = 445
log file = $samba_log

[authenticated]
path = $fixture
read only = yes
guest ok = no
valid users = musicapp-smb

[guest]
path = $fixture
read only = yes
guest ok = yes
EOF_CONFIG
chmod 0644 "$smb_config"
testparm --suppress-prompt "$smb_config" >/dev/null

sudo /usr/sbin/smbd --foreground --no-process-group --configfile="$smb_config" &
smb_pid=$!
for _ in {1..30}; do
  if nc -z 127.0.0.1 445; then
    break
  fi
  sleep 1
done
if ! nc -z 127.0.0.1 445; then
  echo "Samba failed to listen on 127.0.0.1:445" >&2
  sudo find "$temp_root" -maxdepth 1 -type f -name 'samba.log*' -print -exec tail -n 200 {} \; || true
  exit 1
fi

export MUSICAPP_SMB_TEST_AUTH_URL="smb://127.0.0.1/authenticated"
export MUSICAPP_SMB_TEST_GUEST_URL="smb://127.0.0.1/guest"
export MUSICAPP_SMB_TEST_USERNAME="musicapp-smb"
export MUSICAPP_SMB_TEST_PASSWORD="$test_password"
export MUSICAPP_SMB_TEST_FIXTURE_DIR="$fixture"

cargo test --manifest-path rust-libs/Cargo.toml \
  --package storage-backend \
  --test smb_integration -- --ignored --nocapture
