package com.ben.csp.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Headless screenshot autopilot. Loads a world in single-player, then runs a
 * plain-text script that drives the real client: it issues server commands,
 * moves the camera and captures true framebuffer screenshots. Used to show the
 * build on screen rather than on paper. Enabled only when {@code -Dcsp.autopilot}
 * is set, so it never affects normal play.
 */
public final class Autopilot {

    private static List<String> script;
    private static int pc;
    private static int waitTicks;
    private static boolean loadRequested;
    private static String worldName = "cspdemo";

    public static void install(String scriptPath, String world) {
        worldName = world;
        try {
            script = Files.readAllLines(Path.of(scriptPath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[autopilot] cannot read script " + scriptPath + ": " + e);
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(Autopilot::tick);
        System.out.println("[autopilot] installed: " + script.size() + " lines, world=" + world);
    }

    private static void tick(MinecraftClient client) {
        if (!loadRequested) {
            if (client.currentScreen != null && client.world == null) {
                System.out.println("[autopilot] loading world " + worldName);
                client.createIntegratedServerLoader().start(client.currentScreen, worldName);
                loadRequested = true;
            }
            return;
        }

        MinecraftServer server = client.getServer();
        if (client.player == null || server == null || server.getOverworld() == null) {
            return;
        }
        ServerPlayerEntity sp = server.getPlayerManager().getPlayer(client.player.getUuid());
        if (sp == null) {
            return;
        }

        if (waitTicks > 0) {
            waitTicks--;
            return;
        }
        if (pc >= script.size()) {
            return;
        }

        String line = script.get(pc++).trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return;
        }
        String[] parts = line.split("\\s+", 2);
        String op = parts[0];
        String arg = parts.length > 1 ? parts[1] : "";

        switch (op) {
            case "wait" -> waitTicks = Integer.parseInt(arg);
            case "cmd" -> server.execute(() -> server.getCommandManager()
                    .executeWithPrefix(sp.getCommandSource(), arg));
            case "hud" -> client.options.hudHidden = Boolean.parseBoolean(arg);
            case "fov" -> client.options.getFov().setValue(Integer.parseInt(arg));
            case "shot" -> {
                ScreenshotRecorder.saveScreenshot(client.runDirectory,
                        "shot-" + arg + ".png", client.getFramebuffer(), s -> {});
                System.out.println("[autopilot] shot " + arg);
            }
            case "quit" -> {
                System.out.println("[autopilot] quit");
                client.scheduleStop();
            }
            default -> System.out.println("[autopilot] unknown op: " + op);
        }
    }
}
