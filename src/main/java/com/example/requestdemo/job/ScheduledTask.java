package com.example.requestdemo.job;

import com.example.requestdemo.controller.BiliController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
public class ScheduledTask {

    @Autowired
    private BiliController biliController;

/*    @Scheduled(cron = "59 59 23 26 8 ?")
    public void doBiliDay3(){
        biliController.mainReward("e62ce499");
    }*/

/*    @Scheduled(cron = "59 59 23 28 8 ?")
    public void doBiliDay5(){
        biliController.mainReward("1a468055");
    }*/

    @Scheduled(cron = "1 1 1 * * ?")
    public void doBili60(){
        biliController.dayReward("1");
    }

    @Scheduled(cron = "1 1 2 * * ?")
    public void doBili120(){
        biliController.dayReward("2");
    }

    @Scheduled(cron = "1 20 0 * * ?")
    public void doBili10(){
        biliController.dayReward("6");
    }

    @Scheduled(cron = "59 59 23 2 9 ?")
    public void doBiliDay10(){
        biliController.mainReward("56cb6703");
    }

/*    @Scheduled(cron = "59 59 1 10 9 ?")
    public void doBiliDay18(){
        biliController.mainReward("99e34eec");
    }

    @Scheduled(cron = "59 59 1 18 9 ?")
    public void doBiliDay26(){
        biliController.mainReward("c8daa502");
    }

    @Scheduled(cron = "59 59 1 27 9 ?")
    public void doBiliDay35(){
        biliController.mainReward("08bb556a");
    }*/

}