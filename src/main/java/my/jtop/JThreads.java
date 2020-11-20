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
                jt.diff_cpu = jt.cpu - jtp.cpu;
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

        double max_elapsed_max = 0;
        double max_diff_elapsed_max = 0;
        int len_name = "THREADS, CPU cores usage (%):".length();
        int len_c1 = "HIST".length();
        int len_c2 = "CURR".length();
        int len_c3 = "COUNT".length();

        for (JThread jt : threads)
        {
            max_elapsed_max = Math.max(jt.elapsed_max, max_elapsed_max);
            max_diff_elapsed_max = Math.max(jt.diff_elapsed_max, max_diff_elapsed_max);
        }

        for (JThread jt : threads)
        {
            len_name = Math.max(jt.name.length(), len_name);

            String c1 = String.format("%.1f", 100.0 * jt.cpu / max_elapsed_max);
            len_c1 = Math.max(c1.length(), len_c1);

            if (prev != null)
            {
                String c2 = String.format("%.1f", 100.0 * jt.diff_cpu / jt.diff_elapsed);
                len_c2 = Math.max(c2.length(), len_c2);
            }

            String c3 = String.format("%d", jt.count);
            len_c3 = Math.max(c3.length(), len_c3);
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

        String sep = "  ";

        if (prev == null)
        {
            for (JThread jt : threads)
            {
                String c1 = String.format("%.1f", 100.0 * jt.cpu / max_elapsed_max);
                String c2 = "";
                String c3 = (jt.count <= 1) ? "" : String.format("%" + len_c3 + "d", jt.count);

                show.add(left(jt.name, len_name) + sep + right(c1, len_c1) + sep + right(c3, len_c3));
            }
        }
        else
        {
            for (JThread jt : threads)
            {
                String c1 = String.format("%.1f", 100.0 * jt.cpu / max_elapsed_max);
                String c2 = String.format("%.1f", 100.0 * jt.diff_cpu / jt.diff_elapsed);
                String c3 = (jt.count <= 1) ? "" : String.format("%" + len_c3 + "d", jt.count);

                show.add(left(jt.name, len_name) + sep + right(c1, len_c1) + sep + right(c2, len_c2) + sep + right(c3, len_c3));
            }
        }

        return show;
    }

    private String left(String s, int width)
    {
        return s + repeat(' ', width - s.length());
    }

    private String right(String s, int width)
    {
        return repeat(' ', width - s.length()) + s;
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
