package com.plusls.llsmanager.handler;

import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.plusls.llsmanager.util.BridgeUtil;
import com.plusls.llsmanager.util.TabListUtil;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;

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
        LlsPlayer llsPlayer = llsManager.players.remove(player.getRemoteAddress());
        String serverName = llsPlayer.getLastServerName();
        BridgeUtil.sendLeaveMessage(serverName, player);
        // TODO 离开通知 API
        if (llsPlayer.status == LlsPlayer.Status.LOGGED_IN && !llsPlayer.setLastSeenTime(new Date())) {
            llsManager.logger.error("Can't save the last seen time of player {}", username);
        }
        llsManager.server.getAllPlayers().forEach(eachPlayer -> eachPlayer.getTabList().removeEntry(player.getUniqueId()));
    }
}
