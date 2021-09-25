package com.plusls.llsmanager.util;


import net.kyori.adventure.text.Component;

public class LoadPlayerFailException extends LlsManagerException {
    public LoadPlayerFailException(Component message) {
        super(message);
    }
}
