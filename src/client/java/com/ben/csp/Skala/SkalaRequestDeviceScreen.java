package com.ben.csp.skala;

import com.ben.csp.CSPMod;
import com.ben.csp.networking.ModMessages;
import com.ben.csp.networking.packet.SendSkalaRequestC2SPacket;
import com.ben.csp.networking.packet.UpdateSkalaReferenceC2SPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * "Science Grade": A custom screen that renders a faithful representation of the
 * DIVT (Chief Computer Engineer) console for interacting with the SKALA system.
 * This UI is based on detailed descriptions of the real-world hardware.
 */
public class SkalaRequestDeviceScreen extends Screen {
    private static final Identifier TEXTURE = new Identifier(CSPMod.MOD_ID, "textures/gui/skala_request_device_background.png");

    private final SkalaRequestDeviceBlockEntity blockEntity;
    private StringBuilder currentCode = new StringBuilder();
    private StringBuilder currentReferenceValue = new StringBuilder();

    // Key layout for the main 8x10 alphanumeric keypad on the left
    private static final String[] KEY_LAYOUT = {
            "M", "7", "6", "5", "4", "3", "2", "1", "0",
            "T", "7", "6", "5", "4", "3", "2", "1", "0",
            "D", "7", "6", "5", "4", "3", "2", "1", "0",
            "N", "7", "6", "5", "4", "3", "2", "1", "0",
            "P", "7", "6", "5", "4", "3", "2", "1", "0",
            "A", "7", "6", "5", "4", "3", "2", "1", "0",
            "Я", "7", "6", "5", "4", "3", "2", "1", "0", // Cyrillic YA
            "K", "7", "6", "5", "4", "3", "2", "1", "0"
    };

    // Key layout for the right-hand numerical keypad
    private static final String[] NUM_KEY_LAYOUT = {
            "9", "9", "9", "9",
            "8", "8", "8", "8",
            "7", "7", "7", "7",
            "6", "6", "6", "6",
            "5", "5", "5", "5",
            "4", "4", "4", "4",
            "3", "3", "3", "3",
            "2", "2", "2", "2",
            "1", "1", "1", "1",
            "0", "0", "0", "0"
    };

    public SkalaRequestDeviceScreen(SkalaRequestDeviceBlockEntity be) {
        super(Text.literal("SKALA Request Device"));
        this.blockEntity = be;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - 256) / 2;
        int y = (this.height - 256) / 2;
        context.drawTexture(TEXTURE, x, y, 0, 0, 256, 256);

        super.render(context, mouseX, mouseY, delta);

        // --- Render Displays ---
        context.drawText(this.textRenderer, blockEntity.getRequestCode(), x + 30, y + 25, 0x00FF00, false);
        context.drawText(this.textRenderer, blockEntity.getValueDisplay(), x + 30, y + 45, 0x00FF00, false);
        context.drawText(this.textRenderer, blockEntity.getReferenceDisplay(), x + 30, y + 65, 0x00FF00, false);

        // --- Render the input buffers ---
        context.drawText(this.textRenderer, "REQ: " + currentCode.toString(), x + 20, y + 90, 0xFFFFFF, false);
        context.drawText(this.textRenderer, "REF: " + currentReferenceValue.toString(), x + 150, y + 90, 0xFFFFFF, false);

        // --- Render and handle keypads and buttons ---
        renderKeypad(context, "left", mouseX, mouseY, x, y);
        renderKeypad(context, "right", mouseX, mouseY, x, y);
        renderButtons(context, mouseX, mouseY, x, y);
    }

    private void renderKeypad(DrawContext context, String side, int mouseX, int mouseY, int x, int y) {
        int keyWidth, keyHeight, startX, startY, rows, cols;
        String[] layout;

        if ("left".equals(side)) {
            keyWidth = 12; keyHeight = 12; startX = x + 20; startY = y + 110; rows = 8; cols = 9; layout = KEY_LAYOUT;
        } else { // right
            keyWidth = 18; keyHeight = 12; startX = x + 150; startY = y + 110; rows = 10; cols = 4; layout = NUM_KEY_LAYOUT;
        }

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int keyX = startX + col * (keyWidth + 2);
                int keyY = startY + row * (keyHeight + 2);
                String keyChar = layout[row * cols + col];
                boolean hovered = isClickInBounds(mouseX, mouseY, keyX, keyY, keyWidth, keyHeight);
                int color = hovered ? 0xFF808080 : 0xFF404040;
                if ("right".equals(side) && row >= 5) color = hovered ? 0xFFC06060 : 0xFF8B0000;
                context.fill(keyX, keyY, keyX + keyWidth, keyY + keyHeight, color);
                context.drawCenteredTextWithShadow(this.textRenderer, keyChar, keyX + keyWidth / 2, keyY + 2, 0xFFFFFF);
            }
        }
    }

    private void renderButtons(DrawContext context, int mouseX, int mouseY, int x, int y) {
        // --- Left Console Buttons ---
        int sendButtonX = x + 20, buttonY = y + 220, buttonWidth = 50, buttonHeight = 15;
        boolean hovered = isClickInBounds(mouseX, mouseY, sendButtonX, buttonY, buttonWidth, buttonHeight);
        context.fill(sendButtonX, buttonY, sendButtonX + buttonWidth, buttonY + buttonHeight, hovered ? 0xFF00AA00 : 0xFF006400);
        context.drawCenteredTextWithShadow(this.textRenderer, "SEND", sendButtonX + buttonWidth / 2, buttonY + 4, 0xFFFFFF);

        int clearButtonX = sendButtonX + buttonWidth + 5;
        hovered = isClickInBounds(mouseX, mouseY, clearButtonX, buttonY, buttonWidth, buttonHeight);
        context.fill(clearButtonX, buttonY, clearButtonX + buttonWidth, buttonY + buttonHeight, hovered ? 0xFFAA0000 : 0xFF640000);
        context.drawCenteredTextWithShadow(this.textRenderer, "CLEAR", clearButtonX + buttonWidth / 2, buttonY + 4, 0xFFFFFF);

        // --- Right Console Buttons ---
        int plusButtonX = x + 125, plusButtonY = y + 110, signButtonWidth = 20, signButtonHeight = 26;
        hovered = isClickInBounds(mouseX, mouseY, plusButtonX, plusButtonY, signButtonWidth, signButtonHeight);
        context.fill(plusButtonX, plusButtonY, plusButtonX + signButtonWidth, plusButtonY + signButtonHeight, hovered ? 0xFF808080 : 0xFF404040);
        context.drawCenteredTextWithShadow(this.textRenderer, "+", plusButtonX + signButtonWidth / 2, plusButtonY + 9, 0xFFFFFF);

        int minusButtonY = plusButtonY + signButtonHeight + 4;
        hovered = isClickInBounds(mouseX, mouseY, plusButtonX, minusButtonY, signButtonWidth, signButtonHeight);
        context.fill(plusButtonX, minusButtonY, plusButtonX + signButtonWidth, minusButtonY + signButtonHeight, hovered ? 0xFF808080 : 0xFF404040);
        context.drawCenteredTextWithShadow(this.textRenderer, "-", plusButtonX + signButtonWidth / 2, minusButtonY + 9, 0xFFFFFF);

        int startButtonX = x + 150;
        hovered = isClickInBounds(mouseX, mouseY, startButtonX, buttonY, buttonWidth, buttonHeight);
        context.fill(startButtonX, buttonY, startButtonX + buttonWidth, buttonY + buttonHeight, hovered ? 0xFF00AA00 : 0xFF006400);
        context.drawCenteredTextWithShadow(this.textRenderer, "START", startButtonX + buttonWidth / 2, buttonY + 4, 0xFFFFFF);

        int stopButtonX = startButtonX + buttonWidth + 5;
        hovered = isClickInBounds(mouseX, mouseY, stopButtonX, buttonY, buttonWidth, buttonHeight);
        context.fill(stopButtonX, buttonY, stopButtonX + buttonWidth, buttonY + buttonHeight, hovered ? 0xFFAA0000 : 0xFF640000);
        context.drawCenteredTextWithShadow(this.textRenderer, "STOP", stopButtonX + buttonWidth / 2, buttonY + 4, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (this.width - 256) / 2;
        int y = (this.height - 256) / 2;

        // Left Keypad
        int leftKeyWidth = 12, leftKeyHeight = 12, leftStartX = x + 20, leftStartY = y + 110;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 9; col++) {
                int keyX = leftStartX + col * (leftKeyWidth + 2);
                int keyY = leftStartY + row * (leftKeyHeight + 2);
                if (isClickInBounds(mouseX, mouseY, keyX, keyY, leftKeyWidth, leftKeyHeight)) {
                    currentCode.append(KEY_LAYOUT[row * 9 + col]);
                    return true;
                }
            }
        }

        // Right Keypad
        int rightKeyWidth = 18, rightKeyHeight = 12, rightStartX = x + 150, rightStartY = y + 110;
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 4; col++) {
                int keyX = rightStartX + col * (rightKeyWidth + 2);
                int keyY = rightStartY + row * (rightKeyHeight + 2);
                if (isClickInBounds(mouseX, mouseY, keyX, keyY, rightKeyWidth, rightKeyHeight)) {
                    currentReferenceValue.append(NUM_KEY_LAYOUT[row * 4 + col]);
                    return true;
                }
            }
        }

        // --- Button Click Detection ---
        int buttonY = y + 220, buttonWidth = 50, buttonHeight = 15;

        // Left Buttons
        int sendButtonX = x + 20;
        if (isClickInBounds(mouseX, mouseY, sendButtonX, buttonY, buttonWidth, buttonHeight)) {
            if (currentCode.length() > 0) {
                ModMessages.sendToServer(new SendSkalaRequestC2SPacket(blockEntity.getPos(), currentCode.toString()));
                currentCode.setLength(0);
            }
            return true;
        }

        int clearButtonX = sendButtonX + buttonWidth + 5;
        if (isClickInBounds(mouseX, mouseY, clearButtonX, buttonY, buttonWidth, buttonHeight)) {
            currentCode.setLength(0);
            return true;
        }

        // Right Buttons
        int startButtonX = x + 150;
        if (isClickInBounds(mouseX, mouseY, startButtonX, buttonY, buttonWidth, buttonHeight)) {
            String targetCode = blockEntity.getRequestCode();
            if (!targetCode.isEmpty() && currentReferenceValue.length() > 0) {
                ModMessages.sendToServer(new UpdateSkalaReferenceC2SPacket(blockEntity.getPos(), targetCode, currentReferenceValue.toString()));
                currentReferenceValue.setLength(0);
            }
            return true;
        }

        int stopButtonX = startButtonX + buttonWidth + 5;
        if (isClickInBounds(mouseX, mouseY, stopButtonX, buttonY, buttonWidth, buttonHeight)) {
            currentReferenceValue.setLength(0);
            return true;
        }

        // Sign Buttons
        int plusButtonX = x + 125, plusButtonY = y + 110, signButtonWidth = 20, signButtonHeight = 26;
        if (isClickInBounds(mouseX, mouseY, plusButtonX, plusButtonY, signButtonWidth, signButtonHeight)) {
            if (currentReferenceValue.isEmpty()) currentReferenceValue.append('+');
            return true;
        }

        int minusButtonY = plusButtonY + signButtonHeight + 4;
        if (isClickInBounds(mouseX, mouseY, plusButtonX, minusButtonY, signButtonWidth, signButtonHeight)) {
            if (currentReferenceValue.isEmpty()) currentReferenceValue.append('-');
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isClickInBounds(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}