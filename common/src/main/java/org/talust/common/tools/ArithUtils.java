/*
 * MIT License
 *
 * Copyright (c) 2017-2018 talust.org talust.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package org.talust.common.tools;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class ArithUtils
{

    private static final int DEF_DIV_SCALE = 10;

    private ArithUtils()
    {
    }

    public static String add(double v1, double v2)
    {
        BigDecimal b1 = new BigDecimal(Double.valueOf(v1).toString());
        BigDecimal b2 = new BigDecimal(Double.valueOf(v2).toString());
        return b1.add(b2).toString();
    }

    public static String add(String v1, double v2)
    {
        BigDecimal b1 = new BigDecimal(v1);
        BigDecimal b2 = new BigDecimal(Double.valueOf(v2).toString());
        return b1.add(b2).toString();
    }

    public static String add(String v1, String v2)
    {
        BigDecimal b1 = new BigDecimal(v1);
        BigDecimal b2 = new BigDecimal(v2);
        return b1.add(b2).toString();
    }

    public static String sub(String v1, String v2)
    {
        BigDecimal b1 = new BigDecimal(v1);
        BigDecimal b2 = new BigDecimal(v2);
        return b1.subtract(b2).toString();
    }

    public static String sub(String v1 ,BigDecimal v2){
        BigDecimal b1 = new BigDecimal(v1);
        return b1.subtract(v2).toString();
    }

    public static String sub(String v1, double v2)
    {
        BigDecimal b1 = new BigDecimal(v1);
        BigDecimal b2 = new BigDecimal(Double.valueOf(v2).toString());
        return b1.subtract(b2).toString();
    }

    public static String sub(double v1, String v2)
    {
        BigDecimal b1 = new BigDecimal(Double.valueOf(v1).toString());
        BigDecimal b2 = new BigDecimal(v2);
        return b1.subtract(b2).toString();
    }

    public static String sub(double v1, double v2)
    {
        BigDecimal b1 = new BigDecimal(Double.valueOf(v1).toString());
        BigDecimal b2 = new BigDecimal(Double.valueOf(v2).toString());
        return b1.subtract(b2).toString();
    }

    public static String mul(String v1, String v2)
    {
        BigDecimal b1 = new BigDecimal(v1);
        BigDecimal b2 = new BigDecimal(v2);
        return b1.multiply(b2).toString();
    }

    public static String mul(String v1, String v2,int scale)
    {
        BigDecimal b1 = new BigDecimal(v1);
        BigDecimal b2 = new BigDecimal(v2);
        return b1.multiply(b2).setScale(scale,BigDecimal.ROUND_HALF_UP).toString();
    }

    public static String mul(long v1 , long v2 , int scale){
        if(scale < 0)
            throw new IllegalArgumentException("The   scale   must   be   a   positive   integer   or   zero");
        if(v2 == 0.0D)
        {
            return "0";
        } else
        {
            BigDecimal b1 = new BigDecimal(Double.valueOf(v1).toString());
            BigDecimal b2 = new BigDecimal(Double.valueOf(v2).toString());
            return b1.multiply(b2).setScale(scale,BigDecimal.ROUND_HALF_UP).toString();
        }
    }

    public static String div(String v1, String v2)
    {
        return div(v1, v2, 10);
    }

    public static String div (String v1 , long v2 , int scale){
        return div(v1, Double.valueOf(v2).toString(), scale);
    }

    public static String div(String v1, String v2, int scale)
    {
        if(scale < 0)
            throw new IllegalArgumentException("The   scale   must   be   a   positive   integer   or   zero");
        if(v2.equals("0"))
        {
            return "0";
        } else
        {
            BigDecimal b1 = new BigDecimal(v1);
            BigDecimal b2 = new BigDecimal(v2);
//            return b1.divide(b2, scale, 4).toString();
            return b1.divide(b2,scale,4).stripTrailingZeros().toPlainString();//保留小数点后n位，不用科学计数法
        }
    }



    public static String div(double v1, double v2, int scale)
    {
        if(scale < 0)
            throw new IllegalArgumentException("The   scale   must   be   a   positive   integer   or   zero");
        if(v2 == 0.0D)
        {
            return "0";
        } else
        {
            BigDecimal b1 = new BigDecimal(Double.valueOf(v1).toString());
            BigDecimal b2 = new BigDecimal(Double.valueOf(v2).toString());
            return b1.divide(b2, scale, 4).toString();
        }
    }

    public static String round(String v, int scale)
    {
        if(scale < 0)
        {
            throw new IllegalArgumentException("The   scale   must   be   a   positive   integer   or   zero");
        } else
        {
            BigDecimal b = new BigDecimal(v);
            BigDecimal one = new BigDecimal("1");
            return b.divide(one, scale, 4).toString();
        }
    }

    public static int compareStr(String str1, String str2)
    {
        BigDecimal bg1 = new BigDecimal(str1);
        BigDecimal bg2 = new BigDecimal(str2);
        if(bg1.compareTo(bg2) == 1){
            return 1;
        }
        return bg1.compareTo(bg2) != 0 ? -1 : 0;
    }

    public static String tranTimeToGeLintime(long mTime)
    {
        Calendar c = Calendar.getInstance();
        Date date = new Date(mTime);
        TimeZone timeBJ = TimeZone.getTimeZone("PRC");
        c.setTimeInMillis(date.getTime() - (long)timeBJ.getOffset(date.getTime()));
        return c.getTime().toString();
    }

    public static ArrayList divAmt(String tranAmt, String maxLimit)
    {
        ArrayList amtArray = new ArrayList();
        if(Double.parseDouble(tranAmt) <= Double.parseDouble(maxLimit))
        {
            amtArray.add(tranAmt);
            return amtArray;
        }
        if(tranAmt.contains("."))
            tranAmt = tranAmt.split("\\.")[0];
        Long time = Long.valueOf(Long.parseLong(tranAmt) / Long.parseLong(maxLimit));
        Long amt = Long.valueOf(Long.parseLong(tranAmt) % Long.parseLong(maxLimit));
        for(int i = 0; (long)i < time.longValue(); i++)
            amtArray.add(maxLimit);

        if(amt.longValue() != 0L)
            amtArray.add(amt);
        return amtArray;
    }
}
