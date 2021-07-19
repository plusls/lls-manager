package com.plusls.llsmanager.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.plusls.llsmanager.util.TextUtil;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LlsSeenCommand {
    public static void register(LlsManager llsManager) {
        llsManager.commandManager.register(createBrigadierCommand(llsManager));
    }

    private static BrigadierCommand createBrigadierCommand(LlsManager llsManager) {
        LiteralCommandNode<CommandSource> llsWhitelistNode = LiteralArgumentBuilder
                .<CommandSource>literal("lls_seen")
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("username", StringArgumentType.string())
                        .suggests(
                                (context, builder) -> {
                                    llsManager.playerList.forEach(
                                            username -> {
                                                if (username.contains(builder.getRemaining())) {
                                                    builder.suggest(username);
                                                }
                                            }
                                    );
                                    return builder.buildFuture();
                                })
                        .executes(
                                context -> {
                                    CommandSource commandSource = context.getSource();
                                    String username = context.getArgument("username", String.class);
                                    if (!llsManager.playerList.contains(username)) {
                                        TranslatableComponent userNotFoundText = Component.translatable("lls-manager.command.lls_seen.not_found")
                                                .color(NamedTextColor.RED)
                                                .args(Component.text(username));
                                        commandSource.sendMessage(userNotFoundText);
                                        return 0;
                                    }
                                    LlsPlayer llsPlayer = llsManager.players.get(username);
                                    if (llsPlayer != null) {
                                        // 能找到玩家不一定代表它在线，需要去遍历服务器看看是不是在线
                                        // 但是不这里面的玩家一定不在线
                                        for (Player player : llsManager.server.getAllPlayers()) {
                                            if (player.getUsername().equals(username) && player.getCurrentServer().isPresent()) {
                                                TranslatableComponent onlineText = Component.translatable("lls-manager.command.lls_seen.online")
                                                        .args(TextUtil.getUsernameComponent(username),
                                                                TextUtil.getServerNameComponent(player.getCurrentServer().get().getServerInfo().getName()));
                                                commandSource.sendMessage(onlineText);
                                                return 1;
                                            }
                                        }
                                    }
                                    // 玩家不在线的情况直接去查
                                    llsPlayer = new LlsPlayer(username, llsManager.dataFolderPath);
                                    if (llsPlayer.hasUser()) {
                                        if (!llsPlayer.load()) {
                                            commandSource.sendMessage(Component.translatable("lls-manager.command.lls_seen.load_player_data_fail").args(TextUtil.getUsernameComponent(username)));
                                            return 0;
                                        }
                                        Date currentDate = new Date();
                                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                        long diff = currentDate.getTime() - llsPlayer.getLastSeenTime().getTime();
                                        long diffSeconds = diff / 1000 % 60;
                                        long diffMinutes = diff / (60 * 1000) % 60;
                                        long diffHours = diff / (60 * 60 * 1000) % 24;
                                        long diffDays = diff / (24 * 60 * 60 * 1000);

                                        TextComponent.Builder diffTextBuilder = Component.text();
                                        if (diffDays != 0) {
                                            diffTextBuilder.append(Component.text(diffDays + " ").color(NamedTextColor.YELLOW))
                                                    .append(Component.translatable("lls-manager.command.lls_seen.day"));
                                        }
                                        if (diffHours != 0) {
                                            diffTextBuilder.append(Component.text(diffHours + " ").color(NamedTextColor.YELLOW))
                                                    .append(Component.translatable("lls-manager.command.lls_seen.hour"));
                                        }
                                        if (diffMinutes != 0) {
                                            diffTextBuilder.append(Component.text(diffHours + " ").color(NamedTextColor.YELLOW))
                                                    .append(Component.translatable("lls-manager.command.lls_seen.minute"));
                                        }
                                        if (diffSeconds != 0) {
                                            diffTextBuilder.append(Component.text(diffSeconds + " ").color(NamedTextColor.YELLOW))
                                                    .append(Component.translatable("lls-manager.command.lls_seen.second"));
                                        }
                                        TranslatableComponent offlineText = Component.translatable("lls-manager.command.lls_seen.offline")
                                                .args(TextUtil.getUsernameComponent(username),
                                                        Component.text(sdf.format(currentDate)).color(NamedTextColor.YELLOW), diffTextBuilder.build());
                                        commandSource.sendMessage(offlineText);
                                    } else {
                                        TranslatableComponent userNotFoundText = Component.translatable("lls-manager.command.lls_seen.not_found")
                                                .color(NamedTextColor.RED)
                                                .args(Component.text(username));
                                        commandSource.sendMessage(userNotFoundText);
                                        return 0;
                                    }
                                    return 1;

                                })
                ).build();
        return new BrigadierCommand(llsWhitelistNode);
    }
}