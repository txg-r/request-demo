package com.example.requestdemo.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HttpGroup implements Serializable {
    private Group group;
    private Map<String,HttpRequestBase> httpRequests;

}
