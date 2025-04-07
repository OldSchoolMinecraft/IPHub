package net.oldschoolminecraft.iph.tracking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class NameLookup
{
    public List<String> addresses;

    public NameLookup()
    {
        addresses = new ArrayList<>();
    }

    public void enforceNoDuplicates()
    {
        List<String> addresses = this.addresses.stream().distinct().collect(Collectors.toList());
        this.addresses.clear();
        this.addresses.addAll(addresses);
    }

    public void addIP(String ip)
    {
        if (addresses.contains(ip)) return; // IP already recorded
        addresses.add(ip);
    }

    public NameLookup(String ip)
    {
        this.addresses = Collections.singletonList(ip);
    }
}
