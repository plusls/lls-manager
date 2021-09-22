package com.plusls.llsmanager.xaeroWorldSync;

import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.util.ReflectUtil;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.backend.BackendPlaySessionHandler;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.kyori.adventure.identity.Identity;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.zip.CRC32;

public class PlayerSpawnPosition implements MinecraftPacket {
    private long pos;
    private float angle;
    private static final Field serverConnField = ReflectUtil.getField(BackendPlaySessionHandler.class, "serverConn");

    @Override
    public void decode(ByteBuf byteBuf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
        pos = byteBuf.readLong();
        angle = byteBuf.readFloat();
    }

    @Override
    public void encode(ByteBuf byteBuf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
        byteBuf.writeLong(pos);
        byteBuf.writeFloat(angle);
    }

    @Override
    public boolean handle(MinecraftSessionHandler minecraftSessionHandler) {
        VelocityServerConnection serverConn;
        try {
            serverConn = (VelocityServerConnection) serverConnField.get(minecraftSessionHandler);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage());
        }
        serverConn.getPlayer().getConnection().write(this);
        if (!LlsManager.getInstance().config.getXaeroWorldSync()) {
            return true;
        }
        byte [] serverName = serverConn.getServerInfo().getName().getBytes(StandardCharsets.UTF_8);
        CRC32 crc = new CRC32();
        crc.update(serverName);
        ByteBuf xaeroBuf = Unpooled.buffer();
        xaeroBuf.writeByte(0);
        xaeroBuf.writeInt((int) crc.getValue());
        byte [] xaeroArray = xaeroBuf.array();
        xaeroBuf.release();
        serverConn.getPlayer().sendPluginMessage(MinecraftChannelIdentifier.create("xaeroworldmap", "main"), xaeroArray);
        serverConn.getPlayer().sendPluginMessage(MinecraftChannelIdentifier.create("xaerominimap", "main"), xaeroArray);

        ByteBuf voxelBuf = Unpooled.buffer();
        voxelBuf.writeByte(serverName.length);
        voxelBuf.writeBytes(serverName);
        byte [] voxelArray = voxelBuf.array();
        serverConn.getPlayer().sendPluginMessage(MinecraftChannelIdentifier.create("voxelmap", "world_id"), voxelArray);
        return true;
    }
}
