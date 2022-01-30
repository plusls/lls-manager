package com.plusls.llsmanager.tabListSync;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.util.PacketUtil;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.PlayerListItem;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class TabListSyncHandler {

    // 不用 UUID 为 key 是考虑到有的服务器的子服和 velocity uuid 会有不一致的情况
    public final Map<String, PlayerListItem.Item> currentItems = new ConcurrentHashMap<>();
    @Inject
    public LlsManager llsManager;

    public static void init(LlsManager llsManager) {
        StateRegistry.PacketRegistry toReplace = StateRegistry.PLAY.clientbound;
        PacketUtil.replacePacketHandler(toReplace, PlayerListItem.class, MyPlayerListItem.class, MyPlayerListItem::new);
        llsManager.server.getEventManager().register(llsManager, llsManager.injector.getInstance(TabListSyncHandler.class));
    }

    public void removeDisconnectedPlayer() {
        // 删除退出游戏的玩家
        currentItems.entrySet().removeIf(entry -> {
            if (llsManager.server.getPlayer(entry.getKey()).isEmpty()) {
                for (Player player : llsManager.server.getAllPlayers()) {
                    player.getTabList().removeEntry(entry.getValue().getUuid());
                }
                return true;
            } else {
                return false;
            }
        });
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent disconnectEvent) {
        if (llsManager.config.getShowAllPlayerInTabList()) {
            removeDisconnectedPlayer();
        }
    }
}