package org.talust.network.netty;/*
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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ntp.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.NumberFormat;

/**
 * @author Axe-Liu
 * @date 2018/8/1.
 */
@Slf4j
public class NtpTimeService {
    private static final NumberFormat NUM_FORMAT = new java.text.DecimalFormat("0.00");
    private static final String SERVER_IP = "ntp.talust.com";
    private long lastInitTime;
    private boolean running;
    //网络时间是否通过第一种方式进行了同步，如果没有，则通过第二种方式同步
    private static boolean netTimeHasSync;
    /** 时间偏移差距触发点，超过该值会导致本地时间重设，单位毫秒 **/
    public static final long TIME_OFFSET_BOUNDARY = 1000L;
    /*
	 * 网络时间刷新间隔
	 */
    private static final long TIME_REFRESH_TIME = 600000L;
    private static NtpTimeService instance = new NtpTimeService();
    /*
     * 网络时间与本地时间的偏移
     */
    private static long timeOffset;

    private NtpTimeService() {
    }

    public static NtpTimeService get() {
        return instance;
    }
    public void start(){
        init();
        Thread monitorThread = new Thread() {
            @Override
            public void run() {
                monitorTimeChange();
            }
        };
        monitorThread.setName("time change monitor");
        monitorThread.start();
    }


    public void init(){
        NTPUDPClient client = new NTPUDPClient();
        client.setDefaultTimeout(10000);
        try {
            client.open();
            InetAddress hostAddr = InetAddress.getByName(SERVER_IP);
            System.out.println(" > " + hostAddr.getHostName() + "/" + hostAddr.getHostAddress());
            TimeInfo info = client.getTime(hostAddr);
            processResponse(info);
            initSuccess();
        } catch (Exception e) {
            initError();
        }
        client.close();
    }

    public void monitorTimeChange() {
        long lastTime = System.currentTimeMillis();
        running = true;
        while(running) {
            long newTime = System.currentTimeMillis();
            if(Math.abs(newTime - lastTime) > TIME_OFFSET_BOUNDARY) {
                log.info("本地时间调整了：{}", newTime - lastTime);
                init();
            } else if(currentTimeMillis() - lastInitTime > TIME_REFRESH_TIME) {
                init();
            }
            lastTime = newTime;
            try {
                Thread.sleep(500L);
            } catch (InterruptedException e) {
            }
        }
    }

    private void initSuccess() {
        lastInitTime = System.currentTimeMillis();
        if(!netTimeHasSync) {
            netTimeHasSync = true;
        }
    }
    private void initError() {
        //1分钟后重试
        lastInitTime = System.currentTimeMillis() - 60000L;

        log.info("---------------本地时间调整出错---------------");
    }

    private static void processResponse(TimeInfo info) {
        NtpV3Packet message = info.getMessage();
        int stratum = message.getStratum();
        int version = message.getVersion();
        int li = message.getLeapIndicator();
        log.info(" leap=" + li + ", version="
                + version + ", precision=" + message.getPrecision());
        log.info(" mode: " + message.getModeName() + " (" + message.getMode() + ")");
        int poll = message.getPoll();
        log.info(" poll: " + (poll <= 0 ? 1 : (int) Math.pow(2, poll))
                + " seconds" + " (2 ** " + poll + ")");
        double disp = message.getRootDispersionInMillisDouble();
        log.info(" rootdelay=" + NUM_FORMAT.format(message.getRootDelayInMillisDouble())
                + ", rootdispersion(ms): " + NUM_FORMAT.format(disp));
        int refId = message.getReferenceId();
        String refAddr = NtpUtils.getHostAddress(refId);
        String refName = null;
        if (refId != 0) {
            if (refAddr.equals("127.127.1.0")) {
                refName = "LOCAL";
            } else if (stratum >= 2) {
                if (!refAddr.startsWith("127.127")) {
                    try {
                        InetAddress addr = InetAddress.getByName(refAddr);
                        String name = addr.getHostName();
                        if (name != null && !name.equals(refAddr)){
                            refName = name;
                        }
                    } catch (UnknownHostException e) {
                        refName = NtpUtils.getReferenceClock(message);
                    }
                }
            } else if (version >= 3 && (stratum == 0 || stratum == 1)) {
                refName = NtpUtils.getReferenceClock(message);
            }
        }
        if (refName != null && refName.length() > 1){
            refAddr += " (" + refName + ")";
        }
        log.info(" 参考标识符:\t" + refAddr);
        TimeStamp refNtpTime = message.getReferenceTimeStamp();
        log.info(" 参考时间戳:\t" + refNtpTime + "  " + refNtpTime.toDateString());
        TimeStamp origNtpTime = message.getOriginateTimeStamp();
        log.info(" 起始时间戳:\t" + origNtpTime + "  " + origNtpTime.toDateString());
        long destTime = info.getReturnTime();
        TimeStamp rcvNtpTime = message.getReceiveTimeStamp();
        log.info(" 接收时间戳:\t" + rcvNtpTime + "  " + rcvNtpTime.toDateString());
        TimeStamp xmitNtpTime = message.getTransmitTimeStamp();
        log.info(" 发送时间戳:\t" + xmitNtpTime + "  " + xmitNtpTime.toDateString());
        TimeStamp destNtpTime = TimeStamp.getNtpTime(destTime);
        log.info("目的时间戳:\t" + destNtpTime + "  " + destNtpTime.toDateString());
        info.computeDetails();
        Long offsetValue = info.getOffset();
        Long delayValue = info.getDelay();
        String delay = (delayValue == null) ? "N/A" : delayValue.toString();
        String offset = (offsetValue == null) ? "N/A" : offsetValue.toString();
        timeOffset =Long.parseLong(offset);
        log.info(" 往返延迟(ms)=" + delay
                + ", 时钟偏移(ms)=" + offset);
    }

    /**
     * 当前毫秒时间
     * @return long
     */
    public static long currentTimeMillis() {
        return System.currentTimeMillis() + timeOffset;
    }

    /**
     * 当前时间秒数
     * @return long
     */
    public static long currentTimeSeconds() {
        return currentTimeMillis() / 1000;
    }

    public long getLastInitTime() {
        return lastInitTime;
    }

    public void setLastInitTime(long lastInitTime) {
        this.lastInitTime = lastInitTime;
    }

    public static boolean isNetTimeHasSync() {
        return netTimeHasSync;
    }

    public static void setNetTimeHasSync(boolean netTimeHasSync) {
        NtpTimeService.netTimeHasSync = netTimeHasSync;
    }

    public static long getTimeOffset() {
        return timeOffset;
    }

    public static void setTimeOffset(long timeOffset) {
        NtpTimeService.timeOffset = timeOffset;
    }
}
