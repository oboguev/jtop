package my.jtop;

import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import my.jtop.util.Util;

public class JTop
{
    GroupBy groupBy;

    public static void main(String[] args)
    {
        new JTop().do_main(args);
    }

    private static final String OPT_GROUPS = "groups";
    private static final String OPT_HELP = "help";

    private int pid = 0;
    private int refresh = 0;

    public void do_main(String[] args)
    {
        Options options = new Options();

        try
        {
            options.addOption(OPT_GROUPS, true, "thread group definition file");
            options.addOption(OPT_HELP, false, "show help");

            if (args.length == 0)
                usage(options, 0);

            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption(OPT_HELP))
            {
                usage(options, 0);
            }

            if (args.length == 2)
            {
                pid = Integer.parseInt(args[0]);
                refresh = Integer.parseInt(args[1]);
            }
            else if (args.length == 1)
            {
                pid = Integer.parseInt(args[0]);
            }
            else
            {
                usage(options, 1);
            }

            if (pid <= 0 || refresh < 0)
                usage(options, 1);

            options = null;

            groupBy = new GroupBy();
            groupBy.load();

            JThreads prev = null;

            for (;;)
            {
                StringBuilder sb = new StringBuilder();
                String[] jcmd = { "jcmd", "" + pid, "Thread.print" };
                if (0 != Util.shell(jcmd, sb))
                {
                    if (prev != null)
                    {
                        exit(0);
                    }
                    else
                    {
                        System.out.println(sb.toString());
                        exit(1);
                    }
                }

                JThreads jts = JThreads.parse(sb.toString(), groupBy);
                jts.diff(prev);

                List<String> show = jts.show(prev);
                for (String s : show)
                    System.out.println(s);

                if (refresh == 0)
                    break;

                Thread.sleep(refresh * 1000);

                prev = jts;
            }
        }
        catch (Exception ex)
        {
            if (options != null)
            {
                usage(options, 1);
            }
            else
            {
                ex.printStackTrace();
                exit(1);
            }
        }
    }

    private void usage(Options options, int exitcode)
    {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(this.getClass().getName(), options);
        exit(exitcode);
    }

    private void exit(int exitcode)
    {
        System.out.flush();
        System.err.flush();
        System.exit(exitcode);
    }
}
