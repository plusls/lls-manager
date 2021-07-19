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
import java.util.ArrayList;
import java.util.List;

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
    @Override
    public void execute(PreLoginEvent event) {
        String username = event.getUsername();
        List<String> onlineUserList = new ArrayList<>();
        List<String> removeList = new ArrayList<>();
        for (RegisteredServer server : llsManager.server.getAllServers()) {
            for (Player player : server.getPlayersConnected()) {
                onlineUserList.add(player.getUsername());
            }
        }
        for (String llsPlayerName : llsManager.players.keySet()) {
            if (!onlineUserList.contains(llsPlayerName)) {
                removeList.add(llsPlayerName);
            }
        }
        for (String removeUsername : removeList) {
            llsManager.players.remove(removeUsername);
        }

        // 在安装了 Floodgate 的情况下
        if (llsManager.hasFloodgate) {
            try {
                Object mcConnection = INITIAL_MINECRAFT_CONNECTION.get(event.getConnection());
                Channel channel = (Channel) CHANNEL.get(mcConnection);

                FloodgatePlayer player = (FloodgatePlayer) channel.attr(AttributeKey.valueOf("floodgate-player")).get();
                // 只考虑链接了的情况
                if (player != null && player.isLinked()) {
                    username = player.getLinkedPlayer().getJavaUsername();
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
                llsManager.playerList.add(username);
            }
            llsManager.players.put(username, player);
        }
        if (!player.getOnlineMode()) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
        }
    }
}
