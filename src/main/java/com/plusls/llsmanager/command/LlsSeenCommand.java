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
import com.velocitypowered.api.proxy.ServerConnection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

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
                                    llsManager.playerSet.forEach(
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
                                    if (!llsManager.playerSet.contains(username)) {
                                        TranslatableComponent userNotFoundText = Component.translatable("lls-manager.command.lls_seen.not_found")
                                                .color(NamedTextColor.RED)
                                                .args(Component.text(username));
                                        commandSource.sendMessage(userNotFoundText);
                                        return 0;
                                    }
                                    LlsPlayer llsPlayer = llsManager.onlinePlayers.get(username);
                                    if (llsPlayer != null) {
                                        Optional<Player> optionalPlayer = llsManager.server.getPlayer(username);
                                        if (optionalPlayer.isPresent()) {
                                            Optional<ServerConnection> optionalRegisteredServer = optionalPlayer.get().getCurrentServer();
                                            if (optionalRegisteredServer.isEmpty()) {
                                                throw new IllegalStateException("optionalRegisteredServer should not empty.");
                                            }
                                            TranslatableComponent onlineText = Component.translatable("lls-manager.command.lls_seen.online")
                                                    .args(TextUtil.getUsernameComponent(username),
                                                            TextUtil.getServerNameComponent(optionalRegisteredServer.get().getServerInfo().getName()));
                                            commandSource.sendMessage(onlineText);
                                            return 1;
                                        } else {
                                            throw new IllegalStateException("optionalPlayer should not empty.");
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
                                                        Component.text(sdf.format(llsPlayer.getLastSeenTime().getTime())).color(NamedTextColor.YELLOW), diffTextBuilder.build());
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