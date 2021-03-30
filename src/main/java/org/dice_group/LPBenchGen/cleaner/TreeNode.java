package org.dice_group.LPBenchGen.cleaner;

import org.semanticweb.owlapi.model.OWLClass;

import java.util.ArrayList;
import java.util.List;

public class TreeNode {

    public List<TreeNode> children = new ArrayList<TreeNode>();
    public String reprClass;

}
