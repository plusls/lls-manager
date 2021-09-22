package com.plusls.llsmanager.tabListSync;

import com.plusls.llsmanager.LlsManager;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.PlayerListItem;
import io.netty.buffer.ByteBuf;

import java.util.Objects;

public class MyPlayerListItem extends PlayerListItem {
    @Override
    public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
        super.decode(buf, direction, version);
        LlsManager llsManager = Objects.requireNonNull(LlsManager.getInstance());
        if (llsManager.config.getShowAllPlayerInTabList()) {
            TabListUtil.updateTabList(llsManager, this);
        }
    }
}
