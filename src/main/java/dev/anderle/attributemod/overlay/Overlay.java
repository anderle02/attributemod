package dev.anderle.attributemod.overlay;

import dev.anderle.attributemod.utils.Helper;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;

public abstract class Overlay {
    public static final double MIN_OVERLAY_SCALE = 10;

    /** Contains all overlays this mod adds. */
    public static ArrayList<Overlay> ALL = new ArrayList<>();
    /** Initialize all overlays. */
    public static void initAll() { new ChestItemDisplay(); new KuudraProfitDisplay(); }

    protected Dimension size = null;
    protected Point position = null;
    protected double scale = 100;

    protected boolean isFocusedInMoveGui = false; // True if MoveOverlayGui is open and user hovers over this overlay.
    protected boolean isGrabbedInMoveGui = false; // True if MoveOverlayGui is open and user grabs this overlay.

    private final String name;
    private final Color color;
    private final long updateInterval;
    private long lastUpdate = 0;

    /** Create a new overlay element. If updateInterval is set to 0, the overlay's content
     * will be set once and not updated again while the screen containing the overlay is shown. */
    public Overlay(String name, long updateInterval, Color color) {
        this.name = name;
        this.color = color;
        this.updateInterval = updateInterval;
        readScaleAndLocation();
        ALL.add(this);
    }

    /** Draw a rectangle for this overlay to the MoveOverlayGui, including the overlay name. */
    public void drawMoveGuiRepresentation(GuiScreen screen) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();

        GlStateManager.disableTexture2D();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glColor4f(color.getRed(), color.getGreen(), color.getBlue(), isFocusedInMoveGui ? 0.5F : 0.2F);

        renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);

        renderer.pos(position.x, position.y, 0).endVertex();
        renderer.pos(position.x, position.y + size.height, 0).endVertex();
        renderer.pos(position.x + size.width, position.y + size.height, 0).endVertex();
        renderer.pos(position.x + size.width, position.y, 0).endVertex();

        tessellator.draw();
        GlStateManager.enableTexture2D();
        GL11.glDisable(GL11.GL_BLEND);

        screen.drawCenteredString(screen.mc.fontRendererObj, name,
                position.x + size.width / 2,
                position.y + size.height / 2 - screen.mc.fontRendererObj.FONT_HEIGHT / 2,
                color.getRGB());

        if(isFocusedInMoveGui) screen.drawCenteredString(screen.mc.fontRendererObj,
                "(Scroll to adjust size)",
                position.x + size.width / 2,
                position.y + size.height / 2 + 2 + screen.mc.fontRendererObj.FONT_HEIGHT / 2, color.getRGB());
    }
    /** Update this overlay's content, if its last update was long ago. */
    public void updateIfNeeded(GuiScreen screen) {
        if(updateInterval == 0 && lastUpdate != 0) return;
        if(System.currentTimeMillis() - updateInterval > lastUpdate) {
            lastUpdate = System.currentTimeMillis();
            updateData(screen);
        }
    }
    /** Update this overlay as soon as possible. */
    public void scheduleImmediateUpdate() {
        lastUpdate = 0;
    }
    /** Whether the mouse cursor is inside this overlay. */
    public boolean isInside(double mouseX, double mouseY) {
        return new Rectangle(position, size).contains(mouseX, mouseY);
    }
    /** Move the overlay by dx and dy pixels. */
    public void moveBy(int dx, int dy, int screenWidth, int screenHeight) {
        int allowedDX = dx > 0 ? screenWidth - position.x - size.width : - position.x;
        int allowedDY = dy > 0 ? screenHeight - position.y - size.height : - position.y;

        int resultingDX = (dx > 0 && dx > allowedDX) || (dx < 0 && dx < allowedDX) ? allowedDX : dx;
        int resultingDY = (dy > 0 && dy > allowedDY) || (dy < 0 && dy < allowedDY) ? allowedDY : dy;

        position.translate(resultingDX, resultingDY);
    }
    /** Scale the overlay by a multiplier. */
    public void scaleBy(double multiplier, int screenWidth, int screenHeight) {
        Dimension oldSize = size;

        int spaceX = Math.min(position.x, screenWidth - position.x - size.width);
        int spaceY = Math.min(position.y, screenHeight - position.y - size.height);

        double maxMultiplierX = (double) (size.width + spaceX * 2) / size.width;
        double maxMultiplierY = (double) (size.height + spaceY * 2) / size.height;

        scale = Helper.withLimits(scale * multiplier, scale * Math.min(maxMultiplierX, maxMultiplierY), MIN_OVERLAY_SCALE);
        size = getSize();

        position.x -= (size.width - oldSize.width) / 2;
        position.y -= (size.height - oldSize.height) / 2;
    }

    /** Whether this overlay is enabled (set in config). */
    public abstract boolean isEnabled();
    /** Whether this overlay should currently be rendered. */
    public abstract boolean shouldRender(GuiScreen screen);
    /** Get the current overlay size (depends on the overlay's content). */
    public abstract Dimension getSize();
    /** Update this overlay's content and save it. */
    protected abstract void updateData(GuiScreen screen);
    /** Render the overlay to the screen. */
    public abstract void renderOverlay(GuiScreen screen);

    /** Save the current overlay scale and position to config. */
    public abstract void saveScaleAndLocation();
    /** Read the scale and position values from config. Store position, scale and size in this object. */
    public abstract void readScaleAndLocation();
}
