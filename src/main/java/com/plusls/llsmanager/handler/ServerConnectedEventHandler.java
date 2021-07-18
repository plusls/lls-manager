package com.plusls.llsmanager.handler;

import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.plusls.llsmanager.util.BridgeUtil;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.Date;
import java.util.Objects;

public class ServerConnectedEventHandler implements EventHandler<ServerConnectedEvent> {
    private static LlsManager llsManager;

    public static void init(LlsManager llsManager) {
        llsManager.server.getEventManager().register(llsManager, ServerConnectedEvent.class, new ServerConnectedEventHandler());
        ServerConnectedEventHandler.llsManager = llsManager;
    }

    @Override
    public void execute(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        String username = player.getUsername();
        RegisteredServer server = event.getServer();
        String serverName = server.getServerInfo().getName();
        LlsPlayer llsPlayer = Objects.requireNonNull(llsManager.players.get(username));

        // 只处理登陆成功了的玩家（排除掉盗版未登陆的）
        if (llsPlayer.status == LlsPlayer.Status.LOGGED_IN) {
            event.getPreviousServer().ifPresent(registeredServer -> {
                String previousServerName = registeredServer.getServerInfo().getName();
                BridgeUtil.sendLeaveMessage(previousServerName, player);
                // TODO 通知 API
            });
            BridgeUtil.sendJoinMessage(serverName, player);
            // TODO 通知 API

            if (!llsPlayer.setLastServerName(serverName)) {
                LlsManager.logger().error("{} setLastServerName to {} fail.", username, serverName);
            }
        }
    }
}
