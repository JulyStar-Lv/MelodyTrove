#!/usr/bin/env bash
set -euo pipefail

sudo apt-get update
sudo apt-get install --yes --no-install-recommends samba netcat-openbsd
sudo systemctl stop smbd nmbd 2>/dev/null || true

fixture="${RUNNER_TEMP:-/tmp}/musicapp-smb-fixture"
sudo mkdir -p "$fixture/音乐" "$fixture/restricted"
printf '0123456789' | sudo tee "$fixture/range.bin" >/dev/null
printf '0123456789' | sudo tee "$fixture/mutable.bin" >/dev/null
printf 'unicode fixture' | sudo tee "$fixture/音乐/大海.flac" >/dev/null
sudo dd if=/dev/zero of="$fixture/large.flac" bs=1M count=4 status=none
sudo chmod -R a+rX "$fixture"
sudo chmod a+rw "$fixture/mutable.bin"
sudo chmod 000 "$fixture/restricted"

test_password="$(openssl rand -hex 24)"
sudo useradd --no-create-home --shell /usr/sbin/nologin musicapp-smb
printf '%s\n%s\n' "$test_password" "$test_password" | sudo smbpasswd -a -s musicapp-smb

smb_config="${RUNNER_TEMP:-/tmp}/smb.conf"
sudo tee "$smb_config" >/dev/null <<EOF_CONFIG
[global]
server min protocol = SMB2
server max protocol = SMB3
map to guest = Bad User
interfaces = 127.0.0.1
bind interfaces only = yes
smb ports = 445
log file = ${RUNNER_TEMP:-/tmp}/samba.log

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

sudo /usr/sbin/smbd --foreground --no-process-group --configfile="$smb_config" &
for _ in {1..30}; do
  if nc -z 127.0.0.1 445; then
    break
  fi
  sleep 1
done
nc -z 127.0.0.1 445

export MUSICAPP_SMB_TEST_AUTH_URL="smb://127.0.0.1/authenticated"
export MUSICAPP_SMB_TEST_GUEST_URL="smb://127.0.0.1/guest"
export MUSICAPP_SMB_TEST_USERNAME="musicapp-smb"
export MUSICAPP_SMB_TEST_PASSWORD="$test_password"
export MUSICAPP_SMB_TEST_FIXTURE_DIR="$fixture"

cargo test --manifest-path rust-libs/Cargo.toml \
  --package storage-backend \
  --test smb_integration -- --ignored --nocapture
