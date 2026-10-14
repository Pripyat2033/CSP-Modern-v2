package com.ben.csp;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
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

    /**
     * Serialize the Dosye (case file) into an NBT compound for storage.
     * The NbtCompound nbt field is used to persist:
     * - Document attachments with page counts
     * - Additional metadata from related items
     */
    public NbtCompound toNbt() {
        // Store document attachments (page counts)
        if (!this.attachments.isEmpty()) {
            var attachmentList = new NbtList();
            for (var entry : this.attachments.entrySet()) {
                var compound = new NbtCompound();
                compound.putString("filename", entry.getKey());
                compound.putInt("pageCount", entry.getValue());
                attachmentList.add(compound);
            }
            // Put the list of attachments in a string tag to preserve order
            var listNbt = new NbtCompound();
            listNbt.put("list", attachmentList);
            this.nbt.put("Attachments", listNbt);
        } else {
            // Mark as empty attachments list
            this.nbt.putBoolean("HasAttachments", false);
        }
        
        // Store metadata
        this.nbt.putString("Subject", this.subject);
        return this.nbt;
    }

    /**
     * Deserialize NBT data into a new Dosye (case file) instance.
     * @throws IllegalArgumentException if required fields are missing
     */
    public static Dosye fromNbt(NbtCompound compound) throws IllegalArgumentException {
        String id = compound.getString("Subject"); // Use Subject as fallback ID
        
        Dosye dosye = new Dosye(id, compound.getString("Subject"));
        
        // Restore attachments - check if it's a Compound tag first
        var attachmentTag = compound.get("Attachments");
        if (attachmentTag instanceof NbtCompound listContainer) {
            var nbtList = listContainer.getList("list", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < nbtList.size(); i++) {
                var element = nbtList.get(i);
                if (element instanceof NbtCompound compoundElement) {
                    String filename = compoundElement.getString("filename");
                    int pageCount = compoundElement.getInt("pageCount");
                    dosye.addAttachment(filename, pageCount);
                }
            }
        } else if (compound.contains("HasAttachments")) {
            // Empty attachments list case
            dosye.attachments.clear();
        }
        
        return dosye;
    }
}
