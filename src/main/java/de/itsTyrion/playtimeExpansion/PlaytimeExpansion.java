package de.itsTyrion.playtimeExpansion;

import me.clip.placeholderapi.expansion.Cacheable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Taskable;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

public class PlaytimeExpansion extends PlaceholderExpansion implements Listener, Taskable, Cacheable {
    private static final String CHANNEL = "bungeeonlinetime:get";

    public @NotNull String getIdentifier() {return "playtime";}

    public @NotNull String getAuthor() {return "itsTyrion";}

    public @NotNull String getVersion() {return "1.0";}

    @Override
    public void start() {
        Bukkit.getPluginManager().registerEvents(this, getPlaceholderAPI());
        Bukkit.getMessenger().registerIncomingPluginChannel(getPlaceholderAPI(), CHANNEL, (channel, p, data) -> {
            if (channel.equals(CHANNEL)) {
                try {
                    var in = new DataInputStream(new ByteArrayInputStream(data));
                    var uuid = UUID.fromString(in.readUTF());
                    var time = in.readLong();
                    in.close();
                    playTimeMap.put(uuid, time);
                } catch (IOException ex) {
                    severe("Error while receiving plugin message.", ex);
                }
            }
        });
    }

    @Override
    public void stop() {Bukkit.getMessenger().unregisterIncomingPluginChannel(getPlaceholderAPI(), CHANNEL);}

    @Override
    public void clear() {playTimeMap.clear();}


    private final HashMap<UUID, Long> playTimeMap = new HashMap<>();

    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null)
            return "";

        Long seconds = playTimeMap.get(player.getUniqueId());
        if (seconds == null)
            return "";

        return String.valueOf(switch (params.toLowerCase(Locale.ROOT)) {
            case "days" -> (int) (seconds / 86400L);
            case "hours" -> (int) (seconds / 3600L);
            case "minutes" -> (int) (seconds / 60L);
            case "formatted" -> formatAsTime(seconds);
            case "formattedde" -> formatAsTimeDE(seconds);
            default -> "Invalid Unit '" + params + "'";
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        playTimeMap.remove(e.getPlayer().getUniqueId());
    }

    private static @NotNull String formatAsTime(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = ((seconds % 86400) % 3600) / 60;

        var sb = new StringBuilder();
        if (days > 0) sb.append(days).append(" d, ");
        if (hours > 0) sb.append(hours).append(" h, ");

        return sb.append(minutes).append(" m").toString();
    }

    private static @NotNull String formatAsTimeDE(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = ((seconds % 86400) % 3600) / 60;

        var sb = new StringBuilder();
        if (days > 0) sb.append(days).append(days == 1 ? " Tag, " : " Tage, ");
        if (hours > 0) sb.append(hours).append(" Std., ");

        return sb.append(minutes).append(" Min.").toString();
    }
}