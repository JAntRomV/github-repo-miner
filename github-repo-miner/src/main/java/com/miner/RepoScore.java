package com.miner;

public record RepoScore(
    String fullName,
    double technicalScore,
    double testQualityScore,
    double ciHygieneScore,
    double sectorScore,
    double popularityScore,
    double maintenanceScore,
    double totalScore,
    int rank
) {
    public static RepoScore withoutRank(String fullName, double tech, double test,
                                         double ci, double sector, double pop, double maint) {
        double total = tech + test + ci + sector + pop + maint;
        return new RepoScore(fullName, tech, test, ci, sector, pop, maint, total, 0);
    }

    public RepoScore withRank(int newRank) {
        return new RepoScore(fullName, technicalScore, testQualityScore, ciHygieneScore,
            sectorScore, popularityScore, maintenanceScore, totalScore, newRank);
    }
}