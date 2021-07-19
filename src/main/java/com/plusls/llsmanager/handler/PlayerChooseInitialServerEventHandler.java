package com.plusls.llsmanager.handler;

import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;

public class PlayerChooseInitialServerEventHandler implements EventHandler<PlayerChooseInitialServerEvent> {
    private static LlsManager llsManager;

    public static void init(LlsManager llsManager) {
        llsManager.server.getEventManager().register(llsManager, PlayerChooseInitialServerEvent.class, new PlayerChooseInitialServerEventHandler());
        PlayerChooseInitialServerEventHandler.llsManager = llsManager;
    }

    @Override
    public void execute(PlayerChooseInitialServerEvent event) {
        LlsPlayer llsPlayer = llsManager.onlinePlayers.get(event.getPlayer().getUsername());
        String serverName;
        if (llsPlayer == null) {
            serverName = llsManager.config.getAuthServerName();
        } else {
            serverName = llsPlayer.getLastServerName();
        }
        llsManager.server.getServer(serverName).ifPresent(event::setInitialServer);
    }
}
