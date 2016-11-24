package com.winthier.minigames_stub;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.plugin.java.JavaPlugin;

public class MinigamesStubPlugin extends JavaPlugin
{
    final List<AbstractCommand> subcommands = Arrays.<AbstractCommand>asList(
        new AdventureCommand(this),
        new DefaultCommand(this, "Mob Arena", "MobArena"),
        new DefaultCommand(this, "Enderball", "EnderBall"),
        new DefaultCommand(this, "Survival Games", "SurvivalGames"),
        new DefaultCommand(this, "Colorfall", "Colorfall"),
        new DefaultCommand(this, "Spleef", "Spleef"),
        new DefaultCommand(this, "Vertigo", "Vertigo"),
        new DefaultCommand(this, "PvP", "PvP")
        );
    
    @Override
    public void onEnable()
    {
        reloadConfig();
        saveDefaultConfig();
        // getCommand("minigames").setExecutor(new MinigamesCommand(this));
        for (AbstractCommand cmd: subcommands) {
            getCommand(cmd.getGameKey()).setExecutor(cmd);
        }
    }
}
