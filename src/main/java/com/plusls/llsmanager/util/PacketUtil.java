package com.plusls.llsmanager.util;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import io.netty.util.collection.IntObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Supplier;

public class PacketUtil {

    private static final Field versionsField = ReflectUtil.getField(StateRegistry.PacketRegistry.class, "versions");
    private static final Field packetIdToSupplierField = ReflectUtil.getField(StateRegistry.PacketRegistry.ProtocolRegistry.class, "packetIdToSupplier");
    private static final Field packetClassToIdField = ReflectUtil.getField(StateRegistry.PacketRegistry.ProtocolRegistry.class, "packetClassToId");
    private static final Constructor<StateRegistry.PacketMapping> constructor;
    private static final Method registerMethod;

    static {
        try {
            constructor = StateRegistry.PacketMapping.class.getDeclaredConstructor(int.class, ProtocolVersion.class, ProtocolVersion.class, boolean.class);
            constructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage());
        }

        try {
            registerMethod = StateRegistry.PacketRegistry.class.getDeclaredMethod("register", Class.class, Supplier.class, StateRegistry.PacketMapping[].class);
            registerMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage());

        }
    }

    @SuppressWarnings("unchecked")
    public static void replacePacketHandler(StateRegistry.PacketRegistry toReplace,
                                            Class<? extends MinecraftPacket> oldClazz,
                                            Class<? extends MinecraftPacket> newClazz,
                                            Supplier<? extends MinecraftPacket> newSupplier) {
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

    public static void registerPacketHandler(StateRegistry.PacketRegistry toReplace,
                                             Class<? extends MinecraftPacket> newClazz,
                                             Supplier<? extends MinecraftPacket> newSupplier,
                                             StateRegistry.PacketMapping... mappings) {
        try {
            registerMethod.invoke(toReplace, newClazz, newSupplier, mappings);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    public static StateRegistry.PacketMapping map(int id, ProtocolVersion version, boolean encodeOnly) {
        return map(id, version, null, encodeOnly);
    }


    public static StateRegistry.PacketMapping map(int id, ProtocolVersion version,
                                                  ProtocolVersion lastValidProtocolVersion, boolean encodeOnly) {
        try {
            return constructor.newInstance(id, version, lastValidProtocolVersion, encodeOnly);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage());
        }
    }
}
