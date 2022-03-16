package com.distribute.remoting.bean;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ReturnMSG<T> {
    private int code;
    private String msg;
    private T data;
    private Integer size;
}
