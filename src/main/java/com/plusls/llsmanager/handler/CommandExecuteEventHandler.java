package com.plusls.llsmanager.handler;

import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.proxy.Player;

public class CommandExecuteEventHandler implements EventHandler<CommandExecuteEvent> {
    private static LlsManager llsManager;

    public static void init(LlsManager llsManager) {
        llsManager.server.getEventManager().register(llsManager, CommandExecuteEvent.class, new CommandExecuteEventHandler());
        CommandExecuteEventHandler.llsManager = llsManager;
    }

    @Override
    public void execute(CommandExecuteEvent event) {
        if (event.getCommandSource() instanceof Player player) {
            LlsPlayer.Status status = llsManager.players.get(player.getRemoteAddress()).status;
            String commandName = event.getCommand().split(" ")[0];
            if (status != LlsPlayer.Status.LOGGED_IN) {
                if (!(status == LlsPlayer.Status.NEED_REGISTER && commandName.equals("lls_register")) &&
                        !(status == LlsPlayer.Status.NEED_LOGIN && commandName.equals("lls_login"))) {
                    event.setResult(CommandExecuteEvent.CommandResult.denied());
                }
            }
        }
    }
}