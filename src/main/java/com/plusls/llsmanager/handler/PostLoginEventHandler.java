package com.plusls.llsmanager.handler;

import com.plusls.llsmanager.LlsManager;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.connection.PostLoginEvent;

public class PostLoginEventHandler implements EventHandler<PostLoginEvent> {
    private static LlsManager llsManager;

    public static void init(LlsManager llsManager) {
        llsManager.server.getEventManager().register(llsManager, PostLoginEvent.class, new PostLoginEventHandler());
        PostLoginEventHandler.llsManager = llsManager;
    }

    @Override
    public void execute(PostLoginEvent event) {
    }
}
