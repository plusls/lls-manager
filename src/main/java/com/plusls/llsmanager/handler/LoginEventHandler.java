package com.plusls.llsmanager.handler;

import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Objects;

public class LoginEventHandler implements EventHandler<LoginEvent> {
    private static LlsManager llsManager;

    public static void init(LlsManager llsManager) {
        llsManager.server.getEventManager().register(llsManager, LoginEvent.class, new LoginEventHandler());
        LoginEventHandler.llsManager = llsManager;
    }

    @Override
    public void execute(LoginEvent event) {
        Player player = event.getPlayer();
        LlsPlayer llsPlayer = Objects.requireNonNull(llsManager.preLoginPlayers.get(player.getRemoteAddress()));
        String username = player.getUsername();
        // 无法获取到 kick-existing-players，按 false 处理
        // TODO 等新的 API 出来根据 kick-existing-players 决定踢谁
        // 可能存在竞争，但是不想管了
        if (llsManager.onlinePlayers.get(username) != null) {
            player.disconnect(Component.translatable("velocity.error.already-connected-proxy", NamedTextColor.RED));
        }
        if (player.isOnlineMode()) {
            llsManager.preLoginPlayers.remove(player.getRemoteAddress());
            llsManager.onlinePlayers.put(username, llsPlayer);
            llsManager.playerSet.add(username);
        }
    }
}