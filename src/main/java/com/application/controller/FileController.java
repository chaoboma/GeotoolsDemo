package com.application.controller;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.application.common.CodeMsg;
import com.application.common.Result;
import com.application.domain.resp.UploadShpResp;
import com.application.utils.JimFsUtil;
import com.application.utils.TimeUtils;
import com.application.utils.UnPackeUtil;
import com.application.utils.ZipUtils;
import com.application.utils.uuid.IdUtils;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jodd.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;

import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * FileController
 * @Author: chaobo
 * 2025-03-18 16:03
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/file")
@Tag(name = "FileController" )
@Slf4j
public class FileController {
    private FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
    @Value("${file.path}")
    private String localFilePath;

    @GetMapping("/downloadMapped")
    public void downloadMapped(HttpServletResponse response) throws IOException {
        Path filePath = Paths.get("d:\\ext4.vhdx");
        try (FileChannel fileChannel = FileChannel.open(filePath, StandardOpenOption.READ)) {
            // 使用MappedByteBuffer来读取文件内容
            MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());

            // 设置响应头
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filePath.getFileName().toString() + "\"");
            response.setContentLength((int) fileChannel.size());

            // 将MappedByteBuffer的内容写入响应输出流
            OutputStream out = response.getOutputStream();
            byte[] buffer = new byte[8192];
            while (mappedByteBuffer.hasRemaining()) {
                int remaining = Math.min(mappedByteBuffer.remaining(), buffer.length);
                mappedByteBuffer.get(buffer, 0, remaining);
                out.write(buffer, 0, remaining);
            }
            out.flush();
        }
    }

    @GetMapping("/downloadChannel")
    public void downloadChannel(HttpServletResponse response) throws IOException {
        Path filePath = Paths.get("d:\\ext4.vhdx");
        try {
            FileChannel fileChannel = FileChannel.open(filePath, StandardOpenOption.READ);
            File file = filePath.toFile();
            long fileSize=  file.length();
            response.setHeader("Content-Length",String.valueOf(fileSize));
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"");
            OutputStream outputStream = response.getOutputStream();
            WritableByteChannel writableByteChannel = Channels.newChannel(outputStream);
            fileChannel.transferTo(0,fileSize,writableByteChannel);
            fileChannel.close();
            outputStream.flush();
            writableByteChannel.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    @GetMapping("/downloadChannel2")
    public void downloadChannel2(HttpServletResponse response) throws IOException {
        Path filePath = Paths.get("d:\\ext4.vhdx");
        FileChannel inChannel = null;
        OutputStream outputStream = null;
        WritableByteChannel writableByteChannel = null;
        ByteBuffer buffer = null;
        try {
            inChannel = FileChannel.open(filePath, StandardOpenOption.READ);
            File file = filePath.toFile();
            long fileSize=  file.length();
            response.setHeader("Content-Length",String.valueOf(fileSize));
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"");
            outputStream = response.getOutputStream();
            writableByteChannel = Channels.newChannel(outputStream);
            buffer = ByteBuffer.allocateDirect(1024*1024);
            while(inChannel.read(buffer) != -1){
                buffer.flip();
                writableByteChannel.write(buffer);
                buffer.clear();
            }
        }catch(Exception e){
            e.printStackTrace();
        }finally {
            try{
                inChannel.close();
            }catch (Exception e){
                e.printStackTrace();
            }

            try{
                outputStream.flush();
            }catch (Exception e){
                e.printStackTrace();
            }

            try{
                writableByteChannel.close();
            }catch (Exception e){
                e.printStackTrace();
            }

            try{
                buffer = null;
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }

    @GetMapping("/download2")
    public ResponseEntity<Object> download2() throws IOException {
        File filePath =new File("d:\\ext4.vhdx");

        InputStreamResource resource = new InputStreamResource(new FileInputStream(filePath));
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filePath.getName() + "\"");

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(filePath.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }


    @GetMapping("/download")
    public void download(HttpServletResponse response) throws IOException {
        File file = new File("D:\\ext4.vhdx");
        try{
            InputStream inputStream = null;
            ServletOutputStream ouputStream = null;
            inputStream = new FileInputStream(file);
            response.setContentType("application/x-msdownload");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(file.getName(), "UTF-8"));
            long fileSize = file.length();
            response.setHeader("Content-Length",String.valueOf(fileSize));
            ouputStream = response.getOutputStream();
            byte b[] = new byte[1024];
            int n;
            while ((n = inputStream.read(b)) != -1) {
                ouputStream.write(b, 0, n);
            }
            inputStream.close();
            ouputStream.flush();
            ouputStream.close();

        }catch(Exception e){
            e.printStackTrace();
        }

    }

    @Operation(summary = "上传shp压缩包返回geojson，不落磁盘")
    @PostMapping(path = "/upload",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Object> upload(@RequestPart("file") MultipartFile file){

        try{
            UploadShpResp uploadShpResp = new UploadShpResp();
            String separator = "/";
            log.debug("separator:"+separator);
            byte [] byteArr = file.getBytes();
            log.debug("byteArr.length:"+byteArr.length);
            InputStream inputStream = new ByteArrayInputStream(byteArr);
            ZipInputStream zis = new ZipInputStream(inputStream, Charset.forName("GBK"));

            java.util.zip.ZipEntry entry = null;

            String tmpDir = IdUtils.randomUUID();
            Path tmpDirPath1 = fs.getPath(tmpDir);
            Files.createDirectory(tmpDirPath1);
            Path shapePath = null;
            String shpFileName = "";
            Path tmpDirPath = null;
            while ((entry = zis.getNextEntry()) != null) {
                //System.out.printf("条目信息： 名称%1$b, 大小%2$d, 压缩时间%3$d \n",
                //        entry.getName(), entry.getSize(), entry.getTime());
                if (entry.isDirectory()) { // is dir
                    //System.out.println(entry.getName() + "是一个目录");
                    continue;
                }
                String fileNameWhole = entry.getName();
                String[] fileNameArr = fileNameWhole.split(separator);
                int fileNameArrLength = fileNameArr.length;
                String fileName = fileNameArr[fileNameArrLength - 1];
                log.debug("fileName:" + fileName);

                if(tmpDirPath == null){
                    String filePath = fileNameWhole.substring(0,fileNameWhole.lastIndexOf(separator));
                    log.debug("filePath:"+filePath);
                    tmpDirPath = fs.getPath(tmpDir+separator+filePath);
                    log.debug("tmpDir+separator+filePath:"+tmpDir+separator+filePath);
                    Files.createDirectories(tmpDirPath);
                }

                if (fileName.endsWith(".shp")) {
                    //log.debug("fileName:" + fileName);
                    byte[] data = ZipUtils.getByte(zis);
                    log.debug("shp size:" + data.length);

                    shapePath = tmpDirPath.resolve(fileName);
                    log.debug(shapePath.toString());
                    shpFileName = shapePath.getFileName().toString();
                    Files.write(shapePath, data);

                } else if (fileName.endsWith(".shx")) {
                    //log.debug("fileName:" + fileName);
                    byte[] data = ZipUtils.getByte(zis);
                    log.debug("shx size:" + data.length);
                    Path shxFileName = tmpDirPath.resolve(fileName);
                    Files.write(shxFileName, data);

                } else if (fileName.endsWith(".dbf")) {
                    //log.debug("fileName:" + fileName);
                    byte[] data = ZipUtils.getByte(zis);
                    log.debug("dbf size:" + data.length);


                    Path dbfFileName = tmpDirPath.resolve(fileName);
                    Files.write(dbfFileName, data);

                } else if (fileName.endsWith(".prj")) {
                    //log.debug("fileName:" + fileName);
                    byte[] data = ZipUtils.getByte(zis);
                    String prjStr = new String(data);
                    log.debug("prjStr:" + prjStr);
                    if (!prjStr.contains("WGS_1984")) {
                        return Result.error(new CodeMsg("请上传WGS_1984，也就是EPSG:4326坐标系的数据"));
                    }
                    log.debug("prj size:" + data.length);
                    Path prjFileName = tmpDirPath.resolve(fileName);
                    Files.write(prjFileName, data);

                }
            }
            String tableName = null;

            ShapefileDataStore shapefileDataStore = new ShapefileDataStore(shapePath.toUri().toURL());
            //设置编码，防止中文乱码
            Charset charset = Charset.forName("UTF8");
            shapefileDataStore.setCharset(charset);
            tableName = shapefileDataStore.getTypeNames()[0];
//            System.out.println("typeName:"+tableName);
            //根据图层名称来获取要素的source
            SimpleFeatureSource featureSource = shapefileDataStore.getFeatureSource(tableName);
            //读取投影
            CoordinateReferenceSystem crs = featureSource.getSchema().getCoordinateReferenceSystem();
            //log.debug("crs.toWKT():"+crs.toWKT());
            if(!crs.toWKT().contains("WGS_1984")){
//                    try{
//                        FileUtil.deleteDir(folder);
//                    }catch(Exception e){
//                        e.printStackTrace();
//                    }
                return Result.error(new CodeMsg("请上传WGS_1984，也就是EPSG:4326坐标系的数据"));
            }
            //读取bbox
            ReferencedEnvelope bbox = featureSource.getBounds();
            double maxX  = bbox.getMaxX();
            double maxY=bbox.getMaxY();
            double minX= bbox.getMinX();
            double minY=bbox.getMinY();
            uploadShpResp.setMaxX(maxX);
            uploadShpResp.setMaxY(maxY);
            uploadShpResp.setMinX(minX);
            uploadShpResp.setMinY(minY);
            uploadShpResp.setFileName(shpFileName);

            //特征总数
            int count = featureSource.getCount(Query.ALL);
            //获取当前数据的geometry类型（点、线、面）
            GeometryType geometryType = featureSource.getSchema().getGeometryDescriptor().getType();
            log.debug("geometryType：" +geometryType);//GeometryTypeImpl MultiPolygon<MultiPolygon>
            //读取要素
            FeatureCollection<SimpleFeatureType, SimpleFeature> result = featureSource.getFeatures();
            //log.debug("result:"+result);
            FeatureIterator<SimpleFeature> itertor = result.features();

            Map<String, String> columnMap = new HashMap<>();
            int geomIndex = 0;
            int i = 0;
            try{
                List<AttributeDescriptor> attributeDescriptors = shapefileDataStore.getSchema().getAttributeDescriptors();

                for (AttributeDescriptor attributeDescriptor : attributeDescriptors) {
                    String key = attributeDescriptor.getLocalName();
                    if(key.equals("the_geom")){
                        geomIndex = i;
                    }
                    i++;
                    String value = attributeDescriptor.getType().getBinding().getName();
                    columnMap.put(key, value);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            log.debug("columnMap:"+columnMap);
            JSONObject geojsonObject = new JSONObject();
            geojsonObject.put("type", "FeatureCollection");
            JSONArray array = new JSONArray();
            FeatureJSON fjson = new FeatureJSON();
            log.debug("process geojson start");
            while (itertor.hasNext()) {
                SimpleFeature feature = itertor.next();
                StringWriter writer = new StringWriter();
                fjson.writeFeature(feature, writer);
                String temp = writer.toString();

                byte[] b = temp.getBytes("UTF-8");
                temp = new String(b, Charset.forName("UTF8"));
                JSONObject json = new JSONObject(temp);
                array.add(json);
            }
            itertor.close();

            geojsonObject.put("features", array);
            geojsonObject.put("name", shpFileName);
            //String crs = getCoordinateSystemWKT(shpPath);
            //GEOGCS表示这个是地址坐标系,PROJCS则表示是平面投影坐标系
            JSONObject crsJson = new JSONObject();
            JSONObject proJson = new JSONObject();
            crsJson.put("type", "name");
            if (crs.toWKT().startsWith("PROJCS")) {
                proJson.put("name", "urn:ogc:def:crs:EPSG::3857");
                crsJson.put("properties", proJson);
            } else {
                proJson.put("name", "urn:ogc:def:crs:OGC:1.3:CRS84");
                crsJson.put("properties", proJson);
            }
            geojsonObject.put("crs", crsJson);
            uploadShpResp.setGeojson(geojsonObject);

            log.debug("process geojson end");
            try{

                JimFsUtil.deleteDirectoryRecursively(tmpDirPath);
                Boolean result1 = Files.deleteIfExists(tmpDirPath1);
                shapefileDataStore.dispose();
                zis.close();
                inputStream.close();
                log.debug("clear end");
            }catch(Exception e){
                e.printStackTrace();
            }
            return Result.success(uploadShpResp);
        }catch(Exception e){
            return Result.error(CodeMsg.INTERNAL_EXCEPTION);
        }


    }

    @Operation(summary = "上传shp压缩包返回geojson，落磁盘")
    @PostMapping(path = "/upload2",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Object> upload2(@RequestPart("file") MultipartFile file){

        String randomUUID = IdUtils.randomUUID();
        String packFileStr = localFilePath + File.separator +randomUUID;

        File folder = new File(packFileStr);
        File shpfile = null;
        File files = null;
        ShapefileDataStore shapefileDataStore = null;
        if (folder.exists()) {
            try{
                //FileUtil.deleteDir(folder);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        folder.mkdirs();
        UploadShpResp uploadShpResp = new UploadShpResp();
        try{
            String fileName = file.getOriginalFilename();// shape压缩包名
            String packFilePath = packFileStr + File.separator + fileName;

            files = new File(packFilePath);

            try{
                FileUtils.cleanDirectory(folder);
                file.transferTo(files);
            } catch(IOException e){
                e.printStackTrace();
                throw new RuntimeException("压缩文件到:" + packFileStr + " 失败!");
            }
            try{
                //zip解压---------------------------------------------------
                UnPackeUtil.unPackZip(files, null, packFileStr);
                // 获取解压文件目录
                fileName = fileName.substring(0,fileName.lastIndexOf('.'));
                String folderPath = packFileStr + File.separator + fileName;
//        String folderPath = packFileStr + File.separator + fileName;
                File sfiles = new File(folderPath);
                File[] filesList = sfiles.listFiles();
                if( filesList == null){
                    folderPath = packFileStr;
                    sfiles = new File(folderPath);
                    filesList = sfiles.listFiles();
                }
                //log.debug("filesList：" +filesList);
                //log.debug("folderPath：" +folderPath);
                //System.out.println("解压文件目录：" +filesList+folderPath);
                // 获取解压文件shape路径
                String shapePath = "";
                for(int i = 0;i<filesList.length;i++){
                    String path = filesList[i].getPath();
                    //log.debug("path：" +path);
                    String type = path.substring(path.length()-4,path.length());
                    //log.debug("type：" +type);
                    // System.out.println("解压格式："+type );
                    if(type.equals(".shp")){
                        shapePath = path;
                    }
                }
                //System.out.println("解压文件shape路径："+shapePath );
                // 获取shape属性数据信息-----------------------
                shpfile = new File(shapePath);
                String tableName = null;

                shapefileDataStore = new ShapefileDataStore(shpfile.toURI().toURL());

                //设置编码，防止中文乱码
                Charset charset = Charset.forName("UTF8");
                shapefileDataStore.setCharset(charset);
                tableName = shapefileDataStore.getTypeNames()[0];
//            System.out.println("typeName:"+tableName);
                //根据图层名称来获取要素的source
                SimpleFeatureSource featureSource = shapefileDataStore.getFeatureSource(tableName);
                //读取投影
                CoordinateReferenceSystem crs = featureSource.getSchema().getCoordinateReferenceSystem();
                //log.debug("crs.toWKT():"+crs.toWKT());
                if(!crs.toWKT().contains("WGS_1984")){
                    try{
                        FileUtil.deleteDir(folder);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    return Result.error(new CodeMsg("请上传WGS_1984，也就是EPSG:4326坐标系的数据"));
                }
                //读取bbox
                ReferencedEnvelope bbox = featureSource.getBounds();
                double maxX  = bbox.getMaxX();
                double maxY=bbox.getMaxY();
                double minX= bbox.getMinX();
                double minY=bbox.getMinY();
                uploadShpResp.setMaxX(maxX);
                uploadShpResp.setMaxY(maxY);
                uploadShpResp.setMinX(minX);
                uploadShpResp.setMinY(minY);
                uploadShpResp.setFileName(fileName);

                //特征总数
                int count = featureSource.getCount(Query.ALL);
                //获取当前数据的geometry类型（点、线、面）
                GeometryType geometryType = featureSource.getSchema().getGeometryDescriptor().getType();
                log.debug("geometryType：" +geometryType);//GeometryTypeImpl MultiPolygon<MultiPolygon>
                //读取要素
                FeatureCollection<SimpleFeatureType, SimpleFeature> result = featureSource.getFeatures();
                //log.debug("result:"+result);
                FeatureIterator<SimpleFeature> itertor = result.features();

                Map<String, String> columnMap = new HashMap<>();
                int geomIndex = 0;
                int i = 0;
                try{
                    List<AttributeDescriptor> attributeDescriptors = shapefileDataStore.getSchema().getAttributeDescriptors();

                    for (AttributeDescriptor attributeDescriptor : attributeDescriptors) {
                        String key = attributeDescriptor.getLocalName();
                        if(key.equals("the_geom")){
                            geomIndex = i;
                        }
                        i++;
                        String value = attributeDescriptor.getType().getBinding().getName();
                        columnMap.put(key, value);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
                log.debug("columnMap:"+columnMap);
                JSONObject geojsonObject = new JSONObject();
                geojsonObject.put("type", "FeatureCollection");
                JSONArray array = new JSONArray();
                FeatureJSON fjson = new FeatureJSON();
                while (itertor.hasNext()) {
                    SimpleFeature feature = itertor.next();
                    StringWriter writer = new StringWriter();
                    fjson.writeFeature(feature, writer);
                    String temp = writer.toString();

                    byte[] b = temp.getBytes("UTF-8");
                    temp = new String(b, Charset.forName("UTF8"));
                    JSONObject json = new JSONObject(temp);
                    array.add(json);
                }
                itertor.close();

                geojsonObject.put("features", array);
                geojsonObject.put("name", fileName);
                //String crs = getCoordinateSystemWKT(shpPath);
                //GEOGCS表示这个是地址坐标系,PROJCS则表示是平面投影坐标系
                JSONObject crsJson = new JSONObject();
                JSONObject proJson = new JSONObject();
                crsJson.put("type", "name");
                if (crs.toWKT().startsWith("PROJCS")) {
                    proJson.put("name", "urn:ogc:def:crs:EPSG::3857");
                    crsJson.put("properties", proJson);
                } else {
                    proJson.put("name", "urn:ogc:def:crs:OGC:1.3:CRS84");
                    crsJson.put("properties", proJson);
                }
                geojsonObject.put("crs", crsJson);
                uploadShpResp.setGeojson(geojsonObject);
                log.debug("process geojson end");


            }catch (Exception e){
                e.printStackTrace();
                //return Result.error(CodeMsg.SERVER_EXCEPTION,"解析异常");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        if (folder.exists()) {
            try{
                FileUtil.deleteDir(folder);
                log.debug("remove folder end");
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        try{
            shapefileDataStore.dispose();
            log.debug("dispose dataStore end");
        }catch(Exception e){
            e.printStackTrace();
        }
        return Result.success(uploadShpResp);

    }
    /**
     * 用Stream方式上传文件，避免MultipartFile临时文件，没有成功
     */
    @PostMapping(path = "/uploadFileStream")
    public ResponseEntity<String> uploadFileStream(@RequestPart("file") MultipartFile file){
        System.out.println("start:"+ TimeUtils.getNowTime());
        ReadableByteChannel inChannel = null;
        FileChannel outChannel = null;
        FileOutputStream fos = null;
        try{

            inChannel = Channels.newChannel(file.getInputStream());
            System.out.println("inChannel:"+ TimeUtils.getNowTime());
            fos = new FileOutputStream(localFilePath+ File.separator+file.getOriginalFilename());
            System.out.println("new FileOutputStream:"+ TimeUtils.getNowTime());
            outChannel = fos.getChannel();
            System.out.println("fos.getChannel:"+ TimeUtils.getNowTime());
            outChannel.transferFrom(inChannel,0,file.getSize());
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            System.out.println("finally:"+ TimeUtils.getNowTime());
            //关闭资源
            try {
                if (fos != null) {
                    fos.close();
                }
                if (inChannel != null) {
                    inChannel.close();
                }
                if (outChannel != null) {
                    outChannel.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        return ResponseEntity.ok("File uploaded successfully");
    }
    /**
     * 用Stream方式上传文件，避免MultipartFile临时文件，成功了，需要设置spring.servlet.multipart.enabled: false
     */
    @PostMapping("/uploadFileStream2")
    public String uploadFileStream2(HttpServletRequest request){
        //System.out.println("1:"+TimeUtils.getNowTime());
        String fileName = request.getHeader("filename");
        File targetFile = new File("d:\\upload2", fileName);
        //System.out.println("1:"+TimeUtils.getNowTime());
        // 使用FileChannel直接写入磁盘
        try {
            InputStream is = request.getInputStream();

            FileChannel channel = FileChannel.open(
                    targetFile.toPath(),
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);
            // 通过通道传输数据
            ByteBuffer buffer = ByteBuffer.allocateDirect(8192); // 直接缓冲区提升性能
            ReadableByteChannel sourceChannel = Channels.newChannel(is);

            while (sourceChannel.read(buffer) != -1) {
                buffer.flip();
                channel.write(buffer);
                buffer.clear();
            }
            buffer = null;
            is.close();
            channel.close();
            sourceChannel.close();
            System.out.println("1:"+TimeUtils.getNowTime());
            return "Upload success: " + fileName;
        } catch (IOException e) {
            e.printStackTrace();
            return "Upload failed: " + e.getMessage();
        }

    }
    /**
     * 用NIO中channel上传文件，并配置缓冲区的方法
     */
    @PostMapping(path = "/uploadFile3",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Object> uploadFile3(@RequestPart("file") MultipartFile file) {
        System.out.println("start:"+ TimeUtils.getNowTime());
        //获取文件名
        String realName = file.getOriginalFilename();
        System.out.println("getOriginalFilename:"+ TimeUtils.getNowTime());
        //创建流
        FileInputStream fis = null;
        FileOutputStream fos = null;
        //创建通道
        ReadableByteChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            inChannel = Channels.newChannel(file.getInputStream());
            System.out.println("newChannel:"+ TimeUtils.getNowTime());
            fos = new FileOutputStream(localFilePath+ File.separator+realName);
            System.out.println("new FileOutputStream:"+ TimeUtils.getNowTime());
            outChannel = fos.getChannel();
            System.out.println("fos.getChannel:"+ TimeUtils.getNowTime());
            ByteBuffer buffer = ByteBuffer.allocateDirect(1024*1024*1);
            while(inChannel.read(buffer) != -1){
                buffer.flip();
                outChannel.write(buffer);
                //outChannel.transferFrom(inChannel,0,file.getSize());
                buffer.clear();
            }
        }catch (IOException e){
            return Result.error(new CodeMsg("文件上传路径错误"));
        }finally {
            System.out.println("finally:"+ TimeUtils.getNowTime());
            //关闭资源
            try {
                if (fis != null) {
                    fis.close();
                }
                if (fos != null) {
                    fos.close();
                }
                if (inChannel != null) {
                    inChannel.close();
                }
                if (outChannel != null) {
                    outChannel.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Result.success("success");

    }

    /**
     * 用NIO中channel上传文件的方法
     */
    @PostMapping(path = "/uploadFile",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Object> uploadFile(@RequestPart("file") MultipartFile file) {
        //获取文件名
        String realName = file.getOriginalFilename();

        //创建流
        FileInputStream fis = null;
        FileOutputStream fos = null;
        //创建通道
        ReadableByteChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            inChannel = Channels.newChannel(file.getInputStream());
            //开始上传
            fos = new FileOutputStream(localFilePath+ File.separator+realName);
            //通道间传输
            outChannel = fos.getChannel();
            //上传
            outChannel.transferFrom(inChannel,0,file.getSize());
        }catch (IOException e){
            return Result.error(new CodeMsg("文件上传路径错误"));
        }finally {
            //关闭资源
            try {
                if (fis != null) {
                    fis.close();
                }
                if (fos != null) {
                    fos.close();
                }
                if (inChannel != null) {
                    inChannel.close();
                }
                if (outChannel != null) {
                    outChannel.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Result.success("success");

    }
    /**
     * 用传统stream上传文件的方法
     */
    @PostMapping(path = "/uploadFile2",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Object> uploadFile2(@RequestPart("file") MultipartFile file) {
        //获取文件名
        String realName = file.getOriginalFilename();

        //创建流
        InputStream fis = null;
        FileOutputStream fos = null;
        byte [] byteArr = null;
        try {
            byteArr = file.getBytes();
            //log.debug("byteArr.length:"+byteArr.length);
            fis = new ByteArrayInputStream(byteArr);
            //开始上传
            fos = new FileOutputStream(localFilePath+ File.separator+realName);
            fos.write(byteArr);
            //fis.transferTo(fos);
        }catch (IOException e){
            return Result.error(new CodeMsg("文件上传路径错误"));
        }finally {
            //关闭资源
            try {
                if (fos != null) {
                    fos.close();
                }
                if (fis != null) {
                    fis.close();
                }
                if (byteArr != null) {
                    byteArr = null;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Result.success("success");

    }
}
