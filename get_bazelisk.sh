#!/usr/bin/env bash
set -e
if [[ -f ./bazelisk ]]; then
    echo bazelisk exists
else
    if [[ $OSTYPE =~ "darwin" ]]; then
        export URL=https://github.com/bazelbuild/bazelisk/releases/download/v1.7.5/bazelisk-darwin-amd64
    else
        export URL=https://github.com/bazelbuild/bazelisk/releases/download/v1.7.5/bazelisk-linux-amd64
    fi

    curl -L $URL -o bazelisk
    chmod +x ./bazelisk
fi
