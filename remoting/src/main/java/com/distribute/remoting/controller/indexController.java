package com.distribute.remoting.controller;

import com.distribute.remoting.bean.jobBean;
import com.distribute.remoting.bean.returnMSG;
import com.distribute.remoting.netty_server.nameServerController;
import com.distribute.remoting.utils.DataUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@CrossOrigin

public class indexController {

    @Autowired
    nameServerController controller;
    @ResponseBody
    @PostMapping("/add")
    public returnMSG get(@RequestBody Map map) {
        List<Long> jobs = new Gson().fromJson(new Gson().toJson(map.get("jobs")), new TypeToken<List<Long>>(){}.getType());

        String pids= DataUtil.LongTransfer(jobs);
        String msg="";
        int code=200;
        //进行基础的校验
        //1.有上游任务 但是又是被动任务，添加失败
        if(pids==null && (String.valueOf(map.get("jobType")).equals(jobBean.java_passive)||String.valueOf(map.get("jobType")).equals(jobBean.shell_passive))){
            msg="被动任务必须含有依赖任务！";
            code=300;
        }else if(!controller.isExistTheJob(pids)){
            //2.上游任务是否存在？
            msg="依赖任务并不存在！";
            code=300;
        }
        //3.如果有上游任务，判断是否会成环 todo? 不可能成环？

        if(code!=200){
            returnMSG jobVO = new returnMSG(code,msg,null,0);
            return jobVO;
        }
        returnMSG returnMSG = controller.addJobController(
                String.valueOf(map.get("name")),
                pids,
                String.valueOf(map.get("className")),
                String.valueOf(map.get("methodName")),
                String.valueOf(map.get("paramTypes")),
                String.valueOf(map.get("params")),
                String.valueOf(map.get("cron")),
                Integer.parseInt(String.valueOf(map.get("shard"))),
                (Boolean) map.get("transfer"),
                (Boolean) map.get("reStart"),
                String.valueOf(map.get("policy")),
                String.valueOf(map.get("jobType"))
        );
        return returnMSG;
    }

}