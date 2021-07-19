package com.plusls.llsmanager.handler;

import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PreLoginEventHandler implements EventHandler<PreLoginEvent> {
    private static LlsManager llsManager;
    private static Field INITIAL_MINECRAFT_CONNECTION;
    private static Field CHANNEL;

    static {
        try {
            Class<?> initialConnection = Class.forName("com.velocitypowered.proxy.connection.client.InitialInboundConnection");
            Class<?> minecraftConnection = Class.forName("com.velocitypowered.proxy.connection.MinecraftConnection");
            INITIAL_MINECRAFT_CONNECTION = initialConnection.getDeclaredField("connection");
            CHANNEL = minecraftConnection.getDeclaredField("channel");
            INITIAL_MINECRAFT_CONNECTION.setAccessible(true);
            CHANNEL.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public static void init(LlsManager llsManager) {
        llsManager.server.getEventManager().register(llsManager, PreLoginEvent.class, new PreLoginEventHandler());
        PreLoginEventHandler.llsManager = llsManager;
    }

    // 在还未登陆时用户语言并没有初始化，因此显示的都会是英文，除非用客户端自带的 key
    // 在 PreLoginEvent 阶段可能会出现用户重复登陆的情况，其它阶段则不会
    @Override
    public void execute(PreLoginEvent event) {
        String username = event.getUsername();
        List<InetSocketAddress> onlineUserList = new ArrayList<>();
        List<InetSocketAddress> removeList = new ArrayList<>();
        llsManager.server.getAllPlayers().forEach(player -> onlineUserList.add(player.getRemoteAddress()));
        llsManager.preLoginPlayers.forEach((key, value) -> {
            if (!onlineUserList.contains(key)) {
                removeList.add(key);
            }
        });
        removeList.forEach(llsManager.preLoginPlayers::remove);

        boolean isFloodgateUser = false;
        // 在安装了 Floodgate 的情况下
        if (llsManager.hasFloodgate) {
            try {
                Object mcConnection = INITIAL_MINECRAFT_CONNECTION.get(event.getConnection());
                Channel channel = (Channel) CHANNEL.get(mcConnection);

                FloodgatePlayer player = (FloodgatePlayer) channel.attr(AttributeKey.valueOf("floodgate-player")).get();
                // 只考虑链接了的情况
                if (player != null && player.isLinked()) {
                    username = player.getLinkedPlayer().getJavaUsername();
                    isFloodgateUser = true;
                }
            } catch (IllegalAccessException e) {
                // 正常不会走到这，反射时已经给过权限了
                e.printStackTrace();
            }
        }

        if (llsManager.config.getWhitelist() && !llsManager.whitelist.query(username)) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.translatable("multiplayer.disconnect.not_whitelisted").color(NamedTextColor.RED)));
            return;
        }

        LlsPlayer llsPlayer = llsManager.onlinePlayers.get(username);
        if (llsPlayer == null) {
            llsPlayer = new LlsPlayer(username, llsManager.dataFolderPath);
            if (llsPlayer.hasUser()) {
                if (!llsPlayer.load()) {
                    event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text("Load player data fail!")));
                    return;
                }
                if (llsPlayer.getOnlineMode()) {
                    llsPlayer.status = LlsPlayer.Status.ONLINE_USER;
                } else if (llsPlayer.getPassword().equals("")) {
                    llsPlayer.status = LlsPlayer.Status.NEED_REGISTER;
                } else {
                    llsPlayer.status = LlsPlayer.Status.NEED_LOGIN;
                }
            } else {
                if (llsManager.config.getDefaultOnlineMode()) {
                    llsPlayer.status = LlsPlayer.Status.ONLINE_USER;
                } else {
                    llsPlayer.status = LlsPlayer.Status.NEED_REGISTER;
                }
                // 在登陆服务器后 ServerConnectedEvent 时会保存最后登陆的服务器，会将用户配置写入 json
                // 因此在这无需保存用户信息
                // 主要是考虑到开启盗版验证并且不开白名单的情况下，可能会有很多垃圾用户连接，产生很多垃圾数据
                // 因此设置为登陆成功后或者注册成功才能留下用户数据
            }
            llsManager.preLoginPlayers.put(event.getConnection().getRemoteAddress(), llsPlayer);
        }
        // 在这里不处理同一个帐号多次登陆服务器的情况，选择在 LoginEvent 里处理
        if (!llsPlayer.getOnlineMode()) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
        } else if (!isFloodgateUser) {
            // Floodgate 会强制使用盗版验证
            event.setResult(PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
        }
    }
}
