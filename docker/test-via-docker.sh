#!/usr/bin/bash

#PLATFORM=${PLATFORM:-linux/amd64}
PLATFORM=${PLATFORM:-linux/arm64/v8}

docker run --rm --platform=$PLATFORM -it "$@" godotengine/alpine-android:android-35-jdk17 /bin/bash

