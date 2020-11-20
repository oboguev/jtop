# JTop

JTop is an utility to display CPU usage by Java threads inside the Java process.

Internally it uses jcmd and displays results returned by jcmd. Accordingly, JDK (not JRE) is required. JDK must be of sufficiently recent version, as older versions do not return thread CPU usage information. ``jcmd`` must be on the path.

To build:

```sh
$ cd scripts
$ ./build.sh
```
Build result are copied to subdirectory ``dist``.

Command line:

```sh
$ jtop [-nograph] [-groups groupfile] pid [refresh-rate]
```

JTop can optionally group threads into groups and display aggregate data for a group of threads, rather than for every individual thread. Grouping is defined by a file listing thread name patterns for each group. For the example file see ``jtop.groups``. If grouping file is not specified, basic hardwired defaults are used. These defaults will group JVM runtime threads, garbage collection threads and some other threads.

For every thread or thread group, JTop will display its name, accumulated CPU usage over application run time, CPU usage over last refresh interval and the number of threads in a group.

Information is displayed only for currently existing threads, not for terminated threads.

If stdout is a tty, JTop will try to use graphic display (curses style), otherwise it will use plain text display. 
Option ``-nograph`` forces JTop to use plain text display.

``refresh-rate`` is specified in seconds. If ``refresh-rate`` is not specified, JTop will only display (once) accumulated CPU usage for threads over target application run time and then exit.

To quit from JTop during refresh mode, type ``escape`` or ``q``.
To pause refresh cycles, type ``space``.
To resume from the paused state, type ``space`` again.
