package com.plusls.llsmanager.whitelist;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.plusls.llsmanager.util.LoadPlayerFailException;
import com.plusls.llsmanager.util.PlayerNotFoundException;
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
        LlsPlayer llsPlayer;
        try {
            llsPlayer = llsManager.getLlsPlayer(username);
        } catch (LoadPlayerFailException | PlayerNotFoundException e) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.translatable("multiplayer.disconnect.not_whitelisted").color(NamedTextColor.RED)));
            return;
        }
        // 如果白名服务器列表为空也断开连接
        // 如果是离线认证玩家则还需要保证 auth server 在白名单内
        if (llsPlayer.getWhitelistServerList().isEmpty() || (!llsPlayer.getOnlineMode() && !llsPlayer.getWhitelistServerList().contains(llsManager.config.getAuthServerName()))) {
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

        Player player = event.getPlayer();

        LlsPlayer llsPlayer = llsManager.getLlsPlayer(player);

        if (!llsPlayer.getWhitelistServerList().contains(serverName)) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            player.sendMessage(Component.translatable("multiplayer.disconnect.not_whitelisted").color(NamedTextColor.RED));
            if (llsPlayer.getWhitelistServerList().isEmpty() || (!llsPlayer.getOnlineMode() && !llsPlayer.getWhitelistServerList().contains(llsManager.config.getAuthServerName()))) {
                player.disconnect(Component.translatable("multiplayer.disconnect.not_whitelisted").color(NamedTextColor.RED));
            }
        }
    }

}
