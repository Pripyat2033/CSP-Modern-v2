package com.ben.csp.agent;

import net.minecraft.text.Text;
import com.ben.csp.agent.Dosye;

/**
 * Secretary (Секретар) - Administrative and Document Management
 */
public class Sekretar {
    
    private static final String AGENT_NAME = "Секретар";
    private java.util.Map<String, Dosye> activeDosyes = new java.util.HashMap<>();
    private java.util.Queue<String> incomingDocuments = new java.util.LinkedList<>();
    private java.util.Queue<Dosye> pendingFiling = new java.util.LinkedList<>();
    
    public Sekretar() {}
    
    public static Text getAgentName() {
        return Text.literal(AGENT_NAME);
    }
    
    public Dosye createDosye(String dosyeId, String subject) {
        Dosye dosye = new Dosye(dosyeId, subject);
        this.activeDosyes.put(dosyeId, dosye);
        return dosye;
    }
    
    public void receiveDocument(String documentId) {
        this.incomingDocuments.add(documentId);
    }
    
    public String popNextDocument() {
        if (!this.incomingDocuments.isEmpty()) {
            return this.incomingDocuments.poll();
        }
        return null;
    }
    
    public void queueForFiling(String dosyeId) {
        Dosye dosye = this.activeDosyes.get(dosyeId);
        if (dosye != null && !this.pendingFiling.contains(dosye)) {
            this.pendingFiling.add(dosye);
        }
    }
    
    public Dosye processArchiveTask() {
        if (!this.pendingFiling.isEmpty()) {
            return this.pendingFiling.poll();
        }
        return null;
    }
    
    public int getActiveDosyesCount() {
        return this.activeDosyes.size();
    }
    
    public int getIncomingQueueSize() {
        return this.incomingDocuments.size();
    }
}
