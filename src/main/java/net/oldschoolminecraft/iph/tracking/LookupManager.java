package net.oldschoolminecraft.iph.tracking;

import com.google.gson.Gson;
import net.oldschoolminecraft.iph.IPHub;

import java.io.*;

public class LookupManager
{
    private static final Gson gson = new Gson();

    private final File dataFolder = IPHub.instance.getDataFolder();
    private final File trackingDir = new File(dataFolder, "tracking/");
    private final File ipdb = new File(trackingDir, "ipdb/");
    private final File namedb = new File(trackingDir, "namedb/");

    public boolean hasNameLookup(String username)
    {
        return new File(namedb, username + ".json").exists();
    }

    public boolean hasIPLookup(String ip)
    {
        return new File(ipdb, ip + ".json").exists();
    }

    public IPLookup getNamesFromIP(String ip)
    {
        try (FileReader reader = new FileReader(new File(ipdb, ip + ".json")))
        {
            return gson.fromJson(reader, IPLookup.class);
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            return null;
        }
    }

    public NameLookup getIPsFromName(String name)
    {
        try (FileReader reader = new FileReader(new File(namedb, name + ".json")))
        {
            return gson.fromJson(reader, NameLookup.class);
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            return null;
        }
    }

    public void updateLookupData(String name, String ip)
    {
        File nameFile = new File(namedb, name + ".json");
        File ipFile = new File(ipdb, ip + ".json");

        NameLookup nameLookup = null;
        IPLookup ipLookup = null;

        if (nameFile.exists())
        {
            try (FileReader reader = new FileReader(nameFile))
            {
                nameLookup = gson.fromJson(reader, NameLookup.class);
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
                return;
            }
        }

        if (ipFile.exists())
        {
            try (FileReader reader = new FileReader(ipFile))
            {
                ipLookup = gson.fromJson(reader, IPLookup.class);
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
                return;
            }
        }

        try (FileWriter nameWriter = new FileWriter(nameFile);
             FileWriter ipWriter = new FileWriter(ipFile))
        {
            if (nameLookup == null || ipLookup == null)
                System.out.println("[IPHub] Started tracking new IP/user (" + ip + "/" + name + ")");
            else System.out.println("[IPHub] Updated tracking info for IP/user (" + ip + "/" + name + ")");

            if (nameLookup == null) nameLookup = new NameLookup();
            if (ipLookup == null) ipLookup = new IPLookup();

            nameLookup.addIP(ip);
            ipLookup.addName(name);

            nameLookup.enforceNoDuplicates();
            ipLookup.enforceNoDuplicates();

            gson.toJson(nameLookup, NameLookup.class, nameWriter);
            gson.toJson(ipLookup, IPLookup.class, ipWriter);
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }
}
