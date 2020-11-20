package my.jtop;

import my.jtop.util.Util;

public class JTop
{
    GroupBy groupBy;

    public static void main(String[] args)
    {
        new JTop().do_main(args);
    }

    public void do_main(String[] args)
    {
        try
        {
            groupBy = new GroupBy();
            groupBy.load();

            String s = Util.readFileAsUTF8("P:\\@@\\jcmd.out");
            JThreads jts = JThreads.parse(s, groupBy);
            jts = null;
            System.out.println("= OK =");
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
