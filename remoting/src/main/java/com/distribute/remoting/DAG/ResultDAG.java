package com.distribute.remoting.DAG;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashSet;
import java.util.List;

@Data
@AllArgsConstructor
public class ResultDAG {
    private HashSet<NodeDAG> data;

    private List<RelationDAG> relations;
}
