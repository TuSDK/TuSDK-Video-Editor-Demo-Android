package org.lasque.tusdkeditoreasydemo.utils;

import com.arthenica.mobileffmpeg.FFmpeg;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.utils
 * android-ve-demo
 *
 * @author H.ys
 * @Date 2021/8/12  17:13
 * @Copyright (c) 2020 tusdk.com. All rights reserved.
 */
public class PCMUtils {

    public static double db = +22;
    public static double factor = Math.pow(10,db / 20);

    //调节PCM数据音量
    //pData原始音频byte数组，nLen原始音频byte数组长度，data2转换后新音频byte数组，nBitsPerSample采样率，multiple表示Math.pow()返回值
    public static int amplifyPCMData(byte[] pData, int nLen, byte[] data2, double nBitsPerSample, float multiple)
    {
        int nCur = 0;

        while (nCur < nLen)
        {
            short volum = getShort(pData, nCur);

            volum = (short)(volum * multiple);

            int data_1 = ( volum       & 0xFF);
            if (data_1 > 32767){
                data_1 = 32767;
            } else if (data_1 < -32768){
                data_1 = -32768;
            }
            data2[nCur]   = (byte)data_1;

            int data_2 = ((volum >> 8) & 0xFF);
            if (data_2 > 32768){
                data_2 = 32767;
            } else if (data_2 < -32768){
                data_2 = -32768;
            }

            data2[nCur+1] = (byte)data_2;
            nCur += 2;
        }
        return 0;
    }

    private static short getShort(byte[] data, int start)
    {
        return (short)((data[start] & 0xFF) | (data[start+1] << 8));
    }


    public static byte[] bytesToBigEndian(byte[] bytes) {

        if (bytes == null || bytes.length < 0)
            return bytes;
        byte[] result = new byte[bytes.length];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.position(0);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.position(0);
        buffer.get(result);
        return result;
    }

    public static byte[] changeBytes(byte[] a){
        byte[] b = new byte[a.length];
        for (int i = 0; i < b.length; i++) {
            b[i] = a[b.length - i - 1];
        }
        return b;
    }
    
    public static void pcmToMp3(File pcm,File mp3){
        try {
            String command = "-y -f s16be -ac 1 -ar 22050 -acodec pcm_s16le -i "+pcm.getCanonicalPath()+" "+mp3.getCanonicalPath()+"";
            FFmpeg.execute(command);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void mp3ToPcm(File mp3,File outputPcm){
        try {
            String c = String.format(Locale.ENGLISH,
                    "-y -i %s -acodec pcm_s16le -f s16le -ac 1 -ar 22050 %s",
                    mp3.getCanonicalPath(),outputPcm.getCanonicalPath()
                    );
            FFmpeg.execute(c);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
