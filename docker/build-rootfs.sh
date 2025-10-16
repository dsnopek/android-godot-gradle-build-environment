#!/bin/bash

PLATFORM="linux/arm64/v8"
#PLATFORM="linux/amd64"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ALPINE_ANDROID_DIR="$SCRIPT_DIR/../thirdparty/alpine-android"
OUTPUT_DIR="$SCRIPT_DIR/../app/src/main/assets/linux-rootfs"

IMAGE_NAME=godotengine/alpine-android:android-35-jdk17

# Build the alpine-android images.

docker build "$ALPINE_ANDROID_DIR/docker" --platform=$PLATFORM -f "$ALPINE_ANROID_DIR/docker/base.Dockerfile" -t alvrme/alpine-android-base:jdk17 \
	--build-arg="JDK_VERSION=17" --build-arg="CMDLINE_VERSION=latest" --build-arg="SDK_TOOLS_VERSION=13114758"

docker build "$ALPINE_ANDROID_DIR/docker" --platform=$PLATFORM -f "$ALPINE_ANDROID_DIR/docker/android.Dockerfile" -t alvrme/alpine-android:android-35-jdk17 \
	--build-arg="JDK_VERSION=17" --build-arg="BUILD_TOOLS=35.0.0" --build-arg="TARGET_SDK=35"

docker build "$SCRIPT_DIR" --platform=$PLATFORM -f "$SCRIPT_DIR/Dockerfile.godot" -t $IMAGE_NAME \
	--build-arg="JDK_VERSION=17" --build-arg="BUILD_TOOLS=35.0.0" --build-arg="TARGET_SDK=35"

CONTAINER_ID=$(docker create --platform=$PLATFORM $IMAGE_NAME)
docker export --output="$OUTPUT_DIR/alpine-android-35-jdk17.tar" $CONTAINER_ID
#docker inspect $IMAGE_NAME --format='{{json .Config.Env}}' | python -c 'import json, sys; d=json.load(sys.stdin); print(*d, sep="\n")' > "$OUTPUT_DIR/alpine-android-35-jdk17.env"
docker rm $CONTAINER_ID

rm -f "$OUTPUT_DIR/alpine-android-35-jdk17.tar.xz"
(cd "$OUTPUT_DIR" && xz -T0 alpine-android-35-jdk17.tar)

