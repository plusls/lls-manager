package com.plusls.llsmanager.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.plusls.llsmanager.util.BridgeUtil;
import com.plusls.llsmanager.util.TextUtil;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LlsChannelCommand {
    public static void register(LlsManager llsManager) {
        llsManager.commandManager.register(createBrigadierCommand(llsManager));
    }

    private static BrigadierCommand createBrigadierCommand(LlsManager llsManager) {
        LiteralCommandNode<CommandSource> llsWhitelistNode = LiteralArgumentBuilder
                .<CommandSource>literal("lls_channel")
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("channel", StringArgumentType.string())
                        .requires(commandSource -> commandSource instanceof Player)
                        .suggests(
                                (context, builder) -> {
                                    LlsPlayer.channelList.forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                        .executes(
                                context -> {
                                    CommandSource commandSource = context.getSource();
                                    String channel = context.getArgument("channel", String.class);
                                    if (!LlsPlayer.channelList.contains(channel)) {
                                        commandSource.sendMessage(Component.translatable("lls-manager.command.lls_seen.unregistered_channel")
                                                .color(NamedTextColor.RED)
                                                .args(TextUtil.getChannelComponent(channel))
                                        );
                                        return 0;
                                    }
                                    if (commandSource instanceof Player player) {
                                        String username = player.getUsername();
                                        LlsPlayer llsPlayer = llsManager.players.get(username);
                                        if (llsPlayer.getChannel().equals(channel)) {
                                            commandSource.sendMessage(Component.translatable("lls-manager.command.lls_seen.add.already_in_channel")
                                                    .color(NamedTextColor.RED)
                                                    .args(TextUtil.getChannelComponent(channel))
                                            );
                                            return 0;
                                        }
                                        if (llsPlayer.setChannel(channel)) {
                                            commandSource.sendMessage(Component.translatable("lls-manager.command.lls_seen.success")
                                                    .color(NamedTextColor.GREEN)
                                                    .args(TextUtil.getChannelComponent(channel))
                                            );
                                            BridgeUtil.sendMessageToAllPlayer(Component.translatable("lls-manager.command.lls_seen.global_message")
                                                            .args(TextUtil.getUsernameComponent(username), TextUtil.getChannelComponent(channel)),
                                                    LlsPlayer.channelList,
                                                    (playerToSend, serverToSend) -> player == playerToSend);
                                            return 1;
                                        } else {
                                            commandSource.sendMessage(Component.translatable("lls-manager.command.lls_seen.failure")
                                                    .color(NamedTextColor.RED)
                                                    .args(TextUtil.getChannelComponent(channel))
                                            );
                                            return 0;
                                        }
                                    } else {
                                        // 理论上不会执行到这
                                        commandSource.sendMessage(Component.translatable("lls-manager.command.lls_seen.command_source_error").color(NamedTextColor.RED));
                                        return 0;
                                    }
                                }
                        ))
                .executes(
                        context -> {
                            Map<String, List<String>> channelMap = new HashMap<>();
                            CommandSource commandSource = context.getSource();
                            LlsPlayer.channelList.forEach(name -> channelMap.put(name, new ArrayList<>()));
                            for (RegisteredServer server : llsManager.server.getAllServers()) {
                                for (Player player : server.getPlayersConnected()) {
                                    String username = player.getUsername();
                                    LlsPlayer llsPlayer = llsManager.players.get(player.getUsername());
                                    // 在加载配置时会检查 channel 是否合法，因此这里不会出问题
                                    channelMap.get(llsPlayer.getChannel()).add(username);
                                }
                            }
                            for (Map.Entry<String, List<String>> entry : channelMap.entrySet()) {
                                commandSource.sendMessage(Component.translatable("lls-manager.command.lls_seen.channel_info")
                                        .args(TextUtil.getChannelComponent(entry.getKey()), Component.text(entry.getValue().size()).color(NamedTextColor.GREEN)));
                                entry.getValue().forEach(
                                        username -> commandSource.sendMessage(Component.text("- ").append(TextUtil.getUsernameComponent(username)))
                                );
                            }
                            return 1;
                        }
                ).build();
        return new BrigadierCommand(llsWhitelistNode);
    }

}
