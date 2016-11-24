package com.winthier.minigames_stub;

import org.bukkit.command.CommandExecutor;

abstract class AbstractCommand implements CommandExecutor {
    String getTitle() { return getClass().getSimpleName(); }
    String getGameKey() { return getClass().getSimpleName(); }
}
