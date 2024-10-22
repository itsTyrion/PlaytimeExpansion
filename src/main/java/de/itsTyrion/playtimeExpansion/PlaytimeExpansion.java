package de.itsTyrion.playtimeExpansion;

import it.unimi.dsi.fastutil.objects.ObjectLongMutablePair;
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
import java.util.*;

public class PlaytimeExpansion extends PlaceholderExpansion implements Listener, Taskable, Cacheable {
    private static final String CHANNEL_MAIN = "bungeeonlinetime:get";
    private static final String CHANNEL_TOP = "bungeeonlinetime:top";

    public @NotNull String getIdentifier() {return "playtime";}

    public @NotNull String getAuthor() {return "itsTyrion";}

    public @NotNull String getVersion() {return "1.1";}

    @Override
    public void start() {
        Bukkit.getPluginManager().registerEvents(this, getPlaceholderAPI());
        Bukkit.getMessenger().registerIncomingPluginChannel(getPlaceholderAPI(), CHANNEL_MAIN, (channel, p, data) -> {
            if (channel.equals(CHANNEL_MAIN)) {
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
        Bukkit.getMessenger().registerIncomingPluginChannel(getPlaceholderAPI(), CHANNEL_TOP, (channel, p, data) -> {
            if (channel.equals(CHANNEL_TOP)) {
                try {
                    var in = new DataInputStream(new ByteArrayInputStream(data));
                    topPlayTime.left(in.readUTF()).right(in.readLong());
                    in.close();
                } catch (IOException ex) {
                    severe("Error while receiving plugin message.", ex);
                }
            }
        });
    }

    @Override
    public void stop() {
        Bukkit.getMessenger().unregisterIncomingPluginChannel(getPlaceholderAPI(), CHANNEL_MAIN);
    }

    @Override
    public void clear() {playTimeMap.clear();}


    private final HashMap<UUID, Long> playTimeMap = new HashMap<>();
    private final ObjectLongMutablePair<String> topPlayTime = ObjectLongMutablePair.of("", 0);

    @Override
    @SuppressWarnings("SpellCheckingInspection")
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        var paramsLow = params.toLowerCase(Locale.ROOT);
        return switch (paramsLow) {
            case "topplayer" -> topPlayTime.key();
            case "toptime" -> formatAsTime(topPlayTime.valueLong());
            case "toptimede" -> formatAsTimeDE(topPlayTime.valueLong());

            case "topplayeronline" -> playTimeMap.entrySet().stream()
                    .max(Comparator.comparingLong(Map.Entry::getValue))
                    .map(entry -> Bukkit.getOfflinePlayer(entry.getKey()).getName())
                    .orElse("-");
            case "toptimeonline" -> {
                long time = playTimeMap.values().stream()
                        .max(Comparator.comparingLong(x -> x))
                        .orElse(0L);
                yield formatAsTime(time);
            }
            case "toptimeonlinede" -> {
                long time = playTimeMap.values().stream()
                        .max(Comparator.comparingLong(x -> x))
                        .orElse(0L);
                yield formatAsTimeDE(time);
            }
            default -> {
                if (player == null)
                    yield "Invalid onlinetime placeholder or unit: '" + params + "'";
                Long seconds = playTimeMap.get(player.getUniqueId());
                if (seconds == null)
                    yield "";

                yield String.valueOf(switch (paramsLow) {
                    case "days" -> seconds / 86400L;
                    case "hours" -> (seconds % 86400) / 3600;
                    case "minutes" -> (seconds % 3600) / 60;
                    case "formatted" -> formatAsTime(seconds);
                    case "formattedde", "formattedde2" -> formatAsTimeDE(seconds);
                    default -> "Invalid onlinetime placeholder or unit: '" + params + "'";
                });
            }
        };
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        playTimeMap.remove(e.getPlayer().getUniqueId());
    }

    private static @NotNull String formatAsTime(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;

        var sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ").append(hours).append("h ");
        else if (hours > 0) sb.append(hours).append("h ");

        return sb.append(minutes).append('m').toString();
    }

    private static @NotNull String formatAsTimeDE(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;

        var sb = new StringBuilder();
        if (days > 0) sb.append(days).append(" T. ").append(hours).append(" Std. ");
        else if (hours > 0) sb.append(hours).append(" Std. ");

        return sb.append(minutes).append(" Min.").toString();
    }
}