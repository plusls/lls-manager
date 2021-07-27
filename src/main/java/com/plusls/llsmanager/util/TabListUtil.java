package com.plusls.llsmanager.util;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import net.kyori.adventure.text.Component;

public class TabListUtil {
    public static TabListEntry getTabListEntry(Player player, TabList tabList, String serverName) {
        return TabListEntry.builder().gameMode(0).latency(114514)
                .profile(player.getGameProfile()).tabList(tabList)
                .displayName(TextUtil.getUsernameComponent(player.getUsername())
                        .append(Component.text(" <"))
                        .append(TextUtil.getServerNameComponent(serverName))
                        .append(Component.text(">"))).build();
    }
}
