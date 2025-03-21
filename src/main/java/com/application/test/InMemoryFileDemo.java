package com.application.test;

import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class InMemoryFileDemo {
    public static void main(String[] args) {
        testBigFile();

    }
    public static void test1(){
        try{
            FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
            Path foo = fs.getPath("/foo");
            Files.createDirectory(foo);

            Path hello = foo.resolve("hello.txt"); // /foo/hello.txt
            Files.write(hello, ImmutableList.of("hello world"), StandardCharsets.UTF_8);

            List<String> lines = Files.readAllLines(hello, StandardCharsets.UTF_8);
            for (String line : lines) {
                System.out.println(line);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public static void testBigFile(){
        try{

            FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
            Path foo = fs.getPath("/foo");
            Files.createDirectory(foo);
            Path hello = foo.resolve("hello.file");
            FileInputStream fileInputStream = new FileInputStream(new File("D:\\docker\\MapTileDownloader\\docker_images_maptile-downloader-latest.tar.gz"));
            byte[] data = fileInputStream.readAllBytes();

            Files.write(hello, data);
            data = null;
            Boolean result = Files.deleteIfExists(hello);
            System.out.println(result);
            Thread.sleep(600000);
        }catch(Exception e){
            e.printStackTrace();
        }

    }
}
