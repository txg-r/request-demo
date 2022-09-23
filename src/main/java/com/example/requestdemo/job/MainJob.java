package com.example.requestdemo.job;

import com.example.requestdemo.entity.Group;
import com.example.requestdemo.entity.HttpGroup;
import com.example.requestdemo.entity.Request;
import com.example.requestdemo.entity.RequestProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

@Component
@Slf4j
public class MainJob {
    private RequestProperties requestProperties;
    private final ExecutorService pool = Executors.newFixedThreadPool(20);

    public ExecutorService getPool() {
        return pool;
    }

    private List<HttpGroup> httpGroups;

    @Autowired
    public void setRequestProperties(RequestProperties requestProperties) {
        this.requestProperties = requestProperties;
    }

    @PostConstruct
    public void init() {
        initHttpRequestMap();
        log.debug("http请求全部初始化完毕");
    }

    private void initHttpRequestMap() {
        httpGroups = new ArrayList<>();
        requestProperties.getGroups().forEach(group -> {
            Map<String, HttpRequestBase> requestBaseMap = new LinkedHashMap<>();
            log.debug(group.getGroupName() + "初始化http请求...");
            group.getRequests().forEach(request -> {
                HttpRequestBase http = null;
                if ("GET".equalsIgnoreCase(group.getMethod())) {
                    //如果request entity中没有定义请求url,用group全局url
                    http = new HttpGet(StringUtils.hasText(request.getRequestUrl()) ? request.getRequestUrl() : group.getGlobalUrl());
                    //设置get请求参数
                    setParams(group, request, http);
                } else if ("POST".equalsIgnoreCase(group.getMethod())) {
                    http = new HttpPost(StringUtils.hasText(request.getRequestUrl()) ? request.getRequestUrl() : group.getGlobalUrl());
                    //设置post请求体
                    setFormDate(group, request, http);
                }
                assert http != null;
                setHeader(group,request, http);
                requestBaseMap.put(group.getGroupName() + request.getRequestName(), http);
            });
            httpGroups.add(new HttpGroup(group, requestBaseMap));
        });
    }

    private void setParams(Group group, Request request, HttpRequestBase http) {
        Map<String, String> params = new HashMap<>();
        //公共参数
        group.getGlobalParams().forEach(params::put);
        //个体参数
        request.getParams().forEach(params::put);
        setParamsByMap(http, params);
    }

    public void setParamsByMap(HttpRequestBase http, Map<String, String> params) {
        URI baseUrl = http.getURI();
        StringBuilder sb = new StringBuilder(String.valueOf(baseUrl));
        //添加参数到url末尾
        if (!params.isEmpty()){
            sb.append("?");
            params.forEach((k,v)->{
                sb.append(k).append("=").append(v).append("&");
            });
            //删除末尾链接符
            sb.deleteCharAt(sb.length()-1);
        }
        //将get请求参数直接设置在url中
        http.setURI(URI.create(sb.toString()));
    }

    private void setHeader(Group group, Request request, HttpRequestBase http) {
        Map<String, String> headers = new HashMap<>();
        // 代理（模拟浏览器版本）
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
        //设置请求头
        request.getHeaders().forEach(headers::put);
        //公共请求头
        group.getGlobalHeaders().forEach(headers::put);
        setHeaderByMap(http,headers);
    }

    public void setHeaderByMap(HttpRequestBase http, Map<String, String> headers) {
        headers.forEach(http::setHeader);
    }

    private void setFormDate(Group group, Request request, HttpRequestBase http) {
        Map<String, String> params = new HashMap<>();
        //单个请求体
        request.getParams().forEach(params::put);
        //公共请求体
        group.getGlobalParams().forEach(params::put);
        setFormDateByMap(http,params);
    }

    public void setFormDateByMap(HttpRequestBase http, Map<String, String> params) {
        List<BasicNameValuePair> formDate = new ArrayList<>();
        params.forEach((k,v)->{
            formDate.add(new BasicNameValuePair(k,v));
        });
        HttpPost post = (HttpPost) http;
        try {
            post.setEntity(new UrlEncodedFormEntity(formDate, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            log.error("请求体设置失败");
            e.printStackTrace();
        }
    }

    public void handlerHttpGroups(String groupName, String jobName, BiFunction<CloseableHttpResponse, String, Boolean> responseHandler) {
        HttpGroup httpGroup = findGroup(groupName);
        assert httpGroup != null;
        if (httpGroup.getGroup().getOnce()) {
            doOnce(httpGroup, jobName, responseHandler);
        } else {
            doThread(httpGroup, jobName, responseHandler);
        }
    }

    private HttpGroup findGroup(String groupName) {
        for (HttpGroup httpGroup : httpGroups) {
            if (httpGroup.getGroup().getGroupName().equalsIgnoreCase(groupName)) {
                return httpGroup;
            }
        }
        return null;
    }

    private void doOnce(HttpGroup group, String jobName, BiFunction<CloseableHttpResponse, String, Boolean> responseHandler) {
        try (
                CloseableHttpClient httpClient = HttpClients.createDefault()
        ) {
            group.getHttpRequests().forEach((k, v) -> {
                while (true){
                    try {
                        CloseableHttpResponse response = httpClient.execute(v);
                        if (responseHandler.apply(response, k + jobName)) {
                            break;
                        }
                        Thread.sleep(group.getGroup().getInterval());
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info(group.getGroup().getGroupName() + jobName + "任务完成");
        log.info("------------------------------------------------------");
    }

    private void doThread(HttpGroup group, String jobName, BiFunction<CloseableHttpResponse, String, Boolean> responseHandler) {
        //记录任务数量
        AtomicInteger jobNum = new AtomicInteger(group.getHttpRequests().size());
        group.getHttpRequests().forEach((name, request) -> {
            pool.submit(() -> {
                try (
                        CloseableHttpClient httpClient = HttpClients.createDefault()
                ) {
                    while (true) {
                        CloseableHttpResponse response = httpClient.execute(request);
                        if (responseHandler.apply(response, name + jobName)) {
                            if (jobNum.decrementAndGet() == 0) {
                                log.info(group.getGroup().getGroupName() + jobName + "完成");
                                log.info("------------------------------------------------------");
                            }
                            break;
                        }
                        Thread.sleep(group.getGroup().getInterval());
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
        });
    }

    public Group getGroup(String groupName) {
        for (Group group : requestProperties.getGroups()) {
            if (group.getGroupName().equalsIgnoreCase(groupName)) {
                return group;
            }
        }
        return null;
    }

}
