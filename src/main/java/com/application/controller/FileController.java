package com.application.controller;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.application.common.CodeMsg;
import com.application.common.Result;
import com.application.domain.resp.UploadShpResp;
import com.application.utils.JimFsUtil;
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
import jodd.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
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
}
