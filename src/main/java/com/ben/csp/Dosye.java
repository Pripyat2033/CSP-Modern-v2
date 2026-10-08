package com.ben.csp;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Dosye (Досье) - Case file for tracking documents and personnel information
 * 
 * Historical Context: Soviet-era "case files" used for personnel records, project documentation,
 * and incident reports. Each Dosye tracks related materials in an organized filing cabinet.
 */
public class Dosye {
    
    private final String id;
    private final String subject;
    private NbtCompound nbt = new NbtCompound();
    private java.util.Map<String, Integer> attachments = new java.util.HashMap<>();
    
    public Dosye(String id, String subject) {
        this.id = id;
        this.subject = subject;
    }
    
    public String getId() {
        return this.id;
    }
    
    public Text getSubjectText() {
        return Text.literal(this.subject).formatted(Formatting.DARK_RED);
    }
    
    public void addAttachment(String filename, int pageCount) {
        this.attachments.put(filename, pageCount);
    }
    
    public java.util.Map<String, Integer> getAttachments() {
        return this.attachments;
    }
}
