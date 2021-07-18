package com.plusls.llsmanager.handler;

import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.player.ServerConnectedEvent;

import java.util.Objects;

public class ServerConnectedEventHandler implements EventHandler<ServerConnectedEvent> {
    private static LlsManager llsManager;

    public static void init(LlsManager llsManager) {
        llsManager.server.getEventManager().register(llsManager, ServerConnectedEvent.class, new ServerConnectedEventHandler());
        ServerConnectedEventHandler.llsManager = llsManager;
    }

    @Override
    public void execute(ServerConnectedEvent event) {
        LlsPlayer llsPlayer = Objects.requireNonNull(llsManager.players.get(event.getPlayer().getUsername()));
        if (llsPlayer.status == LlsPlayer.Status.LOGGED_IN) {
            // TODO 最后连接时间
            llsPlayer.setLastServerName(event.getServer().getServerInfo().getName());
        }
    }
}
