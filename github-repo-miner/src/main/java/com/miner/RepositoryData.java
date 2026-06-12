package com.miner;

import org.kohsuke.github.GHRepository;
import java.io.IOException;

public class RepositoryData {

    private String fullName;
    private String description;
    private String htmlUrl;
    private int stars;
    private int size;
    private String language;
    private String pushedAt;
    private int forks;
    private int openIssues;

    // Constructor vacío requerido por Jackson
    public RepositoryData() {}

    // Convierte un GHRepository en nuestro modelo de datos
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

    public String getFullName()    { return fullName; }
    public String getDescription() { return description; }
    public String getHtmlUrl()     { return htmlUrl; }
    public int    getStars()       { return stars; }
    public int    getSize()        { return size; }
    public String getLanguage()    { return language; }
    public String getPushedAt()    { return pushedAt; }
    public int    getForks()       { return forks; }
    public int    getOpenIssues()  { return openIssues; }
}