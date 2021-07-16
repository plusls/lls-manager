package com.plusls.llsmanager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.plusls.llsmanager.command.LlsWhitelistCommand;
import com.plusls.llsmanager.data.LlsPlayer;
import com.plusls.llsmanager.data.LlsWhitelist;
import com.plusls.llsmanager.handler.*;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Plugin(
        id = "lls-manager",
        name = "LlsManager",
        version = "@version@",
        description = "Lls Manager",
        url = "https://github/plusls/lls-manager",
        authors = {"plusls"}
)
public class LlsManager {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Inject
    public Logger logger;

    @Inject
    public ProxyServer server;

    @Inject
    @DataDirectory
    public Path dataFolderPath;

    @Inject
    public CommandManager commandManager;

    private static LlsManager instance;

    public final Map<String, LlsPlayer> players = new ConcurrentHashMap<>();

    public LlsWhitelist whitelist;

    @Nullable
    public static LlsManager getInstance() {
        return instance;
    }

    public static Logger logger() {
        return Objects.requireNonNull(getInstance()).logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        Path playerDataDirPath = dataFolderPath.resolve("player");
        if (!Files.exists(playerDataDirPath)) {
            try {
                Files.createDirectories(playerDataDirPath);
            } catch (IOException e) {
                e.printStackTrace();
                LlsManager.logger().error("Lls-Manager load fail! createDirectories {} fail!!", playerDataDirPath);
                throw new RuntimeException("Lls-Manager init fail.");
            }
        } else if (!Files.isDirectory(playerDataDirPath)) {
            logger.error("Lls-Manager load fail! {} is not a directory!", playerDataDirPath);
            throw new RuntimeException("Lls-Manager init fail.");
        }

        whitelist = new LlsWhitelist(dataFolderPath);
        if (!whitelist.load()) {
            logger.error("Lls-Manager load whitelist fail!");
            throw new RuntimeException("Lls-Manager init fail.");
        }

        instance = this;
        PlayerChooseInitialServerEventHandler.init(this);
        ServerConnectedEventHandler.init(this);
        PlayerChatEventHandler.init(this);
        PreLoginEventHandler.init(this);
        PostLoginEventHandler.init(this);
        ProxyReloadEventHandle.init(this);
        DisconnectEventHandler.init(this);
        LlsWhitelistCommand.register(this);
        logger.info("Lls-Manager load success!");
    }
}
