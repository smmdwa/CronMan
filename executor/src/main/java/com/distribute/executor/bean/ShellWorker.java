package com.distribute.executor.bean;

import com.distribute.executor.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class ShellWorker extends worker  {

    private Long jobId;
    private String shell;
    private Integer index;
    private Integer total;
    private List<String> params;
    private final String fileName;
    private final String logName;

    public ShellWorker(Long jobId,String shell,Integer index,Integer total,List<String> params){
        this.fileName="./data/shell/"+jobId+".sh";
        this.logName="./data/shell/"+jobId+"-log.sh";
        this.jobId=jobId;
        this.shell=shell;
        this.index=index;
        this.total=total;
        this.params=params;
    }

    @Override
    public int execute() throws Exception {

        //生成本地的shell文件
        loadShell();
        //设置上下文
//        threadContext.setContext(new threadContext(jobId,index,total));

        String[] contextParam = new String[2];
        contextParam[0] = String.valueOf(threadContext.getExecutorContext().getShardIndex());
        contextParam[1] = String.valueOf(threadContext.getExecutorContext().getShardTotal());

        return realExecute(contextParam);
    }
    public static void printResults(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = "";
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
    }

    public int realExecute(String[] contextParams) throws InterruptedException, IOException {
        List<String> cmd = new ArrayList<>();
        cmd.add("sh");
        cmd.add(fileName);
        if (this.params!=null && this.params.size()>0) {
            cmd.addAll(this.params);
        }
        if (contextParams!=null && contextParams.length>0) {
            cmd.addAll(Arrays.asList(contextParams));
        }
        String[] realCmd = cmd.toArray(new String[0]);

        log.info("cmd:"+ Arrays.toString(realCmd));
        final Process process = Runtime.getRuntime().exec(realCmd);

        FileOutputStream out=new FileOutputStream(logName, true);

        Thread success = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    FileUtil.copyTo(process.getInputStream(), out, new byte[1024]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        Thread error = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    FileUtil.copyTo(process.getErrorStream(), out, new byte[1024]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        success.start();
        error.start();
        // process-wait
        int exitValue = 0;      // exit code: 0=success, 1=error
        try {
            exitValue = process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // log-thread join
        success.join();
        error.join();

        return exitValue;
    }

    //把shell以流的形式写入到本地的shell文件中去
    private void loadShell() throws IOException {
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

    public static void main(String[] args) throws IOException, InterruptedException {
//        List<String> cmd = new ArrayList<>();
//        List<String >paramss=new ArrayList<>();
//        paramss.add("dd");
//        paramss.add("ff");
//        ShellWorker shellWorker = new ShellWorker(111L,"echo hello $0 $1",1,2,paramss );
//        cmd.add("python");
//        cmd.add("py.py");
//        String[] realCmd = cmd.toArray(new String[0]);
//
//        System.out.println("cmd:"+ Arrays.toString(realCmd));
//        final Process process = Runtime.getRuntime().exec(realCmd);
//        FileOutputStream out=new FileOutputStream(shellWorker.logName, true);
//
//        Thread success = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    FileUtil.copyTo(process.getInputStream(), out, new byte[1024]);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//        Thread error = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    FileUtil.copyTo(process.getErrorStream(), out, new byte[1024]);
//                } catch (IOException e) {
//                    e.printStackTrace();
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
    }

}
