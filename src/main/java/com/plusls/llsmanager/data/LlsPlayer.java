package com.plusls.llsmanager.data;

import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;


public class LlsPlayer extends AbstractConfig<LlsPlayer.LlsPlayerData> {

    // TODO last login time
    private LlsPlayerData data = new LlsPlayerData();
    public Status status;

    public static final String SERVER = "server";
    public static final String SUB_SERVER = "sub_server";
    public static final String GLOBAL = "global";

    // 自动补全用
    public static List<String> channelList = Arrays.asList(SERVER, SUB_SERVER, GLOBAL);

    public enum Status {
        LOGGED_IN,
        NEED_LOGIN,
        NEED_REGISTER
    }

    public LlsPlayer(String username, Path dataFolderPath) {
        super(dataFolderPath.resolve("player").resolve(username + ".json"), LlsPlayerData.class);
    }


    public static class LlsPlayerData {
        public boolean onlineMode = true;
        public String password = "";
        public String lastServerName = "";
        public String channel = Objects.requireNonNull(LlsManager.getInstance()).config.getDefaultChannel();
        public Date lastSeenTime = new Date();
    }

    // TODO
    public void init() {
        this.status = Status.NEED_LOGIN;
        save();
    }

    public boolean hasUser() {
        return Files.exists(path);
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
            String defaultChannel = Objects.requireNonNull(LlsManager.getInstance()).config.getDefaultChannel();
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
