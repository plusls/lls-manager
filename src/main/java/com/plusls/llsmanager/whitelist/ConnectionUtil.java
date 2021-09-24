package com.plusls.llsmanager.whitelist;

import com.plusls.llsmanager.LlsManager;
import com.velocitypowered.api.proxy.InboundConnection;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.lang.reflect.Field;

public class ConnectionUtil {
    private static final Field INITIAL_MINECRAFT_CONNECTION;
    private static final Field CHANNEL;

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
            throw new IllegalStateException(e.getMessage());
        }
    }

    public static String getUsername(LlsManager llsManager, String eventUsername, InboundConnection inboundConnection) {
        String ret = eventUsername;
        // 在安装了 Floodgate 的情况下
        if (llsManager.hasFloodgate) {
            try {
                Object mcConnection = INITIAL_MINECRAFT_CONNECTION.get(inboundConnection);
                Channel channel = (Channel) CHANNEL.get(mcConnection);

                FloodgatePlayer player = (FloodgatePlayer) channel.attr(AttributeKey.valueOf("floodgate-player")).get();
                // 只考虑链接了的情况
                if (player != null && player.isLinked()) {
                    ret = player.getLinkedPlayer().getJavaUsername();
                }
            } catch (IllegalAccessException e) {
                // 正常不会走到这，反射时已经给过权限了
                e.printStackTrace();
                throw new IllegalStateException(e.getMessage());
            }
        }
        return ret;
    }
}
