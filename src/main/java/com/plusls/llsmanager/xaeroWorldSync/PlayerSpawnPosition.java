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
import java.util.Objects;
import java.util.zip.CRC32;

public class PlayerSpawnPosition implements MinecraftPacket {
    private long pos;
    private float angle;
    private static final Field serverConnField = ReflectUtil.getField(BackendPlaySessionHandler.class, "serverConn");

    @Inject
    LlsManager llsManager;

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
        CRC32 crc = new CRC32();
        crc.update(serverConn.getServerInfo().getName().getBytes());
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0);
        buf.writeInt((int) crc.getValue());
        byte [] array = buf.array();
        buf.release();
        serverConn.getPlayer().sendPluginMessage(MinecraftChannelIdentifier.create("xaeroworldmap", "main"), array);
        serverConn.getPlayer().sendPluginMessage(MinecraftChannelIdentifier.create("xaerominimap", "main"), array);
        return true;
    }
}
