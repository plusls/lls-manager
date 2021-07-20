package com.plusls.llsmanager.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.mindrot.jbcrypt.BCrypt;

public class LlsRegisterCommand {

    private static LlsManager llsManager;

    public static void register(LlsManager llsManager) {
        llsManager.commandManager.register(createBrigadierCommand(llsManager));
    }

    private static BrigadierCommand createBrigadierCommand(LlsManager llsManager) {
        LlsRegisterCommand.llsManager = llsManager;
        LiteralCommandNode<CommandSource> llsRegisterNode = LiteralArgumentBuilder
                .<CommandSource>literal("lls_register").requires(
                        commandSource -> commandSource instanceof Player player &&
                                llsManager.players.get(player.getRemoteAddress()).status == LlsPlayer.Status.NEED_REGISTER
                )
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("password", StringArgumentType.string()).then(
                        RequiredArgumentBuilder.<CommandSource, String>argument("passwordConfirm", StringArgumentType.string())
                                .executes(LlsRegisterCommand::llsRegister))).build();
        return new BrigadierCommand(llsRegisterNode);
    }

    private static int llsRegister(CommandContext<CommandSource> context) {
        Player player = (Player) context.getSource();

        LlsPlayer llsPlayer = llsManager.players.get(player.getRemoteAddress());
        String password = context.getArgument("password", String.class);
        String passwordConfirm = context.getArgument("passwordConfirm", String.class);
        if (!password.equals(passwordConfirm)) {
            context.getSource().sendMessage(Component.translatable("lls-manager.command.lls_register.password_error", NamedTextColor.RED));
            return 0;
        }
        password = BCrypt.hashpw(password, BCrypt.gensalt());
        llsPlayer.status = LlsPlayer.Status.NEED_LOGIN;
        LlsLoginCommand.register(llsManager);
        llsPlayer.setPassword(password);
        player.sendMessage(Component.translatable("lls-manager.command.lls_register.success", NamedTextColor.GREEN));
        player.sendMessage(Component.translatable("lls-manager.command.lls_login.hint"));
        return 1;
    }
}
