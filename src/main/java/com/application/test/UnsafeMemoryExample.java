package com.application.test;

import sun.misc.Unsafe;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class UnsafeMemoryExample {
    private static Unsafe getUnsafe() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }

    public static void main(String[] args) throws InterruptedException {
        //testReadTxtFromFile();
        //testReadByteFromFile();
        //checkAddressSize();
        //testMaxDirectMemory1();
    }
    public static void checkAddressSize(){
        try{
            Unsafe unsafe = getUnsafe();

            long memoryAddress = unsafe.allocateMemory(1024*1024);
            long maxDirectMemory = unsafe.addressSize();
            System.out.println("Max Direct Memory: " + maxDirectMemory / (1024 * 1024) + " MB");
        }catch(Exception e){
            e.printStackTrace();
        }

    }
    public static void testMaxDirectMemory1(){
        try {
            Class<?> vmClass = Class.forName("jdk.internal.misc.VM");
            Method maxDirectMemoryMethod = vmClass.getDeclaredMethod("maxDirectMemory");
            maxDirectMemoryMethod.setAccessible(true);
            long maxDirectMemory = (long) maxDirectMemoryMethod.invoke(null);
            System.out.println("Max Direct Memory: " + maxDirectMemory / (1024 * 1024) + " MB");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void testReadByteFromFile() throws InterruptedException {

        Thread.sleep(5000);
        File file = new File("d:\\code\\geoserver-SNAPSHOT.zip");
        long fileSize = file.length();
        System.out.println("fileSize:"+fileSize);
        Unsafe unsafe = null;

        try {
            unsafe = getUnsafe();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        long memoryAddress = unsafe.allocateMemory(fileSize);

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            long offset = 0;

            while ((bytesRead = fis.read(buffer)) != -1) {
                for (int i = 0; i < bytesRead; i++) {
                    unsafe.putByte(memoryAddress + offset + i, buffer[i]);
                }
                offset += bytesRead;
            }
            Thread.sleep(5000);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            unsafe.freeMemory(memoryAddress);
        }
    }
    public static void testReadTxtFromFile(){
        File file = new File("d:\\1.txt");
        long fileSize = file.length();
        Unsafe unsafe = null;

        try {
            unsafe = getUnsafe();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        long memoryAddress = unsafe.allocateMemory(fileSize);

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            long offset = 0;

            while ((bytesRead = fis.read(buffer)) != -1) {
                for (int i = 0; i < bytesRead; i++) {
                    unsafe.putByte(memoryAddress + offset + i, buffer[i]);
                }
                offset += bytesRead;
            }

            // 测试打印内存中的内容，假设文件是文本文件
            for (long i = 0; i < fileSize; i++) {
                byte b = unsafe.getByte(memoryAddress + i);
                System.out.print((char) b);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            unsafe.freeMemory(memoryAddress);
        }
    }
}
