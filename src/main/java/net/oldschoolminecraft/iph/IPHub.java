package net.oldschoolminecraft.iph;

import com.google.gson.Gson;
import net.oldschoolminecraft.iph.cmd.IPHistory;
import net.oldschoolminecraft.iph.cmd.Reload;
import net.oldschoolminecraft.iph.cmd.ToggleIPNotif;
import net.oldschoolminecraft.iph.tracking.LookupManager;
import net.oldschoolminecraft.iph.util.PLConfig;
import org.bukkit.plugin.java.JavaPlugin;

public class IPHub extends JavaPlugin
{
    public static IPHub instance;
    private static Gson gson = new Gson();

    public PLConfig config;
    public LookupManager lookupManager;

    public void onEnable()
    {
        instance = this;
        config = new PLConfig();
        lookupManager = new LookupManager();

        getServer().getPluginManager().registerEvents(new PlayerHandler(), this);

        getCommand("iphr").setExecutor(new Reload(config));
        getCommand("iphistory").setExecutor(new IPHistory(lookupManager));
        getCommand("ipnotif").setExecutor(new ToggleIPNotif());

        System.out.println("IPHub enabled");
    }

    public void onDisable()
    {
        System.out.println("IPHub disabled");
    }
}
