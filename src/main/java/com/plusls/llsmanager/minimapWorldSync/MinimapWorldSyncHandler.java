package com.plusls.llsmanager.minimapWorldSync;

import com.google.inject.Singleton;
import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.util.PacketUtil;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.proxy.protocol.StateRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;

@Singleton
public class MinimapWorldSyncHandler {

    private static final MinecraftChannelIdentifier VOXEL_CHANNEL = MinecraftChannelIdentifier.create("worldinfo", "world_id");

    public static void init(LlsManager llsManager) {
        StateRegistry.PacketRegistry toReplace = StateRegistry.PLAY.clientbound;
        PacketUtil.registerPacketHandler(toReplace, PlayerSpawnPosition.class, PlayerSpawnPosition::new,
                PacketUtil.map(0x5, ProtocolVersion.MINECRAFT_1_7_2, false),
                PacketUtil.map(0x43, ProtocolVersion.MINECRAFT_1_9, false),
                PacketUtil.map(0x45, ProtocolVersion.MINECRAFT_1_12, false),
                PacketUtil.map(0x46, ProtocolVersion.MINECRAFT_1_12_1, false),
                PacketUtil.map(0x49, ProtocolVersion.MINECRAFT_1_13, false),
                PacketUtil.map(0x4d, ProtocolVersion.MINECRAFT_1_14, false),
                PacketUtil.map(0x4e, ProtocolVersion.MINECRAFT_1_15, false),
                PacketUtil.map(0x42, ProtocolVersion.MINECRAFT_1_16, false),
                PacketUtil.map(0x4b, ProtocolVersion.MINECRAFT_1_17, false));
        llsManager.server.getEventManager().register(llsManager, llsManager.injector.getInstance(MinimapWorldSyncHandler.class));
        llsManager.server.getChannelRegistrar().register(VOXEL_CHANNEL);
    }

    @Subscribe
    public void onPluginMessageEvent(PluginMessageEvent pluginMessageEvent) {
        if (!LlsManager.getInstance().config.getMinimapWorldSync()) {
            return;
        }
        if (pluginMessageEvent.getSource() instanceof Player player && pluginMessageEvent.getIdentifier().equals(VOXEL_CHANNEL)) {
            player.getCurrentServer().ifPresent(
                    serverConnection -> {
                        byte[] serverName = serverConnection.getServerInfo().getName().getBytes(StandardCharsets.UTF_8);
                        ByteBuf voxelBuf = Unpooled.buffer();
                        voxelBuf.writeByte(0);
                        voxelBuf.writeByte(serverName.length);
                        voxelBuf.writeBytes(serverName);
                        byte[] voxelArray = voxelBuf.array();
                        voxelBuf.release();
                        player.sendPluginMessage(VOXEL_CHANNEL, voxelArray);
                    });
            pluginMessageEvent.setResult(PluginMessageEvent.ForwardResult.handled());
        }
    }
}
