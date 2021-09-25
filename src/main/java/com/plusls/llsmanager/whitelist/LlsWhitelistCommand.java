package com.plusls.llsmanager.whitelist;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.command.Command;
import com.plusls.llsmanager.data.LlsPlayer;
import com.plusls.llsmanager.util.LoadPlayerFailException;
import com.plusls.llsmanager.util.PlayerNotFoundException;
import com.plusls.llsmanager.util.TextUtil;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;

@Singleton
public class LlsWhitelistCommand {

    @Inject
    LlsManager llsManager;

    public BrigadierCommand createBrigadierCommand() {
        LiteralCommandNode<CommandSource> llsSeenNode = LiteralArgumentBuilder
                .<CommandSource>literal("lls_whitelist").requires(commandSource -> commandSource.hasPermission("lls-manager.admin"))
                .then(llsManager.injector.getInstance(AddCommand.class).createSubCommand())
                .then(llsManager.injector.getInstance(RemoveCommand.class).createSubCommand())
                .then(llsManager.injector.getInstance(ListCommand.class).createSubCommand())
                .then(llsManager.injector.getInstance(StatusCommand.class).createSubCommand())
                .then(llsManager.injector.getInstance(HelpCommand.class).createSubCommand())
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
            String username = context.getArgument("username", String.class);
            LlsPlayer llsPlayer;
            try {
                llsPlayer = llsManager.getLlsPlayer(username);
            } catch (LoadPlayerFailException | PlayerNotFoundException e) {
                return builder.buildFuture();
            }
            llsManager.server.getAllServers().forEach(
                    registeredServer -> {
                        String serverName = registeredServer.getServerInfo().getName();
                        if (serverName.contains(builder.getRemaining()) && !llsPlayer.getWhitelistServerList().contains(serverName)) {
                            builder.suggest(serverName);
                        }
                    }
            );
            return builder.buildFuture();
        }

        @Override
        public int run(CommandContext<CommandSource> commandContext) {
            String username = commandContext.getArgument("username", String.class);
            String serverName = commandContext.getArgument("serverName", String.class);
            CommandSource source = commandContext.getSource();
            LlsPlayer llsPlayer;
            try {
                llsPlayer = llsManager.getLlsPlayer(username);
            } catch (LoadPlayerFailException | PlayerNotFoundException e) {
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
            String username = context.getArgument("username", String.class);
            LlsPlayer llsPlayer;
            try {
                llsPlayer = llsManager.getLlsPlayer(username);
            } catch (LoadPlayerFailException | PlayerNotFoundException e) {
                return builder.buildFuture();
            }
            llsManager.server.getAllServers().forEach(
                    registeredServer -> {
                        String serverName = registeredServer.getServerInfo().getName();
                        if (serverName.contains(builder.getRemaining()) && llsPlayer.getWhitelistServerList().contains(serverName)) {
                            builder.suggest(serverName);
                        }
                    }
            );
            return builder.buildFuture();
        }

        @Override
        public int run(CommandContext<CommandSource> commandContext) {
            String username = commandContext.getArgument("username", String.class);
            String serverName = commandContext.getArgument("serverName", String.class);
            CommandSource source = commandContext.getSource();
            LlsPlayer llsPlayer;
            try {
                llsPlayer = llsManager.getLlsPlayer(username);
            } catch (LoadPlayerFailException | PlayerNotFoundException e) {
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

        @Inject
        LlsManager llsManager;

        @Override
        public LiteralArgumentBuilder<CommandSource> createSubCommand() {
            return LiteralArgumentBuilder.<CommandSource>literal("list").executes(this);
        }

        @Override
        public int run(CommandContext<CommandSource> commandContext) {
            CommandSource source = commandContext.getSource();
            int maxLen = 0;
            for (String username : llsManager.playerSet) {
                maxLen = Math.max(maxLen, username.length());
            }
            for (String username : llsManager.playerSet) {
                LlsPlayer llsPlayer;
                try {
                    llsPlayer = llsManager.getLlsPlayer(username);
                } catch (LoadPlayerFailException | PlayerNotFoundException e) {
                    source.sendMessage(e.message);
                    return 0;
                }

                TextComponent.Builder component = Component.text();
                component.content(" - ")
                        .append(TextUtil.getUsernameComponent(username))
                        .append(Component.text(" " + String.join("", Collections.nCopies(maxLen - username.length(), " "))))
                        .append(Component.text(" :["));
                boolean first = true;
                for (String serverName : llsPlayer.getWhitelistServerList()) {
                    HoverEvent<Component> hoverEvent = HoverEvent.showText(Component.translatable("lls-manager.command.lls_whitelist.list.hover_event_info")
                            .args(TextUtil.getServerNameComponent(username)));
                    ClickEvent clickEvent = ClickEvent.suggestCommand(String.format("/lls_whitelist remove %s %s", username, serverName));
                    if (!first) {
                        component.append(Component.text(", "));
                    }
                    first = false;
                    component.append(TextUtil.getServerNameComponent(serverName))
                            .append(Component.text("[X]").color(NamedTextColor.GRAY)
                                    .hoverEvent(hoverEvent)
                                    .clickEvent(clickEvent));
                }
                component.append(Component.text("]"));
                source.sendMessage(component.build());
            }
            return 1;
        }
    }

    @Singleton
    private static class StatusCommand implements Command {

        @Inject
        LlsManager llsManager;

        @Override
        public LiteralArgumentBuilder<CommandSource> createSubCommand() {
            return LiteralArgumentBuilder.<CommandSource>literal("status")
                    .then(RequiredArgumentBuilder.<CommandSource, Boolean>argument("status", BoolArgumentType.bool())
                            //.suggests(this)
                            .executes(this::statusExecute))
                    .executes(this);
        }

        @Override
        public CompletableFuture<Suggestions> getSuggestions(final CommandContext<CommandSource> context, final SuggestionsBuilder builder) {
            if ("true".contains(builder.getRemaining())) {
                builder.suggest("true");
            } else if ("false".contains(builder.getRemaining())) {
                builder.suggest("false");
            }
            return builder.buildFuture();
        }

        public int statusExecute(CommandContext<CommandSource> commandContext) {
            Boolean status = commandContext.getArgument("status", Boolean.class);
            CommandSource source = commandContext.getSource();
            if (status) {
                if (llsManager.config.getWhitelist()) {
                    source.sendMessage(Component.translatable("lls-manager.command.lls_whitelist.set.already")
                            .color(NamedTextColor.RED)
                            .args(Component.translatable("lls-manager.command.lls_whitelist.on")));
                    return 0;
                } else if (llsManager.config.setWhitelist(true)) {
                    source.sendMessage(Component.translatable("lls-manager.command.lls_whitelist.set.success")
                            .args(Component.translatable("lls-manager.command.lls_whitelist.on").color(NamedTextColor.GREEN)));
                    return 1;
                } else {
                    source.sendMessage(Component.translatable("lls-manager.command.lls_whitelist.set.failure")
                            .color(NamedTextColor.RED)
                            .args(Component.translatable("lls-manager.command.lls_whitelist.on")));
                    return 0;
                }
            } else {
                if (!llsManager.config.getWhitelist()) {
                    source.sendMessage(Component.translatable("lls-manager.command.lls_whitelist.set.already")
                            .color(NamedTextColor.RED)
                            .args(Component.translatable("lls-manager.command.lls_whitelist.off")));
                    return 0;
                } else if (llsManager.config.setWhitelist(false)) {
                    source.sendMessage(Component.translatable("lls-manager.command.lls_whitelist.set.success")
                            .args(Component.translatable("lls-manager.command.lls_whitelist.off").color(NamedTextColor.RED)));
                    return 1;
                } else {
                    source.sendMessage(Component.translatable("lls-manager.command.lls_whitelist.set.failure")
                            .color(NamedTextColor.RED)
                            .args(Component.translatable("lls-manager.command.lls_whitelist.off")));
                    return 0;
                }
            }
        }

        @Override
        public int run(CommandContext<CommandSource> commandContext) {
            Component statusTextComponent;
            if (llsManager.config.getWhitelist()) {
                statusTextComponent = Component.translatable("lls-manager.command.lls_whitelist.on").color(NamedTextColor.GREEN);
            } else {
                statusTextComponent = Component.translatable("lls-manager.command.lls_whitelist.off").color(NamedTextColor.RED);
            }
            commandContext.getSource().sendMessage(Component.translatable("lls-manager.command.lls_whitelist.status.info")
                    .args(statusTextComponent));
            return 1;
        }
    }

    @Singleton
    private static class HelpCommand implements Command {

        @Override
        public LiteralArgumentBuilder<CommandSource> createSubCommand() {
            return LiteralArgumentBuilder.<CommandSource>literal("help").executes(this);
        }

        @Override
        public int run(CommandContext<CommandSource> commandContext) {
            CommandSource source = commandContext.getSource();
            for (int i = 0; i < 6; ++i) {
                source.sendMessage(Component.translatable(String.format("lls-manager.command.lls_whitelist.hint%d", i)));
            }
            return 1;
        }
    }
}
