package net.oldschoolminecraft.iph.util;

import net.oldschoolminecraft.iph.IPHub;
import org.bukkit.util.config.Configuration;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class PLConfig extends Configuration
{
    public PLConfig()
    {
        super(new File("plugins/IPHub/config.yml"));
        this.reload();
    }

    public void reload()
    {
        this.load();
        this.write();
        this.save();
    }

    private void write()
    {
        generateConfigOption("settings.api.key", "CHANGEME");
        generateConfigOption("settings.api.backupKey", "CHANGEME");

        generateConfigOption("settings.logging.enabled", true);
        generateConfigOption("settings.logging.msgFormat", "&c{player}: {cnCode} {ip}, {isp} ({asn})");

        generateConfigOption("settings.passthrough.enabled", false);
        generateConfigOption("settings.passthrough.ipList", "first,second,third,etc");
        generateConfigOption("settings.passthrough.nameList", "first,second,third,etc");

        generateConfigOption("settings.blacklist.ipRangeList", "first,second,third");
        generateConfigOption("settings.blacklist.asnList", "first,second,third");
        generateConfigOption("settings.blacklist.ccList", "first,second,third");

        generateConfigOption("settings.messages.vpnDetected", "&cVPN detected");
        generateConfigOption("settings.messages.vpnDetectedNotif", "&cKICKED: &e{player} &cdetected with VPN");
        generateConfigOption("settings.messages.notChecked", "&cFailed to check VPN");
        generateConfigOption("settings.messages.vpnPossible", "&e{player} might have a VPN");
        generateConfigOption("settings.messages.checkingError", "&cError while checking {player} for VPN");
        generateConfigOption("settings.messages.blacklisted", "&cBLACKLISTED");
        generateConfigOption("settings.cache.minutesTTL", 60L);
        generateConfigOption("settings.cache.interval", 3);
        generateConfigOption("settings.cache.maxItems", 10000);

        generateConfigOption("settings.developer.debug", false);
        generateConfigOption("settings.developer.disclaimer", "ONLY ENABLE THIS SETTING IF YOU KNOW WHAT YOU ARE DOING");
    }

    private void generateConfigOption(String key, Object defaultValue)
    {
        if (this.getProperty(key) == null) this.setProperty(key, defaultValue);
        final Object value = this.getProperty(key);
        this.removeProperty(key);
        this.setProperty(key, value);
    }

    public Object getConfigOption(String key)
    {
        return this.getProperty(key);
    }

    public Object getConfigOption(String key, Object defaultValue)
    {
        Object value = getConfigOption(key);
        if (value == null) value = defaultValue;
        return value;
    }

    public List<String> getConfigList(String key)
    {
        return Arrays.asList(String.valueOf(IPHub.instance.config.getConfigOption(key, "")).trim().split(","));
    }
}
