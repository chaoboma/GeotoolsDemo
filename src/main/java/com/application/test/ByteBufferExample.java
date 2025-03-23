package com.application.test;

import java.nio.ByteBuffer;
public class ByteBufferExample {
    public static void main(String[] args) {
        try{
            Thread.sleep(15000);
            // 分配直接内存，大小为1MB
            ByteBuffer directBuffer = ByteBuffer.allocateDirect(1024 * 1024*1000);
            // 模拟在直接内存中写入数据
            for (long i=0;i<=100000000;i++){
                directBuffer.putLong(i);
            }
            directBuffer.putInt(123);
            directBuffer.putDouble(3.14);

            System.out.println(directBuffer.get(10000));
            System.out.println(directBuffer.isDirect());
            // 读取直接内存中的数据
            directBuffer.flip();
            System.out.println("Int value from direct memory: " + directBuffer.getInt());
            System.out.println("Double value from direct memory: " + directBuffer.getDouble());
            // 释放直接内存
            directBuffer.clear();
            directBuffer = null;
            // 假设此处还有其他业务逻辑代码...
            // 当直接内存不再使用时，手动释放
            Thread.sleep(900000);
            System.gc(); // 手动触发垃圾回收
            Thread.sleep(6000000);
        }catch(Exception e){
            e.printStackTrace();
        }

    }
}