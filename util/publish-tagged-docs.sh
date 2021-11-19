#!/bin/bash

set -eux

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

# Publish javadocs to gh-pages
bazel build //:user-docs.jar
git clone --quiet --branch gh-pages \
    https://github.com/google/dagger gh-pages > /dev/null
cd gh-pages
unzip ../bazel-bin/user-docs.jar -d api/$VERSION_NAME
rm -rf api/$VERSION_NAME/META-INF/
git add api/$VERSION_NAME
git commit -m "$VERSION_NAME docs"
git push origin gh-pages
cd ..
rm -rf gh-pages
