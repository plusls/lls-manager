package com.plusls.llsmanager.handler;

import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;

public class ServerPreConnectEventHandler implements EventHandler<ServerPreConnectEvent> {
    private static LlsManager llsManager;

    public static void init(LlsManager llsManager) {
        llsManager.server.getEventManager().register(llsManager, ServerPreConnectEvent.class, new ServerPreConnectEventHandler());
        ServerPreConnectEventHandler.llsManager = llsManager;
    }

    @Override
    public void execute(ServerPreConnectEvent event) {
        // 防止未登陆的用户逃离登陆服
        ServerPreConnectEvent.ServerResult result = event.getResult();
        if (result.getServer().isEmpty()) {
            return;
        }
        RegisteredServer server;
        server = result.getServer().get();

        LlsPlayer llsPlayer = llsManager.players.get(event.getPlayer().getRemoteAddress());

        if (llsPlayer.status != LlsPlayer.Status.LOGGED_IN && !server.getServerInfo().getName().equals(llsManager.config.getAuthServerName())) {
            event.getPlayer().disconnect(Component.text("Can't connect to auth server. Please contact server admin."));
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
        }
    }
}