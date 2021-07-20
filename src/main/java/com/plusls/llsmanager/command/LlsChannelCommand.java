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
        LiteralCommandNode<CommandSource> llsChannelNode = LiteralArgumentBuilder
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
                                    Player player = (Player) context.getSource();
                                    String channel = context.getArgument("channel", String.class);
                                    if (!LlsPlayer.channelList.contains(channel)) {
                                        player.sendMessage(Component.translatable("lls-manager.command.lls_channel.unregistered_channel")
                                                .color(NamedTextColor.RED)
                                                .args(TextUtil.getChannelComponent(channel))
                                        );
                                        return 0;
                                    }
                                    LlsPlayer llsPlayer = llsManager.players.get(player.getRemoteAddress());
                                    if (llsPlayer.getChannel().equals(channel)) {
                                        player.sendMessage(Component.translatable("lls-manager.command.lls_channel.add.already_in_channel")
                                                .color(NamedTextColor.RED)
                                                .args(TextUtil.getChannelComponent(channel))
                                        );
                                        return 0;
                                    }
                                    if (llsPlayer.setChannel(channel)) {
                                        player.sendMessage(Component.translatable("lls-manager.command.lls_channel.success")
                                                .color(NamedTextColor.GREEN)
                                                .args(TextUtil.getChannelComponent(channel))
                                        );
                                        BridgeUtil.sendMessageToAllPlayer(Component.translatable("lls-manager.command.lls_channel.global_message")
                                                        .args(TextUtil.getUsernameComponent(player.getUsername()), TextUtil.getChannelComponent(channel)),
                                                LlsPlayer.channelList,
                                                (playerToSend, serverToSend) -> player == playerToSend ||
                                                        serverToSend.getServerInfo().getName().equals(llsManager.config.getAuthServerName()));
                                        return 1;
                                    } else {
                                        player.sendMessage(Component.translatable("lls-manager.command.lls_channel.failure")
                                                .color(NamedTextColor.RED)
                                                .args(TextUtil.getChannelComponent(channel))
                                        );
                                        return 0;
                                    }

                                }
                        ))
                .executes(
                        context -> {
                            Map<String, List<String>> channelMap = new HashMap<>();
                            CommandSource commandSource = context.getSource();
                            LlsPlayer.channelList.forEach(name -> channelMap.put(name, new ArrayList<>()));
                            for (Player player : llsManager.server.getAllPlayers()) {
                                String username = player.getUsername();
                                LlsPlayer llsPlayer = llsManager.players.get(player.getRemoteAddress());
                                // 在加载配置时会检查 channel 是否合法，因此这里不会出问题
                                channelMap.get(llsPlayer.getChannel()).add(username);
                            }
                            for (Map.Entry<String, List<String>> entry : channelMap.entrySet()) {
                                commandSource.sendMessage(Component.translatable("lls-manager.command.lls_channel.channel_info")
                                        .args(TextUtil.getChannelComponent(entry.getKey()), Component.text(entry.getValue().size()).color(NamedTextColor.GREEN)));
                                entry.getValue().forEach(
                                        username -> commandSource.sendMessage(Component.text("- ").append(TextUtil.getUsernameComponent(username)))
                                );
                            }
                            return 1;
                        }
                ).build();
        return new BrigadierCommand(llsChannelNode);
    }

}
