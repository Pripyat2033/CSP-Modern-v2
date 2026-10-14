package com.ben.csp.agent;

import net.minecraft.text.Text;

/**
 * Secretary (Секретар) - Enterprise Clerk / Director Assistant
 */
public class Sekretar {
    private java.util.Map<String, String> activeFiles = new java.util.HashMap<>();
    
    /**
     * Process a document that needs filing
     */
    public void processFile(String dosyeId, Text subjectText) {
        // Secretary handles document intake and routing
        System.out.println("Secretary processing file: " + subjectText.getString() + 
                           " -> Dosye: " + dosyeId);
        
        // Track that we've seen this file (for audit trail)
        if (!activeFiles.containsKey(dosyeId)) {
            activeFiles.put(dosyeId, "Processing");
        }
    }
    
    /**
     * Get current workload (number of files currently being processed)
     */
    public int getCurrentWorkload() {
        return activeFiles.size();
    }
}
