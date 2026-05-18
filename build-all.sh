#!/bin/bash

GRADLEW="gradlew"
OUT="YukariLauncher/build/outputs/apk/release"
DEST="/sdcard/Download"

ARCHS=("arm64" "arm" "x86" "x86_64")

for ARCH in "${ARCHS[@]}"; do
    echo "Building $ARCH..."
    $GRADLEW clean && \
    $GRADLEW YukariLauncher:assembleRelease -Darch=$ARCH && \
    mv $OUT/YukariLauncher-*.apk "$DEST/"
done

echo "Building ALL..."
$GRADLEW clean && \
$GRADLEW YukariLauncher:assembleRelease && \
mv $OUT/YukariLauncher-*.apk "$DEST/"

echo "Done!"
