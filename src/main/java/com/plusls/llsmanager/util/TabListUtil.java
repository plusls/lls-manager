package com.plusls.llsmanager.util;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import net.kyori.adventure.text.Component;

public class TabListUtil {
    public static TabListEntry getTabListEntry(Player player, TabList tabList) {
        return TabListEntry.builder().profile(player.getGameProfile()).tabList(tabList).build();
    }

    public static void updateTabListEntry(TabListEntry tabListEntry, String username, String serverName) {
        tabListEntry.setGameMode(0).setLatency(114514).setDisplayName(TextUtil.getUsernameComponent(username)
                .append(Component.text(" <"))
                .append(TextUtil.getServerNameComponent(serverName))
                .append(Component.text(">")));
    }

}
