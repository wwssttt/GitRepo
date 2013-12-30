/*
 * Copyright (c) 2005-2006 Gregor Heinrich. All rights reserved. Redistribution and
 * use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met: 1. Redistributions of source
 * code must retain the above copyright notice, this list of conditions and the
 * following disclaimer. 2. Redistributions in binary form must reproduce the
 * above copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESSED OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.knowceans.util;

import java.util.Calendar;
import java.util.Date;

/**
 * TimeIntervals provides methods to find the time intervals (week, month, year)
 * surrounding a given date. Intervals are expressed as 2-arrays of Date object,
 * corresponding to the start and end times. By convention, intervals include
 * their bounds.
 * <p>
 * FIXME: daylight saving time shift can create gaps in week and month intervals
 * 
 * @author gregor
 */
public class TimeIntervals {

    public static void main(String[] args) {
        Date now = new Date();
        Date[] interval;

        // today
        interval = TimeIntervals.dayOf(now);
        System.out.println("today:  \t" + interval[0] + " -- " + interval[1]);
        // tomorrow
        interval = TimeIntervals.daysAway(now, 1);
        System.out.println("tomorrow:\t" + interval[0] + " -- " + interval[1]);

        // this week
        interval = TimeIntervals.weekOf(now);
        System.out.println("this week:\t" + interval[0] + " -- " + interval[1]);
        // next week
        interval = TimeIntervals.weeksAway(now, 1);
        System.out.println("next week:\t" + interval[0] + " -- " + interval[1]);

        // this month
        interval = TimeIntervals.monthOf(now);
        System.out
            .println("this month:\t" + interval[0] + " -- " + interval[1]);
        // next month
        interval = TimeIntervals.monthsAway(now, 1);
        System.out
            .println("next month:\t" + interval[0] + " -- " + interval[1]);

        // this year
        interval = TimeIntervals.yearOf(now);
        System.out.println("this year:\t" + interval[0] + " -- " + interval[1]);
        // next year
        Date[] interval2 = TimeIntervals.yearsAway(now, 1);
        System.out.println("next year:\t" + interval2[0] + " -- "
            + interval2[1]);

        interval = TimeIntervals.join(interval, interval2);
        // this and next year
        System.out.println("this+next year:\t" + interval[0] + " -- "
            + interval[1]);

        // exactly two weeks ago from today
        interval = TimeIntervals.awayExactDay(now, Calendar.YEAR, -2);
        System.out.println("ex. -2 months:\t" + interval[0] + " -- "
            + interval[1]);

    }

    /**
     * returns the start and end times of the day surrounding the date d.
     * 
     * @param d
     * @return
     */
    public static Date[] dayOf(Date d) {
        return daysAway(d, 0);
    }

    /**
     * returns the start and end times of the week surrounding the date d.
     * 
     * @param d
     * @return
     */
    public static Date[] weekOf(Date d) {
        return weeksAway(d, 0);
    }

    /**
     * returns the start and end times of the month surrounding the date d.
     * 
     * @param d
     * @return
     */
    public static Date[] monthOf(Date d) {
        return monthsAway(d, 0);
    }

    /**
     * returns the start and end times of the year surrounding the date d.
     * 
     * @param d
     * @return
     */
    public static Date[] yearOf(Date d) {
        return yearsAway(d, 0);
    }

    /**
     * returns the start and end times of the day before / after the day given
     * in date d.
     * 
     * @param d
     * @param away
     * @return
     */
    public static Date[] daysAway(Date d, int away) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        Calendar start = Calendar.getInstance();
        start.clear();
        start.setLenient(true);
        start.set(Calendar.YEAR, c.get(Calendar.YEAR));
        start.set(Calendar.DAY_OF_YEAR, c.get(Calendar.DAY_OF_YEAR) + away);
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.DAY_OF_YEAR, 1);
        end.add(Calendar.MILLISECOND, -1);
        return new Date[] {start.getTime(), end.getTime()};
    }

    /**
     * returns the start and end times of the week before / after the start of
     * the week surrounding the date d.
     * 
     * @param d
     * @param away
     * @return
     */
    public static Date[] weeksAway(Date d, int away) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        Calendar start = Calendar.getInstance();
        start.clear();
        start.setLenient(true);
        start.set(Calendar.YEAR, c.get(Calendar.YEAR));
        start.set(Calendar.WEEK_OF_YEAR, c.get(Calendar.WEEK_OF_YEAR) + away);
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.WEEK_OF_YEAR, 1);
        end.add(Calendar.MILLISECOND, -1);
        return new Date[] {start.getTime(), end.getTime()};
    }

    /**
     * returns the start and end times of the months before / after the start of
     * the month surrounding the date d.
     * 
     * @param d
     * @param away
     * @return
     */
    public static Date[] monthsAway(Date d, int away) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        Calendar start = Calendar.getInstance();
        start.clear();
        start.setLenient(true);
        start.set(Calendar.YEAR, c.get(Calendar.YEAR));
        start.set(Calendar.MONTH, c.get(Calendar.MONTH) + away);
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.MONTH, 1);
        end.add(Calendar.MILLISECOND, -1);
        return new Date[] {start.getTime(), end.getTime()};

    }

    /**
     * returns the start and end times of the years before / after the start of
     * the year surrounding the date d.
     * 
     * @param d
     * @param away
     * @return
     */
    public static Date[] yearsAway(Date d, int away) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        Calendar start = Calendar.getInstance();
        start.clear();
        start.setLenient(true);
        start.set(Calendar.YEAR, c.get(Calendar.YEAR) + away);
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.YEAR, 1);
        end.add(Calendar.MILLISECOND, -1);
        return new Date[] {start.getTime(), end.getTime()};

    }

    /**
     * returns the start and end times of the field (Calendar.*) before / after
     * the start of the day surrounding the date d.
     * 
     * @param d the date to start from
     * @param whatAway what time measure away (week, day, etc.)
     * @param howManyAway how many of the time measure away
     * @param howManyLong how many time measures as interval
     * @return
     */
    public static Date[] awayExactDay(Date d, int whatAway, int howManyAway,
        int howManyLong) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        Calendar start = Calendar.getInstance();
        start.clear();
        start.setLenient(true);
        start.setTime(d);
        start.add(whatAway, howManyAway);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        Calendar end = (Calendar) start.clone();
        end.add(whatAway, howManyLong);
        end.add(Calendar.MILLISECOND, -1);
        return new Date[] {start.getTime(), end.getTime()};
    }

    /**
     * returns the start and end times of the field (Calendar.*) before / after
     * the start of the day surrounding the date d, rounding the day. E.g.,
     * awayExactDay(now, Calendar.WEEK_OF_YEAR, 8, 8) returns an 8 weeks
     * interval from today 8 weeks ago to yesterday.
     * 
     * @param d the date to start from
     * @param whatAway what time measure away (week, day, etc.)
     * @param howManyAway how many of the time measure away
     * @return
     */
    public static Date[] awayExactDay(Date d, int whatAway, int howManyAway) {
        return awayExactDay(d, whatAway, howManyAway, 1);
    }

    // /**
    // * returns the start and end times of the field (Calendar.*) before /
    // after
    // * the start of the field surrounding the date d, rounding the day. E.g.,
    // * awayFromBeginOf(now, Calendar.WEEK_OF_YEAR, 8, 8) returns an 8 weeks
    // * interval from Monday 8 weeks ago to last Sunday.
    // *
    // * @param d the date to start from
    // * @param whatAway what time measure away (week, day, etc.)
    // * @param howManyAway how many of the time measure away
    // * @param howManyLong how many time measures as interval
    // * @return
    // */
    // public static Date[] awayFromBeginOf(Date d, int whatAway, int
    // howManyAway,
    // int howManyLong) {
    // Calendar c = Calendar.getInstance();
    // c.setTime(d);
    // Calendar start = Calendar.getInstance();
    // start.clear();
    // start.setLenient(true);
    // start.setTime(d);
    // start.add(whatAway, howManyAway);
    // start.set(Calendar.HOUR_OF_DAY, 0);
    // start.set(Calendar.MINUTE, 0);
    // start.set(Calendar.SECOND, 0);
    // start.set(Calendar.MILLISECOND, 0);
    // Calendar end = (Calendar) start.clone();
    // end.add(whatAway, howManyLong);
    // end.add(Calendar.MILLISECOND, -1);
    // return new Date[] {start.getTime(), end.getTime()};
    // }

    /**
     * check whether time is within the interval.
     * 
     * @param time
     * @param interval
     * @return
     */
    public static boolean isIn(Date time, Date[] interval) {
        return !time.before(interval[0]) && !time.after(interval[1]);
    }

    /**
     * joins the limits of the two intervals by taking the start of before and
     * the end of after.
     * 
     * @param before
     * @param after
     * @return
     */
    public static Date[] join(Date[] before, Date[] after) {
        return new Date[] {before[0], after[1]};
    }

}
