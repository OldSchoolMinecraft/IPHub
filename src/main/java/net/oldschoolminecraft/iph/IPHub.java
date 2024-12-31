package net.oldschoolminecraft.iph;

import com.google.gson.Gson;
import net.oldschoolminecraft.iph.cmd.IPHistory;
import net.oldschoolminecraft.iph.cmd.Reload;
import net.oldschoolminecraft.iph.cmd.ToggleIPNotif;
import net.oldschoolminecraft.iph.tracking.LookupManager;
import net.oldschoolminecraft.iph.util.PLConfig;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class IPHub extends JavaPlugin
{
    public static IPHub instance;
    private static Gson gson = new Gson();

    public PLConfig config;
    public LookupManager lookupManager;
    private UpdateManager updateManager;

    public void onEnable()
    {
        instance = this;
        updateManager = new UpdateManager(this, "https://micro.os-mc.net/plugin_ci/IPHub/latest");
        config = new PLConfig();
        lookupManager = new LookupManager();

        getServer().getPluginManager().registerEvents(new PlayerHandler(), this);

        getCommand("iphr").setExecutor(new Reload(config));
        getCommand("iphistory").setExecutor(new IPHistory(lookupManager));
        getCommand("ipnotif").setExecutor(new ToggleIPNotif());

        System.out.println("IPHub enabled");
    }

    public File getPluginFile()
    {
        return getFile();
    }

    public void onDisable()
    {
        updateManager.checkForUpdates();
        System.out.println("IPHub disabled");
    }
}
