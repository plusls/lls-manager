package com.plusls.llsmanager.util;

import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Objects;

public class CommandUtil {

    public static boolean setChannel(String channel, String username, CommandSource source) {
        LlsManager llsManager = Objects.requireNonNull(LlsManager.getInstance());
        LlsPlayer llsPlayer;
        try {
            llsPlayer = llsManager.getLlsPlayer(username);
        } catch (LoadPlayerFailException | PlayerNotFoundException e) {
            source.sendMessage(e.message);
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
