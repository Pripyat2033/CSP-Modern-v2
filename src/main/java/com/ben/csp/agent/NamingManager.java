package com.ben.csp.agent;

public class NamingManager {
    // Manager for entity naming
    
    private static final String DEFAULT_ENTITY_NAME_PREFIX = "csp_";
    
    public static String getDefaultEntityPrefix() {
        return DEFAULT_ENTITY_NAME_PREFIX;
    }
}
