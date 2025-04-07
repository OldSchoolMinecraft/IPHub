package net.oldschoolminecraft.iph.tracking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IPLookup
{
    public List<String> names;

    public IPLookup()
    {
        names = new ArrayList<>();
    }

    public void addName(String name)
    {
        if (names.contains(name)) return; // Name already recorded
        this.names.add(name);
    }

    public IPLookup(String name)
    {
        this.names = Collections.singletonList(name);
    }
}
