package com.plusls.llsmanager.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.plusls.llsmanager.util.CommandUtil;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.mindrot.jbcrypt.BCrypt;

public class LlsPasswdCommand {

    private static LlsManager llsManager;

    public static void register(LlsManager llsManager) {
        LlsPasswdCommand.llsManager = llsManager;
        llsManager.commandManager.register(createBrigadierCommand());
    }

    private static BrigadierCommand createBrigadierCommand() {
        LiteralCommandNode<CommandSource> llsPasswdNode = LiteralArgumentBuilder
                .<CommandSource>literal("lls_passwd").requires(
                        commandSource -> commandSource instanceof Player player &&
                                !llsManager.players.get(player.getRemoteAddress()).getOnlineMode()
                )
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("password", StringArgumentType.string()).then(
                        RequiredArgumentBuilder.<CommandSource, String>argument("passwordConfirm", StringArgumentType.string())
                                .executes(LlsPasswdCommand::llsPasswd))).build();
        return new BrigadierCommand(llsPasswdNode);
    }

    private static int llsPasswd(CommandContext<CommandSource> context) {
        Player player = (Player) context.getSource();

        LlsPlayer llsPlayer = llsManager.players.get(player.getRemoteAddress());
        String password = context.getArgument("password", String.class);
        String passwordConfirm = context.getArgument("passwordConfirm", String.class);
        if (!password.equals(passwordConfirm)) {
            context.getSource().sendMessage(Component.translatable("lls-manager.command.lls_passwd.password_error", NamedTextColor.RED));
            return 0;
        }
        password = BCrypt.hashpw(password, BCrypt.gensalt());
        if (llsPlayer.setPassword(password)) {
            player.sendMessage(Component.translatable("lls-manager.command.lls_passwd.success", NamedTextColor.GREEN));
            return 1;
        } else {
            CommandUtil.saveDataFail("password", player.getUsername(), player);
            return 0;
        }
    }
}
