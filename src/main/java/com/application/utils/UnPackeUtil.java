package com.application.utils;


import net.lingala.zip4j.core.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class UnPackeUtil {
    private static final Logger logger = LoggerFactory.getLogger(UnPackeUtil.class);
    /**
     * zip文件解压
     *
     * @param destPath 解压文件路径
     * @param zipFile  压缩文件
     * @param password 解压密码(如果有)
     */
    public static void unPackZip(File zipFile, String password, String destPath) {
        try {
            ZipFile zip = new ZipFile(zipFile);
            /*zip4j默认用GBK编码去解压,这里设置编码为GBK的*/
            zip.setFileNameCharset("GBK");
            logger.info("begin unpack zip file....");
            zip.extractAll(destPath);
            // 如果解压需要密码
            if (password != null) {
                if (zip.isEncrypted()) {
                    zip.setPassword(password);
                }
            }
        } catch (Exception e) {
            logger.error("解压失败：", e.getMessage(), e);
        }
    }

    /**
     * rar、zip、tar文件解压
     *
     * @param newFile  file文件
     * @param targetFilePath 解压保存路径
     */
//    public static void unPackRar(File newFile, String targetFilePath) {
//        RandomAccessFile randomAccessFile =null;
//        IInArchive inArchive = null;
//        try {
//            randomAccessFile = new RandomAccessFile(newFile.getPath(), "r");
//            inArchive = SevenZip.openInArchive(null, new RandomAccessFileInStream(randomAccessFile));
//            int[] in = new int[inArchive.getNumberOfItems()];
//            for (int i = 0; i < in.length; i++) {
//                in[i] = i;
//            }
//            inArchive.extract(in, false, new ExtractCallback(inArchive,"366",targetFilePath));
//        }catch (FileNotFoundException | SevenZipException e) {
//            e.printStackTrace();
//        }
//    }

}

