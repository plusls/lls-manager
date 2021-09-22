package com.plusls.llsmanager.handler;

import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

public class ServerPostConnectEventHandler implements EventHandler<ServerPostConnectEvent> {
    private static LlsManager llsManager;

    public static void init(LlsManager llsManager) {
        llsManager.server.getEventManager().register(llsManager, ServerPostConnectEvent.class, new ServerPostConnectEventHandler());
        ServerPostConnectEventHandler.llsManager = llsManager;
    }

    @Override
    public void execute(ServerPostConnectEvent event) {
        Player player = event.getPlayer();
        LlsPlayer llsPlayer = llsManager.players.get(player.getRemoteAddress());

        if (llsPlayer.status == LlsPlayer.Status.NEED_LOGIN) {
            player.sendMessage(Component.translatable("lls-manager.command.lls_login.hint"));
        } else if (llsPlayer.status == LlsPlayer.Status.NEED_REGISTER) {
            player.sendMessage(Component.translatable("lls-manager.command.lls_register.hint"));
        }

    }
}