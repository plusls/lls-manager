package com.plusls.llsmanager.seen;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.proxy.Player;

import java.util.Date;
import java.util.Objects;

@Singleton
public class SeenHandler {

    @Inject
    public LlsManager llsManager;

    public static void init(LlsManager llsManager) {
        llsManager.server.getEventManager().register(llsManager, llsManager.injector.getInstance(SeenHandler.class));
        llsManager.commandManager.register(llsManager.injector.getInstance(LlsSeenCommand.class).createBrigadierCommand());
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        String username = player.getUsername();
        LlsPlayer llsPlayer = llsManager.getLlsPlayer(player);
        // TODO 离开通知 API
        if (llsPlayer.status != LlsPlayer.Status.LOGGED_IN) {
            return;
        }
        if (!llsPlayer.setLastSeenTime(new Date())) {
            llsManager.logger.error("Can't save the last seen time of player {}", username);
        }
    }
}
