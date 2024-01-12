package net.oldschoolminecraft.iph.cmd;

import net.oldschoolminecraft.iph.IPHub;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;

public class ToggleIPNotif implements CommandExecutor
{
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (!(sender.hasPermission("iphub.ipview") || sender.isOp()))
        {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        try
        {
            File off = new File(IPHub.instance.getDataFolder(), sender.getName() + ".ipview.off");
            if (off.exists())
            {
                if (off.delete()) off.deleteOnExit();
                sender.sendMessage(ChatColor.GREEN + "You will now see IP notifications");
                return true;
            } else {
                off.createNewFile();
                sender.sendMessage(ChatColor.RED + "You will no longer see IP notifications");
            }
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "Could not toggle your IP notifications");
            return true;
        }

        return true;
    }
}
