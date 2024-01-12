package net.oldschoolminecraft.iph.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimestampConverter
{
    public static String convertMillisToDateTimeString(long millis)
    {
        Date date = new Date(millis);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM/dd/yyyy @ hh:mm a");
        return sdf.format(date);
    }
}
