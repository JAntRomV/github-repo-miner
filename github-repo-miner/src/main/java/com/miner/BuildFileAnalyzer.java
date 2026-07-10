package com.miner;

import java.util.Map;

public interface BuildFileAnalyzer {
    boolean supports(BuildTool tool);
    TechProfile analyze(FileTree tree, Map<String, String> buildFiles);
}