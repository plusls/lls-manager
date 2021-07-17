package com.plusls.llsmanager.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.plusls.llsmanager.LlsManager;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
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
                                                context.getSource().sendMessage(Component.text("User ").color(NamedTextColor.RED)
                                                        .append(Component.text(username).color(NamedTextColor.GOLD))
                                                        .append(Component.text(" already in whitelist.").color(NamedTextColor.RED)));
                                                return 0;
                                            } else if (llsManager.whitelist.add(username)) {
                                                context.getSource().sendMessage(Component.text("Add ").color(NamedTextColor.GREEN)
                                                        .append(Component.text(username).color(NamedTextColor.GOLD))
                                                        .append(Component.text(" to whitelist success!").color(NamedTextColor.GREEN)));
                                                return 1;
                                            } else {
                                                context.getSource().sendMessage(Component.text("Add ").color(NamedTextColor.RED)
                                                        .append(Component.text(username).color(NamedTextColor.GOLD))
                                                        .append(Component.text(" to whitelist fail.").color(NamedTextColor.RED)));
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
                                                context.getSource().sendMessage(Component.text("User ").color(NamedTextColor.RED)
                                                        .append(Component.text(username).color(NamedTextColor.GOLD))
                                                        .append(Component.text(" not in whitelist.").color(NamedTextColor.RED)));
                                                return 0;
                                            } else if (llsManager.whitelist.remove(username)) {
                                                context.getSource().sendMessage(Component.text("Remove ").color(NamedTextColor.GREEN)
                                                        .append(Component.text(username).color(NamedTextColor.GOLD))
                                                        .append(Component.text(" from whitelist success!").color(NamedTextColor.GREEN)));
                                                return 1;
                                            } else {
                                                context.getSource().sendMessage(Component.text("Remove ").color(NamedTextColor.RED)
                                                        .append(Component.text(username).color(NamedTextColor.GOLD))
                                                        .append(Component.text(" from whitelist fail.").color(NamedTextColor.RED)));
                                                return 0;
                                            }
                                        }
                                )
                        )
                ).then(LiteralArgumentBuilder.<CommandSource>literal("reload").executes(
                        context -> {
                            if (llsManager.whitelist.load()) {
                                context.getSource().sendMessage(Component.text("Reload whitelist success!").color(NamedTextColor.GREEN));
                                return 1;
                            } else {
                                context.getSource().sendMessage(Component.text("Reload whitelist fail.").color(NamedTextColor.RED));
                                return 0;
                            }
                        })
                ).then(LiteralArgumentBuilder.<CommandSource>literal("list").executes(
                        context -> {
                            List<String> whitelist = llsManager.whitelist.search("");
                            context.getSource().sendMessage(Component.text("There are ")
                                    .append(Component.text(whitelist.size()).color(NamedTextColor.GREEN))
                                    .append(Component.text(" player in whitelist:")));
                            AtomicInteger maxLen = new AtomicInteger(1);
                            whitelist.forEach(username -> maxLen.set(Math.max(username.length(), maxLen.get())));

                            whitelist.forEach(username -> {
                                HoverEvent<Component> hoverEvent = HoverEvent.showText(Component.text("Remove ")
                                        .append(Component.text(username).color(NamedTextColor.GOLD))
                                        .append(Component.text(" from whitelist.")));

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
                            if (llsManager.whitelist.setStatus(true)) {
                                context.getSource().sendMessage(Component.text("Set whitelist ")
                                        .append(Component.text("on").color(NamedTextColor.GREEN))
                                        .append(Component.text(" success."))
                                );
                                return 1;
                            } else {
                                context.getSource().sendMessage(Component.text("Set whitelist on fail.").color(NamedTextColor.RED));
                                return 0;
                            }
                        })
                ).then(LiteralArgumentBuilder.<CommandSource>literal("off").executes(
                        context -> {
                            if (llsManager.whitelist.setStatus(false)) {
                                context.getSource().sendMessage(Component.text("Set whitelist ")
                                        .append(Component.text("off").color(NamedTextColor.RED))
                                        .append(Component.text(" success."))
                                );
                                return 1;
                            } else {
                                context.getSource().sendMessage(Component.text("Set whitelist off fail.").color(NamedTextColor.RED));
                                return 0;
                            }
                        })
                ).then(LiteralArgumentBuilder.<CommandSource>literal("status").executes(
                        context -> {
                            List<String> whitelist = llsManager.whitelist.search("");
                            TextComponent textComponent = Component.text("There are ")
                                    .append(Component.text(whitelist.size()).color(NamedTextColor.GREEN))
                                    .append(Component.text(" player in whitelist, whitelist is "));
                            if (llsManager.whitelist.getStatus()) {
                                textComponent = textComponent.append(Component.text("on.").color(NamedTextColor.GREEN));
                            } else {
                                textComponent = textComponent.append(Component.text("off.").color(NamedTextColor.RED));
                            }
                            context.getSource().sendMessage(textComponent);
                            return 1;
                        })
                ).build();

        return new BrigadierCommand(llsWhitelistNode);
    }
}
