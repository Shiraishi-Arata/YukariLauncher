package com.arata.yukarilauncher.feature.download.platform;

public class PlatformNotSupportedException extends RuntimeException {
/**
 * PlatformNotSupportedExceptionする
 */
    public PlatformNotSupportedException(String message) {
        super(message);
    }
}
