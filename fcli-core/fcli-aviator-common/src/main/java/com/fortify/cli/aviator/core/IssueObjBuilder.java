package com.fortify.cli.aviator.core;

import com.fortify.cli.aviator.core.model.AnalysisInfo;
import com.fortify.cli.aviator.core.model.File;
import com.fortify.cli.aviator.core.model.IssueData;
import com.fortify.cli.aviator.core.model.UserPrompt;
import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.util.FileTypeLanguageMapperUtil;
import com.fortify.cli.aviator.util.FileUtil;

import java.util.HashSet;
import java.util.Set;

public class IssueObjBuilder {

    public static UserPrompt buildIssueObj(Vulnerability vulnerability) {

        IssueData issueData = IssueData.builder()
                .accuracy(String.valueOf(vulnerability.getAccuracy()))
                .analyzerName(vulnerability.getAnalyzerName())
                .classID(vulnerability.getClassID())
                .defaultSeverity(String.valueOf(vulnerability.getDefaultSeverity()))
                .impact(String.valueOf(vulnerability.getImpact()))
                .instanceID(vulnerability.getInstanceID())
                .instanceSeverity(String.valueOf(vulnerability.getInstanceSeverity()))
                .filetype(vulnerability.getFiletype())
                .kingdom(vulnerability.getKingdom())
                .likelihood(vulnerability.getLikelihood())
                .priority(vulnerability.getPriority())
                .probability(String.valueOf(vulnerability.getProbability()))
                .confidence(String.valueOf(vulnerability.getConfidence()))
                .subType(vulnerability.getSubType())
                .type(vulnerability.getType())
                .build();

        AnalysisInfo analysisInfo = AnalysisInfo.builder()
                .shortDescription(vulnerability.getShortDescription())
                .explanation(vulnerability.getExplanation())
                .build();

        Set<String> programmingLanguages = new HashSet<>();
        if (vulnerability.getFiles() != null) {
            for (File file : vulnerability.getFiles()) {
                String fileExtension = FileUtil.getFileExtension(file.getName());
                String language = FileTypeLanguageMapperUtil.getProgrammingLanguage(fileExtension);
                if (language != null) {
                    programmingLanguages.add(language);
                }
            }
        }

        String language = programmingLanguages.isEmpty() ? null : programmingLanguages.iterator().next();

        return UserPrompt.builder()
                .issueData(issueData)
                .analysisInfo(analysisInfo)
                .stackTrace(vulnerability.getStackTrace())
                .firstStackTrace(vulnerability.getFirstStackTrace())
                .longestStackTrace(vulnerability.getLongestStackTrace())
                .files(vulnerability.getFiles())
                .lastStackTraceElement(vulnerability.getLastStackTraceElement())
                .programmingLanguages(programmingLanguages)
                .fileExtension(FileUtil.getFileExtension(vulnerability.getLastStackTraceElement().getFilename()))
                .language(language)
                .category(vulnerability.getCategory())
                .tier("")
                .source(vulnerability.getSource())
                .sink(vulnerability.getSink())
                .build();
    }
}