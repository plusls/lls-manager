package com.plusls.llsmanager.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class LlsPlayerCommand {

    private static LlsManager llsManager;

    public static void register(LlsManager llsManager) {
        LlsPlayerCommand.llsManager = llsManager;
        llsManager.commandManager.register(createBrigadierCommand());
    }

    private static BrigadierCommand createBrigadierCommand() {
        LiteralCommandNode<CommandSource> llsRegisterNode = LiteralArgumentBuilder
                .<CommandSource>literal("lls_player").requires(commandSource -> commandSource.hasPermission("lls-manager.admin"))
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
                        .then(LiteralArgumentBuilder.<CommandSource>literal("setOnlineMode")
                                .then(
                                        RequiredArgumentBuilder.<CommandSource, Boolean>argument("status", BoolArgumentType.bool())
                                                .executes(LlsPlayerCommand::setOnlineMode)))
                        .then(LiteralArgumentBuilder.<CommandSource>literal("setChannel")
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("channel", StringArgumentType.string())
                                        .suggests(
                                                (context, builder) -> {
                                                    LlsPlayer.channelList.forEach(builder::suggest);
                                                    return builder.buildFuture();
                                                })
                                        .executes(LlsPlayerCommand::setChannel)))
                        .then(LiteralArgumentBuilder.<CommandSource>literal("resetPassword")
                                .executes(LlsPlayerCommand::resetPassword))).build();
        return new BrigadierCommand(llsRegisterNode);
    }


    private static int setChannel(CommandContext<CommandSource> context) {
        String username = context.getArgument("username", String.class);
        String channel = context.getArgument("channel", String.class);
        if (CommandUtil.setChannel(channel, username, context.getSource())) {
            return 1;
        } else {
            return 0;
        }
    }

    private static int resetPassword(CommandContext<CommandSource> context) {
        String username = context.getArgument("username", String.class);
        CommandSource commandSource = context.getSource();
        LlsPlayer llsPlayer = CommandUtil.getLlsPlayer(username, commandSource);
        if (llsPlayer == null) {
            return 0;
        }
        if (llsPlayer.setPassword("")) {
            commandSource.sendMessage(Component.translatable("lls-manager.command.lls_player.reset_password.success").args(TextUtil.getUsernameComponent(username)));
            return 1;
        } else {
            CommandUtil.saveDataFail("password", username, commandSource);
            return 0;
        }
    }


    private static int setOnlineMode(CommandContext<CommandSource> context) {
        String username = context.getArgument("username", String.class);
        CommandSource commandSource = context.getSource();
        LlsPlayer llsPlayer = CommandUtil.getLlsPlayer(username, commandSource);
        if (llsPlayer == null) {
            return 0;
        }
        Boolean onlineMode = context.getArgument("status", Boolean.class);
        if (llsPlayer.setOnlineMode(onlineMode)) {
            commandSource.sendMessage(Component.translatable("lls-manager.command.lls_player.success").args(TextUtil.getUsernameComponent(username),
                    Component.text("onlineMode", NamedTextColor.YELLOW),
                    Component.text(onlineMode, NamedTextColor.YELLOW)
            ));
            return 1;
        } else {
            CommandUtil.saveDataFail("onlineMode", username, commandSource);
            return 0;
        }
    }
}