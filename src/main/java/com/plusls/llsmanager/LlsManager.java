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
import java.util.*;
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
        instance = this;
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
        logger.info("log: {}", logger);
        if (!whitelist.load()) {
            logger.error("Lls-Manager load whitelist fail!");
            throw new RuntimeException("Lls-Manager init fail.");
        }

        registerTranslations();
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

    private static Path getl10nPath() {
        Path l10nPath;
        URL knownResource =  LlsManager.class.getClassLoader().getResource("l10n/messages.properties");

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
        Path l10nPath = getl10nPath();

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
