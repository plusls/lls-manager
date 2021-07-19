package com.plusls.llsmanager.handler;

import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.plusls.llsmanager.util.BridgeUtil;
import com.plusls.llsmanager.util.TextUtil;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;

import java.util.Objects;

public class PlayerChatEventHandler implements EventHandler<PlayerChatEvent> {

    private static LlsManager llsManager;

    public static void init(LlsManager llsManager) {
        llsManager.server.getEventManager().register(llsManager, PlayerChatEvent.class, new PlayerChatEventHandler());
        PlayerChatEventHandler.llsManager = llsManager;
    }


    @Override
    public void execute(PlayerChatEvent event) {
        if (!llsManager.config.getBridgeChatMessage()) {
            return;
        }
        Player player = event.getPlayer();
        String username = player.getUsername();
        String message = event.getMessage();

        LlsPlayer llsPlayer = llsManager.onlinePlayers.get(username);
        if (llsPlayer == null) {
            llsPlayer = Objects.requireNonNull(llsManager.preLoginPlayers.get(player.getRemoteAddress()));
        }
        String channel = llsPlayer.getChannel();
        if (!llsManager.config.getBridgeMessageChannel().contains(channel)) {
            return;
        }
        player.getCurrentServer().ifPresent((serverConnection) -> {
            String serverName = serverConnection.getServerInfo().getName();
            TranslatableComponent translatableComponent = Component.translatable("lls-manager.bridge.chat.format")
                    .args(TextUtil.getServerNameComponent(serverName),
                            TextUtil.getUsernameComponent(username),
                            Component.text(message));
            BridgeUtil.sendMessageToAllPlayer(translatableComponent, llsManager.config.getChatMessageChannelList(),
                    (playerToSend, serverToSend) -> serverToSend.getServerInfo().getName().equals(serverName) ||
                            serverToSend.getServerInfo().getName().equals(llsManager.config.getAuthServerName()));
            BridgeUtil.bridgeMessage(message, username, channel);
        });
    }
}
