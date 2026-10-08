package com.ben.csp.agent;

public class IndustrialProcessManager {
    
    public static class ProcessRecord {
        public final String processId;
        public final String processType;
        private int stage;
        public double completionPercentage;
        public boolean qualityVerified;
        
        public ProcessRecord(String id, String type) {
            this.processId = id;
            this.processType = type;
            this.completionPercentage = 0.0;
            this.qualityVerified = false;
            this.stage = 1;
        }
    }
    
    private static java.util.Map<String, ProcessRecord> activeProcesses = new java.util.HashMap<>();
    
    public static ProcessRecord registerProcess(String id, String type) {
        if (!activeProcesses.containsKey(id)) {
            ProcessRecord record = new ProcessRecord(id, type);
            activeProcesses.put(id, record);
        }
        return activeProcesses.get(id);
    }
    
    public static void updateProgress(String processId, double progress) {
        ProcessRecord record = activeProcesses.get(processId);
        if (record != null) {
            record.completionPercentage = Math.min(Math.max(progress, 0.0), 100.0);
        }
    }
    
    public static void markStageComplete(String processId, int stage) {
        ProcessRecord record = activeProcesses.get(processId);
        if (record != null && stage >= 1 && stage <= 10) {
            record.stage = stage;
            if (stage % 3 == 0) {
                record.qualityVerified = true;
            }
        }
    }
    
    public static java.util.Map<String, ProcessRecord> getActiveProcesses() {
        return new java.util.HashMap<>(activeProcesses);
    }
    
    public static double calculateTotalCompletion() {
        double total = 0;
        for (ProcessRecord record : activeProcesses.values()) {
            total += record.completionPercentage;
        }
        return activeProcesses.isEmpty() ? 0.0 : total / activeProcesses.size();
    }
}
