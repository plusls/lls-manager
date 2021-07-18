package com.plusls.llsmanager.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.plusls.llsmanager.LlsManager;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class LlsWhitelistCommand {

    public static void register(LlsManager llsManager) {
        llsManager.commandManager.register(createBrigadierCommand(llsManager));

    }

    public static BrigadierCommand createBrigadierCommand(LlsManager llsManager) {
        LiteralCommandNode<CommandSource> llsWhitelistNode = LiteralArgumentBuilder
                .<CommandSource>literal("lls_whitelist").requires(commandSource -> commandSource.hasPermission("lls-manager.admin"))
                .then(LiteralArgumentBuilder.<CommandSource>literal("add")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("username", StringArgumentType.string())
                                .executes(
                                        context -> {
                                            String username = context.getArgument("username", String.class);
                                            if (llsManager.whitelist.query(username)) {
                                                context.getSource().sendMessage(Component.translatable("lls-manager.command.lls_whitelist.add.already_in_whitelist")
                                                        .color(NamedTextColor.RED)
                                                        .args(Component.text(username).color(NamedTextColor.GOLD)));
                                                return 0;
                                            } else if (llsManager.whitelist.add(username)) {
                                                context.getSource().sendMessage(Component.translatable("lls-manager.command.lls_whitelist.add.success")
                                                        .color(NamedTextColor.GREEN)
                                                        .args(Component.text(username).color(NamedTextColor.GOLD)));
                                                return 1;
                                            } else {
                                                context.getSource().sendMessage(Component.translatable("lls-manager.command.lls_whitelist.add.failure")
                                                        .color(NamedTextColor.RED)
                                                        .args(Component.text(username).color(NamedTextColor.GOLD)));
                                                return 0;
                                            }
                                        }
                                )
                        )
                ).then(LiteralArgumentBuilder.<CommandSource>literal("remove")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("username", StringArgumentType.string()).suggests(
                                (context, builder) -> {
                                    for (String username : llsManager.whitelist.search(builder.getRemaining())) {
                                        builder.suggest(username);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(
                                        context -> {
                                            String username = context.getArgument("username", String.class);
                                            if (!llsManager.whitelist.query(username)) {
                                                context.getSource().sendMessage(Component.translatable("lls-manager.command.lls_whitelist.remove.not_in_whitelist")
                                                        .color(NamedTextColor.RED)
                                                        .args(Component.text(username).color(NamedTextColor.GOLD)));
                                                return 0;
                                            } else if (llsManager.whitelist.remove(username)) {
                                                context.getSource().sendMessage(Component.translatable("lls-manager.command.lls_whitelist.remove.success")
                                                        .color(NamedTextColor.GREEN)
                                                        .args(Component.text(username).color(NamedTextColor.GOLD)));
                                                return 1;
                                            } else {
                                                context.getSource().sendMessage(Component.translatable("lls-manager.command.lls_whitelist.remove.failure")
                                                        .color(NamedTextColor.RED)
                                                        .args(Component.text(username).color(NamedTextColor.GOLD)));
                                                return 0;
                                            }
                                        }
                                )
                        )
                ).then(LiteralArgumentBuilder.<CommandSource>literal("reload").executes(
                        context -> {
                            if (llsManager.whitelist.load()) {
                                context.getSource().sendMessage(Component.translatable("lls-manager.command.lls_whitelist.reload.success").color(NamedTextColor.GREEN));
                                return 1;
                            } else {
                                context.getSource().sendMessage(Component.translatable("lls-manager.command.lls_whitelist.reload.failure").color(NamedTextColor.RED));
                                return 0;
                            }
                        })
                ).then(LiteralArgumentBuilder.<CommandSource>literal("list").executes(
                        context -> {
                            List<String> whitelist = llsManager.whitelist.search("");
                            context.getSource().sendMessage(Component.translatable("lls-manager.command.lls_whitelist.list.count_info")
                                    .args(Component.text(whitelist.size()).color(NamedTextColor.GREEN)));
                            AtomicInteger maxLen = new AtomicInteger(1);
                            whitelist.forEach(username -> maxLen.set(Math.max(username.length(), maxLen.get())));

                            whitelist.forEach(username -> {
                                HoverEvent<Component> hoverEvent = HoverEvent.showText(Component.translatable("lls-manager.command.lls_whitelist.list.hover_event_info")
                                        .args(Component.text(username).color(NamedTextColor.GOLD)));

                                ClickEvent clickEvent = ClickEvent.suggestCommand("/lls_whitelist remove " + username);

                                context.getSource().sendMessage(Component.text(" - ")
                                        .append(Component.text(username).color(NamedTextColor.GOLD))
                                        .append(Component.text(" " + String.join("", Collections.nCopies(maxLen.get() + 1 - username.length(), "-")) + " "))
                                        .append(Component.text("[X]").color(NamedTextColor.GRAY)
                                                .hoverEvent(hoverEvent)
                                                .clickEvent(clickEvent)));
                            });
                            return 1;
                        })
                ).then(LiteralArgumentBuilder.<CommandSource>literal("on").executes(
                        context -> {
                            if (llsManager.whitelist.getStatus()) {
                                context.getSource().sendMessage(Component.translatable("lls-manager.command.lls_whitelist.set.already")
                                        .color(NamedTextColor.RED)
                                        .args(Component.translatable("lls-manager.command.lls_whitelist.on")));
                                return 0;
                            } else if (llsManager.whitelist.setStatus(true)) {
                                context.getSource().sendMessage(Component.translatable("lls-manager.command.lls_whitelist.set.success")
                                        .args(Component.translatable("lls-manager.command.lls_whitelist.on").color(NamedTextColor.GREEN)));
                                return 1;
                            } else {
                                context.getSource().sendMessage(Component.translatable("lls-manager.command.lls_whitelist.set.failure")
                                        .color(NamedTextColor.RED)
                                        .args(Component.translatable("lls-manager.command.lls_whitelist.on")));
                                return 0;
                            }
                        })
                ).then(LiteralArgumentBuilder.<CommandSource>literal("off").executes(
                        context -> {
                            if (!llsManager.whitelist.getStatus()) {
                                context.getSource().sendMessage(Component.translatable("lls-manager.command.lls_whitelist.set.already")
                                        .color(NamedTextColor.RED)
                                        .args(Component.translatable("lls-manager.command.lls_whitelist.off")));
                                return 0;
                            } else if (llsManager.whitelist.setStatus(false)) {
                                context.getSource().sendMessage(Component.translatable("lls-manager.command.lls_whitelist.set.success")
                                        .args(Component.translatable("lls-manager.command.lls_whitelist.off").color(NamedTextColor.RED)));
                                return 1;
                            } else {
                                context.getSource().sendMessage(Component.translatable("lls-manager.command.lls_whitelist.set.failure")
                                        .color(NamedTextColor.RED)
                                        .args(Component.translatable("lls-manager.command.lls_whitelist.off")));
                                return 0;
                            }
                        })
                ).then(LiteralArgumentBuilder.<CommandSource>literal("status").executes(
                        context -> {
                            List<String> whitelist = llsManager.whitelist.search("");
                            TranslatableComponent statusTextComponent;
                            if (llsManager.whitelist.getStatus()) {
                                statusTextComponent = Component.translatable("lls-manager.command.lls_whitelist.on").color(NamedTextColor.GREEN);
                            } else {
                                statusTextComponent = Component.translatable("lls-manager.command.lls_whitelist.off").color(NamedTextColor.RED);
                            }
                            context.getSource().sendMessage(Component.translatable("lls-manager.command.lls_whitelist.status.info")
                                    .args(Component.text(whitelist.size()).color(NamedTextColor.GREEN), statusTextComponent));
                            return 1;
                        })
                ).build();

        return new BrigadierCommand(llsWhitelistNode);
    }
}
