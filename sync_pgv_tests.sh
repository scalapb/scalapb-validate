#!/usr/bin/env bash
set -e
TAG=0.6.1-java

curl -L https://github.com/envoyproxy/protoc-gen-validate/archive/v$TAG.tar.gz | \
  tar xvz --strip-components=1 -C e2e/src/main/protobuf protoc-gen-validate-$TAG/tests

