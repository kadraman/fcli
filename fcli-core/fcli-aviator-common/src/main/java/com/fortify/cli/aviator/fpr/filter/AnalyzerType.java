package com.fortify.cli.aviator.fpr.filter;

public enum AnalyzerType {
    DATAFLOW("dataflow", "Data Flow"),
    CONTROLFLOW("controlflow", "Control Flow"),
    STRUCTURAL("structural", "Structural"),
    CONFIGURATION("configuration", "Configuration"),
    SEMANTIC("semantic", "Semantic"),
    FINDBUGS("findbugs", "Findbugs"),
    CONTENT("content", "Content"),
    BUFFER("buffer", "Buffer"),
    TRACER_INTEGRATION("tracerintegration", "TracerIntegration"),
    NULLPTR("nullptr", "NullPtr"),
    PENTEST("pentest", "Pentest"),
    STATISTICAL("statistical", "Statistical"),
    REMOVED("removedissue", "RemovedIssue"),
    CUSTOM("customissue", "CustomIssue");

    private final String legacyName;
    private final String canonicalName;

    AnalyzerType(String legacyName, String canonicalName) {
        this.legacyName = legacyName;
        this.canonicalName = canonicalName;
    }

    public String getLegacyName() {
        return legacyName;
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public static String convertAnalyzerForLegacyMappings(String analyzer) {
        for (AnalyzerType type : values()) {
            if (type.canonicalName.equals(analyzer)) {
                return type.legacyName;
            }
        }
        return uncapitalize(analyzer);
    }

    public static String canonicalizeAnalyzerName(String analyzerName) {
        for (AnalyzerType type : values()) {
            if (type.legacyName.equalsIgnoreCase(analyzerName)) {
                return type.canonicalName;
            }
        }
        return capitalize(analyzerName);
    }

    private static String uncapitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}

