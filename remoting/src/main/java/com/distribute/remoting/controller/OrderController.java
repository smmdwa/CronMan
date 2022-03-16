package com.distribute.remoting.controller;

import com.distribute.remoting.bean.JobBean;
import com.distribute.remoting.bean.ReturnMSG;
import com.distribute.remoting.netty_server.NameServerController;
import com.distribute.remoting.utils.DataUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@CrossOrigin
public class OrderController {

    @Autowired
    NameServerController controller;

    @RequestMapping("/data/{page}/{size}")
    @ResponseBody
    public ReturnMSG data(@PathVariable("page") Integer page,
                          @PathVariable("size") Integer size) {
        ReturnMSG vo = controller.getJobController();
        List<JobBean> data = DataUtil.castList(vo.getData(), JobBean.class);
        vo.setData(fenye(data,page,size));
        return vo;
    }

    @RequestMapping("/getDag/{jobId}")
    @ResponseBody
    public ReturnMSG getDag(@PathVariable("jobId") Long jobId) {
        ReturnMSG vo = controller.getDagController(jobId);
        return vo;
    }


    @RequestMapping("/kill/{jobId}")
    @ResponseBody
    public ReturnMSG killJob(@PathVariable("jobId") Long jobId) {
        ReturnMSG vo = controller.killJob(jobId);
        return vo;
    }

    @RequestMapping("/delete/{jobId}")
    @ResponseBody
    public ReturnMSG DeleteJob(@PathVariable("jobId") Long jobId) {
        ReturnMSG vo = controller.DeleteJob(jobId);
        return vo;
    }

    @RequestMapping("/start/{jobId}")
    @ResponseBody
    public ReturnMSG StartJob(@PathVariable("jobId") Long jobId) {
        ReturnMSG vo = controller.StartJob(jobId);
        return vo;
    }

    public List<JobBean> fenye(List<JobBean> list, int page, int size){
        int i=0,j=list.size();
        List<JobBean>newList=new ArrayList<>();
        if(page==0){
            if(j<size)return list;
            else{
                for (int k = 0; k < size; k++) {
                    newList.add(list.get(k));
                }
            }
        }else if(page*size<j){// e.g., j=31 page 5  size4
            for (int k = ((page-1)*size); k < page*size; k++) {
                newList.add(list.get(k));
            }
        }else {// e.g., j=31 page 5  size 7
            for (int k = (page-1)*size; k < j; k++) {
                newList.add(list.get(k));
            }
        }
        return newList;

    }
}
