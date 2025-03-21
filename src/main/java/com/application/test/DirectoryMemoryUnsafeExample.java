package com.application.test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class DirectoryMemoryUnsafeExample {
    private static Unsafe unsafe;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Unable to get Unsafe instance", e);
        }
    }
    public static void main(String[] args) {
        long size = 1024; // 分配1024字节的内存
        long address = unsafe.allocateMemory(size);
        unsafe.putByte(address, (byte) 123); // 在地址处写入一个字节
        byte value = unsafe.getByte(address); // 从地址处读取一个字节
        System.out.println(value);
        unsafe.freeMemory(address);
    }
}
