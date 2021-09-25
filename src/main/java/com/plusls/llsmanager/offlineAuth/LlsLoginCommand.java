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
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;

@Singleton
public class LlsLoginCommand implements Command {

    @Inject
    LlsManager llsManager;

    public BrigadierCommand createBrigadierCommand() {
        LiteralCommandNode<CommandSource> llsLoginNode = createSubCommand().build();
        return new BrigadierCommand(llsLoginNode);
    }

    @Override
    public LiteralArgumentBuilder<CommandSource> createSubCommand() {
        return LiteralArgumentBuilder
                .<CommandSource>literal("lls_login")
                .requires(commandSource -> commandSource instanceof Player player &&
                        llsManager.getLlsPlayer(player).status != LlsPlayer.Status.LOGGED_IN)
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("password", StringArgumentType.string())
                        .executes(this));
    }

    @Override
    public int run(CommandContext<CommandSource> commandContext) {
        Player player = (Player) commandContext.getSource();
        LlsPlayer llsPlayer = llsManager.getLlsPlayer(player);
        String password = commandContext.getArgument("password", String.class);
        if (!BCrypt.checkpw(password, llsPlayer.getPassword())) {
            player.sendMessage(Component.translatable("lls-manager.command.lls_login.password_error", NamedTextColor.RED));
            return 0;
        }
        llsPlayer.status = LlsPlayer.Status.LOGGED_IN;
        player.sendMessage(Component.translatable("lls-manager.command.lls_login.success", NamedTextColor.GREEN));
        Optional<RegisteredServer> registeredServerOptional = llsManager.server.getServer(llsPlayer.getLastServerName());
        registeredServerOptional.ifPresent(registeredServer -> player.createConnectionRequest(registeredServer).connect());
        return 1;
    }
}
