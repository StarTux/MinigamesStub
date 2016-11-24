package com.winthier.minigames_stub;

import com.avaje.ebean.SqlRow;
import com.winthier.minilink.MinilinkPlugin;
import com.winthier.minilink.lobby.LobbyServer;
import com.winthier.minilink.util.PlayerInfo;
import com.winthier.minilink.util.Players;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
@Getter
public class AdventureCommand extends AbstractCommand
{
    final MinigamesStubPlugin plugin;
    final Map<String, Adventure> adventures = new LinkedHashMap<>();
    final List<String> categories = new ArrayList<>();
    final String gameKey = "Adventure";
    final String title = "Adventure";
    long lastLoaded = 0;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (adventures.isEmpty() || lastLoaded + 1000*60*10 < System.currentTimeMillis()) reload();
        Player player = sender instanceof Player ? (Player)sender : null;
        if (player == null) {
            sender.sendMessage("Player expected");
            return true;
        }
        if (args.length == 0) {
            Set<String> beaten = findMapsBeatBy(player);
            player.sendMessage("");
            player.sendMessage("" + ChatColor.AQUA + ChatColor.BOLD + "Adventure Lists");
            Adventure singleAdventure = null;
            for (String category: categories) {
                int adventureCount = 0;
                int beatenCount = 0;
                for (Adventure adventure: adventures.values()) {
                    if (category.equals(adventure.category)) {
                        singleAdventure = adventure;
                        adventureCount += 1;
                        if (beaten.contains(adventure.mapID)) {
                            beatenCount += 1;
                        }
                    }
                }
                ChatColor colorCounter;
                ChatColor colorTitle;
                String formatCounter;
                String cmd = "/adv category " + category;
                if (adventureCount == 1) cmd = "/adv adventure " + singleAdventure.key;
                if (adventureCount == beatenCount) {
                    colorTitle = ChatColor.WHITE;
                    colorCounter = ChatColor.GREEN;
                    formatCounter = "&r[&a"+adventureCount+"&r]";
                } else {
                    colorTitle = ChatColor.GRAY;
                    colorCounter = ChatColor.GRAY;
                    formatCounter = "&8[&7"+beatenCount+"&8/&7"+adventureCount+"&8]";
                }
                Msg.raw(player,
                        " ",
                        Msg.button(colorCounter, formatCounter,
                                   Msg.wrap("You beat " + beatenCount + " out of " + adventureCount + " adventure maps in the " + category + " category.", 32, "\n"),
                                   cmd),
                        " ",
                        Msg.button(colorTitle, category,
                                   Msg.wrap("View all maps in the " + category + " category.", 32, "\n"),
                                   cmd));
            }
            player.sendMessage("");
        } else if (args.length == 2 && args[0].equals("category")) {
            Set<String> beaten = findMapsBeatBy(player);
            String cat = args[1];
            player.sendMessage("");
            player.sendMessage("" + ChatColor.DARK_AQUA + ChatColor.BOLD + cat + " Adventure Maps");
            List<Adventure> list = new ArrayList<>();
            for (Adventure adventure : adventures.values()) {
                if (adventure.hasPermission(player) && adventure.category.equals(cat)) {
                    list.add(adventure);
                }
            }
            Collections.sort(list, Adventure.ORDER_COMPARATOR);
            boolean didBeatPrevious = true;
            for (Adventure adventure: list) {
                int completionLevel;
                boolean isBeat = beaten.contains(adventure.mapID);
                if (isBeat) {
                    completionLevel = 2;
                } else if (adventure.sequential) {
                    if (!didBeatPrevious) {
                        completionLevel = 0;
                    } else {
                        completionLevel = 1;
                    }
                } else {
                    completionLevel = 1;
                }
                String cmd = "/adv adventure " + adventure.key;
                if (completionLevel == 0) {
                    Msg.raw(player,
                            " ",
                            Msg.button(ChatColor.DARK_GRAY, "[  ]", Msg.wrap("Unlock this map to play it.", 32, "\n"), null),
                            " ",
                            Msg.button(ChatColor.DARK_GRAY, adventure.displayName, Msg.wrap("Made by " + Msg.fold(adventure.authors, ", ") + ". Unlock this map to play it.", 32, "\n"), null));
                } else if (completionLevel == 1) {
                    Msg.raw(player,
                            " ",
                            Msg.button(ChatColor.WHITE, "[  ]", Msg.wrap("You have yet to beat this map.", 32, "\n"), null),
                            " ",
                            Msg.button(ChatColor.GRAY, adventure.displayName, Msg.wrap("Made by " + Msg.fold(adventure.authors, ", ") + ". Click here for more info.", 32, "\n"), cmd));
                } else {
                    Msg.raw(player,
                            " ",
                            Msg.button(ChatColor.GREEN, "&r[&a\u2714&r]", Msg.wrap("You beat the " + adventure.displayName + " adventure map. Wanna play it again?", 32, "\n"), null),
                            " ",
                            Msg.button(ChatColor.WHITE, adventure.displayName, Msg.wrap("Made by " + Msg.fold(adventure.authors, ", ") + ". Click here for more info.", 32, "\n"), cmd));
                }
                didBeatPrevious = isBeat;
            }
            player.sendMessage("");
        } else if (args.length == 2 && args[0].equals("adventure")) {
            String key = args[1];
            Adventure adventure = adventures.get(key);
            if (adventure == null) return false;
            showAdventurePage(player, adventure);
        } else if (args.length == 1 && "test".equalsIgnoreCase(args[0])) {
            if (!player.hasPermission("adventure.test")) return false;
            List<Player> players = new ArrayList<>();
            players.add(player);
            for (Player guest : player.getWorld().getPlayers()) if (!guest.equals(player)) players.add(guest);
            testMap(player, players);
            Msg.send(player, "&bPreparing test map...");
        } else if (args.length == 1 && args[0].equals("reload")) {
            reload();
            sender.sendMessage("Adventures reloaded");
        } else if (args.length == 1 || args.length == 2) {
            Adventure adventure = adventures.get(args[0]);
            if (adventure == null || !adventure.hasPermission(player)) {
                player.sendMessage(ChatColor.RED + "Adventure map not found: " + args[0]);
                return true;
            }
            boolean solo = false;
            if (args.length >= 2) {
                String groupArg = args[1];
                if (groupArg.equalsIgnoreCase("solo")) {
                    solo = true;
                } else if (groupArg.equalsIgnoreCase("party")) {
                    if (adventure.soloOnly) { return true; }
                    solo = false;
                } else {
                    player.sendMessage(ChatColor.RED + "Illegal option: " + groupArg);
                    return true;
                }
            }
            joinAdventure(player, adventure, solo);
        } else {
            return false;
        }
        return true;
    }

    void showAdventurePage(Player player, Adventure adventure) {
        Set<String> beaten = findMapsBeatBy(player);
        boolean isBeaten = beaten.contains(adventure.mapID);
        player.sendMessage("");
        String completionTime = findCompletionTime(player, adventure);
        player.sendMessage("" + ChatColor.DARK_AQUA + ChatColor.BOLD + adventure.displayName + " Adventure Map");
        if (isBeaten) {
            Msg.raw(player, " ", Msg.button(ChatColor.GREEN, " &r[&a\u2714&r] Completed in " + completionTime, Msg.wrap("You beat the " + adventure.displayName + " adventure map.", 32, "\n"), null));
        }
        Msg.raw(player, " ", Msg.button(ChatColor.WHITE, "&7&oCategory &r" + adventure.category, "View the " + adventure.category + " category.", "/adv category " + adventure.category));
        Msg.send(player, " &7&oMade by &r%s", Msg.fold(adventure.authors, ", "));
        Msg.send(player, " %s", adventure.description);
        for (Highscore hi: findHighscoreList(adventure, 5)) {
            Msg.send(player, " %s &9%s", hi.getTime(), hi.getName());
        }
        if (adventure.soloOnly) {
            Msg.raw(player, " ",
                    Msg.button(ChatColor.WHITE, "[&9&lSolo&r]", Msg.wrap("Play the " + adventure.displayName + " adventure map alone. It will not be announced.", 32, "\n"), "/adv " + adventure.key + " solo"));
        } else {
            Msg.raw(player, " ",
                    Msg.button(ChatColor.WHITE, "[&9&lSolo&r]", Msg.wrap("Play the " + adventure.displayName + " adventure map alone. It will not be announced.", 32, "\n"), "/adv " + adventure.key + " solo"),
                    " ",
                    Msg.button(ChatColor.WHITE, "[&a&lParty&r]", Msg.wrap("Play the " + adventure.displayName + " adventure map as a party. This will be announced so anyone can join.", 32, "\n"), "/adv " + adventure.key + " party"));
        }
        player.sendMessage("");
    }

    void reload() {
        lastLoaded = System.currentTimeMillis();
        plugin.reloadConfig();
        adventures.clear();
        categories.clear();
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("adventure");
        for (String key : config.getKeys(false)) {
            plugin.getLogger().info("[Adventure] loading adventure " + key);
            Adventure adventure = new Adventure(key, config.getConfigurationSection(key));
            adventures.put(key, adventure);
            if (!categories.contains(adventure.category)) categories.add(adventure.category);
        }
        Collections.sort(categories);
    }

    void joinAdventure(Player player, Adventure adventure, boolean solo)
    {
        MemoryConfiguration config = new MemoryConfiguration();
        String gameKey =  String.format("Adventure-%s-%s", adventure.mapID, (solo ? "Solo" : "Party"));
        config.set("Game", "Adventure");
        config.set("GameKey", gameKey);
        config.set("MaxPlayers", solo ? 1 : 8);
        config.set("MapID", adventure.mapID);
        config.set("MapPath", adventure.mapPath);
        config.set("Solo", solo);
        config.set("Debug", adventure.debug);
        config.set("ShouldAnnounce", false);
        List<PlayerInfo> infos = new ArrayList<>();
        infos.add(PlayerInfo.fromPlayer(player));
        MinilinkPlugin minilink = (MinilinkPlugin)plugin.getServer().getPluginManager().getPlugin("Minilink");
        LobbyServer lobbyServer = minilink.getLobbyServer();
        if (solo) {
            lobbyServer.createGame(gameKey, config, infos);
        } else {
            lobbyServer.joinOrCreateGame(gameKey, config, infos);
        }
    }    

    void testMap(Player player, List<Player> players)
    {
        ConfigurationSection config = new MemoryConfiguration();
        config.set("Game", "Adventure");
        config.set("MaxPlayers", 32);
        String worldName = player.getWorld().getName();
        String mapId = worldName;
        String mapPath = "/home/creative/minecraft/worlds/" + worldName;
        config.set("MapID", mapId);
        config.set("MapPath", mapPath);
        config.set("Debug", true);
        plugin.getLogger().info(String.format("[%s] Using map id=%s, path=%s", "Adventure", mapId, mapPath));
        LobbyServer.getInstance().joinOrCreateGame("Adventure-" + worldName, config, Players.getPlayerInfos(players));
    }

    Set<String> findMapsBeatBy(Player player) {
        MinilinkPlugin minilink = (MinilinkPlugin)plugin.getServer().getPluginManager().getPlugin("Minilink");
        Set<String> result = new HashSet<>();
        for (SqlRow row: minilink.getDatabase().createSqlQuery("select map_id from Minigames.Adventure where finished = 1 and player_uuid = '" + player.getUniqueId() + "' group by map_id").findList()) {
            result.add(row.getString("map_id"));
        }
        return result;
    }

    String findCompletionTime(Player player, Adventure adventure) {
        MinilinkPlugin minilink = (MinilinkPlugin)plugin.getServer().getPluginManager().getPlugin("Minilink");
        SqlRow row = minilink.getDatabase().createSqlQuery("select timediff(end_time,start_time) time from Minigames.Adventure where player_uuid = '" + player.getUniqueId() + "' and finished = 1 and map_id = '" + adventure.mapID + "' order by time asc limit 1").findUnique();
        if (row == null) return "00:00:00";
        return row.getString("time");
    }

    List<Highscore> findHighscoreList(Adventure adventure, int limit) {
        List<Highscore> result = new ArrayList<>();
        MinilinkPlugin minilink = (MinilinkPlugin)plugin.getServer().getPluginManager().getPlugin("Minilink");
        for (SqlRow row: minilink.getDatabase().createSqlQuery("select player_name name, timediff(end_time,start_time) time from Minigames.Adventure where finished = 1 and map_id = '" + adventure.mapID + "' group by player_uuid order by time asc limit " + limit).findList()) {
            result.add(new Highscore(row.getString("name"), row.getString("time")));
        }
        return result;
    }
}

class Adventure
{
    final static Comparator<Adventure> ORDER_COMPARATOR = new Comparator<Adventure>() {
        @Override public int compare(Adventure a, Adventure b) {
            return Integer.compare(a.order, b.order);
        }
    };
    final String key, mapID, mapPath, displayName, permission;
    final List<String> authors;
    final boolean debug;
    final boolean soloOnly;
    final String category;
    final int order;
    final boolean sequential;
    final String description;
    Adventure(String key, ConfigurationSection config)
    {
        this.key = key;
        this.mapID = config.getString("MapID");
        this.mapPath = config.getString("MapPath", "Adventure/" + mapID);
        String displayName = config.getString("DisplayName", this.mapID);
        this.permission = config.getString("Permission", null);
        this.debug = config.getBoolean("Debug", false);
        this.soloOnly = config.getBoolean("SoloOnly", false);
        List<String> authors = config.getStringList("Authors");
        this.category = config.getString("Category", "Default");
        this.order = config.getInt("Order", 0);
        this.sequential = config.getBoolean("Sequential", false);
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
    boolean hasPermission(Player player)
    {
        if (permission == null) return true;
        return player.hasPermission(permission);
    }
    String authorsString() {
        if (authors.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(authors.get(0));
        for (int i = 1; i < authors.size(); ++i) {
            sb.append(" ").append(authors.get(i));
        }
        return sb.toString();
    }
}

@lombok.Value
class Highscore {
    String name, time;
}
