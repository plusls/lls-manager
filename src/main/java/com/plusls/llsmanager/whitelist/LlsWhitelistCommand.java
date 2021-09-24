package com.plusls.llsmanager.whitelist;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.command.Command;
import com.plusls.llsmanager.data.LlsPlayer;
import com.plusls.llsmanager.util.LlsManagerException;
import com.plusls.llsmanager.util.TextUtil;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class LlsWhitelistCommand {

    @Inject
    LlsManager llsManager;

    public BrigadierCommand createBrigadierCommand() {
        LiteralCommandNode<CommandSource> llsSeenNode = LiteralArgumentBuilder
                .<CommandSource>literal("lls_seen").requires(commandSource -> commandSource.hasPermission("lls-manager.admin"))
                .then(llsManager.injector.getInstance(AddCommand.class).createSubCommand())
                .then(llsManager.injector.getInstance(RemoveCommand.class).createSubCommand())
                .then(llsManager.injector.getInstance(ReloadCommand.class).createSubCommand())

                .build();
        return new BrigadierCommand(llsSeenNode);
    }

    @Singleton
    private static class AddCommand implements Command {

        @Inject
        LlsManager llsManager;

        @Override
        public LiteralArgumentBuilder<CommandSource> createSubCommand() {
            return LiteralArgumentBuilder.<CommandSource>literal("add")
                    .then(RequiredArgumentBuilder.<CommandSource, String>argument("username", StringArgumentType.string())
                            .suggests(this::getUsernameSuggestions)
                            .then(RequiredArgumentBuilder.<CommandSource, String>argument("serverName", StringArgumentType.string())
                                    .suggests(this::getServerNameSuggestions)
                                    .executes(this)));
        }

        public CompletableFuture<Suggestions> getUsernameSuggestions(final CommandContext<CommandSource> context, final SuggestionsBuilder builder) {
            llsManager.playerSet.forEach(
                    username -> {
                        if (username.contains(builder.getRemaining())) {
                            builder.suggest(username);
                        }
                    }
            );
            return builder.buildFuture();
        }

        public CompletableFuture<Suggestions> getServerNameSuggestions(final CommandContext<CommandSource> context, final SuggestionsBuilder builder) {
            llsManager.server.getAllServers().forEach(
                    registeredServer -> {
                        String serverName = registeredServer.getServerInfo().getName();
                        if (serverName.contains(builder.getRemaining())) {
                            builder.suggest(serverName);
                        }
                    }
            );
            return builder.buildFuture();
        }

        @Override
        public int run(CommandContext<CommandSource> commandContext) throws CommandSyntaxException {
            String username = commandContext.getArgument("username", String.class);
            String serverName = commandContext.getArgument("serverName", String.class);
            CommandSource source = commandContext.getSource();
            LlsPlayer llsPlayer;
            try {
                llsPlayer = llsManager.getLlsPlayer(username);
            } catch (LlsManagerException e) {
                source.sendMessage(Component.translatable("lls-manager.command.lls_whitelist.add.failure")
                        .color(NamedTextColor.RED)
                        .args(TextUtil.getServerNameComponent(serverName), TextUtil.getUsernameComponent(username)));
                source.sendMessage(e.message);
                return 0;
            }
            ConcurrentSkipListSet<String> whitelistServerList = llsPlayer.getWhitelistServerList();
            if (whitelistServerList.contains(serverName)) {
                source.sendMessage(Component.translatable("lls-manager.command.lls_whitelist.add.already_in_whitelist")
                        .color(NamedTextColor.RED)
                        .args(TextUtil.getServerNameComponent(serverName), TextUtil.getUsernameComponent(username)));
                return 0;
            } else {
                whitelistServerList.add(serverName);
                if (!llsPlayer.save()) {
                    source.sendMessage(Component.translatable("lls-manager.command.lls_whitelist.add.failure")
                            .color(NamedTextColor.RED)
                            .args(TextUtil.getServerNameComponent(serverName), TextUtil.getUsernameComponent(username)));
                    return 0;
                } else {
                    source.sendMessage(Component.translatable("lls-manager.command.lls_whitelist.add.success")
                            .color(NamedTextColor.GREEN)
                            .args(TextUtil.getServerNameComponent(serverName), TextUtil.getUsernameComponent(username)));
                    return 1;
                }
            }
        }
    }

    @Singleton
    private static class RemoveCommand implements Command {

        @Inject
        LlsManager llsManager;

        @Override
        public LiteralArgumentBuilder<CommandSource> createSubCommand() {
            return LiteralArgumentBuilder.<CommandSource>literal("remove")
                    .then(RequiredArgumentBuilder.<CommandSource, String>argument("username", StringArgumentType.string())
                            .suggests(this::getUsernameSuggestions)
                            .then(RequiredArgumentBuilder.<CommandSource, String>argument("serverName", StringArgumentType.string())
                                    .suggests(this::getServerNameSuggestions)
                                    .executes(this)));
        }

        public CompletableFuture<Suggestions> getUsernameSuggestions(final CommandContext<CommandSource> context, final SuggestionsBuilder builder) {
            llsManager.playerSet.forEach(
                    username -> {
                        if (username.contains(builder.getRemaining())) {
                            builder.suggest(username);
                        }
                    }
            );
            return builder.buildFuture();
        }

        public CompletableFuture<Suggestions> getServerNameSuggestions(final CommandContext<CommandSource> context, final SuggestionsBuilder builder) {
            llsManager.server.getAllServers().forEach(
                    registeredServer -> {
                        String serverName = registeredServer.getServerInfo().getName();
                        if (serverName.contains(builder.getRemaining())) {
                            builder.suggest(serverName);
                        }
                    }
            );
            return builder.buildFuture();
        }

        @Override
        public int run(CommandContext<CommandSource> commandContext) throws CommandSyntaxException {
            String username = commandContext.getArgument("username", String.class);
            String serverName = commandContext.getArgument("serverName", String.class);
            CommandSource source = commandContext.getSource();
            LlsPlayer llsPlayer;
            try {
                llsPlayer = llsManager.getLlsPlayer(username);
            } catch (LlsManagerException e) {
                source.sendMessage(Component.translatable("lls-manager.command.lls_whitelist.remove.failure")
                        .color(NamedTextColor.RED)
                        .args(TextUtil.getServerNameComponent(serverName), TextUtil.getUsernameComponent(username)));
                source.sendMessage(e.message);
                return 0;
            }
            ConcurrentSkipListSet<String> whitelistServerList = llsPlayer.getWhitelistServerList();

            if (!whitelistServerList.contains(serverName)) {
                source.sendMessage(Component.translatable("lls-manager.command.lls_whitelist.remove.not_in_whitelist")
                        .color(NamedTextColor.RED)
                        .args(TextUtil.getServerNameComponent(serverName), TextUtil.getUsernameComponent(username)));
                return 0;
            } else {
                whitelistServerList.remove(serverName);
                if (!llsPlayer.save()) {
                    source.sendMessage(Component.translatable("lls-manager.command.lls_whitelist.remove.failure")
                            .color(NamedTextColor.RED)
                            .args(TextUtil.getServerNameComponent(serverName), TextUtil.getUsernameComponent(username)));
                    return 0;
                } else {
                    source.sendMessage(Component.translatable("lls-manager.command.lls_whitelist.remove.success")
                            .color(NamedTextColor.GREEN)
                            .args(TextUtil.getServerNameComponent(serverName), TextUtil.getUsernameComponent(username)));
                    return 1;
                }
            }
        }
    }
    @Singleton
    private static class ListCommand implements Command {
        @Override
        public LiteralArgumentBuilder<CommandSource> createSubCommand() {
            return LiteralArgumentBuilder.<CommandSource>literal("list").executes(this);
        }

        @Override
        public int run(CommandContext<CommandSource> commandContext) {
            List<String> whitelist = llsManager.whitelist.search("");
            context.getSource().sendMessage(Component.translatable("lls-manager.command.lls_whitelist.list.count_info")
                    .args(Component.text(whitelist.size()).color(NamedTextColor.GREEN)));
            AtomicInteger maxLen = new AtomicInteger(1);
            whitelist.forEach(username -> maxLen.set(Math.max(username.length(), maxLen.get())));

            whitelist.forEach(username -> {
                HoverEvent<Component> hoverEvent = HoverEvent.showText(Component.translatable("lls-manager.command.lls_whitelist.list.hover_event_info")
                        .args(TextUtil.getUsernameComponent(username)));

                ClickEvent clickEvent = ClickEvent.suggestCommand("/lls_whitelist remove " + username);

                context.getSource().sendMessage(Component.text(" - ")
                        .append(TextUtil.getUsernameComponent(username))
                        .append(Component.text(" " + String.join("", Collections.nCopies(maxLen.get() + 1 - username.length(), "-")) + " "))
                        .append(Component.text("[X]").color(NamedTextColor.GRAY)
                                .hoverEvent(hoverEvent)
                                .clickEvent(clickEvent)));
            });
            return 1;        }
    }

    public static void register(LlsManager llsManager) {
        llsManager.commandManager.register(createBrigadierCommand(llsManager));
    }

    private static BrigadierCommand createBrigadierCommand(LlsManager llsManager) {
        LiteralCommandNode<CommandSource> llsWhitelistNode = LiteralArgumentBuilder
                .<CommandSource>literal("lls_whitelist").requires(commandSource -> commandSource.hasPermission("lls-manager.admin"))
                .then(
                ).then(
                ).then(
                ).then(
                ).then(LiteralArgumentBuilder.<CommandSource>literal("on").executes(
                        context -> {
                            if (llsManager.config.getWhitelist()) {
                                context.getSource().sendMessage(Component.translatable("lls-manager.command.lls_whitelist.set.already")
                                        .color(NamedTextColor.RED)
                                        .args(Component.translatable("lls-manager.command.lls_whitelist.on")));
                                return 0;
                            } else if (llsManager.config.setWhitelist(true)) {
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
                            if (!llsManager.config.getWhitelist()) {
                                context.getSource().sendMessage(Component.translatable("lls-manager.command.lls_whitelist.set.already")
                                        .color(NamedTextColor.RED)
                                        .args(Component.translatable("lls-manager.command.lls_whitelist.off")));
                                return 0;
                            } else if (llsManager.config.setWhitelist(false)) {
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
                            if (llsManager.config.getWhitelist()) {
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
