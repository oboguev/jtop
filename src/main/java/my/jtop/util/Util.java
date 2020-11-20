package my.jtop.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Util
{
    static public boolean True = true;
    static public boolean False = false;

    static public void noop()
    {
    }

    static public byte[] readFileAsByteArray(String path) throws Exception
    {
        return Files.readAllBytes(Paths.get(path));
    }

    static public String readFileAsUTF8(String path) throws Exception
    {
        byte[] bytes = readFileAsByteArray(path);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static String stripTail(String s, String tail) throws Exception
    {
        if (!s.endsWith(tail))
            throw new Exception("stripTail: [" + s + "] does not end with [" + tail + "]");
        return s.substring(0, s.length() - tail.length());
    }

    public static int shell(String[] cmd, StringBuilder out) throws Exception
    {
        InputStream is = null;
        BufferedReader reader = null;

        out.setLength(0);

        try
        {
            ProcessBuilder builder = new ProcessBuilder(cmd);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            is = process.getInputStream();
            reader = new BufferedReader(new InputStreamReader(is));

            String line = null;
            while ((line = reader.readLine()) != null)
            {
                out.append(line);
                out.append('\n');
            }

            return process.waitFor();
        }
        finally
        {
            release(reader);
            release(is);
        }
    }

    public static void release(Closeable c)
    {
        if (c != null)
        {
            try
            {
                c.close();
            }
            catch (Exception ex)
            {
                noop();
            }
        }
    }

    public static void release(AutoCloseable c)
    {
        if (c != null)
        {
            try
            {
                c.close();
            }
            catch (Exception ex)
            {
                noop();
            }
        }
    }
}
