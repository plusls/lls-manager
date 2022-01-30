package com.plusls.llsmanager.seen;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.command.Command;
import com.plusls.llsmanager.util.LoadPlayerFailException;
import com.plusls.llsmanager.util.PlayerNotFoundException;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Singleton
public class LlsSeenCommand {

    @Inject
    LlsManager llsManager;

    public BrigadierCommand createBrigadierCommand() {
        LiteralCommandNode<CommandSource> llsSeenNode = LiteralArgumentBuilder
                .<CommandSource>literal("lls_seen")
                .then(llsManager.injector.getInstance(HelpCommand.class).createSubCommand())
                .then(llsManager.injector.getInstance(ListCommand.class).createSubCommand())
                .then(llsManager.injector.getInstance(QueryCommand.class).createSubCommand())
                .build();
        return new BrigadierCommand(llsSeenNode);
    }

    @Singleton
    private static class QueryCommand implements Command {
        @Inject
        LlsManager llsManager;

        public LiteralArgumentBuilder<CommandSource> createSubCommand() {
            return LiteralArgumentBuilder.<CommandSource>literal("query")
                    .then(RequiredArgumentBuilder.<CommandSource, String>argument("username", StringArgumentType.string())
                            .suggests(this)
                            .executes(this));
        }

        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
            llsManager.playerSet.forEach(
                    username -> {
                        if (username.contains(builder.getRemaining())) {
                            builder.suggest(username);
                        }
                    }
            );
            return builder.buildFuture();
        }

        @Override
        public int run(CommandContext<CommandSource> context) {
            CommandSource commandSource = context.getSource();
            String username = context.getArgument("username", String.class);
            try {
                SeenData seenData = SeenUtil.getSeenData(llsManager, username);
                commandSource.sendMessage(seenData.text);
                return 1;
            } catch (LoadPlayerFailException | PlayerNotFoundException e) {
                commandSource.sendMessage(e.message);
                return 0;
            }
        }
    }

    @Singleton
    private static class ListCommand implements Command {
        @Inject
        LlsManager llsManager;

        @Override
        public LiteralArgumentBuilder<CommandSource> createSubCommand() {
            return LiteralArgumentBuilder.<CommandSource>literal("list")
                    .then(RequiredArgumentBuilder.<CommandSource, Integer>argument("page", IntegerArgumentType.integer())
                            .executes(this))
                    .executes(this::listAll);
        }

        @Override
        public int run(CommandContext<CommandSource> context) {
            CommandSource commandSource = context.getSource();
            int page = context.getArgument("page", Integer.class);
            List<SeenData> allSeenData;
            try {
                allSeenData = SeenUtil.getAllSeenData(llsManager);
            } catch (LoadPlayerFailException | PlayerNotFoundException e) {
                commandSource.sendMessage(e.message);
                return 0;
            }
            int pageCount = allSeenData.size() / 10 + (allSeenData.size() % 10 != 0 ? 1 : 0);
            if (page < 0) {
                page = page + pageCount;
            }
            if (page > pageCount || page <= 0) {
                commandSource.sendMessage(Component.translatable("lls-manager.command.lls_seen.list.page_not_found",
                        NamedTextColor.RED,
                        Component.text(page)));
                return 0;
            }
            int startIdx = (page - 1) * 10;
            int endIdx = Math.min(startIdx + 10, allSeenData.size());
            commandSource.sendMessage(Component.translatable("lls-manager.command.lls_seen.list").args(Component.text(startIdx + 1), Component.text(endIdx)));

            int prefixLen = (int) Math.log10(allSeenData.size()) + 3;
            int index = 0;
            for (SeenData seenData : allSeenData) {
                if (index >= endIdx) {
                    break;
                } else if (index >= startIdx) {
                    commandSource.sendMessage(Component.text(String.format("%-" + prefixLen + "s", String.format("%d. ", index + 1))).append(seenData.listText));
                }
                ++index;
            }
            TextComponent.Builder textComponentBuilder = Component.text();


            if (page == 1) {
                textComponentBuilder.append(Component.translatable("lls-manager.command.lls_seen.list.previous_page", NamedTextColor.GRAY));
            } else {
                textComponentBuilder.append(Component.translatable("lls-manager.command.lls_seen.list.previous_page", NamedTextColor.GOLD)
                        .clickEvent(ClickEvent.runCommand("/lls_seen list " + (page - 1))));
            }

            textComponentBuilder.append(Component.text(String.format(" (%d/%d) ", page, pageCount)));

            if (page == pageCount) {
                textComponentBuilder.append(Component.translatable("lls-manager.command.lls_seen.list.next_page", NamedTextColor.GRAY));
            } else {
                textComponentBuilder.append(Component.translatable("lls-manager.command.lls_seen.list.next_page", NamedTextColor.GOLD)
                        .clickEvent(ClickEvent.runCommand("/lls_seen list " + (page + 1))));
            }

            commandSource.sendMessage(textComponentBuilder.build());
            return 1;
        }

        public int listAll(CommandContext<CommandSource> context) {
            CommandSource commandSource = context.getSource();
            List<SeenData> allSeenData;
            try {
                allSeenData = SeenUtil.getAllSeenData(llsManager);
            } catch (LoadPlayerFailException | PlayerNotFoundException e) {
                commandSource.sendMessage(e.message);
                return 0;
            }
            commandSource.sendMessage(Component.translatable("lls-manager.command.lls_seen.list_all"));
            int index = 0;
            int prefixLen = (int) Math.log10(allSeenData.size()) + 3;
            for (SeenData seenData : allSeenData) {
                commandSource.sendMessage(Component.text(String.format("%-" + prefixLen + "s", String.format("%d. ", index + 1))).append(seenData.listText));
                ++index;
            }
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
            for (int i = 0; i < 3; ++i) {
                source.sendMessage(Component.translatable(String.format("lls-manager.command.lls_seen.hint%d", i)));
            }
            return 1;
        }
    }
}

