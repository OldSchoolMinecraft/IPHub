package net.oldschoolminecraft.iph.handlers;

import com.google.gson.Gson;
import com.projectposeidon.johnymuffin.ConnectionPause;
import net.oldschoolminecraft.iph.IPHub;
import net.oldschoolminecraft.iph.util.ColorUtil;
import net.oldschoolminecraft.iph.util.IPHubResponse;
import net.oldschoolminecraft.iph.util.MemoryCache;
import net.oldschoolminecraft.iph.util.PLConfig;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerPreLoginEvent;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PlayerHandler extends PlayerListener
{
    private static final Gson gson = new Gson();

    private MemoryCache<String, IPHubResponse> cache;
    private final PLConfig config = IPHub.instance.config;
    private int lastStatusCode = 200;
    private boolean needBackupKey = false;

    public PlayerHandler()
    {
        reloadCacheConfig();
    }

    public void reloadCacheConfig()
    {
        int minutesTTL = (int) config.getConfigOption("settings.cache.minutesTTL");
        int interval = (int) config.getConfigOption("settings.cache.interval");
        int maxItems = (int) config.getConfigOption("settings.cache.maxItems");
        cache = new MemoryCache<>(TimeUnit.MINUTES.toMillis(minutesTTL), interval, maxItems);
    }

    @EventHandler
    public void onPlayerPreLogin(PlayerPreLoginEvent event)
    {
        ConnectionPause pause = event.addConnectionPause(IPHub.instance, "IPHub");
        new Thread(() ->
        {
            List<String> passthroughName = config.getConfigList("settings.passthrough.nameList");
            List<String> passthroughIP = config.getConfigList("settings.passthrough.ipList");

            List<String> ipBlacklist = config.getConfigList("settings.blacklist.ipRangeList");
            List<String> asnBlacklist = config.getConfigList("settings.blacklist.asnList");
            List<String> ccBlacklist = config.getConfigList("settings.blacklist.ccList");

            String ip = event.getAddress().getHostAddress();
            if (ip.equals("127.0.0.1"))
            {
                pause.removeConnectionPause();
                return;
            }

            if (passthroughName.contains(event.getName()))
            {
                System.out.println("[IPHub] Player's name is on the passthrough list; all checks have been skipped");
                pause.removeConnectionPause();
                return;
            }

            if (passthroughIP.contains(event.getAddress().getHostAddress()))
            {
                pause.removeConnectionPause();
                return;
            }

            for (String range : ipBlacklist)
            {
                if (event.getAddress().getHostAddress().matches(range))
                {
                    System.out.println("[IPHub] Player is IP blacklisted: " + event.getName() + ", " + event.getAddress().getHostAddress());
                    event.cancelPlayerLogin(ColorUtil.translateAlternateColorCodes('&', String.valueOf(config.getConfigOption("settings.messages.blacklisted"))));
                    pause.removeConnectionPause();
                    return;
                }
            }

            IPHubResponse iphr = cache.get(ip);
            if (iphr != null && iphr.block == 2)
            {
                // do not cancel login, simply warn online staff
                System.out.println("[IPHub] Player's IP was detected as a possible VPN: " + event.getName() + ", " + iphr.ip);
                adminBroadcast(formatString(String.valueOf(config.getConfigOption("settings.messages.vpnPossible")), event.getName(), iphr), "iphub.warnblock2");
                pause.removeConnectionPause();
                return;
            }

            if (iphr != null && iphr.block == 1)
            {
                System.out.println("[IPHub] Player's IP was detected as a VPN: " + event.getName() + ", " + iphr.ip);
                event.cancelPlayerLogin(ColorUtil.translateAlternateColorCodes('&', String.valueOf(config.getConfigOption("settings.messages.vpnDetected"))));
                adminBroadcast(formatString(String.valueOf(config.getConfigOption("settings.messages.vpnDetectedNotif")), event.getName(), iphr), "iphub.ipalert");
                pause.removeConnectionPause();
                return;
            }

            if (iphr != null && asnBlacklist.contains(String.valueOf(iphr.asn)))
            {
                System.out.println("[IPHub] Player's ISP is blacklisted: " + event.getName() + ", " + iphr.asn);
                event.cancelPlayerLogin(ColorUtil.translateAlternateColorCodes('&', String.valueOf(config.getConfigOption("settings.messages.blacklisted"))));
                pause.removeConnectionPause();
                return;
            }

            if (iphr != null && ccBlacklist.contains(iphr.countryCode))
            {
                System.out.println("[IPHub] Player's country is blacklisted: " + event.getName() + ", " + iphr.countryName);
                event.cancelPlayerLogin(ColorUtil.translateAlternateColorCodes('&', String.valueOf(config.getConfigOption("settings.messages.blacklisted"))));
                pause.removeConnectionPause();
                return;
            }

            if (lastStatusCode == 429) needBackupKey = !needBackupKey; // if rate limit is hit, switch keys.
            System.out.println("[IPHub] Doing fresh API request for player: " + event.getName() + ", " + ip + ", using key: " + (needBackupKey ? "backup" : "primary"));
            try (CloseableHttpClient httpclient = HttpClients.createDefault())
            {
                HttpGet httpGet = new HttpGet("http://v2.api.iphub.info/ip/" + ip);
                httpGet.setHeader("X-Key", String.valueOf(config.getConfigOption(needBackupKey ? "settings.api.backupKey" : "settings.api.key")));
                try (CloseableHttpResponse res = httpclient.execute(httpGet))
                {
                    HttpEntity ent = res.getEntity();
                    int resCode = res.getStatusLine().getStatusCode();
                    lastStatusCode = resCode;
                    String rawResponse = EntityUtils.toString(ent);
                    if ((Boolean) config.getConfigOption("settings.developer.debug"))
                        System.out.println(rawResponse);
                    IPHubResponse response = gson.fromJson(rawResponse, IPHubResponse.class);
                    if (resCode != 200 && resCode != 429)
                    {
                        adminBroadcast(formatString(String.valueOf(config.getConfigOption("settings.messages.checkingError")), event.getName(), response), "iphub.warnblock2");
                        event.cancelPlayerLogin(ColorUtil.translateAlternateColorCodes('&', String.valueOf(config.getConfigOption("settings.messages.notChecked"))));
                        pause.removeConnectionPause();
                        return;
                    }
                    if ((Boolean) config.getConfigOption("settings.logging.enabled"))
                        System.out.println(String.format("[IPHub Log] %s: %s %s, %s (%s)", event.getName(), response.countryCode, ip, response.isp, response.asn));
                    if (response.block == 1)
                    {
                        event.cancelPlayerLogin(ColorUtil.translateAlternateColorCodes('&', String.valueOf(config.getConfigOption("settings.messages.vpnDetected"))));
                        adminBroadcast(formatString(String.valueOf(config.getConfigOption("settings.messages.vpnDetectedNotif")), event.getName(), response), "iphub.ipalert");
                        pause.removeConnectionPause();
                        return;
                    }
                    if (response.block == 2)
                        adminBroadcast(formatString(String.valueOf(config.getConfigOption("settings.messages.vpnPossible")), event.getName(), response), "iphub.warnblock2");
                    EntityUtils.consume(ent);
                    pause.removeConnectionPause();
                    cache.put(ip, response);
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                    event.cancelPlayerLogin(ColorUtil.translateAlternateColorCodes('&', String.valueOf(config.getConfigOption("settings.messages.notChecked"))));
                    pause.removeConnectionPause();
                }
            } catch (IOException e) {
                e.printStackTrace(System.err);
                event.cancelPlayerLogin(ColorUtil.translateAlternateColorCodes('&', String.valueOf(config.getConfigOption("settings.messages.notChecked"))));
                pause.removeConnectionPause();
            }
        }).start();
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
        for (Player p : Bukkit.getOnlinePlayers())
            if (p.hasPermission(permissionRequired) || p.isOp())
                p.sendMessage(ColorUtil.translateAlternateColorCodes('&', msg));
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
