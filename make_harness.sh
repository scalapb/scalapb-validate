#!/usr/bin/env bash 
set -e
./get_bazelisk.sh
git clone https://github.com/thesamet/protoc-gen-validate.git .pgv || git -C .pgv pull
cd .pgv
git checkout v0.3.0

# Currently bazel 4.0.0 fails to build protobuf-java
export USE_BAZEL_VERSION=3.7.2

../bazelisk build tests/harness/executor --build_event_json_file=out.json
binfile=$(jq -r '.completed.importantOutput[0].name|select(.!=null)' out.json)
cp bazel-bin/$binfile ./executor.exe
chmod 0755 ./executor.exe
