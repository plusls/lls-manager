package com.plusls.llsmanager.offlineAuth;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.command.Command;
import com.plusls.llsmanager.data.LlsPlayer;
import com.plusls.llsmanager.util.CommandUtil;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.mindrot.jbcrypt.BCrypt;

@Singleton
public class LlsRegisterCommand implements Command {

    @Inject
    LlsManager llsManager;

    public BrigadierCommand createBrigadierCommand() {
        LiteralCommandNode<CommandSource> llsRegisterNode = createSubCommand().build();
        return new BrigadierCommand(llsRegisterNode);
    }

    @Override
    public LiteralArgumentBuilder<CommandSource> createSubCommand() {
        return LiteralArgumentBuilder
                .<CommandSource>literal("lls_register")
                .requires(commandSource -> commandSource instanceof Player player &&
                        llsManager.getLlsPlayer(player).status == LlsPlayer.Status.NEED_REGISTER)
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("password", StringArgumentType.string()).then(
                        RequiredArgumentBuilder.<CommandSource, String>argument("passwordConfirm", StringArgumentType.string())
                                .executes(this)));
    }

    @Override
    public int run(CommandContext<CommandSource> commandContext) {
        Player player = (Player) commandContext.getSource();

        LlsPlayer llsPlayer = llsManager.getLlsPlayer(player);
        String password = commandContext.getArgument("password", String.class);
        String passwordConfirm = commandContext.getArgument("passwordConfirm", String.class);
        if (!password.equals(passwordConfirm)) {
            commandContext.getSource().sendMessage(Component.translatable("lls-manager.command.lls_register.password_error", NamedTextColor.RED));
            return 0;
        }
        password = BCrypt.hashpw(password, BCrypt.gensalt());
        if (!llsPlayer.setPassword(password)) {
            CommandUtil.saveDataFail("password", player.getUsername(), player);
            return 0;
        } else {
            llsPlayer.status = LlsPlayer.Status.NEED_LOGIN;
            player.sendMessage(Component.translatable("lls-manager.command.lls_register.success", NamedTextColor.GREEN));
            player.sendMessage(Component.translatable("lls-manager.command.lls_login.hint"));
            return 1;
        }
    }
}
