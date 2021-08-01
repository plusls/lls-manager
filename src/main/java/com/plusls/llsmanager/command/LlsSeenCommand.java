package com.plusls.llsmanager.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.plusls.llsmanager.util.CommandUtil;
import com.plusls.llsmanager.util.TextUtil;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.*;

public class LlsSeenCommand {

    private static LlsManager llsManager;

    public static void register(LlsManager llsManager) {
        LlsSeenCommand.llsManager = llsManager;
        llsManager.commandManager.register(createBrigadierCommand(llsManager));
    }

    private static BrigadierCommand createBrigadierCommand(LlsManager llsManager) {
        LiteralCommandNode<CommandSource> llsSeenNode = LiteralArgumentBuilder
                .<CommandSource>literal("lls_seen")
                .then(LiteralArgumentBuilder.<CommandSource>literal("query")
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
                                .executes(LlsSeenCommand::query)))
                .then(LiteralArgumentBuilder.<CommandSource>literal("list")
                        .then(RequiredArgumentBuilder.<CommandSource, Integer>argument("page", IntegerArgumentType.integer())
                                .executes(LlsSeenCommand::list))
                        .executes(LlsSeenCommand::listAll)
                )
                .build();
        return new BrigadierCommand(llsSeenNode);
    }

    static class SeenData implements Comparable<SeenData> {
        long time;
        Component text;
        Component listText;

        public SeenData(long time, Component text, Component listText) {
            this.time = time;
            this.text = text;
            this.listText = listText;
        }

        @Override
        public int compareTo(@NotNull SeenData o) {
            return Long.compare(time, o.time);
        }
    }

    private static int list(CommandContext<CommandSource> context) {
        CommandSource commandSource = context.getSource();
        int page = context.getArgument("page", Integer.class);
        List<SeenData> allSeenData = getAllSeenData(commandSource);
        int pageCount = allSeenData.size() / 10 + (allSeenData.size() % 10 != 0 ? 1 : 0);
        if (page < 0) {
            page = page + pageCount;
        }
        if (page > pageCount || page <= 0) {
            commandSource.sendMessage(Component.translatable("lls-manager.command.lls_seen.list.page_not_found", NamedTextColor.RED).args(Component.text(page)));
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

    private static int listAll(CommandContext<CommandSource> context) {
        CommandSource commandSource = context.getSource();
        List<SeenData> allSeenData = getAllSeenData(commandSource);
        commandSource.sendMessage(Component.translatable("lls-manager.command.lls_seen.list_all"));
        int index = 0;
        int prefixLen = (int) Math.log10(allSeenData.size()) + 3;
        for (SeenData seenData : allSeenData) {
            commandSource.sendMessage(Component.text(String.format("%-" + prefixLen + "s", String.format("%d. ", index + 1))).append(seenData.listText));
            ++index;
        }
        return 1;
    }

    private static List<SeenData> getAllSeenData(CommandSource commandSource) {
        List<SeenData> ret = new ArrayList<>();
        llsManager.playerSet.forEach(username -> getSeenData(username, commandSource).ifPresent(ret::add));
        ret.sort(Comparator.reverseOrder());
        return ret;
    }

    private static Optional<SeenData> getSeenData(String username, CommandSource commandSource) {
        LlsPlayer llsPlayer = CommandUtil.getLlsPlayer(username, commandSource);
        if (llsPlayer == null) {
            return Optional.empty();
        }
        Optional<Player> optionalPlayer = llsManager.server.getPlayer(username);
        Date currentDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (optionalPlayer.isPresent()) {
            Optional<ServerConnection> optionalRegisteredServer = optionalPlayer.get().getCurrentServer();
            if (optionalRegisteredServer.isPresent()) {
                TranslatableComponent onlineText = Component.translatable("lls-manager.command.lls_seen.online_text")
                        .args(TextUtil.getUsernameComponent(username),
                                TextUtil.getServerNameComponent(optionalRegisteredServer.get().getServerInfo().getName()));

                TextComponent onlineListText = Component.text().append(Component.text("- ")
                        .append(Component.text(sdf.format(currentDate.getTime()), NamedTextColor.YELLOW)))
                        .append(Component.text(" "))
                        .append(TextUtil.getUsernameComponent(username))
                        .build();

                return Optional.of(new SeenData(System.currentTimeMillis(), onlineText, onlineListText));
            }
            // 现在这个情况挺离谱的（我想不到触发方式）
        }
        // 玩家不在线的情况直接去查
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
        TranslatableComponent offlineText = Component.translatable("lls-manager.command.lls_seen.offline_text")
                .args(TextUtil.getUsernameComponent(username),
                        Component.text(sdf.format(llsPlayer.getLastSeenTime().getTime()), NamedTextColor.YELLOW), diffTextBuilder.build());

        TextComponent offlineListText = Component.text().append(Component.text("- ")
                .append(Component.text(sdf.format(llsPlayer.getLastSeenTime().getTime()), NamedTextColor.YELLOW)))
                .append(Component.text(" "))
                .append(TextUtil.getUsernameComponent(username))
                .build();

        return Optional.of(new SeenData(llsPlayer.getLastSeenTime().getTime(), offlineText, offlineListText));
    }

    private static int query(CommandContext<CommandSource> context) {
        CommandSource commandSource = context.getSource();
        String username = context.getArgument("username", String.class);
        Optional<SeenData> optionalSeenData = getSeenData(username, commandSource);

        if (optionalSeenData.isEmpty()) {
            return 0;
        } else {
            commandSource.sendMessage(optionalSeenData.get().text);
            return 1;
        }
    }
}