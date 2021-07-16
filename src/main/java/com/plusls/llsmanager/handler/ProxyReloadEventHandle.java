package com.plusls.llsmanager.handler;

import com.plusls.llsmanager.LlsManager;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.proxy.ProxyReloadEvent;

public class ProxyReloadEventHandle implements EventHandler<ProxyReloadEvent> {
    private static LlsManager llsManager;

    public static void init(LlsManager llsManager) {
        llsManager.server.getEventManager().register(llsManager, ProxyReloadEvent.class, new ProxyReloadEventHandle());
        ProxyReloadEventHandle.llsManager = llsManager;
    }

    @Override
    public void execute(ProxyReloadEvent event) {
        if (!llsManager.whitelist.load()) {
            throw new RuntimeException("Lls-Manager whitelist load fail.");
        }
    }
}
