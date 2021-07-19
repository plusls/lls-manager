package com.plusls.llsmanager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.plusls.llsmanager.command.LlsChannelCommand;
import com.plusls.llsmanager.command.LlsSeenCommand;
import com.plusls.llsmanager.command.LlsWhitelistCommand;
import com.plusls.llsmanager.data.Config;
import com.plusls.llsmanager.data.LlsPlayer;
import com.plusls.llsmanager.data.LlsWhitelist;
import com.plusls.llsmanager.handler.*;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyReloadEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.util.UTF8ResourceBundleControl;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Plugin(
        id = "lls-manager",
        name = "LlsManager",
        version = "@version@",
        description = "Lls Manager",
        url = "https://github/plusls/lls-manager",
        authors = {"plusls"},
        dependencies = {@Dependency(id = "floodgate", optional = true)}
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

    public Config config;

    public ConcurrentLinkedQueue<String> playerList = new ConcurrentLinkedQueue<>();

    @Nullable
    public static LlsManager getInstance() {
        return instance;
    }

    public static Logger logger() {
        return Objects.requireNonNull(getInstance()).logger;
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
        PlayerChooseInitialServerEventHandler.init(this);
        ServerConnectedEventHandler.init(this);
        PlayerChatEventHandler.init(this);
        PreLoginEventHandler.init(this);
        PostLoginEventHandler.init(this);
        DisconnectEventHandler.init(this);
        LlsWhitelistCommand.register(this);
        LlsSeenCommand.register(this);
        LlsChannelCommand.register(this);
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

        // 加载白名单
        whitelist = new LlsWhitelist(dataFolderPath);
        if (!whitelist.load()) {
            logger.error("Lls-Manager load whitelist fail!");
            throw new IllegalStateException("Lls-Manager init fail.");
        }

        // 初始化用户列表
        try {
            Files.list(dataFolderPath.resolve("player")).forEach(path -> {
                String filename = path.getFileName().toString();
                playerList.add(filename.substring(0, filename.length() - 5));
            });
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
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
}
