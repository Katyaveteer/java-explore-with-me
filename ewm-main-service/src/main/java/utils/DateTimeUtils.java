package utils;


import java.time.LocalDateTime;

public class DateTimeUtils {
    public static LocalDateTime parse(String dateTimeStr) {
        if (dateTimeStr == null) return null;
        return LocalDateTime.parse(dateTimeStr.replace(" ", "T"));
    }

    public static String format(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
