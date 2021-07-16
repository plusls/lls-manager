package com.plusls.llsmanager.data;

import com.google.gson.JsonParseException;
import com.plusls.llsmanager.LlsManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class LlsPlayer {

    private final Path path;
    private String username;
    private LlsPlayerData data = new LlsPlayerData();
    public int count = 0;
    public Status status;

    public enum Status {
        LOGGED_IN,
        NEED_LOGIN,
        NEED_REGISTER
    }

    public LlsPlayer(String username, Path dataFolderPath) {
        path = dataFolderPath.resolve("player").resolve(username + ".json");
    }


    public static class LlsPlayerData {
        public boolean onlineMode;
        public String password;
        public String lastServerName;
    }

    // TODO
    public void init() {
        this.data.password = "";
        this.data.onlineMode = true;
        this.data.lastServerName = "";
        this.status = Status.NEED_LOGIN;
        save();
    }

    public boolean hasUser() {
        return Files.exists(path);
    }


    public void setPassword(String password) {
        this.data.password = password;
        save();
    }

    public String getPassword() {
        return this.data.password;
    }

    public void setOnlineMode(boolean onlineMode) {
        this.data.onlineMode = onlineMode;
        save();
    }

    public boolean getOnlineMode() {
        return this.data.onlineMode;
    }

    public void setLastServerName(String lastServerName) {
        this.data.lastServerName = lastServerName;
        save();
    }

    public String getLastServerName() {
        return this.data.lastServerName;
    }

    private void save() {
        if (!Files.exists(path)) {
            try {
                Files.createFile(path);
            } catch (IOException e) {
                e.printStackTrace();
                LlsManager.logger().error("Save {} error: createFile fail.", path);
                return;
            }
        }
        BufferedWriter bfw;
        try {
            bfw = Files.newBufferedWriter(path);
        } catch (IOException e) {
            e.printStackTrace();
            LlsManager.logger().error("Save {} error: newBufferedWriter fail.", path);
            return;
        }

        try {
            bfw.write(LlsManager.GSON.toJson(data));
            bfw.close();
        } catch (IOException e) {
            e.printStackTrace();
            LlsManager.logger().error("Save {} error: bfw.write fail.", path);
        }
    }

    public boolean load() {
        try {
            BufferedReader bfr = Files.newBufferedReader(path);
            this.data = LlsManager.GSON.fromJson(bfr, LlsPlayerData.class);
            bfr.close();
        } catch (IOException e) {
            e.printStackTrace();
            LlsManager.logger().error("Load {} error: newBufferedReader fail.", path);
            return false;
        } catch (JsonParseException e) {
            LlsManager.logger().error("Json {} parser fail!!", path);
            return false;
        }
        return true;
    }
}
