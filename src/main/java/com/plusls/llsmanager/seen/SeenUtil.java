package com.plusls.llsmanager.seen;

import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.plusls.llsmanager.util.LoadPlayerFailException;
import com.plusls.llsmanager.util.PlayerNotFoundException;
import com.plusls.llsmanager.util.TextUtil;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.text.SimpleDateFormat;
import java.util.*;

public class SeenUtil {
    public static SeenData getSeenData(LlsManager llsManager, LlsPlayer llsPlayer) {
        String username = llsPlayer.username;
        Optional<Player> optionalPlayer = llsManager.server.getPlayer(username);
        Date currentDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (optionalPlayer.isPresent()) {
            // 玩家在线的情况
            Optional<ServerConnection> optionalRegisteredServer = optionalPlayer.get().getCurrentServer();
            if (optionalRegisteredServer.isPresent()) {
                TranslatableComponent onlineText = Component.translatable().key("lls-manager.command.lls_seen.online_text")
                        .args(TextUtil.getUsernameComponent(username),
                                TextUtil.getServerNameComponent(optionalRegisteredServer.get().getServerInfo().getName()))
                        .build();

                TextComponent onlineListText = Component.text().append(Component.text("- "))
                        .append(Component.text(sdf.format(currentDate.getTime()), NamedTextColor.YELLOW))
                        .append(Component.text(" "))
                        .append(TextUtil.getUsernameComponent(username))
                        .build();

                return new SeenData(System.currentTimeMillis(), onlineText, onlineListText);
            }
            // 现在这个情况挺离谱的（我想不到触发方式）
        }

        // 玩家不在线的情况直接去查
        long diff = currentDate.getTime() - llsPlayer.getLastSeenTime().getTime();
        long diffSeconds = diff / 1000 % 60;
        long diffMinutes = diff / (60 * 1000) % 60;
        long diffHours = diff / (60 * 60 * 1000) % 24;
        long diffDays = diff / (24 * 60 * 60 * 1000);

        TextComponent.Builder diffTextBuilder = Component.text();
        if (diffDays != 0) {
            diffTextBuilder.append(Component.text(String.format(" %d ", diffDays)).color(NamedTextColor.YELLOW))
                    .append(Component.translatable("lls-manager.command.lls_seen.day"));
        }
        if (diffHours != 0) {
            diffTextBuilder.append(Component.text(String.format(" %d ", diffHours)).color(NamedTextColor.YELLOW))
                    .append(Component.translatable("lls-manager.command.lls_seen.hour"));
        }
        if (diffMinutes != 0) {
            diffTextBuilder.append(Component.text(String.format(" %d ", diffMinutes)).color(NamedTextColor.YELLOW))
                    .append(Component.translatable("lls-manager.command.lls_seen.minute"));
        }
        if (diffSeconds != 0) {
            diffTextBuilder.append(Component.text(String.format(" %d ", diffSeconds)).color(NamedTextColor.YELLOW))
                    .append(Component.translatable("lls-manager.command.lls_seen.second"));
        }
        TranslatableComponent offlineText = Component.translatable().key("lls-manager.command.lls_seen.offline_text")
                .args(TextUtil.getUsernameComponent(username),
                        Component.text(sdf.format(llsPlayer.getLastSeenTime().getTime()), NamedTextColor.YELLOW),
                        diffTextBuilder.build())
                .build();

        TextComponent offlineListText = Component.text().append(Component.text("- "))
                .append(Component.text(sdf.format(llsPlayer.getLastSeenTime().getTime()), NamedTextColor.YELLOW))
                .append(Component.text(" "))
                .append(TextUtil.getUsernameComponent(username))
                .build();

        return new SeenData(llsPlayer.getLastSeenTime().getTime(), offlineText, offlineListText);
    }


    public static SeenData getSeenData(LlsManager llsManager, String username) throws LoadPlayerFailException, PlayerNotFoundException {
        return getSeenData(llsManager, llsManager.getLlsPlayer(username));
    }

    public static List<SeenData> getAllSeenData(LlsManager llsManager) throws LoadPlayerFailException, PlayerNotFoundException {
        List<SeenData> ret = new ArrayList<>();
        for (String username : llsManager.playerSet) {
            ret.add(getSeenData(llsManager, username));
        }
        ret.sort(Comparator.reverseOrder());
        return ret;
    }
}
