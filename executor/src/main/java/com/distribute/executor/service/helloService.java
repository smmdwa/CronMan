package com.distribute.executor.service;

import com.distribute.executor.annotation.scheduleJob;
import com.distribute.executor.bean.threadContext;
//import com.distribute.remoting.bean.job;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

@Slf4j
@Component
public class helloService  {

    @scheduleJob(name = "hello")
    public void hello(String name,Integer id){
        int shardIndex = threadContext.getExecutorContext().getShardIndex();
        int total = threadContext.getExecutorContext().getShardTotal();
        threadContext.getExecutorContext().setContent("success");
        log.info("任务开始 hello==============");
        try {
            Thread.sleep(10000);
            log.info("sleep over!");
            log.info("任务结束 hello0==============");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @scheduleJob(name = "hello_passive")
    public void hello_passive(String name,Integer id){
        int shardIndex = threadContext.getExecutorContext().getShardIndex();
        int total = threadContext.getExecutorContext().getShardTotal();
        log.info("任务开始 hello_passive==============");
        log.info("任务结束 hello_passive==============");
    }


    public static byte[] uncompress(final byte[] src) throws IOException {
        byte[] result = src;
        byte[] uncompressData = new byte[src.length];
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(src);
        InflaterInputStream inflaterInputStream = new InflaterInputStream(byteArrayInputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(src.length);

        try {
            while (true) {
                int len = inflaterInputStream.read(uncompressData, 0, uncompressData.length);
                if (len <= 0) {
                    break;
                }
                byteArrayOutputStream.write(uncompressData, 0, len);
            }
            byteArrayOutputStream.flush();
            result = byteArrayOutputStream.toByteArray();
        }
        catch (IOException e) {
            throw e;
        }
        finally {
            try {
                byteArrayInputStream.close();
            }
            catch (IOException e) {
            }
            try {
                inflaterInputStream.close();
            }
            catch (IOException e) {
            }
            try {
                byteArrayOutputStream.close();
            }
            catch (IOException e) {
            }
        }

        return result;
    }


    public static byte[] compress(final byte[] src) throws IOException {
        byte[] result = src;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(src.length);
        java.util.zip.Deflater deflater = new java.util.zip.Deflater(5);
        DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(byteArrayOutputStream, deflater);
        try {
            deflaterOutputStream.write(src);
            deflaterOutputStream.finish();
            deflaterOutputStream.close();
            result = byteArrayOutputStream.toByteArray();
        }
        catch (IOException e) {
            deflater.end();
            throw e;
        }
        finally {
            try {
                byteArrayOutputStream.close();
            }
            catch (IOException e) {
            }

            deflater.end();
        }

        return result;
    }
    public static byte[] joinByteArray(byte[] byte1, byte[] byte2) {

        return ByteBuffer.allocate(byte1.length + byte2.length)
                .put(byte1)
                .put(byte2)
                .array();

    }

    public static byte[] joinByteArray2(byte[] byte1, byte[] byte2) {

        byte[] result = new byte[byte1.length + byte2.length];

        System.arraycopy(byte1, 0, result, 0, byte1.length);
        System.arraycopy(byte2, 0, result, byte1.length, byte2.length);

        return result;

    }
    private static String connectWithShardContent(byte[][]allContent,boolean []isCompress) throws IOException {
        StringBuilder sb=new StringBuilder();
        int i=0;
        for(byte[]content:allContent){
            if(isCompress[i]){
                content=uncompress(content);
            }
            sb.append(new String(content));
            sb.append(";");
            i++;
        }
        return sb.toString();
    }
    public static void main(String[] args) throws IOException {
        String a="hello";
        String b="world";
        byte[][]content=new byte[2][];
        content[0]=a.getBytes();
        content[1]=compress(b.getBytes());
        System.out.println(connectWithShardContent(content,new boolean[]{false,true}));
    }
}
