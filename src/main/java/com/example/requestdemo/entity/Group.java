
package com.example.requestdemo.entity;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Group  implements Serializable {
    private String groupName;
    private String globalUrl;
    private String globalParamUrl;
    private String method;
    private Boolean once;
    private Integer interval;
    private String start;
    private Map<String,String> globalHeaders;
    private Map<String,String> globalParams;
    private List<Request> requests;

    public void clear(){
        globalHeaders = new LinkedHashMap<>();
        globalParams = new LinkedHashMap<>();
        globalParamUrl = "";
        once = true;
        requests.forEach(Request::clear);
    }
}
