package com.ben.csp.agent;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

public class Dosye {
    public final String dosyeId;
    public final String title;
    private final int createdAt;
    private int lastModified;
    
    private java.util.Map<String, String> attachments = new java.util.HashMap<>();
    private java.util.ArrayList<String> relatedDosyes = new java.util.ArrayList<>();
    
    public boolean isArchived = false;
    public boolean isOpen = true;
    
    private final NbtCompound nbtData = new NbtCompound();
    
    public Dosye(String id, String title) {
        this.dosyeId = id;
        this.title = title;
        this.createdAt = (int) System.currentTimeMillis() / 1000;
        this.lastModified = this.createdAt;
    }
    
    public void addAttachment(String key, String value) {
        if (attachments.size() < 20) {
            attachments.put(key, value);
            updateNbt();
        }
    }
    
    public String getAttachment(String key) {
        return attachments.get(key);
    }
    
    public boolean hasAttachment(String key) {
        return attachments.containsKey(key);
    }
    
    public void linkToDosye(String relatedId) {
        if (!relatedDosyes.contains(relatedId)) {
            if (relatedDosyes.size() < 50) {
                relatedDosyes.add(relatedId);
                updateNbt();
            }
        }
    }
    
    public void archive() {
        isArchived = true;
        isOpen = false;
        lastModified = (int) System.currentTimeMillis() / 1000;
        updateNbt();
    }
    
    private void updateNbt() {
        nbtData.putString("id", dosyeId);
        nbtData.putString("title", title);
        nbtData.putInt("createdAt", createdAt);
        nbtData.putInt("lastModified", lastModified);
        nbtData.putBoolean("isArchived", isArchived);
        nbtData.putBoolean("isOpen", isOpen);
        for (var entry : attachments.entrySet()) {
            nbtData.putString(entry.getKey(), entry.getValue());
        }
    }
    
    public static Text getDosyeName(String id, String title) {
        return Text.literal("Досье: ")
            .append(Text.literal(id).styled(style -> style.withColor(0xFFFFFF)))
            .append(Text.literal(": ") + title);
    }
    
    public NbtCompound getNbtData() {
        updateNbt();
        return nbtData;
    }
    
    public String getDosyeId() {
        return dosyeId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public boolean isArchived() {
        return isArchived;
    }
    
    public int getRelatedDosyesCount() {
        return relatedDosyes.size();
    }
}
