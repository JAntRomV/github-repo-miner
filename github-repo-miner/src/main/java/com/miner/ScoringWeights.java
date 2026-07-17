package com.miner;

public class ScoringWeights {
    public static final double GRAALVM_READY       = 15.0;
    public static final double JMH_CONFIRMED       = 15.0;
    public static final double JMH_HEURISTIC_ONLY  = 7.0;

    public static final double TEST_QUALITY_MAX    = 15.0;

    public static final double TRAVIS_CI           = 5.0;
    public static final double HYGIENE_MAX         = 10.0;

    public static final double SECTOR_KNOWN        = 5.0;

    public static final double STARS_MAX           = 10.0;
    public static final double FORKS_MAX           = 6.0;
    public static final double WATCHERS_MAX        = 4.0;

    public static final double COMMITS_MAX         = 8.0;
    public static final double RECENCY_MAX         = 7.0;

    public static final double TOTAL_POSSIBLE      = 100.0;
}