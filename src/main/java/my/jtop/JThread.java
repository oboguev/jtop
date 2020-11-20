package my.jtop;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import my.jtop.util.Util;

public class JThread
{
    public static int PRIO_NONE = Integer.MIN_VALUE;

    public String name = null;
    public boolean daemon = false;
    public String state = null;
    public String state2 = null;
    public List<String> stack = new ArrayList<String>();
    public int prio = PRIO_NONE;
    public int os_prio = PRIO_NONE;
    public double cpu = 0;
    public double elapsed = 0;
    public double elapsed_max = 0;
    public int count = 1;

    public double diff_cpu = 0;
    public double diff_elapsed = 0;
    public double diff_elapsed_max = 0;

    public JThread()
    {

    }

    public JThread(JThread jt)
    {
        this.name = jt.name;
        this.daemon = jt.daemon;
        this.state = jt.state;
        this.state2 = jt.state2;
        this.stack.addAll(jt.stack);
        this.prio = jt.prio;
        this.os_prio = jt.os_prio;
        this.cpu = jt.cpu;
        this.elapsed = jt.elapsed;
        this.elapsed_max = jt.elapsed_max;
        this.count = jt.count;
    }

    private Pattern pat1 = Pattern.compile("^#[0-9]+$");

    // "Lightspeed.0-main" #1 prio=5 os_prio=0 cpu=921.46ms elapsed=806.71s tid=0x00007f4e3023d6f0 nid=0x2731 in Object.wait()  [0x00007f4e369ab000]
    // "Signal Dispatcher" #4 daemon prio=9 os_prio=-4 cpu=0.56ms elapsed=806.65s tid=0x00007f14f05c6d30 nid=0x273a runnable  [0x0000000000000000]
    // "Reference Handler" #2 daemon prio=10 os_prio=-5 cpu=1.15ms elapsed=806.66s tid=0x00007f14f05a9950 nid=0x2738 waiting on condition  [0x00007f4e14b34000]
    public void parse(String line) throws Exception
    {
        Token token = new Token();
        line = extractName(line);

        line = extractToken(line, token);
        if (token.token.startsWith("#") && pat1.matcher(token.token).find())
            line = extractToken(line, token);

        line = extractToken(line, token);
        if (token.token.equals("daemon"))
        {
            daemon = true;
            line = extractToken(line, token);
        }

        if (token.token.startsWith("prio="))
        {
            prio = Integer.parseInt(token.token.substring("prio=".length()));
            line = extractToken(line, token);
        }

        if (token.token.startsWith("os_prio="))
        {
            os_prio = Integer.parseInt(token.token.substring("os_prio=".length()));
            line = extractToken(line, token);
        }

        if (token.token.startsWith("cpu="))
        {
            cpu = getTime(token.token.substring("cpu=".length()));
            line = extractToken(line, token);
        }

        if (token.token.startsWith("elapsed="))
        {
            elapsed_max = elapsed = getTime(token.token.substring("elapsed=".length()));
            line = extractToken(line, token);
        }

        if (token.token.startsWith("tid="))
        {
            // ignore
            line = extractToken(line, token);
        }

        if (token.token.startsWith("nid="))
        {
            // ignore
        }

        line = line.trim();

        if (line.startsWith("runnable"))
        {
            state2 = "runnable";
        }
        else if (line.startsWith("waiting on condition"))
        {
            state2 = "waiting condition";
        }
        else if (line.startsWith("in "))
        {
            state2 = "waiting monitor";
        }
    }

    private static class Token
    {
        String token;
    }

    private String extractName(String line) throws Exception
    {
        StringBuilder sb = new StringBuilder();
        if (line.length() == 0)
            throw badStruct();

        int k = 0;
        boolean escape = false;
        for (;; k++)
        {
            if (k == line.length())
                throw badStruct();

            char c = line.charAt(k);

            if (k == 0)
            {
                if (c != '"')
                    throw badStruct();
            }
            else if (c == '\\')
            {
                escape = true;
            }
            else if (escape)
            {
                sb.append(c);
                escape = false;
            }
            else if (c == '"')
            {
                break;
            }
            else
            {
                sb.append(c);
            }
        }

        if (k == line.length())
            line = "";
        else
            line = next(line.substring(k + 1), true);

        name = sb.toString();

        return line;
    }

    private String extractToken(String line, Token token) throws Exception
    {
        StringBuilder sb = new StringBuilder();
        line = next(line, false);

        int k = 0;

        for (;; k++)
        {
            if (k == line.length())
                break;

            char c = line.charAt(k);

            if (c == ' ')
                break;

            sb.append(c);
        }

        if (k == line.length())
            line = "";
        else
            line = next(line.substring(k + 1), false);

        token.token = sb.toString();

        return line;
    }

    private String next(String line, boolean mustSpace) throws Exception
    {
        if (line.length() == 0)
            return line;

        if (mustSpace && !line.startsWith(" "))
            throw badStruct();

        while (line.startsWith(" "))
            line = line.substring(1);

        return line;
    }

    private double getTime(String s) throws Exception
    {
        double scale = 1.0;

        if (s.endsWith("ms"))
        {
            scale = 0.001;
            s = Util.stripTail(s, "ms");
        }
        else if (s.endsWith("s"))
        {
            scale = 1.0;
            s = Util.stripTail(s, "s");
        }
        else
        {
            throw badStruct();
        }

        return scale * Double.parseDouble(s);
    }

    public void merge(JThread jt)
    {
        cpu += jt.cpu;
        elapsed += jt.elapsed;
        elapsed_max = Math.max(elapsed_max, jt.elapsed_max);
        count += 1;

        if (prio != jt.prio)
            prio = PRIO_NONE;

        if (os_prio != jt.os_prio)
            prio = PRIO_NONE;
    }

    private Exception badStruct()
    {
        return new Exception("Invalid or unexpected structure of jcmd Thread.print output");
    }
}
