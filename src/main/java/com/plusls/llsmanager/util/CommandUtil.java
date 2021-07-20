package com.plusls.llsmanager.util;

import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public class CommandUtil {
    @Nullable
    public static LlsPlayer getLlsPlayer(String username, CommandSource source) {
        LlsManager llsManager = Objects.requireNonNull(LlsManager.getInstance());
        LlsPlayer llsPlayer;
        Optional<Player> playerOptional = llsManager.server.getPlayer(username);
        if (playerOptional.isPresent()) {
            llsPlayer = llsManager.players.get(playerOptional.get().getRemoteAddress());
        } else {
            llsPlayer = new LlsPlayer(username, llsManager.dataFolderPath);
            if (!llsPlayer.hasUser()) {
                source.sendMessage(Component.translatable("lls-manager.text.player_not_found", NamedTextColor.RED).args(TextUtil.getUsernameComponent(username)));
                return null;
            } else if (!llsPlayer.load()) {
                source.sendMessage(Component.translatable("lls-manager.text.load_player_data_fail").args(TextUtil.getUsernameComponent(username)));
                return null;
            }
        }
        return llsPlayer;
    }

    public static boolean setChannel(String channel, String username, CommandSource source) {
        LlsManager llsManager = Objects.requireNonNull(LlsManager.getInstance());
        LlsPlayer llsPlayer = getLlsPlayer(username, source);
        if (llsPlayer == null) {
            return false;
        }
        if (!LlsPlayer.channelList.contains(channel)) {
            source.sendMessage(Component.translatable("lls-manager.text.channel.unregistered_channel")
                    .color(NamedTextColor.RED)
                    .args(TextUtil.getChannelComponent(channel))
            );
            return false;
        }

        if (llsPlayer.getChannel().equals(channel)) {
            source.sendMessage(Component.translatable("lls-manager.text.channel.already_in_channel")
                    .color(NamedTextColor.RED)
                    .args(TextUtil.getUsernameComponent(username), TextUtil.getChannelComponent(channel))
            );
            return false;
        }
        if (llsPlayer.setChannel(channel)) {
            source.sendMessage(Component.translatable("lls-manager.text.channel.switch_success")
                    .args(TextUtil.getUsernameComponent(username), TextUtil.getChannelComponent(channel))
            );

            BridgeUtil.sendMessageToAllPlayer(Component.translatable("lls-manager.text.channel.global_message")
                            .args(TextUtil.getUsernameComponent(username), TextUtil.getChannelComponent(channel)),
                    LlsPlayer.channelList,
                    (playerToSend, serverToSend) -> serverToSend.getServerInfo().getName().equals(llsManager.config.getAuthServerName()));
            return true;
        } else {
            saveDataFail("channel", username, source);
            return false;
        }
    }

    public static void saveDataFail(String field, String username, CommandSource source) {
        source.sendMessage(Component.translatable("lls-manager.text.save_player_fail")
                .color(NamedTextColor.RED)
                .args(Component.text(field, NamedTextColor.YELLOW), TextUtil.getUsernameComponent(username))
        );
    }
}
