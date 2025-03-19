package com.application.domain.resp;


import cn.hutool.json.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadShpResp {

    Double maxX;

    Double maxY;

    Double minX;

    Double minY;

    String fileName;

    JSONObject geojson;
}