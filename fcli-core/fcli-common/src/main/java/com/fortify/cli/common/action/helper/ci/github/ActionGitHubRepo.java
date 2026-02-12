/*
 * Copyright 2021-2026 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.common.action.helper.ci.github;

import static com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction.SpelFunctionCategory.ci;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.ci.github.GitHubEnvironment;
import com.fortify.cli.common.ci.github.GitHubRepo;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionParam;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctions;

import lombok.RequiredArgsConstructor;

/**
 * Action wrapper for GitHubRepo providing environment-aware GitHub repository operations.
 * Used by ActionGitHubSpelFunctions to provide SpEL functions that automatically 
 * use GitHub Actions environment defaults.
 * 
 * @author rsenden
 */
@Reflectable
@RequiredArgsConstructor
@SpelFunctions
public class ActionGitHubRepo {
    private final GitHubRepo repo;
    private final GitHubEnvironment env;
    
    @SpelFunction(cat=ci, desc="Uploads SARIF to GitHub Code Scanning (paid tier, requires GHAS) using repository/ref/commit from the current workflow run",
            returns="Response from GitHub API")
    public ObjectNode uploadSarif(
            @SpelFunctionParam(name="sarifContent", desc="SARIF report content as string") String sarifContent) {
        var ref = env.ciBranch().full();
        var sha = env.ciCommit().id().full();
        return repo.uploadSarif(ref, sarifContent, sha);
    }
    
    @SpelFunction(cat=ci, desc="Tries to upload SARIF, returning {success, reason, message} for fallback logic; does not throw on GHAS unavailable",
            returns="Result object: {success: true, data: ...} or {success: false, reason: 'ghas_unavailable'|'other', message: ...}")
    public ObjectNode tryUploadSarif(
            @SpelFunctionParam(name="sarifContent", desc="SARIF report content as string") String sarifContent) {
        var mapper = JsonHelper.getObjectMapper();
        try {
            var ref = env.ciBranch().full();
            var sha = env.ciCommit().id().full();
            var response = repo.uploadSarif(ref, sarifContent, sha);
            return mapper.createObjectNode()
                .put("success", true)
                .set("data", response);
        } catch (UnexpectedHttpResponseException e) {
            var status = e.getStatus();
            var isGhasUnavailable = (status == 403 || status == 404) && 
                                   e.getMessage().contains("code-scanning");
            return mapper.createObjectNode()
                .put("success", false)
                .put("reason", isGhasUnavailable ? "ghas_unavailable" : "other")
                .put("message", e.getMessage());
        } catch (Exception e) {
            return mapper.createObjectNode()
                .put("success", false)
                .put("reason", "other")
                .put("message", e.getMessage());
        }
    }
    
    @SpelFunction(cat=ci, desc="Creates check run (free tier, for summary only) using repository and commit detected from the current run",
            returns="Response from GitHub API")
    public ObjectNode createCheckRun(
            @SpelFunctionParam(name="name", desc="check run name") String name,
            @SpelFunctionParam(name="status", desc="status: queued, in_progress, completed") String status,
            @SpelFunctionParam(name="conclusion", desc="conclusion: success, failure, neutral, cancelled, skipped, timed_out, action_required") String conclusion,
            @SpelFunctionParam(name="title", desc="summary title") String title,
            @SpelFunctionParam(name="summary", desc="summary text in Markdown (max 65535 chars)") String summary) {
        var sha = env.ciCommit().id().full();
        return repo.createCheckRun(name, sha, status, conclusion, title, summary);
    }
    
    @SpelFunction(cat=ci, desc="Creates check run with file/line annotations (free tier, shows vulnerabilities at source locations, auto-paginates) using the detected repository/commit",
            returns="Response from GitHub API")
    public ObjectNode createCheckRunWithAnnotations(
            @SpelFunctionParam(name="name", desc="check run name") String name,
            @SpelFunctionParam(name="status", desc="status: queued, in_progress, completed") String status,
            @SpelFunctionParam(name="conclusion", desc="conclusion: success, failure, neutral, etc.") String conclusion,
            @SpelFunctionParam(name="title", desc="summary title") String title,
            @SpelFunctionParam(name="summary", desc="summary text in Markdown") String summary,
            @SpelFunctionParam(name="annotations", desc="array of annotation objects with path, line, level, message (auto-paginated)") ArrayNode annotations) {
        var sha = env.ciCommit().id().full();
        return repo.createCheckRunWithAnnotations(name, sha, status, conclusion, title, summary, annotations);
    }
    
    @SpelFunction(cat=ci, desc="Adds a comment to the current pull request detected from the workflow run",
            returns="Created comment object")
    public ObjectNode addPrComment(
            @SpelFunctionParam(name="body", desc="comment body (Markdown supported)") String body) {
        if (!env.pullRequest().active()) {
            throw new FcliSimpleException("Not running in pull request context. GITHUB_HEAD_REF is not set.");
        }
        return repo.createPullRequestComment(env.pullRequest().id(), body);
    }
    
    @SpelFunction(cat=ci, desc="Adds a review comment on a specific file and line in the pull request detected from the workflow run",
            returns="Created review comment object")
    public ObjectNode addReviewComment(
            @SpelFunctionParam(name="path", desc="file path relative to repository root") String path,
            @SpelFunctionParam(name="line", desc="line number") int line,
            @SpelFunctionParam(name="body", desc="comment body (Markdown supported)") String body) {
        if (!env.pullRequest().active()) {
            throw new FcliSimpleException("Not running in pull request context. GITHUB_HEAD_REF is not set.");
        }
        var sha = env.ciCommit().id().full();
        return repo.createReviewComment(env.pullRequest().id(), sha, path, line, body);
    }
}
