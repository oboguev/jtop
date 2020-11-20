package my.jtop;

import java.util.ArrayList;
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
                jt.diff_cpu -= jtp.cpu;
                jt.diff_elapsed -= jtp.elapsed;
                jt.diff_elapsed_max -= jtp.elapsed_max;
            }
        }
    }

    public List<String> show(JThreads prev) throws Exception
    {
        List<String> show = new ArrayList<String>();
        return show;
    }
}
