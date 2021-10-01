#!/bin/bash

set -eu

if [ $# -lt 1 ]; then
  echo "usage $0 <version-name>"
  exit 1;
fi
readonly VERSION_NAME=$1
shift 1

if [[ ! "$VERSION_NAME" =~ ^2\. ]]; then
  echo 'Version name must begin with "2."'
  exit 2
fi

if [[ "$VERSION_NAME" =~ " " ]]; then
  echo "Version name must not have any spaces"
  exit 3
fi

pushd $(mktemp -d)
wget https://github.com/google/dagger/archive/dagger-$VERSION_NAME.zip -P .
OUTPUT=$(shasum -a 256 dagger-$VERSION_NAME.zip)
echo "SHA sum: $OUTPUT"
popd
