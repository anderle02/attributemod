package dev.anderle.attributemod.overlay;

import dev.anderle.attributemod.Events;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.io.IOException;
import java.util.ListIterator;

public class MoveOverlayGui extends GuiScreen {
    private Point lastMousePos = null;

    /** Load all positions and scales from config when this gui is opened. */
    public @Override void initGui() {
        super.initGui();
        Overlay.ALL.forEach(Overlay::readScaleAndLocation);
        addResetButton();
    }

    /** Reset all temp variables, save config and return to Vigilance gui. */
    public @Override void onGuiClosed() {
        super.onGuiClosed();
        lastMousePos = null;

        Overlay.ALL.forEach(element -> {
            element.isGrabbedInMoveGui = false;
            element.isFocusedInMoveGui = false;
            element.saveScaleAndLocation();
        });

        Events.showVigilanceGuiWithNextTick = true;
    }

    /** Render the current state. */
    public @Override void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        drawChestGui();

        ListIterator<Overlay> it = Overlay.ALL.listIterator(Overlay.ALL.size());
        while(it.hasPrevious()) { // Iterate in reverse order so the element in the front is always the one focused when hovered over.
            Overlay element = it.previous();
            if(element.isEnabled()) element.drawMoveGuiRepresentation(this);
        }
    }

    /** Continuously update the grabbed overlay's position when mouse is clicked and moved. */
    protected @Override void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);

        Overlay grabbed = Overlay.ALL.stream()
                .filter(element -> element.isGrabbedInMoveGui)
                .findFirst().orElse(null);

        if(lastMousePos != null && grabbed != null) {
            grabbed.moveBy(mouseX - lastMousePos.x, mouseY - lastMousePos.y, width, height);
        }

        lastMousePos = new Point(mouseX, mouseY);
    }

    /** Grab the focused overlay when any mouse button is pressed. */
    protected @Override void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        Overlay.ALL.forEach(element -> element.isGrabbedInMoveGui = element.isFocusedInMoveGui);
    }

    /** Resets the mouse position when done moving. */
    protected @Override void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);

        Overlay.ALL.forEach(element -> element.isGrabbedInMoveGui = false);
        lastMousePos = null;
    }

    /** Handles all mouse moves without a button pressed, and scrolling. */
    public @Override void handleMouseInput() throws IOException {
        super.handleMouseInput();

        if(Mouse.getEventDWheel() != 0) { // Scrolled...
            double scaleMultiplier = Math.pow(Mouse.getEventDWheel() > 0 ? 1.1 : 0.9, (double) Math.abs(Mouse.getEventDWheel()) / 120);

            for(Overlay element : Overlay.ALL) {
                if(element.isFocusedInMoveGui) element.scaleBy(scaleMultiplier, width, height);
            }
        } else if(!Mouse.isButtonDown(0)) { // Mouse moved...
            int mousePosX = Mouse.getEventX() * width / mc.displayWidth;
            int mousePosY = height - Mouse.getEventY() * height / mc.displayHeight - 1;

            boolean oneIsFocused = false; // Focus only one overlay and un-focus all others.

            for(Overlay element : Overlay.ALL) {
                boolean inside = new Rectangle(element.position, element.size).contains(mousePosX, mousePosY);

                element.isFocusedInMoveGui = !oneIsFocused && inside && element.isEnabled();
                if(element.isFocusedInMoveGui) oneIsFocused = true;
            }
        }
    }

    /** When clicking the reset locations button, reset all overlay locations. */
    protected @Override void actionPerformed(GuiButton button) throws IOException {
        super.actionPerformed(button);
        if(button.id != 0) return;

        Overlay.ALL.forEach(element -> {
            element.position = new Point(0, 0);
            element.scale = 100;
            element.size = element.getSize();
        });
    }

    /* ---------------------------------------------- Helper Functions ---------------------------------------------- */

    private void addResetButton() {
        String buttonText = "Reset Locations";
        int buttonWidth = mc.fontRendererObj.getStringWidth(buttonText) + 20;
        buttonList.add(new GuiButton(0, width - buttonWidth - 10, height - 30, buttonWidth, 20, buttonText));
    }

    private void drawChestGui() {
        Point chestPos = new Point((width - ChestOverlay.CHEST_GUI_WIDTH) / 2, (height - ChestOverlay.CHEST_GUI_HEIGHT) / 2);

        mc.getTextureManager().bindTexture(new ResourceLocation("textures/gui/container/generic_54.png"));
        drawTexturedModalRect(chestPos.x, chestPos.y, 0, 0, ChestOverlay.CHEST_GUI_WIDTH, ChestOverlay.CHEST_GUI_HEIGHT);

        mc.fontRendererObj.drawString("This represents your Chest!",
                chestPos.x + 8, chestPos.y + 6, 4210752); // Minecraft hardcoded values for the chest title.
    }
}
