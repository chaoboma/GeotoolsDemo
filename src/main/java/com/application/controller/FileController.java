package com.application.controller;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.application.common.CodeMsg;
import com.application.common.Result;
import com.application.domain.resp.UploadShpResp;
import com.application.utils.JimFsUtil;
import com.application.utils.ZipUtils;
import com.application.utils.uuid.IdUtils;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
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
 * TileController
 * @Author: chaobo
 * 2025-03-18 16:03
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/file")
//@Api(value = "TileController", tags = { "TileController" })
@Slf4j
public class FileController {
    private FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
    @PostMapping("/upload")
    public Result<Object> upload(MultipartFile file){
        UploadShpResp uploadShpResp = new UploadShpResp();
        try{
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
            }catch(Exception e){
                e.printStackTrace();
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return Result.success(uploadShpResp);
    }
}
