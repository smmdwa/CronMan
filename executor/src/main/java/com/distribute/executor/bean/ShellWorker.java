package com.distribute.executor.bean;

public class ShellWorker extends worker  {

    private Long jobId;
    private String shell;
    private final String fileName;

    public ShellWorker(){
        this.fileName="data/shell/"+jobId+".sh";

    }

    @Override
    public void execute() throws Exception {


    }


}
