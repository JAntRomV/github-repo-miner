package com.miner;

import org.kohsuke.github.GHRepository;
import java.io.IOException;
import java.util.List;

public class RepositoryData {

    // --- Campos Fase 1 (REST API) ---
    private String fullName;
    private String description;
    private String htmlUrl;
    private int stars;
    private int size;
    private String language;
    private String pushedAt;
    private int forks;
    private int openIssues;

    // --- Campos Fase 2 (GraphQL) — vacíos hasta que GraphQL los llene ---
    private int commitCount;
    private String license;
    private List<String> topics;
    private int watchersCount;
    private boolean hasIssuesEnabled;

    // Constructor vacío requerido por Jackson
    public RepositoryData() {}

    // Factory method: llena solo los campos de Fase 1
    public static RepositoryData fromGHRepository(GHRepository repo) throws IOException {
        RepositoryData data = new RepositoryData();
        data.fullName    = repo.getFullName();
        data.description = repo.getDescription() != null ? repo.getDescription() : "";
        data.htmlUrl     = repo.getHtmlUrl().toString();
        data.stars       = repo.getStargazersCount();
        data.size        = repo.getSize();
        data.language    = repo.getLanguage() != null ? repo.getLanguage() : "";
        data.pushedAt    = repo.getPushedAt() != null ? repo.getPushedAt().toString() : "";
        data.forks       = repo.getForksCount();
        data.openIssues  = repo.getOpenIssueCount();
        return data;
    }

    // Getters Fase 1
    public String getFullName()    { return fullName; }
    public String getDescription() { return description; }
    public String getHtmlUrl()     { return htmlUrl; }
    public int    getStars()       { return stars; }
    public int    getSize()        { return size; }
    public String getLanguage()    { return language; }
    public String getPushedAt()    { return pushedAt; }
    public int    getForks()       { return forks; }
    public int    getOpenIssues()  { return openIssues; }

    // Getters Fase 2
    public int          getCommitCount()      { return commitCount; }
    public String       getLicense()          { return license; }
    public List<String> getTopics()           { return topics; }
    public int          getWatchersCount()    { return watchersCount; }
    public boolean      isHasIssuesEnabled()  { return hasIssuesEnabled; }

    // Setters Fase 2 — GraphQL los llena después de Fase 1
    public void setCommitCount(int v)          { this.commitCount = v; }
    public void setLicense(String v)           { this.license = v; }
    public void setTopics(List<String> v)      { this.topics = v; }
    public void setWatchersCount(int v)        { this.watchersCount = v; }
    public void setHasIssuesEnabled(boolean v) { this.hasIssuesEnabled = v; }

    // --- Campos Fase 3 (Filtro Técnico) ---
private String       buildTool;              // "Maven", "Gradle", "None"
private boolean      hasFrameworkDependency; // true si se encontró la dep del framework
private String       detectedFramework;      // "Spring Boot", "Micronaut", "Both", "None"
private List<String> detectedDeps;           // deps exactas encontradas
private boolean      hasSrcMainJava;         // tiene estructura Java estándar

// Getters Fase 3
public String       getBuildTool()              { return buildTool; }
public boolean      isHasFrameworkDependency()  { return hasFrameworkDependency; }
public String       getDetectedFramework()      { return detectedFramework; }
public List<String> getDetectedDeps()           { return detectedDeps; }
public boolean      isHasSrcMainJava()          { return hasSrcMainJava; }

// Setters Fase 3
public void setBuildTool(String v)               { this.buildTool = v; }
public void setHasFrameworkDependency(boolean v) { this.hasFrameworkDependency = v; }
public void setDetectedFramework(String v)       { this.detectedFramework = v; }
public void setDetectedDeps(List<String> v)      { this.detectedDeps = v; }
public void setHasSrcMainJava(boolean v)         { this.hasSrcMainJava = v; }
}