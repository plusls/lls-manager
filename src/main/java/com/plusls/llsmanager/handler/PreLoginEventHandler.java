package com.plusls.llsmanager.handler;

import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import net.kyori.adventure.text.Component;

public class PreLoginEventHandler implements EventHandler<PreLoginEvent> {
    private static LlsManager llsManager;

    public static void init(LlsManager llsManager) {
        llsManager.server.getEventManager().register(llsManager, PreLoginEvent.class, new PreLoginEventHandler());
        PreLoginEventHandler.llsManager = llsManager;
    }

    @Override
    public void execute(PreLoginEvent event) {
        String username = event.getUsername();
        if (llsManager.whitelist.getStatus() && !llsManager.whitelist.query(username)) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text("You are not in whitelist!")));
            return;
        }
        LlsPlayer player = llsManager.players.get(username);
        if (player == null) {
            player = new LlsPlayer(username, llsManager.dataFolderPath);
            if (player.hasUser()) {
                if (!player.load()) {
                    event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text("Load player data fail!")));
                    return;
                }
                player.status = LlsPlayer.Status.NEED_LOGIN;

            } else {
                // TODO 支持盗版验证
                player.init();
            }
            ++player.count;
            llsManager.players.put(username, player);
        } else {
            ++player.count;
        }
        if (!player.getOnlineMode()) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
        } else {
            event.setResult(PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
        }
    }
}
