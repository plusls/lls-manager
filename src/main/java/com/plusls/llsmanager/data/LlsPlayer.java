package com.plusls.llsmanager.data;

import java.nio.file.Files;
import java.nio.file.Path;


public class LlsPlayer extends AbstractConfig<LlsPlayer.LlsPlayerData> {

    // TODO last login time
    private LlsPlayerData data = new LlsPlayerData();
    public int count = 0;
    public Status status;

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
    }

    // TODO
    public void init() {
        this.status = Status.NEED_LOGIN;
        save();
    }

    public boolean hasUser() {
        return Files.exists(path);
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
}
