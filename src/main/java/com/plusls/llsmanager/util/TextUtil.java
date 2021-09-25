package com.plusls.llsmanager.util;

import com.plusls.llsmanager.LlsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class TextUtil {

    public static Component getServerAutoComponent(String name) {
        if (LlsManager.getInstance().config.getServerGroup().containsKey(name)) {
            return getServerGroupComponent(name);
        } else {
            return getServerNameComponent(name);
        }
    }

    public static Component getServerNameComponent(String serverName) {
        HoverEvent<Component> hoverEvent = HoverEvent.showText(Component.translatable("lls-manager.text.server.hover_event")
                .args(getServerNameComponentWithoutEvent(serverName)));
        return getServerNameComponentWithoutEvent(serverName).hoverEvent(hoverEvent)
                .clickEvent(ClickEvent.suggestCommand("/server " + serverName));
    }

    public static Component getServerNameComponentWithoutEvent(String serverName) {
        return Component.text(serverName).color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true);
    }

    public static Component getServerGroupComponent(String serverGroupName) {
        HoverEvent<Component> hoverEvent = HoverEvent.showText(Component.translatable("lls-manager.text.server_group.hover_event")
                .args(getServerGroupNameComponentWithoutEvent(serverGroupName)));
        return getServerGroupNameComponentWithoutEvent(serverGroupName).hoverEvent(hoverEvent)
                .clickEvent(ClickEvent.suggestCommand("/lls_server_group query " + serverGroupName));
    }

    public static Component getServerGroupNameComponentWithoutEvent(String serverName) {
        return Component.text(serverName).color(NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true);
    }

    public static Component getUsernameComponent(String username) {
        HoverEvent<Component> hoverEvent = HoverEvent.showText(Component.translatable("lls-manager.text.user.hover_event")
                .args(getUsernameComponentWithoutEvent(username)));
        return getUsernameComponentWithoutEvent(username).hoverEvent(hoverEvent)
                .clickEvent(ClickEvent.runCommand("/lls_seen query " + username));
    }

    public static Component getUsernameComponentWithoutEvent(String username) {
        return Component.text(username).color(NamedTextColor.GREEN);
    }


    public static Component getChannelComponent(String channel) {
        HoverEvent<Component> hoverEvent = HoverEvent.showText(Component.translatable("lls-manager.text.channel.hover_event")
                .args(getChannelComponentWithoutEvent(channel)));
        return getChannelComponentWithoutEvent(channel).hoverEvent(hoverEvent)
                .clickEvent(ClickEvent.runCommand("/lls_channel " + channel));
    }

    public static Component getChannelComponentWithoutEvent(String username) {
        return Component.text(username).color(NamedTextColor.LIGHT_PURPLE);
    }

}
