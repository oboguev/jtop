package my.jtop.util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Util
{
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
}
