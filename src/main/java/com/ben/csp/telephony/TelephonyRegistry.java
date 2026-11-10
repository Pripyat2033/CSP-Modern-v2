package com.ben.csp.telephony;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Which vertushka stands on which line. Placing a telephone gives it the lowest
 * free number (from 101 up); breaking it frees the number for the next one. Line 0
 * is the operator. This is the directory the exchange needs: a number means a place.
 */
public final class TelephonyRegistry {

    public static final int FIRST_SUBSCRIBER = 101;
    /** Staff extensions start here, so a telephone line and an extension never clash. */
    public static final int FIRST_EXTENSION = 201;
    public static final int OPERATOR = KommutatorExchange.OPERATOR;

    private final Map<Long, Integer> byPos = new LinkedHashMap<>();
    private final Map<Integer, BlockPos> byNumber = new LinkedHashMap<>();
    private final Map<UUID, Integer> workerNumber = new LinkedHashMap<>();
    private final Map<Integer, UUID> workerByNumber = new LinkedHashMap<>();
    private final Map<UUID, String> workerName = new LinkedHashMap<>();

    public int assign(BlockPos pos) {
        Integer existing = byPos.get(pos.asLong());
        if (existing != null) {
            return existing;
        }
        int number = firstFree();
        byPos.put(pos.asLong(), number);
        byNumber.put(number, pos.toImmutable());
        return number;
    }

    public void release(BlockPos pos) {
        Integer number = byPos.remove(pos.asLong());
        if (number != null) {
            byNumber.remove(number);
        }
    }

    public Integer numberAt(BlockPos pos) {
        return byPos.get(pos.asLong());
    }

    public BlockPos posOf(int number) {
        return byNumber.get(number);
    }

    public List<Integer> numbers() {
        return new ArrayList<>(byNumber.keySet());
    }

    public boolean isEmpty() {
        return byNumber.isEmpty();
    }

    private int firstFree() {
        int n = FIRST_SUBSCRIBER;
        while (byNumber.containsKey(n) || workerByNumber.containsKey(n)) {
            n++;
        }
        return n;
    }

    // ------------------------------------------------------- staff extensions

    /** Give a worker an extension, or return the one they already have. */
    public int assignWorker(UUID id, String name) {
        Integer existing = workerNumber.get(id);
        if (existing != null) {
            workerName.put(id, name);
            return existing;
        }
        int n = firstFreeExtension();
        workerNumber.put(id, n);
        workerByNumber.put(n, id);
        workerName.put(id, name);
        return n;
    }

    public Integer extensionFor(UUID id) {
        return workerNumber.get(id);
    }

    public UUID workerAt(int number) {
        return workerByNumber.get(number);
    }

    public String workerName(UUID id) {
        return workerName.get(id);
    }

    public boolean isExtension(int number) {
        return workerByNumber.containsKey(number);
    }

    public List<Integer> extensions() {
        return new ArrayList<>(workerByNumber.keySet());
    }

    private int firstFreeExtension() {
        int n = FIRST_EXTENSION;
        while (workerByNumber.containsKey(n) || byNumber.containsKey(n)) {
            n++;
        }
        return n;
    }

    public NbtCompound writeNbt() {
        NbtList list = new NbtList();
        for (Map.Entry<Long, Integer> e : byPos.entrySet()) {
            NbtCompound c = new NbtCompound();
            c.putLong("pos", e.getKey());
            c.putInt("number", e.getValue());
            list.add(c);
        }
        NbtList ext = new NbtList();
        for (Map.Entry<UUID, Integer> e : workerNumber.entrySet()) {
            NbtCompound c = new NbtCompound();
            c.putString("id", e.getKey().toString());
            c.putInt("number", e.getValue());
            c.putString("name", workerName.getOrDefault(e.getKey(), "worker"));
            ext.add(c);
        }
        NbtCompound nbt = new NbtCompound();
        nbt.put("lines", list);
        nbt.put("extensions", ext);
        return nbt;
    }

    public void readNbt(NbtCompound nbt) {
        byPos.clear();
        byNumber.clear();
        NbtList list = nbt.getList("lines", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound c = list.getCompound(i);
            long p = c.getLong("pos");
            int number = c.getInt("number");
            byPos.put(p, number);
            byNumber.put(number, BlockPos.fromLong(p));
        }
        workerNumber.clear();
        workerByNumber.clear();
        workerName.clear();
        NbtList ext = nbt.getList("extensions", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < ext.size(); i++) {
            NbtCompound c = ext.getCompound(i);
            try {
                UUID id = UUID.fromString(c.getString("id"));
                int number = c.getInt("number");
                workerNumber.put(id, number);
                workerByNumber.put(number, id);
                workerName.put(id, c.getString("name"));
            } catch (IllegalArgumentException ignored) {
                // a malformed entry is dropped rather than failing the whole load
            }
        }
    }
}
