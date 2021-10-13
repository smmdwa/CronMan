package com.distribute.remoting.controller;

import com.distribute.remoting.bean.jobBean;
import com.distribute.remoting.bean.returnMSG;
import com.distribute.remoting.netty_server.nameServerController;
import com.distribute.remoting.utils.DataUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@CrossOrigin
public class tempController {

    @Autowired
    nameServerController controller;

    @RequestMapping("/data/{page}/{size}")
    @ResponseBody
    public returnMSG data(@PathVariable("page") Integer page,
                          @PathVariable("size") Integer size) {
        returnMSG vo = controller.getJobController();
        List<jobBean> data = DataUtil.castList(vo.getData(), jobBean.class);
        vo.setData(fenye(data,page,size));
        return vo;
    }

    @RequestMapping("/getDag/{jobId}")
    @ResponseBody
    public returnMSG getDag(@PathVariable("jobId") Long jobId) {
        returnMSG vo = controller.getDagController(jobId);
        return vo;
    }


    @RequestMapping("/kill/{jobId}")
    @ResponseBody
    public returnMSG killJob(@PathVariable("jobId") Long jobId) {
        returnMSG vo = controller.killJob(jobId);
        return vo;
    }



    public List<jobBean> fenye(List<jobBean> list, int page, int size){
        int i=0,j=list.size();
        List<jobBean>newList=new ArrayList<>();
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
