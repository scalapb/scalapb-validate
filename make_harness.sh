#!/usr/bin/env bash 
set -e
curl -L https://github.com/thesamet/protoc-gen-validate/releases/download/v0.6.1-mod/executor-0.6.1-linux-x86_64.exe -o executor.exe
chmod 0755 ./executor.exe
