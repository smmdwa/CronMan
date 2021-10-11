package com.distribute.remoting.utils;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.util.*;

public class CronExpression implements Serializable, Cloneable {
    private static final long serialVersionUID = 12423409423L;
    protected static final int SECOND = 0;
    protected static final int MINUTE = 1;
    protected static final int HOUR = 2;
    protected static final int DAY_OF_MONTH = 3;
    protected static final int MONTH = 4;
    protected static final int DAY_OF_WEEK = 5;
    protected static final int YEAR = 6;
    protected static final int ALL_SPEC_INT = 99;
    protected static final int NO_SPEC_INT = 98;
    protected static final Integer ALL_SPEC = new Integer(99);
    protected static final Integer NO_SPEC = new Integer(98);
    protected static Map monthMap = new HashMap(20);
    protected static Map dayMap = new HashMap(60);
    protected String cronExpression = null;
    private TimeZone timeZone = null;
    protected transient TreeSet seconds;
    protected transient TreeSet minutes;
    protected transient TreeSet hours;
    protected transient TreeSet daysOfMonth;
    protected transient TreeSet months;
    protected transient TreeSet daysOfWeek;
    protected transient TreeSet years;
    protected transient boolean lastdayOfWeek = false;
    protected transient int nthdayOfWeek = 0;
    protected transient boolean lastdayOfMonth = false;
    protected transient boolean nearestWeekday = false;
    protected transient boolean expressionParsed = false;

    public CronExpression(String cronExpression) throws ParseException {
        if(cronExpression == null) {
            throw new IllegalArgumentException("cronExpression cannot be null");
        } else {
            this.cronExpression = cronExpression.toUpperCase(Locale.US);
            this.buildExpression(this.cronExpression);
        }
    }

    public boolean isSatisfiedBy(Date date) {
        Calendar testDateCal = Calendar.getInstance(this.getTimeZone());
        testDateCal.setTime(date);
        testDateCal.set(14, 0);
        Date originalDate = testDateCal.getTime();
        testDateCal.add(13, -1);
        Date timeAfter = this.getTimeAfter(testDateCal.getTime());
        return timeAfter != null && timeAfter.equals(originalDate);
    }

    public Date getNextValidTimeAfter(Date date) {
        return this.getTimeAfter(date);
    }

    public Date getNextInvalidTimeAfter(Date date) {
        long difference = 1000L;
        Calendar adjustCal = Calendar.getInstance(this.getTimeZone());
        adjustCal.setTime(date);
        adjustCal.set(14, 0);
        Date lastDate = adjustCal.getTime();
        Date newDate = null;

        while(difference == 1000L) {
            newDate = this.getTimeAfter(lastDate);
            difference = newDate.getTime() - lastDate.getTime();
            if(difference == 1000L) {
                lastDate = newDate;
            }
        }

        return new Date(lastDate.getTime() + 1000L);
    }

    public TimeZone getTimeZone() {
        if(this.timeZone == null) {
            this.timeZone = TimeZone.getDefault();
        }

        return this.timeZone;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public String toString() {
        return this.cronExpression;
    }

    public static boolean isValidExpression(String cronExpression) {
        try {
            new CronExpression(cronExpression);
            return true;
        } catch (ParseException var2) {
            return false;
        }
    }

    protected void buildExpression(String expression) throws ParseException {
        this.expressionParsed = true;

        try {
            if(this.seconds == null) {
                this.seconds = new TreeSet();
            }

            if(this.minutes == null) {
                this.minutes = new TreeSet();
            }

            if(this.hours == null) {
                this.hours = new TreeSet();
            }

            if(this.daysOfMonth == null) {
                this.daysOfMonth = new TreeSet();
            }

            if(this.months == null) {
                this.months = new TreeSet();
            }

            if(this.daysOfWeek == null) {
                this.daysOfWeek = new TreeSet();
            }

            if(this.years == null) {
                this.years = new TreeSet();
            }

            int e = 0;

            for(StringTokenizer exprsTok = new StringTokenizer(expression, " \t", false); exprsTok.hasMoreTokens() && e <= 6; ++e) {
                String dow = exprsTok.nextToken().trim();
                if(e == 3 && dow.indexOf(76) != -1 && dow.length() > 1 && dow.indexOf(",") >= 0) {
                    throw new ParseException("Support for specifying \'L\' and \'LW\' with other days of the month is not implemented", -1);
                }

                if(e == 5 && dow.indexOf(76) != -1 && dow.length() > 1 && dow.indexOf(",") >= 0) {
                    throw new ParseException("Support for specifying \'L\' with other days of the week is not implemented", -1);
                }

                StringTokenizer dom = new StringTokenizer(dow, ",");

                while(dom.hasMoreTokens()) {
                    String dayOfMSpec = dom.nextToken();
                    this.storeExpressionVals(0, dayOfMSpec, e);
                }
            }

            if(e <= 5) {
                throw new ParseException("Unexpected end of expression.", expression.length());
            } else {
                if(e <= 6) {
                    this.storeExpressionVals(0, "*", 6);
                }

                TreeSet var10 = this.getSet(5);
                TreeSet var11 = this.getSet(3);
                boolean var12 = !var11.contains(NO_SPEC);
                boolean dayOfWSpec = !var10.contains(NO_SPEC);
                if((!var12 || dayOfWSpec) && (!dayOfWSpec || var12)) {
                    throw new ParseException("Support for specifying both a day-of-week AND a day-of-month parameter is not implemented.", 0);
                }
            }
        } catch (ParseException var8) {
            throw var8;
        } catch (Exception var9) {
            throw new ParseException("Illegal cron expression format (" + var9.toString() + ")", 0);
        }
    }

    protected int storeExpressionVals(int pos, String s, int type) throws ParseException {
        byte incr = 0;
        int i = this.skipWhiteSpace(pos, s);
        if(i >= s.length()) {
            return i;
        } else {
            char c = s.charAt(i);
            if(c >= 65 && c <= 90 && !s.equals("L") && !s.equals("LW")) {
                String var13 = s.substring(i, i + 3);
                boolean var14 = true;
                int eval = -1;
                int var15;
                if(type == 4) {
                    var15 = this.getMonthNumber(var13) + 1;
                    if(var15 <= 0) {
                        throw new ParseException("Invalid Month value: \'" + var13 + "\'", i);
                    }

                    if(s.length() > i + 3) {
                        c = s.charAt(i + 3);
                        if(c == 45) {
                            i += 4;
                            var13 = s.substring(i, i + 3);
                            eval = this.getMonthNumber(var13) + 1;
                            if(eval <= 0) {
                                throw new ParseException("Invalid Month value: \'" + var13 + "\'", i);
                            }
                        }
                    }
                } else {
                    if(type != 5) {
                        throw new ParseException("Illegal characters for this position: \'" + var13 + "\'", i);
                    }

                    var15 = this.getDayOfWeekNumber(var13);
                    if(var15 < 0) {
                        throw new ParseException("Invalid Day-of-Week value: \'" + var13 + "\'", i);
                    }

                    if(s.length() > i + 3) {
                        c = s.charAt(i + 3);
                        if(c == 45) {
                            i += 4;
                            var13 = s.substring(i, i + 3);
                            eval = this.getDayOfWeekNumber(var13);
                            if(eval < 0) {
                                throw new ParseException("Invalid Day-of-Week value: \'" + var13 + "\'", i);
                            }
                        } else if(c == 35) {
                            try {
                                i += 4;
                                this.nthdayOfWeek = Integer.parseInt(s.substring(i));
                                if(this.nthdayOfWeek < 1 || this.nthdayOfWeek > 5) {
                                    throw new Exception();
                                }
                            } catch (Exception var11) {
                                throw new ParseException("A numeric value between 1 and 5 must follow the \'#\' option", i);
                            }
                        } else if(c == 76) {
                            this.lastdayOfWeek = true;
                            ++i;
                        }
                    }
                }

                if(eval != -1) {
                    incr = 1;
                }

                this.addToSet(var15, eval, incr, type);
                return i + 3;
            } else {
                int val;
                if(c == 63) {
                    ++i;
                    if(i + 1 < s.length() && s.charAt(i) != 32 && s.charAt(i + 1) != 9) {
                        throw new ParseException("Illegal character after \'?\': " + s.charAt(i), i);
                    } else if(type != 5 && type != 3) {
                        throw new ParseException("\'?\' can only be specfied for Day-of-Month or Day-of-Week.", i);
                    } else {
                        if(type == 5 && !this.lastdayOfMonth) {
                            val = ((Integer)this.daysOfMonth.last()).intValue();
                            if(val == 98) {
                                throw new ParseException("\'?\' can only be specfied for Day-of-Month -OR- Day-of-Week.", i);
                            }
                        }

                        this.addToSet(98, -1, 0, type);
                        return i;
                    }
                } else if(c != 42 && c != 47) {
                    if(c == 76) {
                        ++i;
                        if(type == 3) {
                            this.lastdayOfMonth = true;
                        }

                        if(type == 5) {
                            this.addToSet(7, 7, 0, type);
                        }

                        if(type == 3 && s.length() > i) {
                            c = s.charAt(i);
                            if(c == 87) {
                                this.nearestWeekday = true;
                                ++i;
                            }
                        }

                        return i;
                    } else if(c >= 48 && c <= 57) {
                        val = Integer.parseInt(String.valueOf(c));
                        ++i;
                        if(i >= s.length()) {
                            this.addToSet(val, -1, -1, type);
                            return i;
                        } else {
                            c = s.charAt(i);
                            if(c >= 48 && c <= 57) {
                                ValueSet vs = this.getValue(val, s, i);
                                val = vs.value;
                                i = vs.pos;
                            }

                            i = this.checkNext(i, s, val, type);
                            return i;
                        }
                    } else {
                        throw new ParseException("Unexpected character: " + c, i);
                    }
                } else if(c == 42 && i + 1 >= s.length()) {
                    this.addToSet(99, -1, incr, type);
                    return i + 1;
                } else if(c != 47 || i + 1 < s.length() && s.charAt(i + 1) != 32 && s.charAt(i + 1) != 9) {
                    if(c == 42) {
                        ++i;
                    }

                    c = s.charAt(i);
                    int var12;
                    if(c != 47) {
                        var12 = 1;
                    } else {
                        ++i;
                        if(i >= s.length()) {
                            throw new ParseException("Unexpected end of string.", i);
                        }

                        var12 = this.getNumericValue(s, i);
                        ++i;
                        if(var12 > 10) {
                            ++i;
                        }

                        if(var12 > 59 && (type == 0 || type == 1)) {
                            throw new ParseException("Increment > 60 : " + var12, i);
                        }

                        if(var12 > 23 && type == 2) {
                            throw new ParseException("Increment > 24 : " + var12, i);
                        }

                        if(var12 > 31 && type == 3) {
                            throw new ParseException("Increment > 31 : " + var12, i);
                        }

                        if(var12 > 7 && type == 5) {
                            throw new ParseException("Increment > 7 : " + var12, i);
                        }

                        if(var12 > 12 && type == 4) {
                            throw new ParseException("Increment > 12 : " + var12, i);
                        }
                    }

                    this.addToSet(99, -1, var12, type);
                    return i;
                } else {
                    throw new ParseException("\'/\' must be followed by an integer.", i);
                }
            }
        }
    }

    protected int checkNext(int pos, String s, int val, int type) throws ParseException {
        byte end = -1;
        if(pos >= s.length()) {
            this.addToSet(val, end, -1, type);
            return pos;
        } else {
            char c = s.charAt(pos);
            int i;
            TreeSet v2;
            if(c == 76) {
                if(type == 5) {
                    this.lastdayOfWeek = true;
                    v2 = this.getSet(type);
                    v2.add(new Integer(val));
                    i = pos + 1;
                    return i;
                } else {
                    throw new ParseException("\'L\' option is not valid here. (pos=" + pos + ")", pos);
                }
            } else if(c == 87) {
                if(type == 3) {
                    this.nearestWeekday = true;
                    v2 = this.getSet(type);
                    v2.add(new Integer(val));
                    i = pos + 1;
                    return i;
                } else {
                    throw new ParseException("\'W\' option is not valid here. (pos=" + pos + ")", pos);
                }
            } else if(c != 35) {
                ValueSet vs;
                int v3;
                int var14;
                if(c == 45) {
                    i = pos + 1;
                    c = s.charAt(i);
                    var14 = Integer.parseInt(String.valueOf(c));
                    int var13 = var14;
                    ++i;
                    if(i >= s.length()) {
                        this.addToSet(val, var14, 1, type);
                        return i;
                    } else {
                        c = s.charAt(i);
                        if(c >= 48 && c <= 57) {
                            vs = this.getValue(var14, s, i);
                            v3 = vs.value;
                            var13 = v3;
                            i = vs.pos;
                        }

                        if(i < s.length() && s.charAt(i) == 47) {
                            ++i;
                            c = s.charAt(i);
                            int var15 = Integer.parseInt(String.valueOf(c));
                            ++i;
                            if(i >= s.length()) {
                                this.addToSet(val, var13, var15, type);
                                return i;
                            } else {
                                c = s.charAt(i);
                                if(c >= 48 && c <= 57) {
                                    ValueSet var16 = this.getValue(var15, s, i);
                                    int v31 = var16.value;
                                    this.addToSet(val, var13, v31, type);
                                    i = var16.pos;
                                    return i;
                                } else {
                                    this.addToSet(val, var13, var15, type);
                                    return i;
                                }
                            }
                        } else {
                            this.addToSet(val, var13, 1, type);
                            return i;
                        }
                    }
                } else if(c == 47) {
                    i = pos + 1;
                    c = s.charAt(i);
                    var14 = Integer.parseInt(String.valueOf(c));
                    ++i;
                    if(i >= s.length()) {
                        this.addToSet(val, end, var14, type);
                        return i;
                    } else {
                        c = s.charAt(i);
                        if(c >= 48 && c <= 57) {
                            vs = this.getValue(var14, s, i);
                            v3 = vs.value;
                            this.addToSet(val, end, v3, type);
                            i = vs.pos;
                            return i;
                        } else {
                            throw new ParseException("Unexpected character \'" + c + "\' after \'/\'", i);
                        }
                    }
                } else {
                    this.addToSet(val, end, 0, type);
                    i = pos + 1;
                    return i;
                }
            } else if(type != 5) {
                throw new ParseException("\'#\' option is not valid here. (pos=" + pos + ")", pos);
            } else {
                i = pos + 1;

                try {
                    this.nthdayOfWeek = Integer.parseInt(s.substring(i));
                    if(this.nthdayOfWeek < 1 || this.nthdayOfWeek > 5) {
                        throw new Exception();
                    }
                } catch (Exception var12) {
                    throw new ParseException("A numeric value between 1 and 5 must follow the \'#\' option", i);
                }

                v2 = this.getSet(type);
                v2.add(new Integer(val));
                ++i;
                return i;
            }
        }
    }

    public String getCronExpression() {
        return this.cronExpression;
    }

    public String getExpressionSummary() {
        StringBuffer buf = new StringBuffer();
        buf.append("seconds: ");
        buf.append(this.getExpressionSetSummary((Set)this.seconds));
        buf.append("\n");
        buf.append("minutes: ");
        buf.append(this.getExpressionSetSummary((Set)this.minutes));
        buf.append("\n");
        buf.append("hours: ");
        buf.append(this.getExpressionSetSummary((Set)this.hours));
        buf.append("\n");
        buf.append("daysOfMonth: ");
        buf.append(this.getExpressionSetSummary((Set)this.daysOfMonth));
        buf.append("\n");
        buf.append("months: ");
        buf.append(this.getExpressionSetSummary((Set)this.months));
        buf.append("\n");
        buf.append("daysOfWeek: ");
        buf.append(this.getExpressionSetSummary((Set)this.daysOfWeek));
        buf.append("\n");
        buf.append("lastdayOfWeek: ");
        buf.append(this.lastdayOfWeek);
        buf.append("\n");
        buf.append("nearestWeekday: ");
        buf.append(this.nearestWeekday);
        buf.append("\n");
        buf.append("NthDayOfWeek: ");
        buf.append(this.nthdayOfWeek);
        buf.append("\n");
        buf.append("lastdayOfMonth: ");
        buf.append(this.lastdayOfMonth);
        buf.append("\n");
        buf.append("years: ");
        buf.append(this.getExpressionSetSummary((Set)this.years));
        buf.append("\n");
        return buf.toString();
    }

    protected String getExpressionSetSummary(Set set) {
        if(set.contains(NO_SPEC)) {
            return "?";
        } else if(set.contains(ALL_SPEC)) {
            return "*";
        } else {
            StringBuffer buf = new StringBuffer();
            Iterator itr = set.iterator();

            for(boolean first = true; itr.hasNext(); first = false) {
                Integer iVal = (Integer)itr.next();
                String val = iVal.toString();
                if(!first) {
                    buf.append(",");
                }

                buf.append(val);
            }

            return buf.toString();
        }
    }

    protected String getExpressionSetSummary(ArrayList list) {
        if(list.contains(NO_SPEC)) {
            return "?";
        } else if(list.contains(ALL_SPEC)) {
            return "*";
        } else {
            StringBuffer buf = new StringBuffer();
            Iterator itr = list.iterator();

            for(boolean first = true; itr.hasNext(); first = false) {
                Integer iVal = (Integer)itr.next();
                String val = iVal.toString();
                if(!first) {
                    buf.append(",");
                }

                buf.append(val);
            }

            return buf.toString();
        }
    }

    protected int skipWhiteSpace(int i, String s) {
        while(i < s.length() && (s.charAt(i) == 32 || s.charAt(i) == 9)) {
            ++i;
        }

        return i;
    }

    protected int findNextWhiteSpace(int i, String s) {
        while(i < s.length() && (s.charAt(i) != 32 || s.charAt(i) != 9)) {
            ++i;
        }

        return i;
    }

    protected void addToSet(int val, int end, int incr, int type) throws ParseException {
        TreeSet set = this.getSet(type);
        if(type != 0 && type != 1) {
            if(type == 2) {
                if((val < 0 || val > 23 || end > 23) && val != 99) {
                    throw new ParseException("Hour values must be between 0 and 23", -1);
                }
            } else if(type == 3) {
                if((val < 1 || val > 31 || end > 31) && val != 99 && val != 98) {
                    throw new ParseException("Day of month values must be between 1 and 31", -1);
                }
            } else if(type == 4) {
                if((val < 1 || val > 12 || end > 12) && val != 99) {
                    throw new ParseException("Month values must be between 1 and 12", -1);
                }
            } else if(type == 5 && (val == 0 || val > 7 || end > 7) && val != 99 && val != 98) {
                throw new ParseException("Day-of-Week values must be between 1 and 7", -1);
            }
        } else if((val < 0 || val > 59 || end > 59) && val != 99) {
            throw new ParseException("Minute and Second values must be between 0 and 59", -1);
        }

        if((incr == 0 || incr == -1) && val != 99) {
            if(val != -1) {
                set.add(new Integer(val));
            } else {
                set.add(NO_SPEC);
            }

        } else {
            int startAt = val;
            int stopAt = end;
            if(val == 99 && incr <= 0) {
                incr = 1;
                set.add(ALL_SPEC);
            }

            if(type != 0 && type != 1) {
                if(type == 2) {
                    if(end == -1) {
                        stopAt = 23;
                    }

                    if(val == -1 || val == 99) {
                        startAt = 0;
                    }
                } else if(type == 3) {
                    if(end == -1) {
                        stopAt = 31;
                    }

                    if(val == -1 || val == 99) {
                        startAt = 1;
                    }
                } else if(type == 4) {
                    if(end == -1) {
                        stopAt = 12;
                    }

                    if(val == -1 || val == 99) {
                        startAt = 1;
                    }
                } else if(type == 5) {
                    if(end == -1) {
                        stopAt = 7;
                    }

                    if(val == -1 || val == 99) {
                        startAt = 1;
                    }
                } else if(type == 6) {
                    if(end == -1) {
                        stopAt = 2099;
                    }

                    if(val == -1 || val == 99) {
                        startAt = 1970;
                    }
                }
            } else {
                if(end == -1) {
                    stopAt = 59;
                }

                if(val == -1 || val == 99) {
                    startAt = 0;
                }
            }

            byte max = -1;
            if(stopAt < startAt) {
                switch(type) {
                    case 0:
                        max = 60;
                        break;
                    case 1:
                        max = 60;
                        break;
                    case 2:
                        max = 24;
                        break;
                    case 3:
                        max = 31;
                        break;
                    case 4:
                        max = 12;
                        break;
                    case 5:
                        max = 7;
                        break;
                    case 6:
                        throw new IllegalArgumentException("Start year must be less than stop year");
                    default:
                        throw new IllegalArgumentException("Unexpected type encountered");
                }

                stopAt += max;
            }

            for(int i = startAt; i <= stopAt; i += incr) {
                if(max == -1) {
                    set.add(new Integer(i));
                } else {
                    int i2 = i % max;
                    if(i2 == 0 && (type == 4 || type == 5 || type == 3)) {
                        i2 = max;
                    }

                    set.add(new Integer(i2));
                }
            }

        }
    }

    protected TreeSet getSet(int type) {
        switch(type) {
            case 0:
                return this.seconds;
            case 1:
                return this.minutes;
            case 2:
                return this.hours;
            case 3:
                return this.daysOfMonth;
            case 4:
                return this.months;
            case 5:
                return this.daysOfWeek;
            case 6:
                return this.years;
            default:
                return null;
        }
    }

    protected ValueSet getValue(int v, String s, int i) {
        char c = s.charAt(i);

        String s1;
        for(s1 = String.valueOf(v); c >= 48 && c <= 57; c = s.charAt(i)) {
            s1 = s1 + c;
            ++i;
            if(i >= s.length()) {
                break;
            }
        }

        ValueSet val = new ValueSet();
        val.pos = i < s.length()?i:i + 1;
        val.value = Integer.parseInt(s1);
        return val;
    }

    protected int getNumericValue(String s, int i) {
        int endOfVal = this.findNextWhiteSpace(i, s);
        String val = s.substring(i, endOfVal);
        return Integer.parseInt(val);
    }

    protected int getMonthNumber(String s) {
        Integer integer = (Integer)monthMap.get(s);
        return integer == null?-1:integer.intValue();
    }

    protected int getDayOfWeekNumber(String s) {
        Integer integer = (Integer)dayMap.get(s);
        return integer == null?-1:integer.intValue();
    }

    protected Date getTimeAfter(Date afterTime) {
        Calendar cl = Calendar.getInstance(this.getTimeZone());
        afterTime = new Date(afterTime.getTime() + 1000L);
        cl.setTime(afterTime);
        cl.set(14, 0);
        boolean gotOne = false;

        while(true) {
            while(true) {
                while(!gotOne) {
                    if(cl.get(1) > 2999) {
                        return null;
                    }

                    SortedSet st = null;
                    boolean t = false;
                    int sec = cl.get(13);
                    int min = cl.get(12);
                    st = this.seconds.tailSet(new Integer(sec));
                    if(st != null && st.size() != 0) {
                        sec = ((Integer)st.first()).intValue();
                    } else {
                        sec = ((Integer)this.seconds.first()).intValue();
                        ++min;
                        cl.set(12, min);
                    }

                    cl.set(13, sec);
                    min = cl.get(12);
                    int hr = cl.get(11);
                    int var19 = -1;
                    st = this.minutes.tailSet(new Integer(min));
                    if(st != null && st.size() != 0) {
                        var19 = min;
                        min = ((Integer)st.first()).intValue();
                    } else {
                        min = ((Integer)this.minutes.first()).intValue();
                        ++hr;
                    }

                    if(min == var19) {
                        cl.set(12, min);
                        hr = cl.get(11);
                        int day = cl.get(5);
                        var19 = -1;
                        st = this.hours.tailSet(new Integer(hr));
                        if(st != null && st.size() != 0) {
                            var19 = hr;
                            hr = ((Integer)st.first()).intValue();
                        } else {
                            hr = ((Integer)this.hours.first()).intValue();
                            ++day;
                        }

                        if(hr == var19) {
                            cl.set(11, hr);
                            day = cl.get(5);
                            int mon = cl.get(2) + 1;
                            var19 = -1;
                            int tmon = mon;
                            boolean dayOfMSpec = !this.daysOfMonth.contains(NO_SPEC);
                            boolean dayOfWSpec = !this.daysOfWeek.contains(NO_SPEC);
                            int year;
                            int dow;
                            int daysToAdd;
                            if(dayOfMSpec && !dayOfWSpec) {
                                st = this.daysOfMonth.tailSet(new Integer(day));
                                Calendar var20;
                                Date var22;
                                if(this.lastdayOfMonth) {
                                    if(!this.nearestWeekday) {
                                        var19 = day;
                                        day = this.getLastDayOfMonth(mon, cl.get(1));
                                    } else {
                                        var19 = day;
                                        day = this.getLastDayOfMonth(mon, cl.get(1));
                                        var20 = Calendar.getInstance(this.getTimeZone());
                                        var20.set(13, 0);
                                        var20.set(12, 0);
                                        var20.set(11, 0);
                                        var20.set(5, day);
                                        var20.set(2, mon - 1);
                                        var20.set(1, cl.get(1));
                                        dow = this.getLastDayOfMonth(mon, cl.get(1));
                                        daysToAdd = var20.get(7);
                                        if(daysToAdd == 7 && day == 1) {
                                            day += 2;
                                        } else if(daysToAdd == 7) {
                                            --day;
                                        } else if(daysToAdd == 1 && day == dow) {
                                            day -= 2;
                                        } else if(daysToAdd == 1) {
                                            ++day;
                                        }

                                        var20.set(13, sec);
                                        var20.set(12, min);
                                        var20.set(11, hr);
                                        var20.set(5, day);
                                        var20.set(2, mon - 1);
                                        var22 = var20.getTime();
                                        if(var22.before(afterTime)) {
                                            day = 1;
                                            ++mon;
                                        }
                                    }
                                } else if(this.nearestWeekday) {
                                    var19 = day;
                                    day = ((Integer)this.daysOfMonth.first()).intValue();
                                    var20 = Calendar.getInstance(this.getTimeZone());
                                    var20.set(13, 0);
                                    var20.set(12, 0);
                                    var20.set(11, 0);
                                    var20.set(5, day);
                                    var20.set(2, mon - 1);
                                    var20.set(1, cl.get(1));
                                    dow = this.getLastDayOfMonth(mon, cl.get(1));
                                    daysToAdd = var20.get(7);
                                    if(daysToAdd == 7 && day == 1) {
                                        day += 2;
                                    } else if(daysToAdd == 7) {
                                        --day;
                                    } else if(daysToAdd == 1 && day == dow) {
                                        day -= 2;
                                    } else if(daysToAdd == 1) {
                                        ++day;
                                    }

                                    var20.set(13, sec);
                                    var20.set(12, min);
                                    var20.set(11, hr);
                                    var20.set(5, day);
                                    var20.set(2, mon - 1);
                                    var22 = var20.getTime();
                                    if(var22.before(afterTime)) {
                                        day = ((Integer)this.daysOfMonth.first()).intValue();
                                        ++mon;
                                    }
                                } else if(st != null && st.size() != 0) {
                                    var19 = day;
                                    day = ((Integer)st.first()).intValue();
                                    year = this.getLastDayOfMonth(mon, cl.get(1));
                                    if(day > year) {
                                        day = ((Integer)this.daysOfMonth.first()).intValue();
                                        ++mon;
                                    }
                                } else {
                                    day = ((Integer)this.daysOfMonth.first()).intValue();
                                    ++mon;
                                }

                                if(day != var19 || mon != tmon) {
                                    cl.set(13, 0);
                                    cl.set(12, 0);
                                    cl.set(11, 0);
                                    cl.set(5, day);
                                    cl.set(2, mon - 1);
                                    continue;
                                }
                            } else {
                                if(!dayOfWSpec || dayOfMSpec) {
                                    throw new UnsupportedOperationException("Support for specifying both a day-of-week AND a day-of-month parameter is not implemented.");
                                }

                                int lDay;
                                if(this.lastdayOfWeek) {
                                    year = ((Integer)this.daysOfWeek.first()).intValue();
                                    dow = cl.get(7);
                                    daysToAdd = 0;
                                    if(dow < year) {
                                        daysToAdd = year - dow;
                                    }

                                    if(dow > year) {
                                        daysToAdd = year + (7 - dow);
                                    }

                                    lDay = this.getLastDayOfMonth(mon, cl.get(1));
                                    if(day + daysToAdd > lDay) {
                                        cl.set(13, 0);
                                        cl.set(12, 0);
                                        cl.set(11, 0);
                                        cl.set(5, 1);
                                        cl.set(2, mon);
                                        continue;
                                    }

                                    while(day + daysToAdd + 7 <= lDay) {
                                        daysToAdd += 7;
                                    }

                                    day += daysToAdd;
                                    if(daysToAdd > 0) {
                                        cl.set(13, 0);
                                        cl.set(12, 0);
                                        cl.set(11, 0);
                                        cl.set(5, day);
                                        cl.set(2, mon - 1);
                                        continue;
                                    }
                                } else if(this.nthdayOfWeek != 0) {
                                    year = ((Integer)this.daysOfWeek.first()).intValue();
                                    dow = cl.get(7);
                                    daysToAdd = 0;
                                    if(dow < year) {
                                        daysToAdd = year - dow;
                                    } else if(dow > year) {
                                        daysToAdd = year + (7 - dow);
                                    }

                                    boolean var21 = false;
                                    if(daysToAdd > 0) {
                                        var21 = true;
                                    }

                                    day += daysToAdd;
                                    int weekOfMonth = day / 7;
                                    if(day % 7 > 0) {
                                        ++weekOfMonth;
                                    }

                                    daysToAdd = (this.nthdayOfWeek - weekOfMonth) * 7;
                                    day += daysToAdd;
                                    if(daysToAdd < 0 || day > this.getLastDayOfMonth(mon, cl.get(1))) {
                                        cl.set(13, 0);
                                        cl.set(12, 0);
                                        cl.set(11, 0);
                                        cl.set(5, 1);
                                        cl.set(2, mon);
                                        continue;
                                    }

                                    if(daysToAdd > 0 || var21) {
                                        cl.set(13, 0);
                                        cl.set(12, 0);
                                        cl.set(11, 0);
                                        cl.set(5, day);
                                        cl.set(2, mon - 1);
                                        continue;
                                    }
                                } else {
                                    year = cl.get(7);
                                    dow = ((Integer)this.daysOfWeek.first()).intValue();
                                    st = this.daysOfWeek.tailSet(new Integer(year));
                                    if(st != null && st.size() > 0) {
                                        dow = ((Integer)st.first()).intValue();
                                    }

                                    daysToAdd = 0;
                                    if(year < dow) {
                                        daysToAdd = dow - year;
                                    }

                                    if(year > dow) {
                                        daysToAdd = dow + (7 - year);
                                    }

                                    lDay = this.getLastDayOfMonth(mon, cl.get(1));
                                    if(day + daysToAdd > lDay) {
                                        cl.set(13, 0);
                                        cl.set(12, 0);
                                        cl.set(11, 0);
                                        cl.set(5, 1);
                                        cl.set(2, mon);
                                        continue;
                                    }

                                    if(daysToAdd > 0) {
                                        cl.set(13, 0);
                                        cl.set(12, 0);
                                        cl.set(11, 0);
                                        cl.set(5, day + daysToAdd);
                                        cl.set(2, mon - 1);
                                        continue;
                                    }
                                }
                            }

                            cl.set(5, day);
                            mon = cl.get(2) + 1;
                            year = cl.get(1);
                            var19 = -1;
                            if(year > 2099) {
                                return null;
                            }

                            st = this.months.tailSet(new Integer(mon));
                            if(st != null && st.size() != 0) {
                                var19 = mon;
                                mon = ((Integer)st.first()).intValue();
                            } else {
                                mon = ((Integer)this.months.first()).intValue();
                                ++year;
                            }

                            if(mon != var19) {
                                cl.set(13, 0);
                                cl.set(12, 0);
                                cl.set(11, 0);
                                cl.set(5, 1);
                                cl.set(2, mon - 1);
                                cl.set(1, year);
                            } else {
                                cl.set(2, mon - 1);
                                year = cl.get(1);
                                t = true;
                                st = this.years.tailSet(new Integer(year));
                                if(st == null || st.size() == 0) {
                                    return null;
                                }

                                var19 = year;
                                year = ((Integer)st.first()).intValue();
                                if(year != var19) {
                                    cl.set(13, 0);
                                    cl.set(12, 0);
                                    cl.set(11, 0);
                                    cl.set(5, 1);
                                    cl.set(2, 0);
                                    cl.set(1, year);
                                } else {
                                    cl.set(1, year);
                                    gotOne = true;
                                }
                            }
                        } else {
                            cl.set(13, 0);
                            cl.set(12, 0);
                            cl.set(5, day);
                            this.setCalendarHour(cl, hr);
                        }
                    } else {
                        cl.set(13, 0);
                        cl.set(12, min);
                        this.setCalendarHour(cl, hr);
                    }
                }

                return cl.getTime();
            }
        }
    }

    protected void setCalendarHour(Calendar cal, int hour) {
        cal.set(11, hour);
        if(cal.get(11) != hour && hour != 24) {
            cal.set(11, hour + 1);
        }

    }

    protected Date getTimeBefore(Date endTime) {
        return null;
    }

    public Date getFinalFireTime() {
        return null;
    }

    protected boolean isLeapYear(int year) {
        return year % 4 == 0 && year % 100 != 0 || year % 400 == 0;
    }

    protected int getLastDayOfMonth(int monthNum, int year) {
        switch(monthNum) {
            case 1:
                return 31;
            case 2:
                return this.isLeapYear(year)?29:28;
            case 3:
                return 31;
            case 4:
                return 30;
            case 5:
                return 31;
            case 6:
                return 30;
            case 7:
                return 31;
            case 8:
                return 31;
            case 9:
                return 30;
            case 10:
                return 31;
            case 11:
                return 30;
            case 12:
                return 31;
            default:
                throw new IllegalArgumentException("Illegal month number: " + monthNum);
        }
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();

        try {
            this.buildExpression(this.cronExpression);
        } catch (Exception var3) {
            ;
        }

    }

    public Object clone() {
        CronExpression copy = null;

        try {
            copy = new CronExpression(this.getCronExpression());
            copy.setTimeZone(this.getTimeZone());
            return copy;
        } catch (ParseException var3) {
            throw new IncompatibleClassChangeError("Not Cloneable.");
        }
    }

    static {
        monthMap.put("JAN", new Integer(0));
        monthMap.put("FEB", new Integer(1));
        monthMap.put("MAR", new Integer(2));
        monthMap.put("APR", new Integer(3));
        monthMap.put("MAY", new Integer(4));
        monthMap.put("JUN", new Integer(5));
        monthMap.put("JUL", new Integer(6));
        monthMap.put("AUG", new Integer(7));
        monthMap.put("SEP", new Integer(8));
        monthMap.put("OCT", new Integer(9));
        monthMap.put("NOV", new Integer(10));
        monthMap.put("DEC", new Integer(11));
        dayMap.put("SUN", new Integer(1));
        dayMap.put("MON", new Integer(2));
        dayMap.put("TUE", new Integer(3));
        dayMap.put("WED", new Integer(4));
        dayMap.put("THU", new Integer(5));
        dayMap.put("FRI", new Integer(6));
        dayMap.put("SAT", new Integer(7));
    }

    static class ValueSet {
        public int value;
        public int pos;

        ValueSet() {
        }
    }
}