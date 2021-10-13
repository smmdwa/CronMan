package com.distribute.executor.bean;

public enum ResultEnum {

    init(0),success(200),error(300),change(400);
    public int result;


    ResultEnum(int result){
        this.result=result;
    }


}
