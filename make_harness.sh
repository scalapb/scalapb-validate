#!/usr/bin/env bash 
set -e
./get_bazelisk.sh
git clone https://github.com/envoyproxy/protoc-gen-validate.git .pgv || git -C .pgv pull
cd .pgv
git checkout v0.4.1
../bazelisk build tests/harness/executor --build_event_json_file=out.json
binfile=$(jq -r '.completed.importantOutput[0].name|select(.!=null)' out.json)
cp bazel-bin/$binfile ./executor.exe
chmod 0755 ./executor.exe
