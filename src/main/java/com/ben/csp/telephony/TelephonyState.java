package com.ben.csp.telephony;

import com.ben.csp.entity.CspPersonEntity;
import com.ben.csp.telephony.KommutatorExchange.Event;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The коммутатор as the world sees it: it owns the exchange and the directory of
 * lines, runs the exchange on the server clock, and voices what happens at each
 * instrument. A real call is set up the way it always was — lift, dial, ring, answer,
 * talk, hang up — and the exchange enforces the same limits.
 */
public final class TelephonyState extends PersistentState {

    private static final String NAME = "csp_telephony";

    private final KommutatorExchange exchange = new KommutatorExchange();
    private final TelephonyRegistry registry = new TelephonyRegistry();
    /** Last state seen for each staff extension, so a call ending can be noticed. */
    private final Map<Integer, KommutatorExchange.State> previous = new HashMap<>();

    public KommutatorExchange exchange() {
        return exchange;
    }

    public TelephonyRegistry registry() {
        return registry;
    }

    public void ensureOperator() {
        if (!exchange.exists(KommutatorExchange.OPERATOR)) {
            exchange.register(KommutatorExchange.OPERATOR, "operator");
        }
    }

    /** A vertushka was placed: give it a line. */
    public int placeLine(BlockPos pos) {
        ensureOperator();
        int number = registry.assign(pos);
        exchange.register(number, "vertushka");
        markDirty();
        return number;
    }

    /** A vertushka was broken: free its line. */
    public void removeLine(BlockPos pos) {
        Integer number = registry.numberAt(pos);
        if (number != null) {
            exchange.unregister(number);
            registry.release(pos);
            markDirty();
        }
    }

    /** Give a worker an extension on first contact, and put it on the exchange. */
    public int ensureWorker(UUID id, String name) {
        Integer existing = registry.extensionFor(id);
        if (existing != null) {
            return existing;
        }
        int number = registry.assignWorker(id, name);
        exchange.register(number, name);
        markDirty();
        return number;
    }

    public String workerNameAt(int number) {
        UUID id = registry.workerAt(number);
        return id == null ? "worker" : registry.workerName(id);
    }

    /** The worker standing on an extension, or null if they are not in the world. */
    public CspPersonEntity workerAt(ServerWorld world, int number) {
        UUID id = registry.workerAt(number);
        if (id == null) {
            return null;
        }
        Entity e = world.getEntity(id);
        return e instanceof CspPersonEntity p ? p : null;
    }

    /** True if this worker is on the telephone right now. */
    public boolean isOnCall(CspPersonEntity worker) {
        Integer ext = registry.extensionFor(worker.getUuid());
        if (ext == null) {
            return false;
        }
        KommutatorExchange.Line l = exchange.line(ext);
        return l != null && (l.state == KommutatorExchange.State.TALKING
            || l.state == KommutatorExchange.State.RINGING);
    }

    /** The staff extension this player is speaking on, or -1. */
    public int talkingExtensionFor(net.minecraft.entity.player.PlayerEntity player) {
        int number = talkingLineFor(player);
        return number >= 0 && registry.isExtension(number) ? number : -1;
    }

    public int lineAt(BlockPos pos) {
        Integer n = registry.numberAt(pos);
        return n == null ? -1 : n;
    }

    /** Server ticker entry point: run the exchange for this world. */
    public static void tick(MinecraftServer server) {
        get(server).tickState(server);
    }

    /** Run the exchange and say aloud whatever it did. */
    public void tickState(MinecraftServer server) {
        ensureOperator();
        List<Event> events = exchange.tick();
        if (events.isEmpty()) {
            return;
        }
        ServerWorld world = server.getOverworld();
        for (Event e : events) {
            announce(world, e);
        }
        tickExtensions(world);
        markDirty();
    }

    /** A worker's extension rings and the worker answers; when the call ends it is noticed. */
    private void tickExtensions(ServerWorld world) {
        for (int number : registry.extensions()) {
            KommutatorExchange.Line l = exchange.line(number);
            if (l == null) {
                continue;
            }
            if (l.state == KommutatorExchange.State.RINGING && !l.outgoing) {
                int caller = l.peer;
                exchange.answer(number);
                ServerPlayerEntity p = playerAtLine(world, caller);
                if (p != null) {
                    p.sendMessage(Text.literal("Ext " + number + " (" + workerNameAt(number)
                        + ") answers. Speak plainly; right-click to hang up.")
                        .formatted(Formatting.GREEN), false);
                }
            }
            previous.put(number, l.state);
        }
    }

    private void announce(ServerWorld world, Event e) {
        switch (e.kind()) {
            case RING -> {
                BlockPos pos = registry.posOf(e.line());
                if (pos != null) {
                    world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(),
                        SoundCategory.BLOCKS, 0.7f, 1.2f);
                }
                ServerPlayerEntity called = playerAt(world, registry.posOf(e.line()));
                if (called != null) {
                    called.sendMessage(Text.literal("The vertushka rings.")
                        .formatted(Formatting.GOLD), true);
                }
            }
            case ANSWERED -> {
                to(world, e.line(), "Connected.", Formatting.GREEN);
                to(world, e.other(), "Connected.", Formatting.GREEN);
            }
            case BUSY -> {
                BlockPos pos = registry.posOf(e.line());
                if (pos != null) {
                    world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(),
                        SoundCategory.BLOCKS, 0.7f, 0.6f);
                }
                to(world, e.line(), "Engaged. The line is in use.", Formatting.RED);
            }
            case OPERATOR_CALL -> {
                to(world, KommutatorExchange.OPERATOR,
                    "Line " + e.line() + " asks for " + e.other() + ".", Formatting.YELLOW);
                to(world, e.line(), "The operator is connecting you.", Formatting.YELLOW);
            }
            case RELEASED -> {
                to(world, e.line(), "The line is clear.", Formatting.GRAY);
                to(world, e.other(), "The line is clear.", Formatting.GRAY);
            }
            default -> { }
        }
    }

    private void to(ServerWorld world, int number, String message, Formatting colour) {
        ServerPlayerEntity p = playerAt(world, registry.posOf(number));
        if (p != null) {
            p.sendMessage(Text.literal(message).formatted(colour), false);
        }
    }

    private static ServerPlayerEntity playerAt(ServerWorld world, BlockPos pos) {
        if (pos == null) {
            return null;
        }
        for (ServerPlayerEntity p : world.getPlayers()) {
            if (p.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 16.0) {
                return p;
            }
        }
        return null;
    }

    /** The line number of the vertushka this player is holding a call on, or -1. */
    public int talkingLineFor(net.minecraft.entity.player.PlayerEntity player) {
        for (int number : registry.numbers()) {
            if (registry.isExtension(number)) {
                continue;
            }
            BlockPos pos = registry.posOf(number);
            if (pos == null) {
                continue;
            }
            KommutatorExchange.Line l = exchange.line(number);
            if (l != null && l.state == KommutatorExchange.State.TALKING
                && player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 16.0) {
                return number;
            }
        }
        return -1;
    }

    /** The player standing at the instrument on a given line, or null. */
    public ServerPlayerEntity playerAtLine(ServerWorld world, int number) {
        return playerAt(world, registry.posOf(number));
    }

    // ------------------------------------------------------------- persistence

    public static TelephonyState get(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        PersistentStateManager manager = world.getPersistentStateManager();
        return manager.getOrCreate(TelephonyState::fromNbt, TelephonyState::new, NAME);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.put("registry", registry.writeNbt());
        return nbt;
    }

    private static TelephonyState fromNbt(NbtCompound nbt) {
        TelephonyState state = new TelephonyState();
        state.registry.readNbt(nbt.getCompound("registry"));
        for (int number : state.registry.numbers()) {
            state.exchange.register(number, "vertushka");
        }
        return state;
    }
}
