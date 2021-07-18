package com.plusls.llsmanager.handler;

import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.connection.PostLoginEvent;

import java.util.Objects;

public class PostLoginEventHandler implements EventHandler<PostLoginEvent> {
    private static LlsManager llsManager;

    public static void init(LlsManager llsManager) {
        llsManager.server.getEventManager().register(llsManager, PostLoginEvent.class, new PostLoginEventHandler());
        PostLoginEventHandler.llsManager = llsManager;
    }

    // TODO 支持盗版验证
    @Override
    public void execute(PostLoginEvent event) {
        LlsPlayer llsPlayer = Objects.requireNonNull(llsManager.players.get(event.getPlayer().getUsername()));
        llsPlayer.status = LlsPlayer.Status.LOGGED_IN;
    }
}
