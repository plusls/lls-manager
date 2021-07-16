package com.plusls.llsmanager.handler;

import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.Objects;

public class PlayerChooseInitialServerEventHandler implements EventHandler<PlayerChooseInitialServerEvent> {
    private static LlsManager llsManager;

    public static void init(LlsManager llsManager) {
        llsManager.server.getEventManager().register(llsManager, PlayerChooseInitialServerEvent.class, new PlayerChooseInitialServerEventHandler());
        PlayerChooseInitialServerEventHandler.llsManager = llsManager;
    }

    @Override
    public void execute(PlayerChooseInitialServerEvent event) {
        LlsPlayer player = Objects.requireNonNull(llsManager.players.get(event.getPlayer().getUsername()));
        for (RegisteredServer server : llsManager.server.getAllServers()) {
            if (server.getServerInfo().getName().equals(player.getLastServerName())) {
                event.setInitialServer(server);
                return;
            }
        }
    }
}
