package com.plusls.llsmanager.handler;

import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.connection.DisconnectEvent;

public class DisconnectEventHandler implements EventHandler<DisconnectEvent> {
    private static LlsManager llsManager;

    public static void init(LlsManager llsManager) {
        llsManager.server.getEventManager().register(llsManager, DisconnectEvent.class, new DisconnectEventHandler());
        DisconnectEventHandler.llsManager = llsManager;
    }

    @Override
    public void execute(DisconnectEvent event) {
        String username = event.getPlayer().getUsername();
        LlsPlayer llsPlayer = llsManager.players.get(event.getPlayer().getUsername());
        if (llsPlayer != null) {
            llsPlayer.count--;
            if (llsPlayer.count == 0) {
                llsManager.players.remove(username);
            }
        }
    }
}
