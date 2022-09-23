package com.example.requestdemo.controller;

import com.example.requestdemo.entity.Group;
import com.example.requestdemo.entity.RequestProperties;
import com.example.requestdemo.job.MainJob;
import com.example.requestdemo.util.HttpUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/huya")
@Slf4j
@Api(tags = "虎牙功能")
public class HuyaController {
    private MainJob job;
    private Group group;

    @Autowired
    public void setJob(MainJob job) {
        this.job = job;
        this.group = job.getGroup("虎牙");
    }

    @GetMapping("/changeLive")
    @ApiOperation("虎牙换播(原神5489,幻塔6437,深空6877)")
    public void changeLive(String gameId) {
        group.clear();
        group.setGlobalUrl("https://i.huya.com/index.php");
        Map<String, String> globalParams = group.getGlobalParams();
        globalParams.put("m", "ProfileSetting");
        globalParams.put("do", "ajaxChangeLiveInfo");
        globalParams.put("game_id", gameId);
        group.getRequests().forEach(request -> {
            request.setParamFromLib("live_desc");
            request.setHeaderFromLib("cookie");
        });
        job.init();
        job.handlerHttpGroups("虎牙", "换播", (response, name) -> {
            ObjectNode node = HttpUtil.handleResponse(response);
            Assert.notNull(node, "response解析错误");
            if (node.get("status").intValue() == 200) {
                log.info(name + "换播成功");
                return true;
            }
            log.info(name + "换播失败");
            return false;
        });
    }
}
