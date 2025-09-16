#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ALPINE_ANDROID_DIR="$SCRIPT_DIR/../thirdparty/alpine-android"
OUTPUT_DIR="$SCRIPT_DIR/../app/src/main/assets/linux-rootfs"

docker build "$ALPINE_ANDROID_DIR/docker" --platform=linux/arm64/v8 -f "$ALPINE_ANROID_DIR/docker/base.Dockerfile" -t alvrme/alpine-android-base:jdk17 \
	--build-arg="JDK_VERSION=17" --build-arg="CMDLINE_VERSION=latest" --build-arg="SDK_TOOLS_VERSION=13114758"

docker build "$ALPINE_ANDROID_DIR/docker" --platform=linux/arm64/v8 -f "$ALPINE_ANDROID_DIR/docker/android.Dockerfile" -t alvrme/alpine-android:android-35-jdk17 \
	--build-arg="JDK_VERSION=17" --build-arg="BUILD_TOOLS=35.0.0" --build-arg="TARGET_SDK=35"

docker build "$SCRIPT_DIR" --platform=linux/arm64/v8 -f "$SCRIPT_DIR/Dockerfile.godot" -t godotengine/alpine-android:android-35-jdk17 \
	--build-arg="JDK_VERSION=17" --build-arg="BUILD_TOOLS=35.0.0" --build-arg="TARGET_SDK=35"

CONTAINER_ID=$(docker create --platform=linux/arm64/v8 godotengine/alpine-android:android-35-jdk17)
docker export --output="$OUTPUT_DIR/alpine-android-35-jdk17.tar" $CONTAINER_ID
docker rm $CONTAINER_ID

rm -f "$OUTPUT_DIR/alpine-android-35-jdk17.tar.xz"
(cd "$OUTPUT_DIR" && xz -T0 alpine-android-35-jdk17.tar)
