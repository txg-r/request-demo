package com.example.requestdemo.util;

import com.example.requestdemo.entity.Group;
import com.example.requestdemo.entity.Request;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpUtil {
    private static final HttpClient client = HttpClients.createDefault();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static ObjectNode handleGet(String url, Map<String,String> params,Map<String, String> headers){
        try {
            //设置请求参数
            StringBuilder sb = new StringBuilder(url);
            if (!params.isEmpty()){
                sb.append("?");
                params.forEach((k,v)->{
                    sb.append(k).append("=").append(v).append("&");
                });
                sb.deleteCharAt(sb.length()-1);
            }
            HttpGet httpGet = new HttpGet(sb.toString());
            //设置请求头
            headers.forEach(httpGet::setHeader);
            HttpResponse response = client.execute(httpGet);
            String json = EntityUtils.toString(response.getEntity());
            return mapper.readValue(json,ObjectNode.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static ObjectNode handleResponse(CloseableHttpResponse response) {
        try {
            String json = EntityUtils.toString(response.getEntity());
            response.close();
            return mapper.readValue(json, ObjectNode.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static HttpPost createPostByRequest(Group group,Request request) {
        HttpPost post = new HttpPost(StringUtils.hasText(request.getRequestUrl())?request.getRequestUrl():group.getGlobalUrl());
        //设置头
        group.getGlobalHeaders().forEach(post::setHeader);
        request.getHeaders().forEach(post::setHeader);
        //设置请求体
        List<BasicNameValuePair> formDate = new ArrayList<>();
        group.getGlobalParams().forEach((k,v)->{
            formDate.add(new BasicNameValuePair(k,v));
        });
        request.getParams().forEach((k,v)->{
            formDate.add(new BasicNameValuePair(k,v));
        });
        try {
            post.setEntity(new UrlEncodedFormEntity(formDate, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return post;
    }
}
