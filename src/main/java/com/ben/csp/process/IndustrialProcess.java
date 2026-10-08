package com.ben.csp.process;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Represents a data-driven state machine for an industrial process,
 * loaded from a .proc.json file.
 */
public class IndustrialProcess {

    public record State(String type, String description, @Nullable Action action, @Nullable Map<String, String> transitions, @Nullable Integer duration_ticks) {}
    public record Action(String id, String hmi_type) {}

    private final String processId;
    private final Map<String, State> states;
    private String currentStateId;

    public IndustrialProcess(String processId, String initialState, Map<String, State> states) {
        this.processId = processId;
        this.states = states;
        this.currentStateId = initialState;
    }

    public String getProcessId() { return processId; }
    public String getCurrentStateId() { return currentStateId; }
    @Nullable public State getCurrentState() { return states.get(currentStateId); }

    /**
     * Advances the process to the next state based on a condition.
     * @param condition The condition that was met (e.g., "ACTION_COMPLETE").
     */
    public void advanceState(String condition) {
        State currentState = getCurrentState();
        if (currentState != null && currentState.transitions() != null) {
            String nextStateId = currentState.transitions().get(condition);
            if (nextStateId != null && states.containsKey(nextStateId)) {
                this.currentStateId = nextStateId;
            }
        }
    }

    public static IndustrialProcess fromJson(String jsonContent) {
        Gson gson = new Gson();
        JsonObject root = JsonParser.parseString(jsonContent).getAsJsonObject();
        String processId = root.get("process_id").getAsString();
        String initialState = root.get("initial_state").getAsString();
        
        // Using Gson to deserialize the map of states
        Map<String, State> states = gson.fromJson(root.get("states"), new com.google.gson.reflect.TypeToken<Map<String, State>>() {}.getType());

        return new IndustrialProcess(processId, initialState, states);
    }

    public void writeToNbt(NbtCompound nbt) {
        NbtCompound processNbt = new NbtCompound();
        processNbt.putString("ProcessId", this.processId);
        processNbt.putString("CurrentStateId", this.currentStateId);
        nbt.put("IndustrialProcess", processNbt);
    }

    public static IndustrialProcess fromNbt(NbtCompound nbt) {
        if (!nbt.contains("IndustrialProcess")) {
            return null;
        }
        NbtCompound processNbt = nbt.getCompound("IndustrialProcess");
        String processId = processNbt.getString("ProcessId");
        String currentStateId = processNbt.getString("CurrentStateId");

        // This is a simplification. A full implementation would need to reload the process
        // definition from a manager, not just reconstruct the state.
        // For now, we'll return null as this requires a ProcessLoader.
        return null; 
    }
}