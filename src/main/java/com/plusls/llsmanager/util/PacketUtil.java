package com.plusls.llsmanager.util;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import io.netty.util.collection.IntObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Supplier;

public class PacketUtil {

    @SuppressWarnings(value = "unchecked")
    public static void replacePacketHandler(StateRegistry.PacketRegistry toReplace,
                                            Class<? extends MinecraftPacket> oldClazz,
                                            Class<? extends MinecraftPacket> newClazz,
                                            Supplier<? extends MinecraftPacket> newSupplier) {
        Field versionsField = ReflectUtil.getField(StateRegistry.PacketRegistry.class, "versions");
        Field packetIdToSupplierField = ReflectUtil.getField(StateRegistry.PacketRegistry.ProtocolRegistry.class, "packetIdToSupplier");
        Field packetClassToIdField = ReflectUtil.getField(StateRegistry.PacketRegistry.ProtocolRegistry.class, "packetClassToId");
        if (versionsField == null || packetIdToSupplierField == null || packetClassToIdField == null) {
            return;
        }
        Map<ProtocolVersion, StateRegistry.PacketRegistry.ProtocolRegistry> versions;
        try {
            versions = (Map<ProtocolVersion, StateRegistry.PacketRegistry.ProtocolRegistry>) versionsField.get(toReplace);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return;
        }
        for (StateRegistry.PacketRegistry.ProtocolRegistry protocolRegistry : versions.values()) {
            IntObjectMap<Supplier<? extends MinecraftPacket>> packetIdToSupplier;
            Object2IntMap<Class<? extends MinecraftPacket>> packetClassToId;

            try {
                packetIdToSupplier = (IntObjectMap<Supplier<? extends MinecraftPacket>>) packetIdToSupplierField.get(protocolRegistry);
                packetClassToId = (Object2IntMap<Class<? extends MinecraftPacket>>) packetClassToIdField.get(protocolRegistry);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return;
            }

            int packetId = -1;
            for (Object2IntMap.Entry<Class<? extends MinecraftPacket>> entry : packetClassToId.object2IntEntrySet()) {
                if (oldClazz.isAssignableFrom(entry.getKey())) {
                    packetId = entry.getIntValue();
                    break;
                }
            }
            if (packetId != -1) {
                packetIdToSupplier.put(packetId, newSupplier);
                packetClassToId.put(newClazz, packetId);
            }
        }

    }
}
