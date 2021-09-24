package com.plusls.llsmanager.whitelist;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.plusls.llsmanager.util.LlsManagerException;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@Singleton
public class WhitelistHandler {
    @Inject
    public LlsManager llsManager;

    public static void init(LlsManager llsManager) {
        llsManager.server.getEventManager().register(llsManager, llsManager.injector.getInstance(WhitelistHandler.class));
        llsManager.commandManager.register(llsManager.injector.getInstance(LlsWhitelistCommand.class).createBrigadierCommand());
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onPreLogin(PreLoginEvent event) {
        // 如果别的插件阻止了，那就不需要做检查了
        if (!event.getResult().isAllowed()) {
            return;
        }
        // 未开启白名单则不需要检查
        if (!llsManager.config.getWhitelist()) {
            return;
        }
        String username = ConnectionUtil.getUsername(llsManager, event.getUsername(), event.getConnection());

        // 如果没有这个用户，则说明它不在白名内，直接断开连接
        try {
            llsManager.getLlsPlayer(username);
        } catch (LlsManagerException e) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.translatable("multiplayer.disconnect.not_whitelisted").color(NamedTextColor.RED)));
        }
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onServerPreConnect(ServerPreConnectEvent event) {
        // 如果别的插件阻止了，那就不需要做检查了
        if (event.getResult().getServer().isEmpty()) {
            return;
        }
        // 未开启白名单则不需要检查
        if (!llsManager.config.getWhitelist()) {
            return;
        }
        String serverName = event.getResult().getServer().get().getServerInfo().getName();
        // 登陆服无需检查
        if (serverName.equals(llsManager.config.getAuthServerName())) {
            return;
        }
        Player player = event.getPlayer();

        LlsPlayer llsPlayer;
        try {
            llsPlayer = llsManager.getLlsPlayer(player.getUsername());
        } catch (LlsManagerException e) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            player.sendMessage(e.message);
            e.printStackTrace();
            return;
        }

        if (!llsPlayer.getWhitelistServerList().contains(serverName)) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            player.sendMessage(Component.translatable("multiplayer.disconnect.not_whitelisted").color(NamedTextColor.RED));
        }
    }

}
