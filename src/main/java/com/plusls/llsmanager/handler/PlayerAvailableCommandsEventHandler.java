package com.plusls.llsmanager.handler;

import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.command.PlayerAvailableCommandsEvent;

public class PlayerAvailableCommandsEventHandler implements EventHandler<PlayerAvailableCommandsEvent> {
    private static LlsManager llsManager;

    public static void init(LlsManager llsManager) {
        llsManager.server.getEventManager().register(llsManager, PlayerAvailableCommandsEvent.class, new PlayerAvailableCommandsEventHandler());
        PlayerAvailableCommandsEventHandler.llsManager = llsManager;
    }

    @Override
    public void execute(PlayerAvailableCommandsEvent event) {
        LlsPlayer.Status status = llsManager.players.get(event.getPlayer().getRemoteAddress()).status;

        event.getRootNode().getChildren().removeIf(
                commandNode -> {
                    String commandName = commandNode.getName();
                    if (status != LlsPlayer.Status.LOGGED_IN) {
                        return !commandName.equals("lls_register") && !commandName.equals("lls_login") && !commandName.equals("server");
                    }
                    return false;
                }
        );
    }
}