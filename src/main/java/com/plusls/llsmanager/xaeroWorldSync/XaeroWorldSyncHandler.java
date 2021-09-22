package com.plusls.llsmanager.xaeroWorldSync;

import com.google.inject.Singleton;
import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.tabListSync.MyPlayerListItem;
import com.plusls.llsmanager.tabListSync.TabListSyncHandler;
import com.plusls.llsmanager.util.PacketUtil;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.PlayerListItem;

@Singleton
public class XaeroWorldSyncHandler {
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

        PacketUtil.replacePacketHandler(toReplace, PlayerListItem.class, MyPlayerListItem.class, MyPlayerListItem::new);
        llsManager.server.getEventManager().register(llsManager, llsManager.injector.getInstance(TabListSyncHandler.class));
    }
}
