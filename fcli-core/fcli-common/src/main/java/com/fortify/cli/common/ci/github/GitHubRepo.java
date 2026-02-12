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
package com.fortify.cli.common.ci.github;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.Break;
import com.fortify.cli.common.util.GzipHelper;

import kong.unirest.UnirestInstance;
import lombok.RequiredArgsConstructor;

/**
 * Repository-scoped GitHub REST API operations. This class provides methods
 * for interacting with a specific GitHub repository including code scanning,
 * check runs, pull requests, branches, and commits.
 * 
 * @author rsenden
 */
@Reflectable
@RequiredArgsConstructor
public class GitHubRepo {
    private final UnirestInstance unirest;
    private final String owner;
    private final String repo;
    
    // === Code Scanning / SARIF Upload (Advanced Security) ===
    
    /**
     * Upload SARIF report to GitHub Code Scanning (requires GitHub Advanced Security).
     * 
     * SARIF format: https://docs.oasis-open.org/sarif/sarif/v2.1.0/sarif-v2.1.0.html
     * GitHub SARIF support: https://docs.github.com/en/code-security/code-scanning/integrating-with-code-scanning/sarif-support-for-code-scanning
     * API documentation: https://docs.github.com/en/rest/code-scanning
     * 
     * @param ref Git ref (e.g., refs/heads/main)
     * @param sarifContent SARIF report content as string
     * @param commitSha Commit SHA (required)
     * @return Response from GitHub API
     */
    public ObjectNode uploadSarif(String ref, String sarifContent, String commitSha) {
        var compressed = GzipHelper.gzipAndBase64(sarifContent);
        
        var body = JsonHelper.getObjectMapper().createObjectNode()
            .put("sarif", compressed)
            .put("ref", ref)
            .put("commit_sha", commitSha);
        
        return unirest
            .post("/repos/{owner}/{repo}/code-scanning/sarifs")
            .routeParam("owner", owner)
            .routeParam("repo", repo)
            .body(body)
            .asObject(ObjectNode.class)
            .getBody();
    }
    
    /**
     * Create a check run for the commit (available on all tiers including free).
     * This can report scan results without requiring GitHub Advanced Security.
     * For file/line-level findings, use createCheckRunWithAnnotations instead.
     * 
     * API documentation: https://docs.github.com/en/rest/checks/runs
     * 
     * @param name Check run name
     * @param headSha Commit SHA
     * @param status Status (queued, in_progress, completed)
     * @param conclusion Conclusion (success, failure, neutral, cancelled, skipped, timed_out, action_required) - required if status=completed
     * @param title Summary title
     * @param summary Summary text (Markdown supported, max 65535 chars)
     * @return Response from GitHub API
     */
    public ObjectNode createCheckRun(String name, String headSha, String status, 
                                      String conclusion, String title, String summary) {
        return createCheckRunWithAnnotations(name, headSha, status, conclusion, title, summary, null);
    }
    
    /**
     * Create a check run with annotations for file/line-level details (available on all tiers).
     * Annotations allow displaying vulnerability details at specific file locations.
     * Automatically handles pagination for more than 50 annotations by creating the check run
     * with the first 50, then updating with remaining annotations in batches of 50.
     * 
     * Annotation format:
     * - path: File path relative to repository root
     * - start_line: Starting line number (required)
     * - end_line: Ending line number (defaults to start_line)
     * - annotation_level: notice, warning, or failure
     * - message: Annotation message (Markdown supported)
     * - title: Optional short title
     * 
     * API documentation: https://docs.github.com/en/rest/checks/runs#create-a-check-run
     * 
     * @param name Check run name
     * @param headSha Commit SHA
     * @param status Status (queued, in_progress, completed)
     * @param conclusion Conclusion (required if status=completed)
     * @param title Summary title
     * @param summary Summary text (Markdown supported)
     * @param annotations Array of annotation objects (automatic pagination for >50)
     * @return Response from GitHub API (from initial create request)
     */
    public ObjectNode createCheckRunWithAnnotations(String name, String headSha, String status,
                                                     String conclusion, String title, String summary, 
                                                     ArrayNode annotations) {
        ArrayNode firstBatch = null;
        if (annotations != null && annotations.size() > 0) {
            firstBatch = JsonHelper.getObjectMapper().createArrayNode();
            int batchSize = Math.min(50, annotations.size());
            for (int i = 0; i < batchSize; i++) {
                firstBatch.add(annotations.get(i));
            }
        }
        
        var body = JsonHelper.getObjectMapper().createObjectNode()
            .put("name", name)
            .put("head_sha", headSha)
            .put("status", status);
        
        var now = java.time.Instant.now().toString();
        body.put("started_at", now);
        if ("completed".equals(status)) {
            body.put("completed_at", now);
        }
        
        if (conclusion != null) {
            body.put("conclusion", conclusion);
        }
        
        if (title != null || summary != null || firstBatch != null) {
            var output = body.putObject("output");
            if (title != null) output.put("title", title);
            if (summary != null) output.put("summary", summary);
            if (firstBatch != null) {
                output.set("annotations", firstBatch);
            }
        }
        
        var response = unirest
            .post("/repos/{owner}/{repo}/check-runs")
            .routeParam("owner", owner)
            .routeParam("repo", repo)
            .body(body)
            .asObject(ObjectNode.class)
            .getBody();
        
        if (annotations != null && annotations.size() > 50) {
            var checkRunId = response.get("id").asLong();
            for (int offset = 50; offset < annotations.size(); offset += 50) {
                var batch = JsonHelper.getObjectMapper().createArrayNode();
                int batchEnd = Math.min(offset + 50, annotations.size());
                for (int i = offset; i < batchEnd; i++) {
                    batch.add(annotations.get(i));
                }
                updateCheckRunAnnotations(checkRunId, batch);
            }
        }
        
        return response;
    }
    
    /**
     * Update check run with additional annotations.
     * Used internally by createCheckRunWithAnnotations for pagination.
     * 
     * @param checkRunId Check run ID
     * @param annotations Array of annotation objects (max 50)
     */
    private void updateCheckRunAnnotations(long checkRunId, ArrayNode annotations) {
        var body = JsonHelper.getObjectMapper().createObjectNode();
        var output = body.putObject("output");
        output.set("annotations", annotations);
        
        unirest
            .patch("/repos/{owner}/{repo}/check-runs/{check_run_id}")
            .routeParam("owner", owner)
            .routeParam("repo", repo)
            .routeParam("check_run_id", String.valueOf(checkRunId))
            .body(body)
            .asObject(ObjectNode.class)
            .getBody();
    }
    
    // === Pull Request Operations ===
    
    /**
     * Create a general comment on a pull request (issue comment).
     * 
     * @param pullNumber Pull request number
     * @param body Comment body (Markdown supported)
     * @return Created comment object
     */
    public ObjectNode createPullRequestComment(String pullNumber, String body) {
        return unirest
            .post("/repos/{owner}/{repo}/issues/{issue_number}/comments")
            .routeParam("owner", owner)
            .routeParam("repo", repo)
            .routeParam("issue_number", pullNumber)
            .body(JsonHelper.getObjectMapper().createObjectNode().put("body", body))
            .asObject(ObjectNode.class)
            .getBody();
    }
    
    /**
     * Create a review comment on a specific line in a pull request.
     * 
     * @param pullNumber Pull request number
     * @param commitId Commit SHA to comment on
     * @param path File path in repository
     * @param line Line number to comment on
     * @param body Comment body (Markdown supported)
     * @return Created review comment object
     */
    public ObjectNode createReviewComment(String pullNumber, String commitId, 
                                           String path, int line, String body) {
        var requestBody = JsonHelper.getObjectMapper().createObjectNode()
            .put("body", body)
            .put("commit_id", commitId)
            .put("path", path)
            .put("line", line);
        
        return unirest
            .post("/repos/{owner}/{repo}/pulls/{pull_number}/comments")
            .routeParam("owner", owner)
            .routeParam("repo", repo)
            .routeParam("pull_number", pullNumber)
            .body(requestBody)
            .asObject(ObjectNode.class)
            .getBody();
    }
    
    // === Branch and Commit Operations ===
    
    /**
     * Query builder for branches.
     * 
     * @return Query builder for branches
     */
    public GitHubBranchesQuery queryBranches() {
        return new GitHubBranchesQuery();
    }
    
    /**
     * Query builder for commits.
     * 
     * @return Query builder for commits
     */
    public GitHubCommitsQuery queryCommits() {
        return new GitHubCommitsQuery();
    }
    
    /**
     * Get the latest commit for a specific branch.
     * 
     * @param sha Branch SHA to get commit for
     * @return ArrayNode containing the commit (single element)
     */
    public ArrayNode getLatestCommit(String sha) {
        return unirest.get("/repos/{owner}/{repo}/commits")
            .routeParam("owner", owner)
            .routeParam("repo", repo)
            .queryString("sha", sha)
            .queryString("per_page", 1)
            .asObject(ArrayNode.class)
            .getBody();
    }
    
    // === Query Builders ===
    
    /**
     * Query builder for branches.
     */
    @Reflectable
    public class GitHubBranchesQuery {
        private final Map<String, Object> queryParams = new HashMap<>();
        
        /**
         * Add a custom query parameter.
         * 
         * @param key Parameter name
         * @param value Parameter value (null values are ignored)
         * @return This query builder for chaining
         */
        public GitHubBranchesQuery queryParam(String key, Object value) {
            if (value != null) {
                queryParams.put(key, value);
            }
            return this;
        }
        
        /**
         * Execute the query and process results.
         * 
         * @param processor Function that returns Break.TRUE to stop processing, Break.FALSE to continue
         */
        public void process(Function<JsonNode, Break> processor) {
            var request = unirest.get("/repos/{owner}/{repo}/branches?per_page=100")
                .routeParam("owner", owner)
                .routeParam("repo", repo);
            
            queryParams.forEach(request::queryString);
            GitHubPagingHelper.processPagedItems(unirest, request, processor);
        }
    }
    
    /**
     * Query builder for commits.
     */
    @Reflectable
    public class GitHubCommitsQuery {
        private final Map<String, Object> queryParams = new HashMap<>();
        
        /**
         * Filter by branch name or commit SHA.
         * 
         * @param sha Branch name or commit SHA to start from
         * @return This query builder for chaining
         */
        public GitHubCommitsQuery sha(String sha) {
            return queryParam("sha", sha);
        }
        
        /**
         * Filter by date - only commits after this date.
         * 
         * @param since ISO 8601 timestamp (e.g., 2024-01-01T00:00:00Z)
         * @return This query builder for chaining
         */
        public GitHubCommitsQuery since(String since) {
            return queryParam("since", since);
        }
        
        /**
         * Filter by date - only commits before this date.
         * 
         * @param until ISO 8601 timestamp
         * @return This query builder for chaining
         */
        public GitHubCommitsQuery until(String until) {
            return queryParam("until", until);
        }
        
        /**
         * Filter by author.
         * 
         * @param author GitHub login or email address
         * @return This query builder for chaining
         */
        public GitHubCommitsQuery author(String author) {
            return queryParam("author", author);
        }
        
        /**
         * Filter by file path.
         * 
         * @param path File path to filter commits
         * @return This query builder for chaining
         */
        public GitHubCommitsQuery path(String path) {
            return queryParam("path", path);
        }
        
        /**
         * Add a custom query parameter.
         * 
         * @param key Parameter name
         * @param value Parameter value (null values are ignored)
         * @return This query builder for chaining
         */
        public GitHubCommitsQuery queryParam(String key, Object value) {
            if (value != null) {
                queryParams.put(key, value);
            }
            return this;
        }
        
        /**
         * Execute the query and process results.
         * 
         * @param processor Function that returns Break.TRUE to stop processing, Break.FALSE to continue
         */
        public void process(Function<JsonNode, Break> processor) {
            var request = unirest.get("/repos/{owner}/{repo}/commits?per_page=100")
                .routeParam("owner", owner)
                .routeParam("repo", repo);
            
            queryParams.forEach(request::queryString);
            GitHubPagingHelper.processPagedItems(unirest, request, processor);
        }
    }
}
