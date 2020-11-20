package my.jtop;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import my.jtop.util.Util;

public class GroupBy
{
    private static class GroupMatcher
    {
        String group;
        Pattern pattern;
    }

    private List<GroupMatcher> matchers = new ArrayList<GroupMatcher>();

    public void load(String file) throws Exception
    {
        String content = Util.readFileAsUTF8(file);

        for (String line : content.replace("\r", "").split("\n"))
        {
            line = line.trim();
            if (line.equals("") || line.startsWith("#"))
                continue;
            int k = line.indexOf("=");
            if (k == -1)
                throw new Exception("Invalid file");
            String group = line.substring(0, k);
            String pattern = line.substring(k + 1);
            addGroupPattern(group, pattern);
        }
    }

    public void loadDefault() throws Exception
    {
        addGroupPattern("JVM runtime", "Reference Handler");
        addGroupPattern("JVM runtime", "Finalizer");
        addGroupPattern("JVM runtime", "Signal Dispatcher");
        addGroupPattern("JVM runtime", "Service Thread");
        addGroupPattern("JVM runtime", "C1 CompilerThread[0-9]+");
        addGroupPattern("JVM runtime", "C2 CompilerThread[0-9]+");
        addGroupPattern("JVM runtime", "Sweeper thread");
        addGroupPattern("JVM runtime", "Common-Cleaner");
        addGroupPattern("JVM runtime", "Notification Thread");
        addGroupPattern("JVM runtime", "Attach Listener");
        addGroupPattern("JVM runtime", "VM Thread");
        addGroupPattern("JVM runtime", "VM Periodic Task Thread");

        addGroupPattern("worker", "worker-[0-9]+");
        addGroupPattern("worker", "Worker-[0-9]+");

        addGroupPattern("MYSQL", "mysql-cj-abandoned-connection-cleanup");

        addGroupPattern("YJP", "YJPAgent-.*");
        addGroupPattern("YJP", "YJP-Plugin-RequestListener");

        addGroupPattern("GC", "GC Thread#[0-9]+");
        addGroupPattern("GC", "G1 Main Marker");
        addGroupPattern("GC", "G1 Conc#[0-9]+");
        addGroupPattern("GC", "G1 Refine#[0-9]+");
        addGroupPattern("GC", "G1 Young RemSet Sampling");
        addGroupPattern("GC", "G1 Main Concurrent Mark GC Thread");
        addGroupPattern("GC", "G1 Concurrent Refinement Thread#[0-9]+");
        addGroupPattern("GC", "Gang worker#[0-9]+ (Parallel GC Threads)");
        addGroupPattern("GC", "Gang worker#[0-9]+ (G1 Parallel Marking Threads)");
        addGroupPattern("GC", "G1 Concurrent Refinement Thread#[0-9]+");
        addGroupPattern("GC", "Surrogate Locker Thread (Concurrent GC)");

        addGroupPattern("Lightspeed-event", "Lightspeed\\.[0-9]+-event-[0-9]+");
        addGroupPattern("Lightspeed-tenant", "Lightspeed\\.[0-9]+-tenant-.*");
    }

    private void addGroupPattern(String group, String pattern) throws Exception
    {
        GroupMatcher gm = new GroupMatcher();
        gm.group = group;
        gm.pattern = Pattern.compile("^" + pattern + "$");
        matchers.add(gm);
    }

    public String resolve(String name) throws Exception
    {
        String group = null;

        for (GroupMatcher gm : matchers)
        {
            if (gm.pattern.matcher(name).find())
            {
                if (group == null)
                {
                    group = gm.group;
                }
                else if (!group.equals(gm.group))
                {
                    throw new Exception("Thread [" + name + "] matches multiple name groups");
                }
            }
        }

        if (group != null)
            return "[" + group + "]";

        return name;
    }
}
