package com.plusls.llsmanager.tabListSync;

import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.util.TextUtil;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.protocol.packet.PlayerListItem;
import net.kyori.adventure.text.Component;

import java.util.Map;
import java.util.Objects;

public class TabListUtil {
    public static TabListEntry getTabListEntry(TabList tabList, PlayerListItem.Item item) {
        return TabListEntry.builder()
                .tabList(tabList)
                .profile(new GameProfile(Objects.requireNonNull(item.getUuid()), item.getName(), item.getProperties()))
                .displayName(item.getDisplayName())
                .latency(item.getLatency())
                .gameMode(item.getGameMode())
                .build();
    }

    public static void updateTabListEntry(TabListEntry tabListEntry, PlayerListItem.Item item, Player player, Player itemPlayer) {
        Component component = null;
        if (tabListEntry.getDisplayNameComponent().isPresent()) {
            component = tabListEntry.getDisplayNameComponent().get();
        }

        if (inSameServer(player, itemPlayer)) {
            if (component != item.getDisplayName()) {
                tabListEntry.setDisplayName(item.getDisplayName());
            }
        } else {
            itemPlayer.getCurrentServer().ifPresent(
                    serverConnection -> tabListEntry.setDisplayName(TextUtil.getUsernameComponent(item.getName())
                            .append(Component.text(" ["))
                            .append(TextUtil.getServerNameComponent(serverConnection.getServerInfo().getName()))
                            .append(Component.text("]")))
            );
        }

        if (tabListEntry.getLatency() != item.getLatency()) {
            tabListEntry.setLatency(item.getLatency());
        }

        if (tabListEntry.getGameMode() != item.getGameMode()) {
            tabListEntry.setGameMode(item.getGameMode());
        }

    }

    private static boolean inSameServer(Player player1, Player player2) {
        if (player1.getCurrentServer().isEmpty()) {
            return false;
        }
        String server1 = player1.getCurrentServer().get().getServerInfo().getName();
        if (player2.getCurrentServer().isEmpty()) {
            return false;
        }
        String server2 = player2.getCurrentServer().get().getServerInfo().getName();
        return server1.equals(server2);
    }

    // 已知，若是服务器存在同名玩家不使用 velocity 登陆会出现 bug，比如存在多个代理
    // 或者 carpet 假人使用了 shadow
    // TODO 鉴权，未登录玩家不能看到全服列表
    public static void updateTabList(LlsManager llsManager, PlayerListItem playerListItem) {
        Map<String, PlayerListItem.Item> currentItems = llsManager.injector.getInstance(TabListSyncHandler.class).currentItems;

        synchronized (llsManager.injector.getInstance(TabListSyncHandler.class).currentItems) {
            for (PlayerListItem.Item item : playerListItem.getItems()) {
                String name = null;
                if (!item.getName().equals("")) {
                    name = item.getName();
                } else {
                    for (Map.Entry<String, PlayerListItem.Item> entry : currentItems.entrySet()) {
                        if (Objects.equals(item.getUuid(), entry.getValue().getUuid())) {
                            name = entry.getKey();
                            break;
                        }
                    }
                }
                // 只处理在服务器中的玩家
                if (name == null || llsManager.server.getPlayer(name).isEmpty()) {
                    continue;
                }
                switch (playerListItem.getAction()) {
                    case PlayerListItem.ADD_PLAYER:
                        currentItems.put(name, item);
                        break;
                    case PlayerListItem.UPDATE_GAMEMODE:
                        Objects.requireNonNull(currentItems.get(name)).setGameMode(item.getGameMode());
                        break;
                    case PlayerListItem.UPDATE_LATENCY:
                        Objects.requireNonNull(currentItems.get(name)).setLatency(item.getLatency());
                        break;
                    case PlayerListItem.UPDATE_DISPLAY_NAME:
                        Objects.requireNonNull(currentItems.get(name)).setDisplayName(item.getDisplayName());
                        break;
                    case PlayerListItem.REMOVE_PLAYER:
                        break;
                    default:
                        throw new UnsupportedOperationException("Unknown action " + playerListItem.getAction());
                }
            }

            for (Player player : llsManager.server.getAllPlayers()) {
                TabList tabList = player.getTabList();
                // 添加缺失的  tabListEntry
                for (Map.Entry<String, PlayerListItem.Item> entry : currentItems.entrySet()) {
                    // tabList 不处理自己
                    if (!entry.getKey().equals(player.getGameProfile().getName())) {

                        boolean shouldAdd = true;
                        for (TabListEntry tabListEntry : tabList.getEntries()) {
                            if (tabListEntry.getProfile().getName().equals(entry.getKey())) {
                                shouldAdd = false;
                                break;
                            }
                        }
                        if (shouldAdd) {
                            tabList.addEntry(TabListUtil.getTabListEntry(tabList, entry.getValue()));
                        }
                    }
                }
                // 更新 tabListEntry
                for (TabListEntry tabListEntry : tabList.getEntries()) {
                    llsManager.server.getPlayer(tabListEntry.getProfile().getName()).ifPresent(itemPlayer -> {
                        if (player != itemPlayer) {
                            updateTabListEntry(tabListEntry, currentItems.get(tabListEntry.getProfile().getName()), player, itemPlayer);
                        }
                    });
                }
            }
        }
        if (playerListItem.getAction() == PlayerListItem.REMOVE_PLAYER) {
            // 玩家只是切换了服务器，并没有离开游戏
            playerListItem.getItems().removeIf(item -> llsManager.server.getPlayer(item.getName()).isPresent());
        }
    }
}
