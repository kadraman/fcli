package com.fortify.cli.util.mcp_server.helper.mcp.runner;

import java.util.List;
import java.util.stream.Collectors;

import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.util.mcp_server.helper.mcp.arg.IMCPToolArgHandler;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/**
 * IMCPToolRunner implementation for running fcli actions through 'fcli <module> action run <action>'
 * Collects stdout and returns as plain text MCP tool result.
 */
public final class MCPToolFcliRunnerAction implements IMCPToolRunner {
    private final String module;
    private final Action action;
    private final List<IMCPToolArgHandler> argHandlers;
    
    public MCPToolFcliRunnerAction(String module, Action action, List<IMCPToolArgHandler> argHandlers) {
        this.module = module;
        this.action = action;
        this.argHandlers = argHandlers;
    }

    @Override
    public CallToolResult run(McpSyncServerExchange exchange, CallToolRequest request) {
        var fullCmd = buildFullCmd(request);
        var result = MCPToolFcliRunnerHelper.collectStdout(fullCmd);
        return MCPToolResultPlainText.from(result).asCallToolResult();
    }
    
    private String buildFullCmd(CallToolRequest request) {
        var base = String.format("fcli %s action run %s", module, action.getMetadata().getName());
        var argStr = argHandlers.stream()
                .map(h->h.getFcliCmdArgs(request==null?null:request.arguments()))
                .filter(s->s!=null && !s.isBlank())
                .collect(Collectors.joining(" "));
        return argStr.isBlank()?base:String.format("%s %s", base, argStr);
    }
}
