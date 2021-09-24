package com.plusls.llsmanager.handler;

import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class PreLoginEventHandler implements EventHandler<PreLoginEvent> {

    private static LlsManager llsManager;

    public static void init(LlsManager llsManager) {
        llsManager.server.getEventManager().register(llsManager, PreLoginEvent.class, new PreLoginEventHandler());
        PreLoginEventHandler.llsManager = llsManager;
    }

    // 在还未登陆时用户语言并没有初始化，因此显示的都会是英文，除非用客户端自带的 key
    // 在 PreLoginEvent 阶段可能会出现用户重复登陆的情况，其它阶段则不会
    @Override
    public void execute(PreLoginEvent event) {
        // 如果别的插件阻止了，那就不需要做
        if (event.getResult() != PreLoginEvent.PreLoginComponentResult.allowed()){
            return;
        }
        String username = event.getUsername();
        List<InetSocketAddress> onlineUserList = new ArrayList<>();
        List<InetSocketAddress> removeList = new ArrayList<>();
        llsManager.server.getAllPlayers().forEach(player -> onlineUserList.add(player.getRemoteAddress()));
        llsManager.players.forEach((key, value) -> {
            if (!onlineUserList.contains(key)) {
                removeList.add(key);
            }
        });
        removeList.forEach(llsManager.players::remove);



        if (llsManager.config.getWhitelist() && !llsManager.whitelist.query(username)) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.translatable("multiplayer.disconnect.not_whitelisted").color(NamedTextColor.RED)));
            return;
        }

        LlsPlayer llsPlayer = new LlsPlayer(username, llsManager.dataFolderPath);
        if (llsPlayer.hasUser()) {
            if (!llsPlayer.load()) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text("Load player data fail!")));
                return;
            }
            if (llsPlayer.getOnlineMode()) {
                llsPlayer.status = LlsPlayer.Status.LOGGED_IN;
            } else if (llsPlayer.getPassword().equals("")) {
                llsPlayer.status = LlsPlayer.Status.NEED_REGISTER;
            } else {
                llsPlayer.status = LlsPlayer.Status.NEED_LOGIN;
            }
        } else {
            if (llsManager.config.getDefaultOnlineMode()) {
                llsPlayer.status = LlsPlayer.Status.LOGGED_IN;
            } else {
                llsPlayer.status = LlsPlayer.Status.NEED_REGISTER;
            }
            // 在登陆服务器后 ServerConnectedEvent 时会保存最后登陆的服务器，会将用户配置写入 json
            // 因此在这无需保存用户信息
            // 主要是考虑到开启盗版验证并且不开白名单的情况下，可能会有很多垃圾用户连接，产生很多垃圾数据
            // 因此设置为登陆成功后或者注册成功才能留下用户数据
        }
        llsManager.players.put(event.getConnection().getRemoteAddress(), llsPlayer);
        if (!llsPlayer.getOnlineMode()) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
        } else if (!isFloodgateUser) {
            // Floodgate 会强制使用盗版验证
            event.setResult(PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
        }
    }
}
