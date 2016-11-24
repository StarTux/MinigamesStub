package com.winthier.minigames_stub;

import com.winthier.minilink.MinilinkPlugin;
import com.winthier.minilink.lobby.LobbyServer;
import com.winthier.minilink.sql.GameTable;
import com.winthier.minilink.sql.PlayerTable;
import com.winthier.minilink.util.Players;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import org.bukkit.configuration.file.YamlConfiguration;
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
    Map<String, MyMap> maps = null;
    long lastLoaded = 0;
    
    static MinilinkPlugin getMinilink() {
        Plugin pl = Bukkit.getServer().getPluginManager().getPlugin("Minilink");
        if (pl instanceof MinilinkPlugin) return (MinilinkPlugin)pl;
        return null;
    }

    Map<String, MyMap> getMaps() {
        if (maps == null || lastLoaded + 1000*60*10 < System.currentTimeMillis()) {
            lastLoaded = System.currentTimeMillis();
            plugin.reloadConfig();
            maps = new HashMap<>();
            ConfigurationSection mapsConfig = getConfig().getConfigurationSection("maps");
            for (String key: mapsConfig.getKeys(false)) {
                ConfigurationSection mapConfig = mapsConfig.getConfigurationSection(key);
                MyMap map = new MyMap(key, mapConfig);
                maps.put(key, map);
            }
        }
        return maps;
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
                message.add(Msg.button("&r[&6Join&r]", "&7Try to join this game", "/"+getKey()+" join " + game.getUuid()));
                message.add(" ");
                message.add(Msg.button("&r[&3Spec&r]", "&7Spectate this game", "/"+getKey()+" spectate " + game.getUuid()));
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
        List<MyMap> mapList = new ArrayList<>(getMaps().values());
        if (!mapList.isEmpty()) {
            MyMap map = mapList.get(random.nextInt(mapList.size()));
            config.set("MapID", map.mapID);
            config.set("MapPath", map.mapPath);
        }
        LobbyServer.getInstance().joinOrCreateGame(getGameKey(), config, Players.getPlayerInfos(player));
    }

    void launchMap(Player player, String key)
    {
        if (!player.hasPermission(getKey() + ".create")) return;
        MyMap map = getMaps().get(key);
        if (map == null) return;
        ConfigurationSection config = new MemoryConfiguration();
        int maxPlayers = getConfig().getInt("MaxPlayers", 32);
        config.set("Game", getGameKey());
        config.set("MaxPlayers", maxPlayers);
        config.set("MapID", map.mapID);
        config.set("MapPath", map.mapPath);
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
            buttons.add(Msg.button("&r[&6&lPlay&r]", "&7Auto join a game", "/"+getKey()+" join"));
            if (player.hasPermission(getKey() + ".maps") && getConfig().isSet("maps")) {
                buttons.add(Msg.format(" &3or select a "));
                buttons.add(Msg.button("&r[&3Map&r]", "&7Select a map", "/"+getKey()+" maps"));
            }
            Msg.raw(player, buttons);
        }
        Msg.send(player, "");
    }

    void showMaps(Player player) {
        Msg.send(player, "");
        Msg.send(player, "&b&l%s Maps &7&o(Click to play)", getTitle());
        List<Object> messages = new ArrayList<>();
        List<MyMap> mapList = new ArrayList<>(getMaps().values());
        Collections.sort(mapList, MyMap.NAME_COMPARATOR);
        int count = 0;
        for (MyMap map: mapList) {
            messages.add(" ");
            messages.add(Msg.button(ChatColor.GOLD,
                                    "&r[&6"+map.displayName+"&r]",
                                    "&6"+map.displayName+"\n" +
                                    Msg.wrap("&7Made by &r" + Msg.fold(map.authors, ", "), 32, "\n") + "\n" +
                                    Msg.wrap(map.description, 32, "\n") + "\n" +
                                    "&7Click to play this map.",
                                    "/"+getKey()+" maps " + map.key));
            count += 1;
            if (count >= 3) {
                Msg.raw(player, messages);
                messages.clear();
                count = 0;
            }
        }
        if (count > 0) {
            Msg.raw(player, messages);
        }
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
            maps = null;
            Msg.send(player, "&eConfig reloaded");
        } else {
            return false;
        }
        return true;
    }
}

class MyMap {
    final static Comparator<MyMap> NAME_COMPARATOR = new Comparator<MyMap>() {
        @Override public int compare(MyMap a, MyMap b) {
            return a.displayName.compareTo(b.displayName);
        }
    };
    final String key, mapID, mapPath, displayName, permission;
    final List<String> authors;
    final boolean debug;
    final String description;

    MyMap(String key, ConfigurationSection config) {
        this.key = key;
        this.mapID = config.getString("MapID");
        this.mapPath = config.getString("MapPath", "MyMap/" + mapID);
        String displayName = config.getString("DisplayName", this.mapID);
        this.permission = config.getString("Permission", null);
        this.debug = config.getBoolean("Debug", false);
        List<String> authors = config.getStringList("Authors");
        String description = config.getString("Description", "");
        File file = new File(this.mapPath, "config.yml");
        YamlConfiguration config2 = YamlConfiguration.loadConfiguration(file);
        description = config2.getString("user.Description", description);
        displayName = config2.getString("user.Name", displayName);
        if (config2.isSet("user.Authors")) {
            authors = config2.getStringList("user.Authors");
        }
        this.description = description;
        this.displayName = displayName;
        this.authors = authors;
    }

    boolean hasPermission(Player player) {
        if (permission == null) return true;
        return player.hasPermission(permission);
    }
}
