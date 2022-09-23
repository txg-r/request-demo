package com.example.requestdemo.controller;

import com.example.requestdemo.entity.Group;
import com.example.requestdemo.entity.Request;
import com.example.requestdemo.job.MainJob;
import com.example.requestdemo.util.HttpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


@RestController
@RequestMapping("/Bzhan")
@Api(tags = "B站功能")
@Slf4j
public class BiliController {
    private MainJob job;
    private final List<String> msgList = new ArrayList<>();
    private final Random random = new Random();

    @PostConstruct
    private void init() {
        msgList.add("哈哈哈哈");
        msgList.add("主播牛的");
        msgList.add("捞");
        msgList.add("卧槽");
        msgList.add("!!!!!!");
        msgList.add("牛牛牛");
    }

    @Autowired
    public void setJob(MainJob job) {
        this.job = job;
        this.group = job.getGroup("B站");
    }

    private Group group;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/yuanshi")
    @ApiOperation("抢原石")
    public void mainReward(String id) {
        //设置group基本参数
        group.clear();
        group.setGlobalUrl("https://api.bilibili.com/x/activity/mission/task/reward/receive");
        group.setMethod("post");
        //设置结果处理中各种状况
        Map<Integer, String> states = new LinkedHashMap<>();
        states.put(0, "领取成功");
        states.put(75086, "已领取");
//        states.put(75154, "领完了");

        //任务数量
        AtomicInteger taskNum = new AtomicInteger(group.getRequests().size());

        for (Request request : group.getRequests()) {
            //初始化参数和头
            Map<String, String> params = new HashMap<>();
            params.put("id", id);
            params.put("csrf", request.getParamLib().get("csrf"));
            Map<String, String> headers = new HashMap<>();
            headers.put("cookie", request.getHeaderLib().get("cookie"));
            job.getPool().submit(() -> {
                //循环查询receive_id参数(是否能够领取)
                while (true) {
                    //发请求获取结果
                    ObjectNode node = HttpUtil.handleGet("https://api.bilibili.com/x/activity/mission/single_task", params, headers);
/*                    try {
                        Thread.sleep(interval);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }*/
                    //解析结果,找到需要的参数
                    Assert.notNull(node, "解析失败");
                    JsonNode taskInfo = node.get("data").get("task_info");
                    if (taskInfo.get("receive_id").intValue() == 0) {
                        log.info("原石" + request.getRequestName() + "参数获取中...");
                        continue;
                    }
                    JsonNode groupNode = taskInfo.get("group_list").get(0);
                    //设置参数
                    request.getParams().put("act_id", String.valueOf(groupNode.get("act_id").intValue()));
                    request.getParams().put("task_id", String.valueOf(groupNode.get("task_id").intValue()));
                    request.getParams().put("group_id", String.valueOf(groupNode.get("group_id").intValue()));
                    request.getParams().put("receive_id", String.valueOf(taskInfo.get("receive_id").intValue()));
                    request.getParams().put("receive_from", "missionPage");
                    //从参数库中取参数
                    request.setParamFromLib("csrf");
                    //从请求头库中取请求头
                    request.setHeaderFromLib("cookie");
                    break;
                }
                //开始发请求领取
                HttpPost httpPost = HttpUtil.createPostByRequest(group, request);
                try (
                        CloseableHttpClient client = HttpClients.createDefault()
                ) {
                    while (true) {
                        CloseableHttpResponse response = client.execute(httpPost);
/*                        try {
                            Thread.sleep(interval);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }*/
                        ObjectNode node = HttpUtil.handleResponse(response);
                        Assert.notNull(node, "解析失败");
                        int code = node.get("code").intValue();
                        String stateInfo = states.getOrDefault(code, null);
                        if (!Objects.isNull(stateInfo)) {
                            log.info(group.getGroupName() + request.getRequestName() + "原石" + "-----------" + stateInfo);
                            if (taskNum.decrementAndGet() <= 0) {
                                log.info(group.getGroupName() + "原石" + "任务完成");
                            }
                            break;
                        }
                        log.info(group.getGroupName() + request.getRequestName() + "原石" + node.toString());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }


    @GetMapping("/dayReward")
    @ApiOperation("每日奖励(1:开播60分钟,2:开播120分钟,3:10电池,4:弹幕六条,5:礼物两人,6:看十分钟)")
    public void dayReward(String jobIndex) {
        group.clear();
        group.setGlobalUrl("https://api.bilibili.com/x/activity/mission/task/reward/receive");
        group.setMethod("post");
        group.setOnce(true);
        group.setInterval(50);
        //初始化奖励map
        LinkedHashMap<String, String> rewards = new LinkedHashMap<>();
        rewards.put("1", "029ffcc2");//开播60分钟
        rewards.put("2", "61e38a90");//开播120分钟
        rewards.put("3", "b5f6c5bc");//10电池
        rewards.put("4", "116b3436");//弹幕6条"
        rewards.put("5", "43cf4581");//礼物两人
        rewards.put("6", "76c6c9d4");//看10分钟
        Map<Integer, String> states = new LinkedHashMap<>();
        states.put(0, "领取成功");
        states.put(-400, "任务未完成");
        states.put(75086, "已领取");
        states.put(75154, "领完了");
        if (jobIndex.equals("0")){
            rewards.forEach((jobName, id) -> {
                getReward(id, jobName, states);
            });
        }
        getReward(rewards.get(jobIndex),jobIndex,states);

    }

    @GetMapping("/start")
    @ApiOperation("开播(原神321,深空598,无期675,幻塔550)")
    public void startLive(String area_v2) {
        group.clear();
        group.setGlobalUrl("https://api.live.bilibili.com/room/v1/Room/startLive");
        group.setMethod("post");
        group.getGlobalParams().put("area_v2", area_v2);
        group.getGlobalParams().put("platform", "pc");
        group.getRequests().forEach(request -> {
            request.setHeaderFromLib("cookie");
            request.setParamFromLib("room_id");
            request.setParamFromLib("csrf_token");
            request.setParamFromLib("csrf");
        });
        job.init();
        job.handlerHttpGroups("B站", "开播", ((response, name) -> {
            //结果处理
            ObjectNode node = HttpUtil.handleResponse(response);
            Assert.notNull(node, "response解析错误");
            int code = node.get("code").intValue();
            if (code == 0) {
                log.info(name + "开播成功");
                return true;
            }
            log.info(name + "开播失败" + node.toString());
            return true;
        }));
    }

    @GetMapping("/changeLive")
    @ApiOperation("切换直播")
    public void changeLive(String area_v2) {
        group.clear();
        group.setGlobalUrl("https://api.live.bilibili.com/room/v1/Room/update");
        group.setMethod("post");
        group.getGlobalParams().put("area_id", area_v2);
        group.getRequests().forEach(request -> {
            request.setHeaderFromLib("cookie");
            request.setParamFromLib("room_id");
            request.setParamFromLib("csrf_token");
            request.setParamFromLib("csrf");
        });
        job.init();
        job.handlerHttpGroups("B站", "切换直播", ((response, name) -> {
            //结果处理
            ObjectNode node = HttpUtil.handleResponse(response);
            Assert.notNull(node, "response解析错误");
            int code = node.get("code").intValue();
            if (code == 0) {
                log.info(name + "切换成功");
                return true;
            }
            log.info(name + "切换失败" + node.toString());
            return true;
        }));
    }

    @GetMapping("/sendMsg")
    @ApiOperation("发弹幕")
    public void sendMsg() {
        group.clear();
        group.setGlobalUrl("https://api.live.bilibili.com/msg/send");
        group.setMethod("post");
        group.setInterval(800);
        group.getGlobalParams().put("bubble", "0");
        group.getGlobalParams().put("color", "16777215");
        group.getGlobalParams().put("mode", "1");
        group.getGlobalParams().put("fontsize", "25");
        group.getGlobalParams().put("rnd", "1661353437");
        for (int i = 0; i < group.getRequests().size(); i++) {
            Request request = group.getRequests().get(i);
            request.setParamFromLib("csrf");
            request.setParamFromLib("csrf_token");
            request.setHeaderFromLib("cookie");
            String roomId;
            if (i == group.getRequests().size() - 1) {
                roomId = group.getRequests().get(0).getParamLib().get("room_id");
            } else {
                roomId = group.getRequests().get(i + 1).getParamLib().get("room_id");
            }

            request.getParams().put("roomid", roomId);
        }
        for (int i = 0; i < 6; i++) {
            group.getGlobalParams().put("msg", getRandMsg());
            job.init();
            job.handlerHttpGroups("B站", "第" + (i + 1) + "条弹幕", (response, name) -> {
                ObjectNode node = HttpUtil.handleResponse(response);
                Assert.notNull(node, "response解析错误");
                if (node.get("code").intValue() == 0) {
                    log.info(name + "发送成功");
                    return true;
                }
                log.info(name + "发送失败" + node.toString());
                return false;
            });
        }
    }

    @GetMapping("/sendGold")
    @ApiOperation("送礼物")
    public void sendGold() {
        group.clear();
        //初始化全局参数
        group.setGlobalUrl("https://api.live.bilibili.com/xlive/revenue/v1/gift/sendGold");
        Map<String, String> globalParams = group.getGlobalParams();
        globalParams.put("gift_id", "31039");
        globalParams.put("send_ruid", "0");
        globalParams.put("gift_num", "5");
        globalParams.put("coin_type", "gold");
        globalParams.put("bag_id", "0");
        globalParams.put("platform", "pc");
        globalParams.put("biz_code", "Live");
        globalParams.put("storm_beat_id", "0");
        globalParams.put("metadata", "");
        globalParams.put("price", "100");
        globalParams.put("visit_id", "bo0mf7yi7z41");
        //初始化参数,顺着送一轮
        for (int i = 0; i < group.getRequests().size(); i++) {
            Request request = group.getRequests().get(i);
            request.setParamFromLib("csrf");
            request.setParamFromLib("csrf_token");
            request.setParamFromLib("uid");
            request.setHeaderFromLib("cookie");
            String ruid;
            String biz_id;
            if (i == group.getRequests().size() - 1) {
                ruid = group.getRequests().get(0).getParamLib().get("uid");
                biz_id = group.getRequests().get(0).getParamLib().get("room_id");
            } else {
                ruid = group.getRequests().get(i + 1).getParamLib().get("uid");
                biz_id = group.getRequests().get(i + 1).getParamLib().get("room_id");
            }
            request.getParams().put("ruid", ruid);
            request.getParams().put("biz_id", biz_id);
        }
        job.init();
        job.handlerHttpGroups("B站", "顺着送一轮礼物", (response, name) -> {
            ObjectNode node = HttpUtil.handleResponse(response);
            Assert.notNull(node, "response解析错误");
            if (node.get("code").intValue() == 0) {
                log.info(name + "送礼物成功");
                return true;
            }
            log.info(name + "送礼物失败" + node.toString());
            return true;
        });
        //初始化参数,反着送一轮
        for (int i = 0; i < group.getRequests().size(); i++) {
            Request request = group.getRequests().get(i);
            request.setParamFromLib("csrf");
            request.setParamFromLib("csrf_token");
            request.setParamFromLib("uid");
            request.setHeaderFromLib("cookie");
            String ruid;
            String biz_id;
            if (i == 0) {
                ruid = group.getRequests().get(group.getRequests().size() - 1).getParamLib().get("uid");
                biz_id = group.getRequests().get(group.getRequests().size() - 1).getParamLib().get("room_id");
            } else {
                ruid = group.getRequests().get(i - 1).getParamLib().get("uid");
                biz_id = group.getRequests().get(i - 1).getParamLib().get("room_id");
            }
            request.getParams().put("ruid", ruid);
            request.getParams().put("biz_id", biz_id);
        }
        job.init();
        job.handlerHttpGroups("B站", "反着送一轮礼物", (response, name) -> {
            ObjectNode node = HttpUtil.handleResponse(response);
            Assert.notNull(node, "response解析错误");
            if (node.get("code").intValue() == 0) {
                log.info(name + "送礼物成功");
                return true;
            }
            log.info(name + "送礼物失败" + node.toString());
            return true;
        });

    }

    private void getReward(String id, String jobName, Map<Integer, String> state) {
        //设置请求参数和头
        group.getRequests().forEach(r -> {
            //初始化参数和头
            Map<String, String> params = new HashMap<>();
            params.put("id", id);
            params.put("csrf", r.getParamLib().get("csrf"));
            Map<String, String> headers = new HashMap<>();
            headers.put("cookie", r.getHeaderLib().get("cookie"));
            //发请求获取结果
            ObjectNode node = HttpUtil.handleGet("https://api.bilibili.com/x/activity/mission/single_task", params, headers);
            //解析结果,找到需要的参数
            assert node != null;
            JsonNode taskInfo = node.get("data").get("task_info");
            JsonNode groupNode = taskInfo.get("group_list").get(0);
            //设置参数
            r.getParams().put("act_id", String.valueOf(groupNode.get("act_id").intValue()));
            r.getParams().put("task_id", String.valueOf(groupNode.get("task_id").intValue()));
            r.getParams().put("group_id", String.valueOf(groupNode.get("group_id").intValue()));
            r.getParams().put("receive_id", String.valueOf(taskInfo.get("receive_id").intValue()));
            r.getParams().put("receive_from", "missionPage");
            //从参数库中取参数
            r.setParamFromLib("csrf");
            //从请求头库中取请求头
            r.setHeaderFromLib("cookie");
        });
        job.init();
        //执行并定义处理函数
        job.handlerHttpGroups("B站", "日常奖励" + jobName, (response, name) -> {
            //结果处理
            ObjectNode node = HttpUtil.handleResponse(response);
            Assert.notNull(node, "response解析错误");
            int code = node.get("code").intValue();
            String stateInfo = state.getOrDefault(code, null);
            if (!Objects.isNull(stateInfo)) {
                log.info(name + "-----------" + stateInfo);
                return true;
            }
            log.info(name + "领取失败(再次尝试)" + node.toString());
            return false;
        });
    }

    private String getRandMsg() {
        return msgList.get(random.nextInt(6));
    }


}
