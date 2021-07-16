package com.plusls.llsmanager.data;

import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.plusls.llsmanager.LlsManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LlsWhitelist {
    private final Path path;
    private WhitelistData whitelistData = new WhitelistData();

    public LlsWhitelist(Path dataFolderPath) {
        path = dataFolderPath.resolve("lls-whitelist.json");
    }

    public static class WhitelistData {
        public ConcurrentLinkedQueue<String> whiteList = new ConcurrentLinkedQueue<>();
        public boolean status = false;
    }

    public boolean query(String username) {
        return whitelistData.whiteList.contains(username);
    }

    public boolean add(String username) {
        if (whitelistData.whiteList.add(username)) {
            return save();
        }
        LlsManager.logger().error("whiteList.add error.");
        return false;
    }

    public boolean getStatus() {
        return whitelistData.status;
    }

    public boolean setStatus(boolean status) {
        whitelistData.status = status;
        return save();
    }

    public boolean remove(String username) {
        if (whitelistData.whiteList.remove(username)) {
            return save();
        }
        LlsManager.logger().error("whiteList.remove error.");
        return false;
    }

    public ArrayList<String> search(String username) {
        ArrayList<String> ret = new ArrayList<>();
        for (String whiteListUsername: whitelistData.whiteList) {
            if (whiteListUsername.contains(username)) {
                ret.add(whiteListUsername);
            }
        }
        return ret;
    }

    private boolean save() {
        if (!Files.exists(path)) {
            try {
                Files.createFile(path);
            } catch (IOException e) {
                e.printStackTrace();
                LlsManager.logger().error("Save {} error: createFile fail.", path);
                return false;
            }
        }
        BufferedWriter bfw;
        try {
            bfw = Files.newBufferedWriter(path);
        } catch (IOException e) {
            e.printStackTrace();
            LlsManager.logger().error("Save {} error: newBufferedWriter fail.", path);
            return false;
        }

        try {
            bfw.write(LlsManager.GSON.toJson(whitelistData));
            bfw.close();
        } catch (IOException e) {
            e.printStackTrace();
            LlsManager.logger().error("Save {} error: bfw.write fail.", path);
            return false;
        }
        return true;
    }

    public boolean load() {
        if (!Files.exists(path)) {
            save();
        }
        try {
            BufferedReader bfr = Files.newBufferedReader(path);
            this.whitelistData = LlsManager.GSON.fromJson(bfr, WhitelistData.class);
            bfr.close();
        } catch (IOException e) {
            e.printStackTrace();
            LlsManager.logger().error("Load {} error: newBufferedReader fail.", path);
            return false;
        } catch (JsonParseException e) {
            e.printStackTrace();
            LlsManager.logger().error("Json {} parser fail!!", path);
            return false;
        }
        return true;
    }
}
