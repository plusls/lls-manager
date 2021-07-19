package com.plusls.llsmanager.handler;

import com.plusls.llsmanager.LlsManager;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.connection.LoginEvent;

public class LoginEventHandler implements EventHandler<LoginEvent> {
    private static LlsManager llsManager;

    public static void init(LlsManager llsManager) {
        llsManager.server.getEventManager().register(llsManager, LoginEvent.class, new LoginEventHandler());
        LoginEventHandler.llsManager = llsManager;
    }

    @Override
    public void execute(LoginEvent event) {

    }
}