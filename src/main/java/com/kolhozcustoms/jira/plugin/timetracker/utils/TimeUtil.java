package com.kolhozcustoms.jira.plugin.timetracker.utils;

import java.util.concurrent.TimeUnit;
import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.text.ParseException;

public final class TimeUtil { // public
    /**
     * Convert number of seconds into a readable time string
     * Example readable time String
     * - 2 days 3 hours 1 minute 1 second
     * - 1 day 1 hour 4 minutes 5 seconds
     * - 10 hours 45 minutes 50 seconds
     * @param numberOfSeconds number of seconds
     * @return readable time string
     */
    public static String getReadableTimeFromSeconds(long numberOfSeconds) {
        //long numberOfDays = TimeUnit.SECONDS.toDays(numberOfSeconds);
        //numberOfSeconds -= TimeUnit.DAYS.toSeconds(numberOfDays);
        long numberOfDays = TimeUtil.toDays8h(numberOfSeconds);
        numberOfSeconds -= TimeUtil.dayToSeconds8h(numberOfDays);

        long numberOfHours = TimeUnit.SECONDS.toHours(numberOfSeconds);
        numberOfSeconds -= TimeUnit.HOURS.toSeconds(numberOfHours);

        long numberOfMinutes = TimeUnit.SECONDS.toMinutes(numberOfSeconds);
        numberOfSeconds -= TimeUnit.MINUTES.toSeconds(numberOfMinutes);

        StringBuilder stringBuilder = new StringBuilder();

        if(numberOfDays > 0) {
                stringBuilder.append(String.format("%dd ", numberOfDays));
        }

        if(numberOfHours > 0) {
                stringBuilder.append(String.format("%dh ", numberOfHours));
        }

        if(numberOfMinutes > 0) {
                stringBuilder.append(String.format("%dm ", numberOfMinutes));
        }

        if(numberOfSeconds > 0) {
                stringBuilder.append(String.format("%ds ", numberOfSeconds));
        }

        return stringBuilder.toString();
    }

    private static Long toDays8h(long numberOfSeconds){
        long numberOfDays = numberOfSeconds / (60 * 60 * 8);
        return numberOfDays;
    }

    private static Long dayToSeconds8h(long numberOfDays){
        long numberOfSeconds = numberOfDays * 60 * 60 * 8;
        return numberOfSeconds;
    }

    public static Long toTimeStamp(String inputString, String format) {
        Long timestamp = null;
        SimpleDateFormat oldFormat = new SimpleDateFormat(format);
        try {
            timestamp = oldFormat.parse(inputString).getTime();
        } catch (ParseException e) {
            System.out.println("toTimeStamp exeption: " + e.toString() +"; ");
        }
        return timestamp;
    }

    public static String fromTimeStamp(Long inputTimeStamp, String format) {
        String reformattedStr = null;
        SimpleDateFormat newFormat = new SimpleDateFormat(format);
            reformattedStr = newFormat.format(new Date(inputTimeStamp));
        return reformattedStr;
    }

    public static String now(String format) {
        String reformattedStr = null;
        SimpleDateFormat newFormat = new SimpleDateFormat(format);
            reformattedStr = newFormat.format(new Date());
        return reformattedStr;
    }
    public static Long nowTimeStamp() {
        return new Date().getTime();
    }

    public static String dateTimeFormat(String inputString, String fromFormat, String toFormat) {
        String reformattedStr = null;
        SimpleDateFormat oldFormat = new SimpleDateFormat(fromFormat);
        SimpleDateFormat newFormat = new SimpleDateFormat(toFormat);
        try {
            reformattedStr = newFormat.format(oldFormat.parse(inputString));
        } catch (ParseException e) {
            System.out.println("dateTimeFormat exeption: " + e.toString() +"; ");
        }
        return reformattedStr;
    }
}