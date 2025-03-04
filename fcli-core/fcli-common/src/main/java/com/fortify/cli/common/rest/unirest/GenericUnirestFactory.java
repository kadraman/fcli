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
package com.fortify.cli.common.rest.unirest;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.json.JsonHelper;

import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;
import kong.unirest.jackson.JacksonObjectMapper;

public final class GenericUnirestFactory {
    private static final Logger LOG = LoggerFactory.getLogger(GenericUnirestFactory.class);
    private static final ConcurrentMap<String, UnirestInstance> instances = new ConcurrentHashMap<>();
    
    /**
     * Create a new {@link UnirestInstance}. Callers are responsible for closing the
     * {@link UnirestInstance} after use, for example using a try-with-resources block.
     * @return
     */
    public static final UnirestInstance createUnirestInstance() {
        UnirestInstance instance = Unirest.spawnInstance();
        instance.config().setObjectMapper(new JacksonObjectMapper(JsonHelper.getObjectMapper()));
        return instance;
    }
    
    /**
     * Check whether a {@link UnirestInstance} for the given key already exists.
     * @param key
     * @return
     */
    public static final boolean hasUnirestInstance(String key) {
        return instances.containsKey(key);
    }
    
    /**
     * Get a {@link UnirestInstance} instance for the given key. If an instance
     * for the given key doesn't exist yet, a new instance will be created.
     * All previously created instances can be shut down by calling the {@link #shutdown()}
     * method, individual instances can be shut down by calling the {@link #shutdown(String)}
     * method.
     * @return
     */
    public static final UnirestInstance getUnirestInstance(String key, Consumer<UnirestInstance> configurer) {
        UnirestInstance instance = instances.get(key);
        if ( instance==null ) {
            instance = createUnirestInstance();
            if ( configurer!=null ) { configurer.accept(instance); }
            instances.put(key, instance);
            LOG.debug(String.format("Created UnirestInstance: %s: %s", key, instance));
        }
        LOG.debug(String.format("getUnirestInstance: %s: %s", key, instance));
        return new NonClosingUnirestInstanceWrapper(instance);
    }
    
    public static final void shutdown() {
        instances.keySet().stream().forEach(GenericUnirestFactory::shutdown);
    }
    
    public static final void shutdown(String key) {
        UnirestInstance instance = instances.remove(key);
        if ( instance!=null ) {
            try {
                LOG.debug(String.format("Shut down UnirestInstance: %s: %s", key, instance));
                instance.shutDown(true);
            } catch ( Exception e ) {
                String msg = "Error shutting down unirest instance"; 
                LOG.warn(msg);
                LOG.debug(msg, e);
            }
        }
    }
}
