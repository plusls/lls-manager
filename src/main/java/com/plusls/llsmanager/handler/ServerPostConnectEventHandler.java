package com.plusls.llsmanager.handler;

import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.plusls.llsmanager.util.TabListUtil;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;

public class ServerPostConnectEventHandler implements EventHandler<ServerPostConnectEvent> {
    private static LlsManager llsManager;

    public static void init(LlsManager llsManager) {
        llsManager.server.getEventManager().register(llsManager, ServerPostConnectEvent.class, new ServerPostConnectEventHandler());
        ServerPostConnectEventHandler.llsManager = llsManager;
    }

    @Override
    public void execute(ServerPostConnectEvent event) {
        Player player = event.getPlayer();
        LlsPlayer llsPlayer = llsManager.players.get(player.getRemoteAddress());
        String username = player.getUsername();
        if (player.getCurrentServer().isEmpty()) {
            llsManager.logger.error("The current server of {} in ServerPostConnectEvent is null.", username);
            return;
        }
        RegisteredServer server = player.getCurrentServer().get().getServer();
        String serverName = server.getServerInfo().getName();

        if (llsPlayer.status == LlsPlayer.Status.NEED_LOGIN) {
            player.sendMessage(Component.translatable("lls-manager.command.lls_login.hint"));
        } else if (llsPlayer.status == LlsPlayer.Status.NEED_REGISTER) {
            player.sendMessage(Component.translatable("lls-manager.command.lls_register.hint"));
        }

        // 由于在加入服务器后会重置当前玩家的 TabList，重置发生在 ServerConnectedEvent 之后
        // 因此必须在 ServerPostConnectEvent 中进行处理
        TabList playerTabList = player.getTabList();


        // TabList 本质上是一个 Map
        llsManager.server.getAllPlayers().forEach(eachPlayer -> eachPlayer.getCurrentServer().ifPresent(serverConnection -> {
            if (!llsManager.config.getShowAllPlayerInTabList()) {
                return;
            }
            String eachPlayerServerName = serverConnection.getServerInfo().getName();

            // 更新自己的 TabList
            // 不知道为啥 playerTabList 中不会包含自己
            if (llsPlayer.status == LlsPlayer.Status.LOGGED_IN && !playerTabList.containsEntry(eachPlayer.getUniqueId()) && !player.getUniqueId().equals(eachPlayer.getUniqueId())) {
                TabListEntry playerTabListEntry = TabListUtil.getTabListEntry(eachPlayer, playerTabList);
                TabListUtil.updateTabListEntry(playerTabListEntry, eachPlayer.getUsername(), eachPlayerServerName);
                playerTabList.addEntry(playerTabListEntry);
                // llsManager.logger.warn("if update eachPlayerServerName: {} serverName: {} eachPlayer: {} player: {}", eachPlayerServerName, serverName, eachPlayer.getUsername(), username);
            }

            // 更新其它玩家的 TabList
            if (!eachPlayerServerName.equals(llsManager.config.getAuthServerName()) && !eachPlayerServerName.equals(serverName)) {
                TabList eachPlayerTabList = eachPlayer.getTabList();
                // 不存在则先添加，在玩家离开时处理移除逻辑
                if (!eachPlayerTabList.containsEntry(player.getUniqueId())) {
                    eachPlayerTabList.addEntry(TabListUtil.getTabListEntry(player, eachPlayerTabList));
                }
                for (TabListEntry tabListEntry : eachPlayerTabList.getEntries()) {
                    if (tabListEntry.getProfile().getId().equals(player.getUniqueId())) {
                        // llsManager.logger.warn("for update eachPlayerServerName: {} serverName: {} eachPlayer: {} player: {}", eachPlayerServerName, serverName, eachPlayer.getUsername(), username);
                        TabListUtil.updateTabListEntry(tabListEntry, username, serverName);
                        break;
                    }
                }
            }
        }));

    }
}