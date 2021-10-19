package com.distribute.remoting.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DataUtil {
    public static List<String> transferString(String temp){
        if(temp==null||temp.length()==0)return null;
        List<String> list=new ArrayList<>();
        String[] split = temp.split(";");
        Collections.addAll(list, split);
        return list;
    }

    public static List<Long> transferLong(String temp){
        if(temp==null)return null;
        List<Long> list=new ArrayList<>();
        String[] split = temp.split(";");
        for (String s : split) {
            list.add(Long.parseLong(s));
        }
        return list;
    }
    public static String LongTransfer(List<Long> longList){
        if(longList==null||longList.size()==0)return null;
        StringBuilder sb=new StringBuilder();
        for (Long aLong : longList) {
            sb.append(aLong);
            sb.append(";");
        }
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }
    public static <T> List<T> castList(Object obj, Class<T> clazz)
    {
        List<T> result = new ArrayList<T>();
        if(obj instanceof List<?>)
        {
            for (Object o : (List<?>) obj)
            {
                result.add(clazz.cast(o));
            }
            return result;
        }
        return null;
    }
}
