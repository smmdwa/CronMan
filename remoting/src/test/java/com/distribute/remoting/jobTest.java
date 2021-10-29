package com.distribute.remoting;

import com.distribute.remoting.bean.ResultEnum;
import com.distribute.remoting.bean.jobBean;
import com.distribute.remoting.bean.jobExecInfo;
import com.distribute.remoting.bean.jobFinishDetail;
import com.distribute.remoting.mapper.JobMapper;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


import javax.sql.DataSource;
import javax.annotation.Resource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

//@MapperScan("com.distribute.remoting.mapper")
@SpringBootTest
public class jobTest {
    @Autowired
    JobMapper mapper;
    @Test
    public void test(){
//        jobBean jobBean = new jobBean(11,null,"com.distribute.executor.service.helloService","hello1","java.lang.String;java.lang.Integer","dyw;18","first_job","* * * 8-31 * ?",2,false,false,"random",new Date(),new Date());
//        mapper.insertJob(jobBean);
//
//        jobBean jobById = mapper.getJobById(11);
//        System.out.println(jobById);\



//               jobBean jobBean = new jobBean(11,null,"com.distribute.executor.service.helloService","hello1","java.lang.String;java.lang.Integer","dyw;18","first_job","* * * 8-31 * ?",2,false,false,"random",new Date(),new Date().getTime(),0,0);
//        mapper.insertJob(jobBean);
//
//        List<com.distribute.remoting.bean.jobBean> toBeRunJob = mapper.getToBeRunJob(new Date(System.currentTimeMillis() + 1000L).getTime());
//        System.out.println(toBeRunJob);


//        mapper.insertJobInfo(new jobExecInfo(jobBean.getName(),jobBean.getId(),jobBean,0,0));
//
//        jobExecInfo jobInfoById = mapper.getJobInfoById(jobBean.getId());
//        System.out.println(jobInfoById);
//
//        mapper.updateJobInfoByStatusAndTimes(1,4,jobBean.getId());
//        System.out.println(mapper.getJobInfoById(jobBean.getId()));
////
//        System.out.println(mapper.getAllJobInfo());

//        int i = mapper.updateJobDetail(1L, 0, "executor_2", 400, "");
//        System.out.println(i);

//        int executor_dd = mapper.insertJobDetail(new jobFinishDetail(124L, 0, jobBean, ResultEnum.init.result, "executor_dd", 0, 1));
//        System.out.println(executor_dd);
//

//        System.out.println(mapper.getNotExecJobs("executor_1"));


//        List<String> execName = mapper.getExecName(11);
//        System.out.println(execName);

        List<jobFinishDetail> jobDetail = mapper.getJobDetail(11, 0);
        System.out.println(jobDetail);

//        mapper.insertJobDetail(new jobFinishDetail(12L,0,jobBean,0,false,"executor_4",0,1));

//        mapper.updateJobDetail(11,0,200);

//        System.out.println(mapper.getJobDetail(11, 0));

    }

//    @Resource
//    DataSource dataSource;
//    @Test
//    public void contextLoads() throws SQLException {
//        Connection connection = dataSource.getConnection();
//        DatabaseMetaData metaData = connection.getMetaData();
//
//        //数据源>>>>>>class com.zaxxer.hikari.HikariDataSource
//        System.out.println("数据源>>>>>>" + dataSource.getClass());
//        System.out.println("连接>>>>>>>>" + connection);
//        System.out.println("连接地址>>>>" + connection.getMetaData().getURL());
//        System.out.println("驱动名称>>>>" + metaData.getDriverName());
//        System.out.println("驱动版本>>>>" + metaData.getDriverVersion());
//        System.out.println("数据库名称>>" + metaData.getDatabaseProductName());
//        System.out.println("数据库版本>>" + metaData.getDatabaseProductVersion());
//        System.out.println("连接用户名称>" + metaData.getUserName());
//
//        connection.close();
//    }

}
