package net.oldschoolminecraft.iph.util;

public class ColorUtil
{
    public static String translateAlternateColorCodes(final char altColorChar, final String textToTranslate)
    {
        final char[] b = textToTranslate.toCharArray();
        for (int i = 0; i < b.length - 1; ++i)
        {
            if (b[i] == altColorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(b[i + 1]) > -1)
            {
                b[i] = '\u00a7';
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }
        return new String(b);
    }
}
