package com.plusls.llsmanager.util;

import net.kyori.adventure.text.Component;

public class PlayerNotFoundException extends LlsManagerException {
    public PlayerNotFoundException(Component message) {
        super(message);
    }
}
