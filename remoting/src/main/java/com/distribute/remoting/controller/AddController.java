package com.distribute.remoting.controller;

import com.distribute.remoting.bean.JobBean;
import com.distribute.remoting.bean.ReturnMSG;
import com.distribute.remoting.netty_server.NameServerController;
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

public class AddController {

    @Autowired
    NameServerController controller;
    @ResponseBody
    @PostMapping("/add")
    public ReturnMSG get(@RequestBody Map map) {
//        System.out.println(map);
        List<Long> jobs=null;
        if(map.get("jobs")!=null)
            jobs = new Gson().fromJson(new Gson().toJson(map.get("jobs")), new TypeToken<List<Long>>(){}.getType());

        String pids= DataUtil.LongTransfer(jobs);
        String msg="";
        int code=200;
        //进行基础的校验
        //1.有上游任务 但是又是被动任务，添加失败
        if(pids==null && (String.valueOf(map.get("jobType")).equals(JobBean.java_passive)||String.valueOf(map.get("jobType")).equals(JobBean.shell_passive))){
            msg="被动任务必须含有依赖任务！";
            code=300;
        }else if(!controller.isExistTheJob(pids)||(jobs!=null&&jobs.size()==1&&jobs.get(0)==-1L)){
            //2.上游任务是否存在？ 允许上游任务是自己,-1代表自己
            msg="依赖任务并不存在！";
            code=300;
        }
        //3.如果有上游任务，判断是否会成环 todo? 不可能成环？

        if(code!=200){
            ReturnMSG jobVO = new ReturnMSG(code,msg,null,0);
            return jobVO;
        }
        ReturnMSG returnMSG = controller.addJobController(
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
                String.valueOf(map.get("jobType")),
                String.valueOf(map.get("shell"))
        );
        return returnMSG;
    }




}