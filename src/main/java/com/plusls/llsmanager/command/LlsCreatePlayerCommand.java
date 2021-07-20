package com.plusls.llsmanager.command;

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

public class LlsCreatePlayerCommand {

    private static LlsManager llsManager;

    public static void register(LlsManager llsManager) {
        LlsCreatePlayerCommand.llsManager = llsManager;
        llsManager.commandManager.register(createBrigadierCommand());
    }

    private static BrigadierCommand createBrigadierCommand() {
        LiteralCommandNode<CommandSource> llsCreatePlayerNode = LiteralArgumentBuilder
                .<CommandSource>literal("lls_create_player").requires(commandSource -> commandSource.hasPermission("lls-manager.admin"))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("username", StringArgumentType.string())
                        .executes(LlsCreatePlayerCommand::llsCreatePlayer)).build();
        return new BrigadierCommand(llsCreatePlayerNode);
    }

    private static int llsCreatePlayer(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        String username = context.getArgument("username", String.class);
        if (llsManager.playerSet.contains(username)) {
            source.sendMessage(Component.translatable("lls-manager.command.lls_create_player.already_exists", NamedTextColor.RED).args(TextUtil.getUsernameComponent(username)));
            return 0;
        }
        LlsPlayer llsPlayer = new LlsPlayer(username, llsManager.dataFolderPath);
        if (llsPlayer.save()) {
            llsManager.playerSet.add(username);
            source.sendMessage(Component.translatable("lls-manager.command.lls_create_player.success").args(TextUtil.getUsernameComponent(username)));
            return 1;
        } else {
            CommandUtil.saveDataFail("allData", username, source);
            return 0;
        }
    }
}
