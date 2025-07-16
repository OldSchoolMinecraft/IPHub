package net.oldschoolminecraft.iph.cmd;

import net.oldschoolminecraft.iph.IPHub;
import net.oldschoolminecraft.iph.handlers.PlayerHandler;
import net.oldschoolminecraft.iph.util.ColorUtil;
import net.oldschoolminecraft.iph.util.PLConfig;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class Reload implements CommandExecutor
{
    private PLConfig config;
    private PlayerHandler playerHandler;

    public Reload(IPHub plugin)
    {
        this.config = plugin.config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (!(sender.hasPermission("iphub.reload") || sender.isOp()))
        {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        config.reload();
        playerHandler.reloadCacheConfig();
        sender.sendMessage(ColorUtil.translateAlternateColorCodes('&', "&aIPHub configuration reloaded"));
        return true;
    }
}
