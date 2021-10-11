package com.distribute.remoting.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class executorInfo {

    private String name;

    private String executorAddr;

    //记录执行了几次job
    private Integer count;

    //优先级
    private Integer level;
}
