package com.plusls.llsmanager.serverGroup;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.command.Command;
import com.plusls.llsmanager.util.TextUtil;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;

@Singleton
public class LlsServerGroupCommand {
    @Inject
    LlsManager llsManager;

    public BrigadierCommand createBrigadierCommand() {
        LiteralCommandNode<CommandSource> llsSeenNode = LiteralArgumentBuilder
                .<CommandSource>literal("lls_server_group").requires(commandSource -> commandSource.hasPermission("lls-manager.admin"))
                .then(llsManager.injector.getInstance(AddCommand.class).createSubCommand())
                .then(llsManager.injector.getInstance(ListCommand.class).createSubCommand())
                .then(llsManager.injector.getInstance(QueryCommand.class).createSubCommand())
                .then(llsManager.injector.getInstance(RemoveCommand.class).createSubCommand())
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
                    .then(RequiredArgumentBuilder.<CommandSource, String>argument("serverGroupName", StringArgumentType.string())
                            .suggests(this::getServerGroupNameSuggestions)
                            .then(RequiredArgumentBuilder.<CommandSource, String>argument("serverName", StringArgumentType.string())
                                    .suggests(this::getServerNameSuggestions)
                                    .executes(this)));
        }

        public CompletableFuture<Suggestions> getServerGroupNameSuggestions(final CommandContext<CommandSource> context, final SuggestionsBuilder builder) {
            llsManager.config.getServerGroup().keySet().forEach(
                    serverGroup -> {
                        if (serverGroup.contains(builder.getRemaining())) {
                            builder.suggest(serverGroup);
                        }
                    }
            );
            return builder.buildFuture();
        }

        public CompletableFuture<Suggestions> getServerNameSuggestions(final CommandContext<CommandSource> context, final SuggestionsBuilder builder) {
            String serverGroupName = context.getArgument("serverGroupName", String.class);
            llsManager.server.getAllServers().forEach(
                    registeredServer -> {
                        String serverName = registeredServer.getServerInfo().getName();
                        ConcurrentSkipListSet<String> serverGroup = llsManager.config.getServerGroup().get(serverGroupName);
                        if (serverName.contains(builder.getRemaining()) && (serverGroup == null || !serverGroup.contains(serverName))) {
                            builder.suggest(serverName);
                        }
                    }
            );
            return builder.buildFuture();
        }

        @Override
        public synchronized int run(CommandContext<CommandSource> commandContext) {
            String serverGroupName = commandContext.getArgument("serverGroupName", String.class);
            String serverName = commandContext.getArgument("serverName", String.class);
            CommandSource source = commandContext.getSource();
            ConcurrentSkipListSet<String> serverGroup = llsManager.config.getServerGroup().get(serverGroupName);
            if (serverGroup == null) {
                serverGroup = new ConcurrentSkipListSet<>();
                llsManager.config.getServerGroup().put(serverGroupName, serverGroup);
            }
            if (!serverGroup.add(serverName)) {
                source.sendMessage(Component.translatable("lls-manager.command.lls_server_group.add.already_in_server_group",
                        NamedTextColor.RED,
                        TextUtil.getServerNameComponent(serverName),
                        TextUtil.getServerGroupComponent(serverGroupName)));
                return 0;
            }

            if (!llsManager.config.save()) {
                source.sendMessage(Component.translatable("lls-manager.command.lls_server_group.add.failure",
                        NamedTextColor.RED,
                        TextUtil.getServerNameComponent(serverName),
                        TextUtil.getServerGroupComponent(serverGroupName)));
                return 0;
            }

            source.sendMessage(Component.translatable("lls-manager.command.lls_server_group.add.success",
                    NamedTextColor.GREEN,
                    TextUtil.getServerNameComponent(serverName),
                    TextUtil.getServerGroupComponent(serverGroupName)));

            return 1;
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
            llsManager.config.getServerGroup().forEach(
                    (serverGroupName, serverGroup) -> {
                        source.sendMessage(TextUtil.getServerGroupComponent(serverGroupName)
                                .append(Component.text(":")));
                        for (String serverName : serverGroup) {
                            source.sendMessage(Component.text("    -")
                                    .append(TextUtil.getServerNameComponent(serverName)));
                        }
                    }
            );
            return 0;
        }
    }

    @Singleton
    private static class QueryCommand implements Command {

        @Inject
        LlsManager llsManager;

        @Override
        public LiteralArgumentBuilder<CommandSource> createSubCommand() {
            return LiteralArgumentBuilder.<CommandSource>literal("query")
                    .then(RequiredArgumentBuilder.<CommandSource, String>argument("serverGroupName", StringArgumentType.string())
                            .suggests(this)
                            .executes(this));
        }

        @Override
        public CompletableFuture<Suggestions> getSuggestions(final CommandContext<CommandSource> context, final SuggestionsBuilder builder) {
            llsManager.config.getServerGroup().keySet().forEach(
                    serverGroupName -> {
                        if (serverGroupName.contains(builder.getRemaining())) {
                            builder.suggest(serverGroupName);
                        }
                    }
            );
            return builder.buildFuture();
        }

        @Override
        public int run(CommandContext<CommandSource> commandContext) {
            CommandSource source = commandContext.getSource();

            String serverGroupName = commandContext.getArgument("serverGroupName", String.class);
            ConcurrentSkipListSet<String> serverGroup = llsManager.config.getServerGroup().get(serverGroupName);
            if (serverGroup == null) {
                source.sendMessage(Component.translatable("lls-manager.command.lls_server_group.server_group_not_found",
                        NamedTextColor.RED,
                        TextUtil.getServerGroupComponent(serverGroupName)));
                return 0;
            }

            source.sendMessage(TextUtil.getServerGroupComponent(serverGroupName)
                    .append(Component.text(":")));
            for (String serverName : serverGroup) {
                source.sendMessage(Component.text("    -")
                        .append(TextUtil.getServerNameComponent(serverName)));
            }
            return 1;
        }
    }

    @Singleton
    private static class RemoveCommand implements Command {

        @Inject
        LlsManager llsManager;

        @Override
        public LiteralArgumentBuilder<CommandSource> createSubCommand() {
            return LiteralArgumentBuilder.<CommandSource>literal("remove")
                    .then(RequiredArgumentBuilder.<CommandSource, String>argument("serverGroupName", StringArgumentType.string())
                            .suggests(this::getServerGroupNameSuggestions)
                            .then(RequiredArgumentBuilder.<CommandSource, String>argument("serverName", StringArgumentType.string())
                                    .suggests(this::getServerNameSuggestions)
                                    .executes(this)));
        }

        public CompletableFuture<Suggestions> getServerGroupNameSuggestions(final CommandContext<CommandSource> context, final SuggestionsBuilder builder) {
            llsManager.config.getServerGroup().keySet().forEach(
                    serverGroup -> {
                        if (serverGroup.contains(builder.getRemaining())) {
                            builder.suggest(serverGroup);
                        }
                    }
            );
            return builder.buildFuture();
        }

        public CompletableFuture<Suggestions> getServerNameSuggestions(final CommandContext<CommandSource> context, final SuggestionsBuilder builder) {
            String serverGroupName = context.getArgument("serverGroupName", String.class);
            ConcurrentSkipListSet<String> serverGroup = llsManager.config.getServerGroup().get(serverGroupName);
            if (serverGroup != null) {
                for (String s : serverGroup) {
                    if (s.contains(builder.getRemaining())) {
                        builder.suggest(s);
                    }
                }
            }
            return builder.buildFuture();
        }

        @Override
        public synchronized int run(CommandContext<CommandSource> commandContext) {
            String serverGroupName = commandContext.getArgument("serverGroupName", String.class);
            String serverName = commandContext.getArgument("serverName", String.class);
            CommandSource source = commandContext.getSource();
            ConcurrentSkipListSet<String> serverGroup = llsManager.config.getServerGroup().get(serverGroupName);
            if (serverGroup == null) {
                source.sendMessage(Component.translatable("lls-manager.command.lls_server_group.server_group_not_found",
                        NamedTextColor.RED,
                        TextUtil.getServerGroupComponent(serverGroupName)));
                return 0;
            }
            if (!serverGroup.remove(serverName)) {
                source.sendMessage(Component.translatable("lls-manager.command.lls_server_group.remove.not_in_server_group",
                        NamedTextColor.RED,
                        TextUtil.getServerNameComponent(serverName),
                        TextUtil.getServerGroupComponent(serverGroupName)));
                return 0;
            }

            if (serverGroup.isEmpty()) {
                llsManager.config.getServerGroup().remove(serverGroupName);
            }

            if (!llsManager.config.save()) {
                source.sendMessage(Component.translatable("lls-manager.command.lls_server_group.remove.failure",
                        NamedTextColor.RED,
                        TextUtil.getServerNameComponent(serverName),
                        TextUtil.getServerGroupComponent(serverGroupName)));
                return 0;
            }

            source.sendMessage(Component.translatable("lls-manager.command.lls_server_group.remove.success",
                    NamedTextColor.GREEN,
                    TextUtil.getServerNameComponent(serverName),
                    TextUtil.getServerGroupComponent(serverGroupName)));

            return 1;
        }
    }
}
