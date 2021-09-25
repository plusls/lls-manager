package com.plusls.llsmanager.offlineAuth;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.plusls.llsmanager.util.LoadPlayerFailException;
import com.plusls.llsmanager.util.PlayerNotFoundException;
import com.plusls.llsmanager.whitelist.ConnectionUtil;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.command.PlayerAvailableCommandsEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

@Singleton
public class OfflineAuthHandler {

    @Inject
    public LlsManager llsManager;

    public static void init(LlsManager llsManager) {
        llsManager.server.getEventManager().register(llsManager, llsManager.injector.getInstance(com.plusls.llsmanager.offlineAuth.OfflineAuthHandler.class));
        llsManager.commandManager.register(llsManager.injector.getInstance(LlsRegisterCommand.class).createBrigadierCommand());
        llsManager.commandManager.register(llsManager.injector.getInstance(LlsPasswdCommand.class).createBrigadierCommand());
        llsManager.commandManager.register(llsManager.injector.getInstance(LlsLoginCommand.class).createBrigadierCommand());
    }

    // 在还未登陆时用户语言并没有初始化，因此显示的都会是英文，除非用客户端自带的 key
    // 在 PreLoginEvent 阶段可能会出现用户重复登陆的情况，其它阶段则不会
    @Subscribe
    public void onPreLoginEvent(PreLoginEvent event) {
        // 新用户连接时清理连接池
        llsManager.autoRemovePlayers();

        // 如果别的插件阻止了，那就不需要做
        if (event.getResult() != PreLoginEvent.PreLoginComponentResult.allowed()) {
            return;
        }

        String username = ConnectionUtil.getUsername(llsManager, event.getUsername(), event.getConnection());

        LlsPlayer llsPlayer;

        try {
            llsPlayer = llsManager.getLlsPlayer(username);
            if (llsPlayer.getOnlineMode()) {
                llsPlayer.status = LlsPlayer.Status.LOGGED_IN;
            } else if (llsPlayer.getPassword().equals("")) {
                llsPlayer.status = LlsPlayer.Status.NEED_REGISTER;
            } else {
                llsPlayer.status = LlsPlayer.Status.NEED_LOGIN;
            }
        } catch (PlayerNotFoundException e) {
            // 不存在则创建用户
            llsPlayer = new LlsPlayer(username, llsManager.dataFolderPath);
            if (llsManager.config.getDefaultOnlineMode()) {
                llsPlayer.status = LlsPlayer.Status.LOGGED_IN;
            } else {
                llsPlayer.status = LlsPlayer.Status.NEED_REGISTER;
            }
        } catch (LoadPlayerFailException e) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(e.message));
            return;
        }
        if (!llsPlayer.getOnlineMode()) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
        } else {
            event.setResult(PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
        }

        // 在这 put 的原因在于，在这会初始化 llsPlayer 的状态，在后续登录阶段需要使用
        llsManager.addLlsPlayer(event.getConnection(), llsPlayer);

        // 在登陆服务器后 ServerConnectedEvent 时会保存最后登陆的服务器，会将用户配置写入 json
        // 因此在这无需保存用户信息
        // 主要是考虑到开启盗版验证并且不开白名单的情况下，可能会有很多垃圾用户连接，产生很多垃圾数据
        // 因此设置为登陆成功后或者注册成功才能留下用户数据
    }

    @Subscribe
    public void onCommandExecute(CommandExecuteEvent event) {
        if (event.getCommandSource() instanceof Player player) {
            LlsPlayer.Status status = llsManager.getLlsPlayer(player).status;
            String commandName = event.getCommand().split(" ")[0];
            if (status != LlsPlayer.Status.LOGGED_IN) {
                if (!(status == LlsPlayer.Status.NEED_REGISTER && commandName.equals("lls_register")) &&
                        !(status == LlsPlayer.Status.NEED_LOGIN && commandName.equals("lls_login"))) {
                    event.setResult(CommandExecuteEvent.CommandResult.denied());
                }
            }
        }
    }

    @Subscribe
    public void onPlayerAvailableCommands(PlayerAvailableCommandsEvent event) {
        LlsPlayer.Status status = llsManager.getLlsPlayer(event.getPlayer()).status;
        event.getRootNode().getChildren().removeIf(
                commandNode -> {
                    String commandName = commandNode.getName();
                    if (status != LlsPlayer.Status.LOGGED_IN) {
                        return !commandName.equals("lls_register") && !commandName.equals("lls_login") && !commandName.equals("server");
                    }
                    return false;
                }
        );
    }

    @Subscribe
    public void onServerPostConnect(ServerPostConnectEvent event) {
        Player player = event.getPlayer();
        LlsPlayer llsPlayer = llsManager.getLlsPlayer(player);

        if (llsPlayer.status == LlsPlayer.Status.NEED_LOGIN) {
            player.sendMessage(Component.translatable("lls-manager.command.lls_login.hint"));
        } else if (llsPlayer.status == LlsPlayer.Status.NEED_REGISTER) {
            player.sendMessage(Component.translatable("lls-manager.command.lls_register.hint"));
        }

    }

    @Subscribe(order = PostOrder.LATE)
    public void onDisconnect(DisconnectEvent event) {
        llsManager.autoRemovePlayers();
    }

}