package com.plusls.llsmanager.data;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;

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

    public ConcurrentHashMap<String, ConcurrentSkipListSet<String>> getServerGroup() {
        return config.serverGroup;
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

    public static class ConfigData {
        // ????????????????????? channel
        private final ConcurrentLinkedQueue<String> chatMessageChannelList = new ConcurrentLinkedQueue<>();
        // ???????????????????????????????????? channel
        private final ConcurrentLinkedQueue<String> bridgeMessageChannelList = new ConcurrentLinkedQueue<>();
        // ????????????????????? channel
        private final ConcurrentLinkedQueue<String> leaveMessageChannelList = new ConcurrentLinkedQueue<>();
        // ??????????????????????????? channel
        private final ConcurrentLinkedQueue<String> joinMessageChannelList = new ConcurrentLinkedQueue<>();
        // ????????????
        private final ConcurrentHashMap<String, ConcurrentSkipListSet<String>> serverGroup = new ConcurrentHashMap<>();
        // ????????? TabList ??????????????????
        private boolean showAllPlayerInTabList = false;
        // ????????????????????????
        private boolean bridgeChatMessage = false;
        // ??????????????????????????????
        private boolean bridgePlayerJoinMessage = false;
        // ??????????????????????????????
        private boolean bridgePlayerLeaveMessage = false;
        // ?????????
        private boolean whitelist = false;
        // ?????????????????????
        private boolean defaultOnlineMode = true;
        // ??????????????????
        private String defaultChannel = LlsPlayer.SERVER;
        // ???????????????????????????
        private String authServerName = "lls-auth";
        // ?????????????????????
        private boolean minimapWorldSync = false;


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
}
