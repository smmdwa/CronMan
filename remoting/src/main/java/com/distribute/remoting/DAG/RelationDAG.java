package com.distribute.remoting.DAG;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RelationDAG {
    private String source;
    private String target;
}
