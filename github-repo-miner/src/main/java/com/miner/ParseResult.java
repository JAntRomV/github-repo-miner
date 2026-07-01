package com.miner;

import java.util.ArrayList;
import java.util.List;

public class ParseResult {

    private String       buildTool     = "None"; // "Maven", "Gradle", "None"
    private boolean      hasSpringBoot = false;
    private boolean      hasMicronaut  = false;
    private List<String> detectedDeps  = new ArrayList<>();

    public String       getBuildTool()     { return buildTool; }
    public boolean      isHasSpringBoot()  { return hasSpringBoot; }
    public boolean      isHasMicronaut()   { return hasMicronaut; }
    public List<String> getDetectedDeps()  { return detectedDeps; }

    public void setBuildTool(String v)      { this.buildTool = v; }
    public void setHasSpringBoot(boolean v) { this.hasSpringBoot = v; }
    public void setHasMicronaut(boolean v)  { this.hasMicronaut = v; }
    public void setDetectedDeps(List<String> v) { this.detectedDeps = v; }
}