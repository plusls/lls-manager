package com.plusls.llsmanager.data;

import com.plusls.llsmanager.LlsManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListSet;


public class LlsPlayer extends AbstractConfig<LlsPlayer.LlsPlayerData> {

    private static final LlsManager llsManager = Objects.requireNonNull(LlsManager.getInstance());

    // TODO last login time
    private LlsPlayerData data = new LlsPlayerData();
    public Status status;
    public String username;
    public static final String SERVER = "server";
    public static final String SUB_SERVER = "sub_server";
    public static final String GLOBAL = "global";

    // 自动补全用
    public static List<String> channelList = Arrays.asList(SERVER, SUB_SERVER, GLOBAL);

    public enum Status {
        LOGGED_IN,
        NEED_LOGIN,
        NEED_REGISTER,
        OFFLINE
    }

    public LlsPlayer(String username, Path dataFolderPath) {
        super(dataFolderPath.resolve("player").resolve(username + ".json"), LlsPlayerData.class);
        status = Status.OFFLINE;
        this.username = username;
    }


    public static class LlsPlayerData {
        private boolean onlineMode = llsManager.config.getDefaultOnlineMode();
        private String password = "";
        private String lastServerName = "";
        private String channel = llsManager.config.getDefaultChannel();
        private Date lastSeenTime = new Date();
        private final ConcurrentSkipListSet<String> whitelistServerList = new ConcurrentSkipListSet<>();
    }

    public boolean hasUser() {
        return Files.exists(path);
    }

    public ConcurrentSkipListSet<String> getWhitelistServerList() {
        return data.whitelistServerList;
    }

    public Date getLastSeenTime() {
        return data.lastSeenTime;
    }

    public boolean setLastSeenTime(Date lastSeenTime) {
        data.lastSeenTime = lastSeenTime;
        return save();
    }

    public String getChannel() {
        return data.channel;
    }

    public boolean setChannel(String channel) {
        data.channel = channel;
        return save();
    }

    public boolean setPassword(String password) {
        this.data.password = password;
        return save();
    }

    public String getPassword() {
        return this.data.password;
    }

    public boolean setOnlineMode(boolean onlineMode) {
        this.data.onlineMode = onlineMode;
        return save();
    }

    public boolean getOnlineMode() {
        return this.data.onlineMode;
    }

    public boolean setLastServerName(String lastServerName) {
        this.data.lastServerName = lastServerName;
        return save();
    }

    public String getLastServerName() {
        return this.data.lastServerName;
    }

    @Override
    protected LlsPlayerData getData() {
        return data;
    }

    @Override
    protected void setData(LlsPlayerData data) {
        this.data = data;
    }

    @Override
    public boolean load() {
        boolean ret = super.load();
        if (ret && !LlsPlayer.channelList.contains(data.channel)) {
            String defaultChannel = llsManager.config.getDefaultChannel();
            LlsManager.logger().warn("The channel {} in {} is unregistered, switch to default channel {}",
                    data.channel, path.getFileName(), defaultChannel);
            if (!setChannel(defaultChannel)) {
                LlsManager.logger().error("{}: Can't set the channel to {}.", path.getFileName(), defaultChannel);
                return false;
            }
        }
        return ret;
    }
}
