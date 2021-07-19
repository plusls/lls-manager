package com.plusls.llsmanager.handler;

import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.plusls.llsmanager.util.BridgeUtil;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.proxy.Player;

import java.util.Date;

public class DisconnectEventHandler implements EventHandler<DisconnectEvent> {
    private static LlsManager llsManager;

    public static void init(LlsManager llsManager) {
        llsManager.server.getEventManager().register(llsManager, DisconnectEvent.class, new DisconnectEventHandler());
        DisconnectEventHandler.llsManager = llsManager;
    }

    @Override
    public void execute(DisconnectEvent event) {
        Player player = event.getPlayer();
        String username = player.getUsername();
        LlsPlayer llsPlayer = llsManager.players.get(username);

        if (llsPlayer != null) {
            String serverName = llsPlayer.getLastServerName();
            BridgeUtil.sendLeaveMessage(serverName, player);
            // TODO 离开通知 API
            if (llsPlayer.status == LlsPlayer.Status.LOGGED_IN || llsPlayer.status == LlsPlayer.Status.ONLINE_USER) {
                if (!llsPlayer.setLastSeenTime(new Date())) {
                    llsManager.logger.error("Can't save the last seen time of player {}", username);
                }
            }
        }
    }
}
