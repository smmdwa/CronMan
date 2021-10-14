package com.distribute.executor.bean;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShellWorker extends worker  {

    private Long jobId;
    private String shell;
    private Integer index;
    private Integer total;
    private String params;
    private final String fileName;

    public ShellWorker(){
        this.fileName="data/shell/"+jobId+".sh";
    }

    @Override
    public void execute() throws Exception {

        //生成本地的shell文件
        loadShell();
        //设置上下文
//        threadContext.setContext(new threadContext(jobId,index,total));

        String[] scriptParams = new String[3];
        scriptParams[0] = params;
        scriptParams[1] = String.valueOf(threadContext.getExecutorContext().getShardIndex());
        scriptParams[2] = String.valueOf(threadContext.getExecutorContext().getShardTotal());

        int exitValue = execToFile(scriptParams);
//
//        if (exitValue == 0) {
//            XxlJobHelper.handleSuccess();
//            return;
//        } else {
//            XxlJobHelper.handleFail("script exit value("+exitValue+") is failed");
//            return ;
//        }
    }

    public int execToFile(String[] params) throws InterruptedException {
        List<String> cmdarray = new ArrayList<>();
        if (params!=null && params.length>0) {
            cmdarray.addAll(Arrays.asList(params));
        }
        String[] cmdarrayFinal = cmdarray.toArray(new String[cmdarray.size()]);

        // process-exec
        final Process process;
        try {
            process = Runtime.getRuntime().exec(cmdarrayFinal);
        } catch (IOException e) {
            e.printStackTrace();
        }

//        final FileOutputStream finalFileOutputStream = fileOutputStream;
//        Thread success = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    copy(process.getInputStream(), finalFileOutputStream, new byte[1024]);
//                } catch (IOException e) {
//                    XxlJobHelper.log(e);
//                }
//            }
//        });
//        Thread error = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    copy(process.getErrorStream(), finalFileOutputStream, new byte[1024]);
//                } catch (IOException e) {
//                    XxlJobHelper.log(e);
//                }
//            }
//        });
//        success.start();
//        error.start();
//        // process-wait
//        int exitValue = 0;      // exit code: 0=success, 1=error
//        try {
//            exitValue = process.waitFor();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        // log-thread join
//        success.join();
//        error.join();

//        return exitValue;
        return 1;
    }

    //把shell以流的形式写入到本地的shell文件中去
    public void loadShell() throws IOException {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(this.fileName);
            fileOutputStream.write(this.shell.getBytes("UTF-8"));
            fileOutputStream.close();
        } catch (Exception e) {
            throw e;
        }finally{
            if(fileOutputStream != null){
                fileOutputStream.close();
            }
        }
    }

}
