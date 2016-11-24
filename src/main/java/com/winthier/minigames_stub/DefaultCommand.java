package com.winthier.minigames_stub;

import com.winthier.minilink.MinilinkPlugin;
import com.winthier.minilink.lobby.LobbyServer;
import com.winthier.minilink.sql.GameTable;
import com.winthier.minilink.sql.PlayerTable;
import com.winthier.minilink.util.Players;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

@RequiredArgsConstructor
@Getter
class DefaultCommand extends AbstractCommand
{
    final MinigamesStubPlugin plugin;
    final String title;
    final String gameKey;
    final Random random = new Random(System.currentTimeMillis());
    String getKey() { return gameKey.toLowerCase(); }
    
    static MinilinkPlugin getMinilink() {
        Plugin pl = Bukkit.getServer().getPluginManager().getPlugin("Minilink");
        if (pl instanceof MinilinkPlugin) return (MinilinkPlugin)pl;
        return null;
    }

    static List<GameTable> getOpenGames(String... keys)
    {
        List<GameTable> games = getMinilink().database.getOpenGamesByKey(keys);
        List<GameTable> result = new ArrayList<>();
        for (GameTable game : games) {
            if (game.getPlayers().size() > 0) result.add(game);
        }
        return result;
    }

    List<GameTable> getOpenGames() {
        return getOpenGames(getGameKey());
    }

    void sendGameList(Player player, List<GameTable> games)
    {
        for (GameTable game : games) {
            List<Object> message = new ArrayList<>();
            message.add(Msg.format("&b \u2022 "));
            if (player.hasPermission(getKey() + ".join")) {
                message.add(button("&r[&6Join&r]", "&7Try to join this game", "/"+getKey()+" join " + game.getUuid()));
                message.add(" ");
                message.add(button("&r[&3Spec&r]", "&7Spectate this game", "/"+getKey()+" spectate " + game.getUuid()));
            }
            for (PlayerTable playerTable : game.getPlayers()) {
                message.add(" ");
                message.add(Msg.format("&3%s", playerTable.getName()));
            }
            Msg.raw(player, message);
        }
    }

    void sendGameList(Player player)
    {
        List<GameTable> games = getOpenGames();
        if (games.isEmpty()) return;
        Msg.send(player, " &3Open Games");
        sendGameList(player, games);
    }

    void join(Player player, UUID uuid)
    {
        if (!player.hasPermission(getKey() + ".join")) return;
        LobbyServer.getInstance().joinGame(uuid, Players.getPlayerInfos(player));
    }

    void spectate(Player player, UUID uuid)
    {
        LobbyServer.getInstance().spectateGame(uuid, Players.getPlayerInfos(player));
    }

    void autoJoin(Player player)
    {
        if (!player.hasPermission(getKey() + ".join")) return;
        if (!player.hasPermission(getKey() + ".create")) {
            LobbyServer.getInstance().joinGame(getGameKey(), Players.getPlayerInfos(player));
            return;
        }
        ConfigurationSection config = new MemoryConfiguration();
        int maxPlayers = getConfig().getInt("MaxPlayers", 32);
        config.set("Game", getGameKey());
        config.set("MaxPlayers", maxPlayers);
        config.set("ShouldAnnounce", true);
        if (getConfig().isSet("maps")) {
            ConfigurationSection mapsConfig = getConfig().getConfigurationSection("maps");
            List<String> keys = new ArrayList<>(mapsConfig.getKeys(false));
            String key = keys.get(random.nextInt(keys.size()));
            ConfigurationSection mapConfig = mapsConfig.getConfigurationSection(key);
            String mapId = mapConfig.getString("MapID");
            String mapPath = mapConfig.getString("MapPath", mapId);
            config.set("MapID", mapId);
            config.set("MapPath", mapPath);
        }
        LobbyServer.getInstance().joinOrCreateGame(getGameKey(), config, Players.getPlayerInfos(player));
    }

    void launchMap(Player player, String key)
    {
        if (!player.hasPermission(getKey() + ".create")) return;
        ConfigurationSection mapConfig = getConfig().getConfigurationSection("maps." + key);
        if (mapConfig == null) return;
        ConfigurationSection config = new MemoryConfiguration();
        int maxPlayers = getConfig().getInt("MaxPlayers", 32);
        config.set("Game", getGameKey());
        config.set("MaxPlayers", maxPlayers);
        config.set("MapID", mapConfig.getString("MapID"));
        config.set("MapPath", mapConfig.getString("MapPath"));
        config.set("ShouldAnnounce", true);
        LobbyServer.getInstance().createGame(getGameKey(), config, Players.getPlayerInfos(player));
    }
    
    void testMap(Player player, List<Player> players)
    {
        ConfigurationSection config = new MemoryConfiguration();
        int maxPlayers = getConfig().getInt("MaxPlayers", 32);
        config.set("Game", getGameKey());
        config.set("MaxPlayers", maxPlayers);
        String worldName = player.getWorld().getName();
        String mapId = worldName;
        String mapPath = "/home/creative/minecraft/worlds/" + worldName;
        config.set("MapID", mapId);
        config.set("MapPath", mapPath);
        config.set("Debug", true);
        config.set("ShouldAnnounce", false);
        plugin.getLogger().info(String.format("[%s] Using map id=%s, path=%s", getKey(), mapId, mapPath));
        LobbyServer.getInstance().joinOrCreateGame(getGameKey() + "-" + worldName, config, Players.getPlayerInfos(players));
    }

    ConfigurationSection getConfig()
    {
        return plugin.getConfig().getConfigurationSection(getKey());
    }

    String getDescription()
    {
        return Msg.format(getConfig().getString("Description", ""));
    }

    void showMenu(Player player)
    {
        Msg.send(player, "");
        Msg.send(player, "&b&l%s", getTitle());
        Msg.send(player, " &r%s", getDescription());
        sendGameList(player);
        if (player.hasPermission(getKey() + ".join")) {
            List<Object> buttons = new ArrayList<>();
            buttons.add(Msg.format(" &3Click here to "));
            buttons.add(button("&r[&6&lPlay&r]", "&7Auto join a game", "/"+getKey()+" join"));
            if (player.hasPermission(getKey() + ".maps") && getConfig().isSet("maps")) {
                buttons.add(Msg.format(" &3or select a "));
                buttons.add(button("&r[&3Map&r]", "&7Select a map", "/"+getKey()+" maps"));
            }
            Msg.raw(player, buttons);
        }
        Msg.send(player, "");
    }

    void showMaps(Player player) {
        Msg.send(player, "");
        Msg.send(player, "&b&l%s Maps &7&o(Click to play)", getTitle());
        ConfigurationSection mapsConfig = getConfig().getConfigurationSection("maps");
        List<Object> messages = new ArrayList<>();
        for (String key : mapsConfig.getKeys(false)) {
            messages.add(" ");
            ConfigurationSection mapConfig = mapsConfig.getConfigurationSection(key);
            String mapId = mapConfig.getString("MapID");
            String displayName = mapConfig.getString("DisplayName", mapId);
            String[] tokens = displayName.split(" ");
            StringBuilder sb = new StringBuilder("&6").append(tokens[0]);
            for (int i = 1; i < tokens.length; ++i) sb.append(" &6").append(tokens[i]);
            displayName = sb.toString();
            List<String> authorList = mapConfig.getStringList("Authors");
            sb = new StringBuilder("&6").append(displayName);
            if (!authorList.isEmpty()) {
                sb.append("\n&7Made by: ");
                sb.append(authorList.get(0));
                for (int i = 1; i < authorList.size(); ++i) sb.append(", ").append(authorList.get(i));
            } else {
                sb.append("\n&7Made by Winthier");
            }
            String description = Msg.wrap(sb.toString(), 32, "\n&7");
            messages.add(button("&r[&6"+displayName+"&r]", description, "/"+getKey()+" maps "+key));
        }
        Msg.raw(player, messages);
        Msg.send(player, "");
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String args[]) {
        final Player player = sender instanceof Player ? (Player)sender : null;
        if (player == null) {
            sender.sendMessage("Player expected");
            return true;
        }
        String cmd = args.length > 0 ? args[0].toLowerCase() : null;
        if (cmd == null) {
            showMenu(player);
        } else if ("join".startsWith(cmd)) {
            if (!player.hasPermission(getKey() + ".join")) {
                Msg.send(player, "&cYou don't have permission");
                return true;
            }
            if (args.length == 1) {
                autoJoin(player);
            } else if (args.length == 2) {
                UUID uuid = null;
                try {
                    uuid = UUID.fromString(args[1]);
                } catch (IllegalArgumentException iae) {
                    return false;
                }
                join(player, uuid);
            }
        } else if ("spectate".startsWith(cmd)) {
            if (args.length != 2) {
                showMenu(player);
                return true;
            }
            UUID uuid = null;
            try {
                uuid = UUID.fromString(args[1]);
            } catch (IllegalArgumentException iae) {
                return false;
            }
            spectate(player, uuid);
        } else if ("maps".startsWith(cmd)) {
            if (!player.hasPermission(getKey() + ".maps") ||
                !player.hasPermission(getKey() + ".create") ||
                !getConfig().isSet("maps")) return false;
            if (args.length == 2) {
                launchMap(player, args[1]);
            } else {
                showMaps(player);
            }
        } else if ("test".startsWith(cmd)) {
            if (!player.hasPermission(getKey() + ".test")) return false;
            List<Player> players = new ArrayList<>();
            players.add(player);
            for (Player guest : player.getWorld().getPlayers()) if (!guest.equals(player)) players.add(guest);
            testMap(player, players);
            Msg.send(player, "&bPreparing test map...");
        } else if ("reload".equals(cmd) && player.hasPermission(getKey() + ".reload")) {
            plugin.reloadConfig();
            Msg.send(player, "&eConfig reloaded");
        } else {
            return false;
        }
        return true;
    }

    Object button(String chat, String tooltip, String command)
    {
        Map<String, Object> map = new HashMap<>();
        // map.put("text", Msg.format(chat));
        map.put("text", "");
        List<Object> extraList = new ArrayList<>();
        for (String token : Msg.format(chat).split(" ")) {
            if (!extraList.isEmpty()) extraList.add(" ");
            extraList.add(token);
        }
        map.put("extra", extraList);
        Map<String, Object> map2 = new HashMap<>();
        map.put("clickEvent", map2);
        map2.put("action", "run_command");
        map2.put("value", command);
        map2 = new HashMap<>();
        map.put("hoverEvent", map2);
        map2.put("action", "show_text");
        map2.put("value", Msg.format(tooltip));
        return map;
    }
}
