
package com.example.requestdemo.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Request  implements Serializable {
    private String requestName;
    private Map<String, String> params;
    private Map<String, String> headers;
    private Map<String, String> paramLib;
    private Map<String, String> headerLib;
    private String requestUrl;
    private String paramsUrl;
    public void setParamFromLib(String paramName){
        String param = paramLib.get(paramName);
        if (Objects.isNull(param)){
            throw new RuntimeException("参数库中没有该参数:"+paramName);
        }
        params.put(paramName,param);
    }

    public void setHeaderFromLib(String headerName){
        String header = headerLib.get(headerName);
        if (Objects.isNull(header)){
            throw new RuntimeException("参数库中没有该参数:"+headerName);
        }
        headers.put(headerName,header);
    }

    public void clear(){
        params.clear();
        headers.clear();
        paramsUrl = "";
    }
}
