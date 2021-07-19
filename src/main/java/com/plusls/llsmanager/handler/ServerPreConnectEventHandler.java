package com.plusls.llsmanager.handler;

import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import net.kyori.adventure.text.Component;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Objects;

public class ServerPreConnectEventHandler implements EventHandler<ServerPreConnectEvent> {
    private static LlsManager llsManager;

    public static void init(LlsManager llsManager) {
        llsManager.server.getEventManager().register(llsManager, ServerPreConnectEvent.class, new ServerPreConnectEventHandler());
        ServerPreConnectEventHandler.llsManager = llsManager;
    }

    @Override
    public void execute(ServerPreConnectEvent event) {
        // 防止未登陆的用户逃离登陆服
        LlsPlayer llsPlayer = Objects.requireNonNull(llsManager.players.get(event.getPlayer().getUsername()));
        if (!llsPlayer.getOnlineMode() && llsPlayer.status != LlsPlayer.Status.LOGGED_IN) {
            event.getPlayer().disconnect(Component.text("Can't found auth server. Please contact server admin."));
        }
        llsManager.logger.info("{}", BCrypt.checkpw("123", "456"));
    }
}