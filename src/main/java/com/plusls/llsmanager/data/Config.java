package com.plusls.llsmanager.data;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Config extends AbstractConfig<Config.ConfigData> {
    private ConfigData config = new ConfigData();

    public Config(Path dataFolderPath) {
        super(dataFolderPath.resolve("config.json"), ConfigData.class);
    }

    @Override
    protected ConfigData getData() {
        return config;
    }

    @Override
    protected void setData(ConfigData data) {
        config = data;
    }

    public static class ConfigData {
        // 是否在 TabList 显示子服玩家
        public boolean showAllPlayerInTabList = false;
        // 是否同步聊天信息
        public boolean bridgeChatMessage = false;
        // 是否同步玩家加入信息
        public boolean bridgePlayerJoinMessage = false;
        // 是否同步玩家离开信息
        public boolean bridgePlayerLeaveMessage = false;
        // 白名单
        public boolean whitelist = false;
        // 默认为在线模式
        public boolean defaultOnlineMode = true;
        // 默认聊天频道
        public String defaultChannel = LlsPlayer.SERVER;
        // 离线认证服务器名字
        public String authServerName = "lls-auth";
        // 接受聊天信息的 channel
        public ConcurrentLinkedQueue<String> chatMessageChannelList = new ConcurrentLinkedQueue<>();
        // 服务器内部互相传递信息的 channel
        public ConcurrentLinkedQueue<String> bridgeMessageChannelList = new ConcurrentLinkedQueue<>();
        // 接受离开信息的 channel
        public ConcurrentLinkedQueue<String> leaveMessageChannelList = new ConcurrentLinkedQueue<>();
        // 接受加入游戏信息的 channel
        public ConcurrentLinkedQueue<String> joinMessageChannelList = new ConcurrentLinkedQueue<>();
        // 启用小地图同步
        public boolean minimapWorldSync = false;

        public ConfigData() {
            chatMessageChannelList.add(LlsPlayer.SERVER);
            chatMessageChannelList.add(LlsPlayer.GLOBAL);
            bridgeMessageChannelList.add(LlsPlayer.SERVER);
            bridgeMessageChannelList.add(LlsPlayer.GLOBAL);
            leaveMessageChannelList.add(LlsPlayer.SERVER);
            leaveMessageChannelList.add(LlsPlayer.GLOBAL);
            joinMessageChannelList.add(LlsPlayer.SERVER);
            joinMessageChannelList.add(LlsPlayer.GLOBAL);
        }
    }

    public boolean getMinimapWorldSync() {
        return config.minimapWorldSync;
    }

    public boolean setMinimapWorldSync(boolean minimapWorldSync) {
        config.minimapWorldSync = minimapWorldSync;
        return save();
    }

    public boolean getShowAllPlayerInTabList() {
        return config.showAllPlayerInTabList;
    }

    public boolean setShowAllPlayerInTabList(boolean showAllPlayerInTabList) {
        config.showAllPlayerInTabList = showAllPlayerInTabList;
        return save();
    }

    public boolean getDefaultOnlineMode() {
        return config.defaultOnlineMode;
    }

    public boolean setDefaultOnlineMode(boolean defaultOnlineMode) {
        config.defaultOnlineMode = defaultOnlineMode;
        return save();
    }

    public String getAuthServerName() {
        return config.authServerName;
    }

    public boolean setAuthServerName(String authServerName) {
        config.authServerName = authServerName;
        return save();
    }

    public String getDefaultChannel() {
        return config.defaultChannel;
    }

    public boolean setDefaultChannel(String defaultChannel) {
        config.defaultChannel = defaultChannel;
        return save();
    }

    public boolean getBridgeChatMessage() {
        return config.bridgeChatMessage;
    }

    public boolean setBridgeChatMessage(boolean bridgeChatMessage) {
        config.bridgeChatMessage = bridgeChatMessage;
        return save();
    }

    public boolean getBridgePlayerJoinMessage() {
        return config.bridgePlayerJoinMessage;
    }

    public boolean setBridgePlayerJoinMessage(boolean bridgePlayerJoinMessage) {
        config.bridgePlayerJoinMessage = bridgePlayerJoinMessage;
        return save();
    }

    public boolean getBridgePlayerLeaveMessage() {
        return config.bridgePlayerLeaveMessage;
    }

    public boolean setBridgePlayerLeaveMessage(boolean bridgePlayerLeaveMessage) {
        config.bridgePlayerLeaveMessage = bridgePlayerLeaveMessage;
        return save();
    }

    public List<String> getChatMessageChannelList() {
        return List.copyOf(config.chatMessageChannelList);
    }

    public boolean addChatMessageChannel(String channel) {
        return config.chatMessageChannelList.add(channel) && save();
    }

    public List<String> getBridgeMessageChannel() {
        return List.copyOf(config.bridgeMessageChannelList);
    }

    public boolean addBridgeMessageChannel(String channel) {
        return config.bridgeMessageChannelList.add(channel) && save();
    }

    public List<String> getLeaveMessageChannelList() {
        return List.copyOf(config.leaveMessageChannelList);
    }

    public boolean addLeaveMessageChannelList(String channel) {
        return config.leaveMessageChannelList.add(channel) && save();
    }

    public List<String> getJoinMessageChannelList() {
        return List.copyOf(config.joinMessageChannelList);
    }

    public boolean addJoinMessageChannelList(String channel) {
        return config.joinMessageChannelList.add(channel) && save();
    }

    public boolean getWhitelist() {
        return config.whitelist;
    }

    public boolean setWhitelist(boolean status) {
        config.whitelist = status;
        return save();
    }
}
