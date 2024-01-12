package net.oldschoolminecraft.iph.cmd;

import net.oldschoolminecraft.iph.IPHub;
import net.oldschoolminecraft.iph.tracking.LookupManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class IPHistory implements CommandExecutor
{
    private LookupManager lookupManager;

    public IPHistory(LookupManager lookupManager)
    {
        this.lookupManager = lookupManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (!(sender.hasPermission("iphub.ipview") || sender.isOp()))
        {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return false;
        }

        if (args.length != 1)
        {
            sender.sendMessage(ChatColor.RED + "Invalid arguments! Usage: /iphistory <name/ip>");
            return true;
        }
        String target = args[0];
        boolean isName = !target.matches("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$");

        if (isName)
        {
            if (!lookupManager.hasNameLookup(target))
            {
                sender.sendMessage(ChatColor.RED + "No IPs for that user!");
                return true;
            }
            sender.sendMessage("&8All IPs for user: " + target);
            for (String ip : lookupManager.getIPsFromName(target).addresses)
            {
                String msg = String.format("- &8IP: &7%s", ip);
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
            }
        } else {
            if (!lookupManager.hasIPLookup(target))
            {
                sender.sendMessage(ChatColor.RED + "No usernames for that IP!");
                return true;
            }
            sender.sendMessage("&8All names for IP: " + target);
            for (String name : lookupManager.getNamesFromIP(target).names)
            {
                String msg = String.format("- &7%s", name);
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
            }
        }
        return true;
    }
}
