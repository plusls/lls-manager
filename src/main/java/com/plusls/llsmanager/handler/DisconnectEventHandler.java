package com.plusls.llsmanager.handler;

import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.plusls.llsmanager.util.BridgeUtil;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.proxy.Player;

public class DisconnectEventHandler implements EventHandler<DisconnectEvent> {
    private static LlsManager llsManager;

    public static void init(LlsManager llsManager) {
        llsManager.server.getEventManager().register(llsManager, DisconnectEvent.class, new DisconnectEventHandler());
        DisconnectEventHandler.llsManager = llsManager;
    }

    @Override
    public void execute(DisconnectEvent event) {
        Player player = event.getPlayer();
        LlsPlayer llsPlayer = llsManager.players.get(player.getRemoteAddress());
        String serverName = llsPlayer.getLastServerName();
        BridgeUtil.sendLeaveMessage(serverName, player);
    }
}
