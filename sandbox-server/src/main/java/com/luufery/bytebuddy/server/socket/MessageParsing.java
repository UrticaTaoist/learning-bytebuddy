package com.luufery.bytebuddy.server.socket;

import java.util.HashMap;
import java.util.Map;

public class MessageParsing {
    /**
     * 字符串转换为 map
     * @param arr
     * @return
     */
    public static Map stringToMap(String arr){
        arr=arr.replaceAll(",",";");
        Map map = new HashMap();
        if (null != arr) {
            String[] param = arr.split(";");
            for (int i = 0; i < param.length; i++) {
                //这里的 index 要 >-1 才是 map格式
                int index = param[i].indexOf('=');
                if(index>-1)
                    map.put(param[i].substring(0,index), param[i].substring((index + 1)));
            }
        }
        return map;
    }

    /**
     * byte[]数组转换为16进制的字符串
     *
     * @param bytes 要转换的字节数组
     * @return 转换后的结果
     */
    public static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * 16进制转换成为string类型字符串
     * 这个方法中文会乱码，字母和数字都不会乱码
     *
     * @Author zhouhe
     * @param s
     * @return
     */
    public static String hexStringToString(String s) {
        if (s == null || s.equals("")) {
            return null;
        }
        s = s.replace(" ", "");
        byte[] baKeyword = new byte[s.length() / 2];
        for (int i = 0; i < baKeyword.length; i++) {
            try {
                baKeyword[i] = (byte) (0xff & Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            s = new String(baKeyword, "UTF-8");
            new String();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return s;
    }
}