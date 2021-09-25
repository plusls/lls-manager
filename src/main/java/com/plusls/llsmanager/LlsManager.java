package com.plusls.llsmanager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.plusls.llsmanager.command.LlsChannelCommand;
import com.plusls.llsmanager.command.LlsCreatePlayerCommand;
import com.plusls.llsmanager.command.LlsPlayerCommand;
import com.plusls.llsmanager.data.Config;
import com.plusls.llsmanager.data.LlsPlayer;
import com.plusls.llsmanager.handler.DisconnectEventHandler;
import com.plusls.llsmanager.handler.PlayerChatEventHandler;
import com.plusls.llsmanager.handler.PlayerChooseInitialServerEventHandler;
import com.plusls.llsmanager.handler.ServerConnectedEventHandler;
import com.plusls.llsmanager.minimapWorldSync.MinimapWorldSyncHandler;
import com.plusls.llsmanager.offlineAuth.OfflineAuthHandler;
import com.plusls.llsmanager.seen.SeenHandler;
import com.plusls.llsmanager.tabListSync.TabListSyncHandler;
import com.plusls.llsmanager.util.LoadPlayerFailException;
import com.plusls.llsmanager.util.PlayerNotFoundException;
import com.plusls.llsmanager.util.TextUtil;
import com.plusls.llsmanager.whitelist.WhitelistHandler;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyReloadEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.util.UTF8ResourceBundleControl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

@Plugin(
        id = "lls-manager",
        name = "LlsManager",
        version = "@version@",
        description = "Lls Manager",
        url = "https://github/plusls/lls-manager",
        authors = {"plusls"},
        dependencies = {@Dependency(id = "floodgate", optional = true)}
)
@Singleton
public class LlsManager {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Inject
    public Logger logger;

    @Inject
    public ProxyServer server;

    @Inject
    public CommandManager commandManager;

    @Inject
    public EventManager eventManager;

    @Inject
    @DataDirectory
    public Path dataFolderPath;

    @Inject
    public Injector injector;

    @Nullable
    private static LlsManager instance;

    // 在线的玩家
    public final Map<InetSocketAddress, LlsPlayer> players = new ConcurrentHashMap<>();

    // 玩家缓存，不包含在线玩家
    public static final int PLAYER_CACHE_SIZE = 10;
    public final Map<String, LlsPlayer> playersCache = new ConcurrentHashMap<>();


    public Config config;

    // 所有玩家，包含不在线的玩家
    public ConcurrentSkipListSet<String> playerSet = new ConcurrentSkipListSet<>();

    @NotNull
    public static LlsManager getInstance() {
        return Objects.requireNonNull(instance);
    }

    public static Logger logger() {
        return getInstance().logger;
    }

    public boolean hasFloodgate = false;

    @Inject(optional = true)
    public void initFloodgate(@Named("floodgate") PluginContainer luckPermsContainer) {
        this.hasFloodgate = luckPermsContainer != null;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        instance = this;
        load();
        // 注册本地化字符
        registerTranslations();
        SeenHandler.init(this);
        TabListSyncHandler.init(this);
        MinimapWorldSyncHandler.init(this);
        WhitelistHandler.init(this);
        OfflineAuthHandler.init(this);

        PlayerChooseInitialServerEventHandler.init(this);
        ServerConnectedEventHandler.init(this);
        PlayerChatEventHandler.init(this);
        DisconnectEventHandler.init(this);

        LlsChannelCommand.register(this);
        LlsPlayerCommand.register(this);
        LlsCreatePlayerCommand.register(this);

        logger.info("Lls-Manager load success!");
    }

    @Subscribe
    public void proxyReloadEventHandler(ProxyReloadEvent event) {
        load();
    }

    @Subscribe
    public void proxyProxyShutdownEventHandler(ProxyShutdownEvent event) {
    }

    private void load() {
        playerSet.clear();
        playersCache.clear();
        // 检查并创建 player 目录
        Path playerDataDirPath = dataFolderPath.resolve("player");
        if (!Files.exists(playerDataDirPath)) {
            try {
                Files.createDirectories(playerDataDirPath);
            } catch (IOException e) {
                e.printStackTrace();
                LlsManager.logger().error("Lls-Manager load fail! createDirectories {} fail!!", playerDataDirPath);
                throw new IllegalStateException("Lls-Manager init fail.");
            }
        } else if (!Files.isDirectory(playerDataDirPath)) {
            logger.error("Lls-Manager load fail! {} is not a directory!", playerDataDirPath);
            throw new IllegalStateException("Lls-Manager init fail.");
        }

        // 加载 config
        config = new Config(dataFolderPath);
        if (!config.load()) {
            logger.error("Lls-Manager load config fail!");
            throw new IllegalStateException("Lls-Manager init fail.");
        }
        // 同步配置文件为最新的
        config.save();

        // 初始化用户列表
        try {
            Files.list(dataFolderPath.resolve("player")).forEach(path -> {
                String filename = path.getFileName().toString();
                playerSet.add(filename.substring(0, filename.length() - 5));
            });
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }

        // 重载用户
        for (LlsPlayer llsPlayer : players.values()) {
            llsPlayer.load();
            llsPlayer.save();
        }
    }

    private static Path getL10nPath() {
        Path l10nPath;
        URL knownResource = LlsManager.class.getClassLoader().getResource("l10n/messages.properties");

        if (knownResource == null) {
            throw new IllegalStateException("messages.properties does not exist, don't know where we are");
        }
        if (knownResource.getProtocol().equals("jar")) {
            // Running from a JAR
            // 如果在 jar 中路径类似 my.jar!/Main.class
            String jarPathRaw = knownResource.toString().split("!")[0];
            URI path = URI.create(jarPathRaw + "!/");

            FileSystem fileSystem;
            try {
                fileSystem = FileSystems.newFileSystem(path, Map.of("create", "true"));
            } catch (IOException e) {
                e.printStackTrace();
                throw new IllegalStateException(e);
            }
            l10nPath = fileSystem.getPath("l10n");
            if (!Files.exists(l10nPath)) {
                throw new IllegalStateException("l10n does not exist, don't know where we are");
            }
        } else {
            // Running from the file system
            URI uri;
            try {
                URL url = LlsManager.class.getClassLoader().getResource("l10n");
                if (url == null) {
                    throw new IllegalStateException("l10n does not exist, don't know where we are");
                }
                uri = url.toURI();
            } catch (URISyntaxException e) {
                e.printStackTrace();
                throw new IllegalStateException(e);
            }
            l10nPath = Paths.get(uri);
        }
        return l10nPath;
    }

    // from velocity FileSystemUtils, VelocityServer
    private void registerTranslations() {
        logger.info("Loading localizations...");
        final TranslationRegistry translationRegistry = TranslationRegistry
                .create(Key.key("lls-manager", "translations"));
        translationRegistry.defaultLocale(Locale.US);

        // get l10nPath
        Path l10nPath = getL10nPath();

        try {
            Files.walk(l10nPath).forEach(file -> {
                if (!Files.isRegularFile(file)) {
                    return;
                }
                String filename = com.google.common.io.Files
                        .getNameWithoutExtension(file.getFileName().toString());
                String localeName = filename.replace("messages_", "")
                        .replace("messages", "")
                        .replace('_', '-');
                Locale locale;
                if (localeName.isEmpty()) {
                    locale = Locale.US;
                } else {
                    locale = Locale.forLanguageTag(localeName);
                }
                translationRegistry.registerAll(locale,
                        ResourceBundle.getBundle("l10n/messages",
                                locale, UTF8ResourceBundleControl.get()), false);
            });
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
        GlobalTranslator.get().addSource(translationRegistry);
    }

    @NotNull
    public synchronized LlsPlayer getLlsPlayer(String username) throws PlayerNotFoundException, LoadPlayerFailException {
        LlsPlayer llsPlayer;
        Optional<Player> playerOptional = server.getPlayer(username);
        if (playerOptional.isPresent()) {
            llsPlayer = Objects.requireNonNull(players.get(playerOptional.get().getRemoteAddress()));
        } else {
            llsPlayer = playersCache.get(username);
            if (llsPlayer == null) {
                if (playersCache.size() > PLAYER_CACHE_SIZE) {
                    playersCache.clear();
                }
                llsPlayer = new LlsPlayer(username, dataFolderPath);
                playersCache.put(username, llsPlayer);
            }

            if (!llsPlayer.hasUser()) {
                throw new PlayerNotFoundException(Component.translatable("lls-manager.text.player_not_found", NamedTextColor.RED).args(TextUtil.getUsernameComponent(username)));
            } else if (!llsPlayer.load()) {
                throw new LoadPlayerFailException(Component.translatable("lls-manager.text.load_player_data_fail").args(TextUtil.getUsernameComponent(username)));
            }
        }
        return llsPlayer;
    }

}
