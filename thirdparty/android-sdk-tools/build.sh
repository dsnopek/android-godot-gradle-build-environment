#!/bin/bash

BUILD_TOOLS_VERSION="${BUILD_TOOLS_VERSION:-35.0.2}"

if [ -z "$ANDROID_NDK_ROOT" ]; then
	echo "ERROR: The ANDROID_NDK_ROOT environment variable must be defined" > /dev/stderr
	exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SOURCE_DIR="$SCRIPT_DIR/android-sdk-tools-source"
BUILD_DIR="$SCRIPT_DIR/build"
OUTPUT_DIR="$SCRIPT_DIR/../../docker/android-sdk-tools/build-tools"

function die() {
    echo "$@" > /dev/stderr
    exit 1
}

mkdir -p $BUILD_DIR
cd $BUILD_DIR

cd $SOURCE_DIR

# Get all the Android source we need.
python get_source.py --tags platform-tools-$BUILD_TOOLS_VERSION \
	|| die "Unable to get Android SDK tools source"

# Apply patches.
(cd src/protobuf && git checkout -- .)
patch -p1 < patches/protobuf_CMakeLists.txt.patch \
	|| die "Unable to apply protobuf patch"
(cd src/art && git checkout -- . && patch -p1 < $SCRIPT_DIR/patches/art.patch) \
	|| die "Unable to apply art patch"
(cd src/core && git checkout -- . && patch -p1 < $SCRIPT_DIR/patches/core.patch) \
	|| die "Unable to apply core patch"

# Build protobuf for the host.
PROTOBUF_BUILD_DIR="src/protobuf/build"
mkdir -p $PROTOBUF_BUILD_DIR
pushd $PROTOBUF_BUILD_DIR >/dev/null
cmake -GNinja -Dprotobuf_BUILD_TESTS=OFF .. \
	|| die "Unable to configure protobuf"
cmake --build . \
	|| die "Unable to build protobuf"
popd >/dev/null

# Build all the tools we need!
ANDROID_TARGETS="aapt aapt2 aidl dexdump split-select zipalign"
for target in $ANDROID_TARGETS; do
	echo "Target: $target"
	python build.py --ndk="$ANDROID_NDK_ROOT" --abi=arm64-v8a --build="$BUILD_DIR/arm64-v8a" --protoc="$SOURCE_DIR/$PROTOBUF_BUILD_DIR/protoc" --target=$target \
		|| die "Failed building $target"
	if ! [ -f "$BUILD_DIR/arm64-v8a/bin/build-tools/$target" ]; then
		die "Cannot find '$target' - it probably failed to build"
	fi
done

# Copy the build tools into place.
rm -rf "$OUTPUT_DIR"
cp -r "$BUILD_DIR/arm64-v8a/bin/build-tools" "$OUTPUT_DIR" \
	|| die "Unable to copy built build-tools into place"

