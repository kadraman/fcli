/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.sc_dast._common.session.helper;

import java.time.OffsetDateTime;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.rest.unirest.config.IUserCredentialsConfig;
import com.fortify.cli.common.rest.unirest.config.UrlConfig;
import com.fortify.cli.common.session.helper.AbstractSessionDescriptor;
import com.fortify.cli.common.session.helper.SessionSummary;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.sc_dast._common.util.SCDastConstants;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.ssc._common.session.helper.ISSCCredentialsConfig;
import com.fortify.cli.ssc._common.session.helper.ISSCUserCredentialsConfig;
import com.fortify.cli.ssc.access_control.helper.SSCTokenCreateRequest;
import com.fortify.cli.ssc.access_control.helper.SSCTokenGetOrCreateResponse;
import com.fortify.cli.ssc.access_control.helper.SSCTokenHelper;
import com.fortify.cli.ssc.access_control.helper.SSCTokenGetOrCreateResponse.SSCTokenData;

import kong.unirest.UnirestInstance;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data @EqualsAndHashCode(callSuper = true) @JsonIgnoreProperties(ignoreUnknown = true)
@Reflectable @NoArgsConstructor 
public class SCDastSessionDescriptor extends AbstractSessionDescriptor {
    @JsonDeserialize(as = UrlConfig.class) private IUrlConfig sscUrlConfig;
    @JsonDeserialize(as = UrlConfig.class) private IUrlConfig scDastUrlConfig;
    private char[] predefinedToken;
    private SSCTokenGetOrCreateResponse cachedTokenResponse;
    
    public SCDastSessionDescriptor(IUrlConfig sscUrlConfig, ISSCCredentialsConfig credentialsConfig) {
        this(sscUrlConfig, null, credentialsConfig);
    }
    
    public SCDastSessionDescriptor(IUrlConfig sscUrlConfig, IUrlConfig scDastUrlConfig, ISSCCredentialsConfig credentialsConfig) {
        this.sscUrlConfig = sscUrlConfig;
        this.predefinedToken = credentialsConfig.getPredefinedToken();
        this.cachedTokenResponse = getOrGenerateToken(sscUrlConfig, credentialsConfig);
        char[] activeToken = getActiveToken();
        this.scDastUrlConfig = activeToken==null ? null : buildScDastUrlConfig(sscUrlConfig, scDastUrlConfig, activeToken);
    }

    @JsonIgnore
    public void logout(IUserCredentialsConfig userCredentialsConfig) {
     // We only revoke the token if we generated a token upon login, 
        // and that token hasn't expired yet.
        if ( predefinedToken==null && hasActiveCachedTokenResponse() ) {
            SSCTokenHelper.revokeToken(getSscUrlConfig(), userCredentialsConfig, cachedTokenResponse.getData().getToken());
        }
    }
    
    @Override @JsonIgnore
    public String getUrlDescriptor() {
        return String.format("SSC:     %s\nSC-DAST: %s", 
                sscUrlConfig==null || sscUrlConfig.getUrl()==null ? "unknown" : sscUrlConfig.getUrl(),
                scDastUrlConfig==null || scDastUrlConfig.getUrl()==null ? "unknown" : scDastUrlConfig.getUrl());
    }

    @JsonIgnore 
    public final char[] getActiveToken() {
        if ( hasActiveCachedTokenResponse() ) {
            return getCachedTokenResponseData().getToken();
        } else {
            return predefinedToken;
        }
    }
    
    @JsonIgnore
    public final boolean hasActiveCachedTokenResponse() {
        return getCachedTokenResponseData()!=null && getCachedTokenResponseData().getTerminalDate().after(new Date()); 
    }
    
    @JsonIgnore
    public Date getExpiryDate() {
        Date sessionExpiryDate = SessionSummary.EXPIRES_UNKNOWN;
        if ( getCachedTokenTerminalDate()!=null ) {
            sessionExpiryDate = getCachedTokenTerminalDate();
        }
        return sessionExpiryDate;
    }
    
    @JsonIgnore @Override
    public String getType() {
        return SCDastSessionHelper.instance().getType();
    }
    
    @JsonIgnore
    protected SSCTokenGetOrCreateResponse getOrGenerateToken(IUrlConfig urlConfig, ISSCCredentialsConfig credentialsConfig) {
        return credentialsConfig.getPredefinedToken()==null 
                ? generateToken(urlConfig, credentialsConfig) 
                : getToken(urlConfig, credentialsConfig);
    }

    @JsonIgnore
    protected SSCTokenGetOrCreateResponse getToken(IUrlConfig urlConfig, ISSCCredentialsConfig credentialsConfig) {
        return SSCTokenHelper.getTokenData(urlConfig, credentialsConfig.getPredefinedToken());
    }
    
    @JsonIgnore
    protected SSCTokenGetOrCreateResponse generateToken(IUrlConfig urlConfig, ISSCCredentialsConfig credentialsConfig) {
        ISSCUserCredentialsConfig uc = credentialsConfig.getUserCredentialsConfig();
        if ( uc!=null && StringUtils.isNotBlank(uc.getUser()) && uc.getPassword()!=null ) {
            SSCTokenCreateRequest tokenCreateRequest = SSCTokenCreateRequest.builder()
                    .description("Auto-generated by fcli session login command")
                    .terminalDate(getExpiresAt(uc.getExpiresAt())) 
                    .type("CIToken")
                    .build();
            return SSCTokenHelper.createToken(urlConfig, uc, tokenCreateRequest, SSCTokenGetOrCreateResponse.class);
        }
        return null;
    }
    
    private OffsetDateTime getExpiresAt(OffsetDateTime expiresAt) {
        return expiresAt!=null 
            ? expiresAt 
            : DateTimePeriodHelper.byRange(Period.MINUTES, Period.DAYS).getCurrentOffsetDateTimePlusPeriod(SCDastConstants.DEFAULT_TOKEN_EXPIRY);
    }

    @JsonIgnore 
    private final String getTokenId() {
        if ( hasActiveCachedTokenResponse() ) {
            return getCachedTokenResponseData().getId();
        } else {
            return null;
        }
    }
    
    @JsonIgnore
    private Date getCachedTokenTerminalDate() {
        return getCachedTokenResponseData()==null ? null : getCachedTokenResponseData().getTerminalDate();
    }
    
    @JsonIgnore
    private SSCTokenData getCachedTokenResponseData() {
        return cachedTokenResponse==null || cachedTokenResponse.getData()==null 
                ? null
                : cachedTokenResponse.getData();
    }
    
    private static final IUrlConfig buildScDastUrlConfig(IUrlConfig sscUrlConfig, IUrlConfig scDastUrlConfig, char[] activeToken) {
        String scDastUrl = scDastUrlConfig!=null && StringUtils.isNotBlank(scDastUrlConfig.getUrl())
                ? scDastUrlConfig.getUrl()
                : getScDastUrl(sscUrlConfig, activeToken);
        UrlConfig.UrlConfigBuilder builder = UrlConfig.builderFrom(sscUrlConfig, scDastUrlConfig);
        builder.url(scDastUrl);
        return builder.build();
    }

    private static String getScDastUrl(IUrlConfig sscUrlConfig, char[] activeToken) {
        return SSCTokenHelper.run(sscUrlConfig, activeToken, SCDastSessionDescriptor::getScDastUrl);
    }

    private static final String getScDastUrl(UnirestInstance unirest) {
        ArrayNode properties = getScDastConfigurationProperties(unirest);
        checkScDastIsEnabled(properties);
        String scDastUrl = getScDastUrlFromProperties(properties);
        return normalizeScDastUrl(scDastUrl);
    }
    
    private static final ArrayNode getScDastConfigurationProperties(UnirestInstance sscUnirest) {
        ObjectNode configData = sscUnirest.get("/api/v1/configuration?group=edast")
                .asObject(ObjectNode.class)
                .getBody(); 
        
        return JsonHelper.evaluateSpelExpression(configData, "data.properties", ArrayNode.class);
    }
    
    private static final void checkScDastIsEnabled(ArrayNode properties) {
        boolean scDastEnabled = JsonHelper.evaluateSpelExpression(properties, "^[name=='edast.enabled']?.value=='true'", Boolean.class);
        if (!scDastEnabled) {
            throw new IllegalStateException("ScanCentral DAST must be enabled in SSC");
        }
    }
    
    private static final String getScDastUrlFromProperties(ArrayNode properties) {
        String scDastUrl = JsonHelper.evaluateSpelExpression(properties, "^[name=='edast.server.url']?.value", String.class);
        if ( scDastUrl.isEmpty() ) {
            throw new IllegalStateException("SSC returns an empty ScanCentral DAST URL");
        }
        return scDastUrl;
    }
    
    private static final String normalizeScDastUrl(String scDastUrl) {
        // We remove '/api' and any trailing slashes from the URL as most users will specify relative URL's starting with /api/v2/...
        return scDastUrl.replaceAll("/api/?$","").replaceAll("/+$", "");
    }
}
