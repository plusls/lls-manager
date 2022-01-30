package com.plusls.llsmanager.util;

import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TranslatableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class BridgeUtil {

    private static final LlsManager llsManager = Objects.requireNonNull(LlsManager.getInstance());

    public static void sendMessageToAllPlayer(ComponentLike componentLike, List<String> channelList, @Nullable Exclude exclude) {
        for (RegisteredServer server : llsManager.server.getAllServers()) {
            for (Player player : server.getPlayersConnected()) {
                if (exclude != null && exclude.test(player, server)) {
                    continue;
                }
                LlsPlayer llsPlayerToSend = llsManager.getLlsPlayer(player);
                if (channelList.contains(llsPlayerToSend.getChannel())) {
                    player.sendMessage(componentLike);
                }
            }
        }
    }

    public static void sendLeaveMessage(String serverName, Player player) {
        if (!llsManager.config.getBridgePlayerLeaveMessage()) {
            return;
        }
        TranslatableComponent leaveMessage = Component.translatable("lls-manager.bridge.leave.format")
                .args(TextUtil.getServerNameComponent(serverName),
                        TextUtil.getUsernameComponent(player.getUsername()));
        BridgeUtil.sendMessageToAllPlayer(leaveMessage, llsManager.config.getLeaveMessageChannelList(),
                (playerToSend, serverToSend) -> serverToSend.getServerInfo().getName().equals(serverName) ||
                        serverToSend.getServerInfo().getName().equals(llsManager.config.getAuthServerName()) ||
                        playerToSend.equals(player));
    }

    public static void sendJoinMessage(String serverName, Player player) {
        if (!llsManager.config.getBridgePlayerJoinMessage()) {
            return;
        }
        TranslatableComponent leaveMessage = Component.translatable("lls-manager.bridge.join.format")
                .args(TextUtil.getServerNameComponent(serverName),
                        TextUtil.getUsernameComponent(player.getUsername()));
        BridgeUtil.sendMessageToAllPlayer(leaveMessage, llsManager.config.getLeaveMessageChannelList(),
                (playerToSend, serverToSend) -> serverToSend.getServerInfo().getName().equals(serverName) ||
                        serverToSend.getServerInfo().getName().equals(llsManager.config.getAuthServerName()) ||
                        playerToSend.equals(player));
    }

    public static void bridgeMessage(String message, String messageSource, String channel) {
        // TODO
        // 发送信息给外界信息中转，比如 qq
    }


    @FunctionalInterface
    public interface Exclude {
        boolean test(Player playerToSend, RegisteredServer serverToSend);
    }
}
