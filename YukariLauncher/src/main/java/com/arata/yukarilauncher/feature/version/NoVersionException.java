package com.arata.yukarilauncher.feature.version;

public class NoVersionException extends RuntimeException {
    public NoVersionException(String message) {
        super(message);
    }
}
