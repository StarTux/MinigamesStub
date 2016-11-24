package com.winthier.minigames_stub;

import com.winthier.minilink.util.JSON;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

@RequiredArgsConstructor
public class MinigamesCommand implements CommandExecutor {
    final MinigamesStubPlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String args[]) {
        return true;
    }
}
