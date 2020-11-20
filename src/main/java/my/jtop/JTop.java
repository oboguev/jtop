package my.jtop;

import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

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
    private static final String OPT_NOGRAPH = "nograph";

    private int pid = 0;
    private int refresh = 0;

    private Terminal terminal;
    private Screen screen;
    private boolean nograph = false;

    public void do_main(String[] args)
    {
        Options options = new Options();

        try
        {
            options.addOption(OPT_GROUPS, true, "thread group definition file");
            options.addOption(OPT_NOGRAPH, false, "do not use graphics");
            options.addOption(OPT_HELP, false, "show help");

            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args, true);

            args = cmd.getArgs();

            if (cmd.hasOption(OPT_HELP))
                usage(options, 0);

            if (cmd.hasOption(OPT_NOGRAPH))
                nograph = true;

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

            if (cmd.hasOption(OPT_GROUPS))
            {
                groupBy.load(cmd.getOptionValue(OPT_GROUPS));
            }
            else
            {
                groupBy.loadDefault();
            }

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

                if (nograph)
                {
                    System.out.println("");
                    for (String s : show)
                        System.out.println(s);
                }
                else
                {
                    show(show);
                }

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
                try
                {
                    if (screen != null)
                        screen.stopScreen();

                    if (terminal != null)
                    {
                        terminal.exitPrivateMode();
                        terminal.close();
                    }
                }
                catch (Exception ex2)
                {
                    Util.noop();
                }
                ex.printStackTrace();
                exit(1);
            }
        }
    }

    private void usage(Options options, int exitcode)
    {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(this.getClass().getName() + " [options] pid [refresh-rate]", options);
        exit(exitcode);
    }

    private void exit(int exitcode)
    {
        System.out.flush();
        System.err.flush();
        System.exit(exitcode);
    }

    private void show(List<String> show) throws Exception
    {
        // http://mabe02.github.io/lanterna/apidocs/3.0/index.html?overview-summary.html
        // http://mabe02.github.io/lanterna/apidocs/3.0/com/googlecode/lanterna/screen/Screen.html
        if (terminal == null)
        {
            terminal = new DefaultTerminalFactory().createTerminal();
            // terminal.enterPrivateMode();
            terminal.clearScreen();
        }

        if (screen == null)
        {
            screen = new TerminalScreen(terminal);
            screen.startScreen();
        }

        screen.clear();

        for (int row = 0; row < show.size(); row++)
        {
            String s = show.get(row);
            for (int col = 0; col < s.length(); col++)
            {
                char c = s.charAt(col);
                screen.setCharacter(col, row, new com.googlecode.lanterna.TextCharacter(c));
            }
        }

        screen.refresh(Screen.RefreshType.DELTA);
        terminal.flush();
    }
}
