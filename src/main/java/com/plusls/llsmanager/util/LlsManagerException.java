package com.plusls.llsmanager.util;

import net.kyori.adventure.text.Component;

public abstract class LlsManagerException extends Exception {
    public Component message;

    public LlsManagerException(Component message) {
        this.message = message;
    }
}
