package net.oldschoolminecraft.iph;

import com.google.gson.Gson;
import com.projectposeidon.johnymuffin.ConnectionPause;
import net.oldschoolminecraft.iph.tracking.LookupManager;
import net.oldschoolminecraft.iph.util.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerPreLoginEvent;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PlayerHandlerOld extends PlayerListener
{
    private static final Gson gson = new Gson();

    private final MemoryCache<String, IPHubResponse> cache;
    private final PLConfig config = IPHub.instance.config;
    private int lastStatusCode = 200;
    private boolean needBackupKey = false;

    private List<String> passthroughName;
    private List<String> passthroughIP;
    private List<String> ipBlacklist;
    private List<String> asnBlacklist;
    private List<String> cnBlacklist;
    private HashMap<String, String> messages = new HashMap<>();

    private String apiKey;
    private String backupKey;

    public PlayerHandlerOld()
    {
        int minutesTTL = (int) config.getConfigOption("settings.cache.minutesTTL");
        int interval = (int) config.getConfigOption("settings.cache.interval");
        int maxItems = (int) config.getConfigOption("settings.cache.maxItems");
        cache = new MemoryCache<>(TimeUnit.MINUTES.toMillis(minutesTTL), interval, maxItems);

        passthroughName = config.getConfigList("settings.passthrough.nameList");
        passthroughIP = config.getConfigList("settings.passthrough.ipList");

        ipBlacklist = config.getConfigList("settings.blacklist.ipRangeList");
        asnBlacklist = config.getConfigList("settings.blacklist.asnList");
        cnBlacklist = config.getConfigList("settings.blacklist.ccList");

        apiKey = String.valueOf(config.getConfigOption("settings.api.key"));
        backupKey = String.valueOf(config.getConfigOption("settings.api.backupKey"));

        messages.put("vpnDetected", String.valueOf(config.getConfigOption("settings.messages.vpnDetected", "&cVPN detected")));
        messages.put("vpnDetectedNotif", String.valueOf(config.getConfigOption("settings.messages.vpnDetectedNotif", "&cKICKED: &e{player} &cdetected with VPN")));
        messages.put("vpnPossible", String.valueOf(config.getConfigOption("settings.messages.vpnPossible", "&e{player} &cmight have a VPN")));
        messages.put("checkingError", String.valueOf(config.getConfigOption("settings.messages.checkingError", "&cError while checking {player} for VPN")));
        messages.put("notChecked", String.valueOf(config.getConfigOption("settings.messages.notChecked", "&cFailed to check VPN")));
        messages.put("blacklisted", String.valueOf(config.getConfigOption("settings.messages.blacklisted", "&cBLACKLISTED")));
    }

    @EventHandler
    public void onPlayerPreLogin(PlayerPreLoginEvent event)
    {
        // do not waste requests on non-whitelisted players while the whitelist is enabled
        if (Bukkit.hasWhitelist() && !Bukkit.getOfflinePlayer(event.getName()).isWhitelisted())
            return;

        ConnectionPause pause = event.addConnectionPause(IPHub.instance, "IPHub");
        new Thread(() ->
        {
            String ip = event.getAddress().getHostAddress();
            if (ip.equals("127.0.0.1"))
            {
                pause.removeConnectionPause();
                return;
            }

            if (checkPassThrough(event.getName(), ip, pause))
                return;

            for (String range : ipBlacklist)
            {
                if (event.getAddress().getHostAddress().matches(range))
                {
                    System.out.println("[IPHub] Player is IP blacklisted: " + event.getName() + ", " + event.getAddress().getHostAddress());
                    event.cancelPlayerLogin(ColorUtil.translateAlternateColorCodes('&', messages.get("blacklisted")));
                    pause.removeConnectionPause();
                    return;
                }
            }

            IPHubResponse iphr = cache.get(ip);
            if (iphr != null && iphr.block != 1)
            {
                checkBlacklists(event, iphr, pause);
                checkVPN(event, iphr, pause);
                handleTracking(ip, event.getName());
                if (iphr.block == 2)
                {
                    System.out.println("[IPHub] Player might have VPN: " + event.getName() + ", " + event.getAddress().getHostAddress());
                    adminBroadcast(formatString(messages.get("vpnPossible"), event.getName(), iphr), "iphub.warnblock2");
                }
                pause.removeConnectionPause();
                return;
            }

            if (lastStatusCode == 429) needBackupKey = !needBackupKey; // if rate limit is hit, switch keys.

            IPHubRequest iphRequest = new IPHubRequest(ip, needBackupKey ? backupKey : apiKey).onFail((ex) ->
            {
                System.out.println("[IPHub] Error while checking " + event.getName() + " for VPN: " + ex.getMessage());
                event.cancelPlayerLogin(ColorUtil.translateAlternateColorCodes('&', messages.get("notChecked")));
                pause.removeConnectionPause();
            }).complete();

            int statusCode = iphRequest.getStatusCode();

            if (statusCode == -1) return; // something failed in the request and the login was cancelled

            lastStatusCode = statusCode;
            iphr = iphRequest.getResponse();

            if (statusCode != 200 && statusCode != 429)
            {
                adminBroadcast(formatString(messages.get("checkingError"), event.getName(), iphr), "iphub.warnblock2");
                event.cancelPlayerLogin(ColorUtil.translateAlternateColorCodes('&', messages.get("notChecked")));
                pause.removeConnectionPause();
                return;
            }

            checkBlacklists(event, iphr, pause);
            checkVPN(event, iphr, pause);
            handleTracking(ip, event.getName());
            adminBroadcast(String.format("%s: %s %s, %s (%s)", event.getName(), iphr.countryCode, ip, iphr.isp, iphr.asn), "iphub.ipview", true);
            pause.removeConnectionPause();
            cache.put(ip, iphr);
        }).start();
    }

    /**
     * Check if a player/IP is on the passthrough list.
     * @param name Player's username
     * @param ip Player's IPv4 address
     * @param pause The <code>ConnectionPause</code> object
     * @return <code>True</code> if they are on the passthrough list, <code>False</code> if not.
     */
    private boolean checkPassThrough(String name, String ip, ConnectionPause pause)
    {
        if (passthroughName.contains(name))
        {
            pause.removeConnectionPause();
            return true;
        }

        if (passthroughIP.contains(ip))
        {
            pause.removeConnectionPause();
            return true;
        }
        return false;
    }

    /**
     * Check if a player is on the blacklist.
     * @param event The pre-login event
     * @param iphr The <code>iphub.info</code> API response
     * @param pause The <code>ConnectionPause</code> object
     * @return <code>True</code> if they are on the blacklist, <code>False</code> if not.
     */
    private boolean checkBlacklists(PlayerPreLoginEvent event, IPHubResponse iphr, ConnectionPause pause)
    {
        if (asnBlacklist.contains(iphr.asn))
        {
            System.out.println("[IPHub] Player is ASN blacklisted: " + event.getName() + ", " + iphr.asn);
            event.cancelPlayerLogin(ColorUtil.translateAlternateColorCodes('&', String.valueOf(config.getConfigOption("settings.messages.blacklisted"))));
            return true;
        }

        if (cnBlacklist.contains(iphr.countryCode))
        {
            System.out.println("[IPHub] Player is CN blacklisted: " + event.getName() + ", " + iphr.countryCode);
            event.cancelPlayerLogin(ColorUtil.translateAlternateColorCodes('&', String.valueOf(config.getConfigOption("settings.messages.blacklisted"))));
            return true;
        }
        return false;
    }

    private boolean checkVPN(PlayerPreLoginEvent event, IPHubResponse iphr, ConnectionPause pause)
    {
        if (iphr.block == 1)
        {
            event.cancelPlayerLogin(ColorUtil.translateAlternateColorCodes('&', messages.get("vpnDetected")));
            adminBroadcast(formatString(messages.get("vpnDetectedNotif"), event.getName(), iphr), "iphub.ipalert");
            return true;
        }
        if (iphr.block == 2)
        {
            adminBroadcast(formatString(String.valueOf(config.getConfigOption("settings.messages.vpnPossible")), event.getName(), iphr), "iphub.warnblock2");
            return true;
        }
        return false;
    }

    private void handleTracking(String ip, String username)
    {
        LookupManager lookupManager = IPHub.instance.lookupManager;
        lookupManager.updateLookupData(username, ip);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        IPHubResponse iphr = cache.get(event.getPlayer().getName());
        if (iphr == null || iphr.hasNullData()) return;
        for (Player p : Bukkit.getOnlinePlayers())
            if (p.hasPermission("iphub.ipalert") || p.isOp())
                p.sendMessage(ColorUtil.translateAlternateColorCodes('&', formatString(String.valueOf(config.getConfigOption("settings.logging.msgFormat")), event.getPlayer().getName(), iphr)));
    }

    private void adminBroadcast(String msg, String permissionRequired)
    {
        adminBroadcast(msg, permissionRequired, false);
    }

    private void adminBroadcast(String msg, String permissionRequired, boolean checkIPNotif)
    {
        for (Player p : Bukkit.getOnlinePlayers())
        {
            if (p.hasPermission(permissionRequired) || p.isOp())
            {
                if (checkIPNotif && new File(IPHub.instance.getDataFolder(), p.getName() + ".ipview.off").exists()) continue;
                p.sendMessage(ColorUtil.translateAlternateColorCodes('&', msg));
            }
        }
    }

    private String formatString(String input, String player, IPHubResponse iphData)
    {
        if (iphData == null || iphData.hasNullData()) return input;
        String temp = input.replace("{player}", player);
        temp = temp.replace("{ip}", iphData.ip);
        temp = temp.replace("{cnCode}", iphData.countryCode);
        temp = temp.replace("{cnName}", iphData.countryName);
        temp = temp.replace("{asn}", String.valueOf(iphData.asn));
        temp = temp.replace("{isp}", iphData.isp);
        temp = temp.replace("{block}", String.valueOf(iphData.block));
        return temp;
    }
}
