package com.miner;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale; 

public class ScoringEngine {

  private static final SimpleDateFormat DATE_FORMAT =
    new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);

    public List<RepoScore> scoreAndRank(List<RepositoryData> repos) {
        System.out.println("\n========================================");
        System.out.println("  ISSUE #4: SCORING Y RANKING");
        System.out.println("  Repositorios a puntuar: " + repos.size());
        System.out.println("========================================\n");

        double[] starsLog    = repos.stream().mapToDouble(r -> Math.log1p(r.getStars())).toArray();
        double[] forksLog    = repos.stream().mapToDouble(r -> Math.log1p(r.getForks())).toArray();
        double[] watchersLog = repos.stream().mapToDouble(r -> Math.log1p(r.getWatchersCount())).toArray();
        double[] commitsLog  = repos.stream().mapToDouble(r -> Math.log1p(r.getCommitCount())).toArray();
        double[] testFiles   = repos.stream().mapToDouble(r -> r.getTechProfile().testFileCount()).toArray();

        double starsMax = max(starsLog), starsMin = min(starsLog);
        double forksMax = max(forksLog), forksMin = min(forksLog);
        double watchersMax = max(watchersLog), watchersMin = min(watchersLog);
        double commitsMax = max(commitsLog), commitsMin = min(commitsLog);
        double testMax = max(testFiles), testMin = min(testFiles);

        List<RepoScore> scores = new ArrayList<>();

        for (int i = 0; i < repos.size(); i++) {
            RepositoryData repo = repos.get(i);
            TechProfile profile = repo.getTechProfile();

            double technical   = scoreTechnical(profile);
            double testQuality = normalize(testFiles[i], testMin, testMax) * ScoringWeights.TEST_QUALITY_MAX;
            double ciHygiene   = scoreCiHygiene(repo, profile);
            double sector      = profile.sector() != Sector.UNKNOWN ? ScoringWeights.SECTOR_KNOWN : 0.0;

            double popularity =
                normalize(starsLog[i], starsMin, starsMax) * ScoringWeights.STARS_MAX +
                normalize(forksLog[i], forksMin, forksMax) * ScoringWeights.FORKS_MAX +
                normalize(watchersLog[i], watchersMin, watchersMax) * ScoringWeights.WATCHERS_MAX;

            double maintenance =
                normalize(commitsLog[i], commitsMin, commitsMax) * ScoringWeights.COMMITS_MAX +
                scoreRecency(repo.getPushedAt());

            RepoScore score = RepoScore.withoutRank(
                repo.getFullName(), technical, testQuality, ciHygiene, sector, popularity, maintenance
            );

            scores.add(score);

            System.out.printf("  %-45s total=%.1f (tech=%.1f test=%.1f ci=%.1f sec=%.1f pop=%.1f maint=%.1f)%n",
                repo.getFullName(), score.totalScore(), technical, testQuality, ciHygiene, sector, popularity, maintenance);
        }

    scores.sort(
    Comparator.comparingDouble(RepoScore::totalScore).reversed()
        .thenComparing(Comparator.comparingDouble(RepoScore::popularityScore).reversed())
         );
    

        List<RepoScore> ranked = new ArrayList<>();
        for (int i = 0; i < scores.size(); i++) {
            ranked.add(scores.get(i).withRank(i + 1));
        }

        System.out.println("\n  === TOP 5 DEL RANKING ===");
        ranked.stream().limit(5).forEach(s ->
            System.out.printf("  #%d  %-45s  %.1f/100%n", s.rank(), s.fullName(), s.totalScore()));

        return ranked;
    }

    private double scoreTechnical(TechProfile profile) {
        double score = 0;
        if (profile.graalvmReady()) score += ScoringWeights.GRAALVM_READY;
        if (profile.jmhPresent()) {
            score += ScoringWeights.JMH_CONFIRMED;
        } else if (profile.jmhCandidate()) {
            score += ScoringWeights.JMH_HEURISTIC_ONLY;
        }
        return score;
    }

    private double scoreCiHygiene(RepositoryData repo, TechProfile profile) {
        double score = 0;
        if (profile.travisCi()) score += ScoringWeights.TRAVIS_CI;

        double hygiene = 0;
        if (repo.getLicense() != null && !repo.getLicense().equals("No license")) hygiene += 4;
        if (repo.isHasIssuesEnabled()) hygiene += 3;
        if (repo.getStars() > 0 && (double) repo.getOpenIssues() / repo.getStars() < 0.1) hygiene += 3;
        score += hygiene;

        return score;
    }

    private double scoreRecency(String pushedAtRaw) {
        try {
            Date pushedAt = DATE_FORMAT.parse(pushedAtRaw);
            long daysSincePush = (new Date().getTime() - pushedAt.getTime()) / (1000L * 60 * 60 * 24);
            double fraction = Math.max(0, 1.0 - (daysSincePush / 365.0));
            return fraction * ScoringWeights.RECENCY_MAX;
        } catch (ParseException e) {
            System.err.println("  [WARN] No se pudo parsear pushedAt: " + pushedAtRaw);
            return ScoringWeights.RECENCY_MAX * 0.5;
        }
    }

    private double normalize(double value, double min, double max) {
        if (max == min) return 1.0;
        return (value - min) / (max - min);
    }

    private double max(double[] arr) {
        double m = Double.NEGATIVE_INFINITY;
        for (double v : arr) if (v > m) m = v;
        return m;
    }

    private double min(double[] arr) {
        double m = Double.POSITIVE_INFINITY;
        for (double v : arr) if (v < m) m = v;
        return m;
    }
}