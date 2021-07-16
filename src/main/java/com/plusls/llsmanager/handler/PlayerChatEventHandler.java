package com.plusls.llsmanager.handler;

import com.plusls.llsmanager.LlsManager;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class PlayerChatEventHandler implements EventHandler<PlayerChatEvent> {

    private static LlsManager llsManager;

    public static void init(LlsManager llsManager) {
        llsManager.server.getEventManager().register(llsManager, PlayerChatEvent.class, new PlayerChatEventHandler());
        PlayerChatEventHandler.llsManager = llsManager;
    }

    @Override
    public void execute(PlayerChatEvent event) {
        event.getPlayer().getCurrentServer().ifPresent((serverConnection) -> {
            String serverName = serverConnection.getServerInfo().getName();
            TextComponent textComponent = Component.text("[")
                    .append(Component.text(serverName)
                            .color(NamedTextColor.GOLD)
                            .decoration(TextDecoration.BOLD, true)
                            .clickEvent(ClickEvent.suggestCommand("/server " + serverName)))
                    .append(Component.text("] <"))
                    .append(Component.text(event.getPlayer().getUsername()).color(NamedTextColor.GREEN))
                    .append(Component.text("> "))
                    .append(Component.text(event.getMessage()));
            for (RegisteredServer server : llsManager.server.getAllServers()) {
                if (server != serverConnection.getServer()) {
                    server.sendMessage(textComponent);
                }
            }
        });
    }
}
