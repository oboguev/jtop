package my.jtop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JThreads
{
    public List<JThread> threads = new ArrayList<JThread>();
    private Map<String, JThread> name2thread = new HashMap<String, JThread>();

    static public JThreads parse(String s, GroupBy groupBy) throws Exception
    {
        JThreads jts = new JThreads();
        jts.do_parse(s);
        jts = jts.engroup(groupBy);
        return jts;
    }

    private void do_parse(String hsout) throws Exception
    {
        JThread jt = null;

        hsout = hsout.replace("\r", "");
        for (String s : hsout.split("\n"))
        {
            if (jt == null && s.startsWith("\""))
            {
                jt = new JThread();
                jt.parse(s);
            }
            else if (jt == null)
            {
                if (threads.size() == 0 || s.startsWith("JNI global refs:") || s.startsWith("JNI global references:"))
                {
                    // ignore the line
                }
                else if (isExceptionLine(s))
                {
                    throw new Exception(s);
                }
                else
                {
                    throw new Exception("Invalid structure of Threads.print output");
                }
            }
            else if (s.length() != 0)
            {
                String prefix = "java.lang.Thread.State: ";
                String trim = s.trim();
                if (trim.startsWith(prefix))
                    jt.state = trim.substring(prefix.length());
                else
                    jt.stack.add(s);
            }
            else
            {
                JThread jtx = name2thread.get(jt.name);
                if (jtx == null)
                {
                    threads.add(jt);
                    name2thread.put(jt.name, jt);
                }
                else
                {
                    jtx.merge(jt);
                }
                jt = null;
            }
        }
    }

    private JThreads engroup(GroupBy groupBy) throws Exception
    {
        JThreads jts = new JThreads();

        for (JThread jt : threads)
        {
            JThread jtx = new JThread(jt);
            jtx.name = groupBy.resolve(jtx.name);
            JThread jtm = jts.name2thread.get(jtx.name);
            if (jtm == null)
            {
                jts.threads.add(jtx);
                jts.name2thread.put(jtx.name, jtx);
            }
            else
            {
                jtm.merge(jtx);
            }
        }

        return jts;
    }

    private boolean isExceptionLine(String s) throws Exception
    {
        return s.length() != 0 && s.charAt(0) != ' ' && s.contains("Exception");
    }

    public void diff(JThreads prev) throws Exception
    {
        if (prev == null)
        {
            for (JThread jt : threads)
            {
                jt.diff_cpu = jt.cpu;
                jt.diff_elapsed = jt.elapsed;
                jt.diff_elapsed_max = jt.elapsed_max;
            }
            return;
        }

        for (String tn : name2thread.keySet())
        {
            JThread jt = name2thread.get(tn);
            JThread jtp = prev.name2thread.get(tn);

            if (jtp == null)
            {
                jt.diff_cpu = jt.cpu;
                jt.diff_elapsed = jt.elapsed;
                jt.diff_elapsed_max = jt.elapsed_max;
            }
            else
            {
                jt.diff_cpu = jtp.cpu - jtp.cpu;
                jt.diff_elapsed = jt.elapsed - jtp.elapsed;
                jt.diff_elapsed_max = jt.elapsed_max - jtp.elapsed_max;
            }
        }
    }

    private static class SortByCpu implements Comparator<JThread>
    {
        @Override
        public int compare(JThread jt1, JThread jt2)
        {
            if (jt1.cpu > jt2.cpu)
                return -1;
            else if (jt1.cpu < jt2.cpu)
                return 1;
            else
                return 0;
        }
    }

    public List<String> show(JThreads prev) throws Exception
    {
        List<String> show = new ArrayList<String>();

        double max_elapsed = 0;
        double max_diff_elapsed = 0;
        int len_name = 0;
        int len_c1 = "HIST".length();
        int len_c2 = "CURR".length();
        int len_c3 = "COUNT".length();

        for (JThread jt : threads)
        {
            max_elapsed = Math.max(jt.elapsed, max_elapsed);
            max_diff_elapsed = Math.max(jt.diff_elapsed, max_diff_elapsed);
            len_name = Math.max(jt.name.length(), len_name);

            String s = String.format("%.1f", 100.0 * jt.cpu / max_elapsed);
            len_c1 = Math.max(s.length(), len_c1);

            if (prev != null)
            {
                s = String.format("%.1f", 100.0 * jt.diff_cpu / jt.diff_elapsed);
                len_c2 = Math.max(s.length(), len_c2);
            }

            len_c1 = Math.max(width(jt.count), len_c3);
        }

        Collections.sort(threads, new SortByCpu());

        String header = center("THREADS, CPU cores usage (%):", len_name) + "  " + center("HIST", len_c1);
        if (prev != null)
            header += "  " + center("CURR", len_c1);
        header += "  " + center("COUNT", len_c3);
        show.add(header);

        header = repeat('=', len_name) + "  " + repeat('=', len_c1);
        if (prev != null)
            header += "  " + repeat('=', len_c1);
        header += "  " + repeat('=', len_c3);
        show.add(header);

        if (prev == null)
        {
            for (JThread jt : threads)
            {
                String count = "";
                if (jt.count > 1)
                    count = String.format("%" + len_c3 + "d", jt.count);

                show.add(String.format("%-" + len_name + "s  %" + len_c1 + ".1f  %s",
                                       jt.name,
                                       100.0 * jt.cpu / max_elapsed,
                                       count));
            }
        }
        else
        {
            for (JThread jt : threads)
            {
                String count = "";
                if (jt.count > 1)
                    count = String.format("%" + len_c3 + "d", jt.count);

                show.add(String.format("%-" + len_name + "s  %" + len_c1 + ".1f  %" + len_c2 + ".1f  %s",
                                       jt.name,
                                       100.0 * jt.cpu / max_elapsed,
                                       100.0 * jt.diff_cpu / jt.diff_elapsed,
                                       count));
            }
        }

        return show;
    }

    private int width(double f)
    {
        int len = 3;

        while (f >= 10)
        {
            f /= 10;
            len++;
        }

        return len;
    }

    private int width(int i)
    {
        int len = 1;

        while (i >= 10)
        {
            i /= 10;
            len++;
        }

        return len;
    }

    private String repeat(char c, int width)
    {
        StringBuilder sb = new StringBuilder();
        while (width-- > 0)
            sb.append(c);
        return sb.toString();
    }

    private String center(String s, int width)
    {
        String xs;
        xs = repeat(' ', (width - s.length()) / 2);
        if (0 != ((width - s.length()) % 2))
            xs += " ";
        xs += s;
        xs = xs + repeat(' ', width - xs.length());
        return xs;
    }
}
