package com.miner;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SharedCatalogWriter {

    public void writeCatalog(List<RepositoryData> repos, List<RepoScore> scores) {
        MongoCollection<Document> collection = MongoManager.getCatalogCollection();

        Map<String, RepoScore> scoreByName = scores.stream()
            .collect(Collectors.toMap(RepoScore::fullName, s -> s));

        int written = 0;

        for (RepositoryData repo : repos) {
            RepoScore score = scoreByName.get(repo.getFullName());
            Document miningDoc = buildMiningDoc(repo, score);

       
            Document setFields = new Document("fullName", repo.getFullName())
                .append("htmlUrl", repo.getHtmlUrl())
                .append("defaultBranch", repo.getDefaultBranch())
                .append("mining", miningDoc);

            Document setOnInsertFields = new Document("metrics", null)
                .append("status", "pending_metrics");

            Document update = new Document("$set", setFields)
                .append("$setOnInsert", setOnInsertFields);

            collection.updateOne(
                Filters.eq("_id", repo.getFullName()),
                update,
                new UpdateOptions().upsert(true)
            );
            written++;
        }

        System.out.println("✓ " + written + " documentos escritos/actualizados en shared_catalog.repo_catalog");
    }

    private Document buildMiningDoc(RepositoryData repo, RepoScore score) {
        TechProfile p = repo.getTechProfile();

        Document techProfileDoc = new Document()
            .append("buildTool", p != null ? p.buildTool().name() : null)
            .append("framework", p != null ? p.framework().name() : null)
            .append("javaVersion", p != null ? p.javaVersion() : null)
            .append("java21", p != null && p.java21())
            .append("graalvmReady", p != null && p.graalvmReady())
            .append("hasTestSuite", p != null && p.hasTestSuite())
            .append("testFramework", p != null ? p.testFramework().name() : null)
            .append("testFileCount", p != null ? p.testFileCount() : 0)
            .append("jmhPresent", p != null && p.jmhPresent())
            .append("jmhCandidate", p != null && p.jmhCandidate())
            .append("profilingCandidate", p != null && p.profilingCandidate())
            .append("sector", p != null ? p.sector().name() : null)
            .append("travisCi", p != null && p.travisCi())
            .append("passesHardFilters", p != null && p.passesHardFilters());

        Document scoreDoc = (score != null) ? new Document()
            .append("technicalScore", score.technicalScore())
            .append("testQualityScore", score.testQualityScore())
            .append("ciHygieneScore", score.ciHygieneScore())
            .append("sectorScore", score.sectorScore())
            .append("popularityScore", score.popularityScore())
            .append("maintenanceScore", score.maintenanceScore())
            .append("totalScore", score.totalScore())
            .append("rank", score.rank())
            : null;

        return new Document()
            .append("writtenBy", "eber-mining-framework")
            .append("writtenAt", Instant.now().toString())
            .append("stars", repo.getStars())
            .append("forks", repo.getForks())
            .append("watchersCount", repo.getWatchersCount())
            .append("commitCount", repo.getCommitCount())
            .append("license", repo.getLicense())
            .append("topics", repo.getTopics())
            .append("techProfile", techProfileDoc)
            .append("score", scoreDoc);
    }
}