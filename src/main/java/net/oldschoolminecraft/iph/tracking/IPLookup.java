package net.oldschoolminecraft.iph.tracking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class IPLookup
{
    public List<String> names;

    public IPLookup()
    {
        names = new ArrayList<>();
    }

    public void enforceNoDuplicates()
    {
        List<String> names = this.names.stream().distinct().collect(Collectors.toList());
        this.names.clear();
        this.names.addAll(names);
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
