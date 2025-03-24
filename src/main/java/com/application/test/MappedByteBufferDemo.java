package com.application.test;

import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MappedByteBufferDemo {

    public static void main(String[] args) {
        testMappedByteBuffer();
    }
    public static void testMappedByteBuffer(){
        try{
            FileChannel channel = new RandomAccessFile("d:\\geotools-SNAPSHOT.zip", "rw").getChannel();
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, channel.size());

            System.out.println(buffer.isDirect());
            Thread.sleep(15000);
            buffer.clear();
            buffer = null;
            channel.close();
            channel = null;
            Thread.sleep(15000);
            System.gc();
            System.out.println("release memory now");
            Thread.sleep(15000);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
