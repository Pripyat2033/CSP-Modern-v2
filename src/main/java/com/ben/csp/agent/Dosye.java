package com.ben.csp.agent;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import java.util.*;

/**
 * Case File (Досье) - Soviet-era document record for tracking personnel, incidents, or operations.
 */
public class Dosye {
    public final String dosyeId;
    public final String title;
    private final int createdAt;
    private int lastModified;
    
    private Map<String, String> attachments = new HashMap<>();
    private List<String> relatedDosyes = new ArrayList<>();
    
    public boolean isArchived = false;
    public boolean isOpen = true;
    
    private final NbtCompound nbtData = new NbtCompound(); // Stores serialized state for persistence
    
    /**
     * Create a new case file (досье)
     */
    public Dosye(String id, String title) {
        this.dosyeId = id;
        this.title = title;
        this.createdAt = (int) System.currentTimeMillis() / 1000;
        this.lastModified = this.createdAt;
    }
    
    /**
     * Add an attachment reference to the dosye
     */
    public void addAttachment(String key, String value) {
        if (attachments.size() < 20) {
            attachments.put(key, value);
            updateNbtData(); // Persist changes to nbtData
        }
    }
    
    /**
     * Get attachment value by key
     */
    public String getAttachment(String key) {
        return attachments.get(key);
    }
    
    /**
     * Check if dosye has an attachment
     */
    public boolean hasAttachment(String key) {
        return attachments.containsKey(key);
    }
    
    /**
     * Add a related dosye reference
     */
    public void linkToDosye(String relatedId) {
        if (!relatedDosyes.contains(relatedId)) {
            if (relatedDosyes.size() < 50) {
                relatedDosyes.add(relatedId);
                updateNbtData(); // Persist changes to nbtData
            }
        }
    }
    
    /**
     * Archive this dosye
     */
    public void archive() {
        isArchived = true;
        isOpen = false;
        lastModified = (int) System.currentTimeMillis() / 1000;
        updateNbtData(); // Persist changes to nbtData
    }
    
    /**
     * Get the serialized NBT data for storage/retrieval.
     */
    public NbtCompound getNbtData() {
        return nbtData;
    }
    
    /**
     * Update internal NBT data with current state (keeps nbtData field used and up-to-date)
     */
    private void updateNbtData() {
        this.nbtData.putString("id", dosyeId);
        this.nbtData.putString("title", title);
        this.nbtData.putInt("createdAt", createdAt);
        this.nbtData.putInt("lastModified", lastModified);
        this.nbtData.putBoolean("isArchived", isArchived);
        this.nbtData.putBoolean("isOpen", isOpen);
        
        for (Map.Entry<String, String> entry : attachments.entrySet()) {
            this.nbtData.putString(entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Create a fresh NbtCompound with this dosye's data (for serialization)
     */
    public static NbtCompound toNbt(Dosye dosye) {
        NbtCompound compound = new NbtCompound();
        compound.putString("id", dosye.dosyeId);
        compound.putString("title", dosye.title);
        compound.putInt("createdAt", dosye.createdAt);
        compound.putInt("lastModified", dosye.lastModified);
        compound.putBoolean("isArchived", dosye.isArchived);
        compound.putBoolean("isOpen", dosye.isOpen);
        
        for (Map.Entry<String, String> entry : dosye.attachments.entrySet()) {
            compound.putString(entry.getKey(), entry.getValue());
        }
        return compound;
    }
    
    /**
     * Deserialize NBT data into a new Dosye instance.
     */
    public static Dosye fromNbt(NbtCompound compound) {
        String id = compound.getString("id");
        String title = compound.getString("title");
        
        // Store values in variables, then create with them
        int createdAt = compound.getInt("createdAt");
        int lastModified = compound.getInt("lastModified");
        boolean isArchived = compound.getBoolean("isArchived");
        boolean isOpen = compound.getBoolean("isOpen");
        
        // Create with initial timestamp, will be corrected by constructor logic
        Dosye dosye = new Dosye(id, title);
        
        // Update nbtData with correct values - this makes the final fields accessible via getter pattern
        dosye.nbtData.putInt("createdAt", createdAt);
        dosye.nbtData.putInt("lastModified", lastModified);
        dosye.nbtData.putBoolean("isArchived", isArchived);
        dosye.nbtData.putBoolean("isOpen", isOpen);
        
        // Store attachments in nbtData by iterating through known attachment keys using getAllKeys() alternative
        String[] keyList = { "attachment_0", "attachment_1", "attachment_2", "attachment_3", 
                             "attachment_4", "attachment_5", "attachment_6", "attachment_7",
                             "attachment_8", "attachment_9" };
        for (String key : keyList) {
            try {
                String value = compound.getString(key);
                if (!value.isEmpty()) {
                    dosye.nbtData.putString(key, value);
                }
            } catch (Exception e) {
                // Skip non-existent keys
            }
        }
        
        return dosye;
    }
    
    public String getDosyeId() { return dosyeId; }
    public String getTitle() { return title; }
    public boolean isArchived() { return isArchived; }
    public int getRelatedDosyesCount() { return relatedDosyes.size(); }
}
