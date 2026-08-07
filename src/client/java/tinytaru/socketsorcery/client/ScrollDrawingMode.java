package tinytaru.socketsorcery.client;

import java.util.HashSet;
import java.util.Set;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import tinytaru.socketsorcery.item.BlankScrollItem;
import tinytaru.socketsorcery.net.TranscribeCellC2SPayload;

/** Client-only state for the cursor-enabled, held-up Blank Scroll. */
public final class ScrollDrawingMode {
	private static boolean active;
	private static boolean leftHeld;
	private static final Set<Integer> requestedCells = new HashSet<>();

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (!active) return;
			if (client.player == null || client.screen != null
					|| !(client.player.getMainHandItem().getItem() instanceof BlankScrollItem)) {
				exit();
				return;
			}
			if (leftHeld) paintAtCursor();
		});
	}

	public static boolean isActive() { return active; }

	public static boolean openIfHoldingBlankScroll() {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.screen != null
				|| !(client.player.getMainHandItem().getItem() instanceof BlankScrollItem)) return false;
		active = true;
		leftHeld = false;
		requestedCells.clear();
		client.mouseHandler.releaseMouse();
		return true;
	}

	public static void exit() {
		if (!active) return;
		active = false;
		leftHeld = false;
		requestedCells.clear();
		Minecraft client = Minecraft.getInstance();
		if (client.screen == null) client.mouseHandler.grabMouse();
	}

	public static void paintAtCursor() {
		Minecraft client = Minecraft.getInstance();
		if (!active || client.player == null) return;
		int cell = cellAtCursor(client);
		if (cell < 0 || !requestedCells.add(cell)) return;
		ClientPlayNetworking.send(new TranscribeCellC2SPayload(InteractionHand.MAIN_HAND, cell));
	}

	/** Receives raw mouse buttons before vanilla can recapture the released cursor. */
	public static void onMouseButton(int button, int action) {
		if (!active) return;
		if (button == org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			leftHeld = action != org.lwjgl.glfw.GLFW.GLFW_RELEASE;
			if (leftHeld) paintAtCursor();
		} else if (button == org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT
				&& action == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
			exit();
		}
	}

	/** Camera-plane pose shared exactly with {@code ItemInHandRendererMixin}. */
	public static final float SCROLL_SCALE = 1.72F;
	public static final float SCROLL_Y_OFFSET = -0.08F;
	public static final float SCROLL_DISTANCE = 1.15F;
	private static final float FRONT_FACE_Z = 0.5F - 1.0F / 32.0F;
	// The item renderer's first-person projection applies a small additional scale to the flat
	// special-model quad. Calibrated from the debug overlay so the hit grid covers the visible paper.
	private static final double SCREEN_SCALE_CORRECTION = 1.094;

	public record CanvasBounds(int left, int top, int size) {}

	/** Projects the same scroll plane used by the click ray into GUI pixels for the debug grid. */
	public static CanvasBounds canvasBounds(Minecraft client) {
		double width = client.getWindow().getGuiScaledWidth();
		double height = client.getWindow().getGuiScaledHeight();
		double depth = SCROLL_DISTANCE + SCROLL_SCALE * (0.5 - FRONT_FACE_Z);
		double halfViewHeight = depth * Math.tan(Math.toRadians(client.options.fov().get() / 2.0));
		double pixelsPerWorld = height / (2.0 * halfViewHeight);
		int rawSize = Math.max(1, (int) Math.round(SCROLL_SCALE * pixelsPerWorld));
		int size = Math.max(1, (int) Math.round(rawSize * SCREEN_SCALE_CORRECTION));
		int left = (int) Math.round(width / 2.0 - size / 2.0);
		int top = (int) Math.round(height / 2.0
				- (SCROLL_Y_OFFSET + SCROLL_SCALE / 2.0) * pixelsPerWorld
				- (size - rawSize) / 2.0);
		return new CanvasBounds(left, top, size);
	}

	public static int cellAtCursor(Minecraft client) {
		double x = client.mouseHandler.getScaledXPos(client.getWindow());
		double y = client.mouseHandler.getScaledYPos(client.getWindow());
		CanvasBounds bounds = canvasBounds(client);
		if (x < bounds.left || y < bounds.top || x >= bounds.left + bounds.size || y >= bounds.top + bounds.size) return -1;
		int col = Math.min(15, (int) ((x - bounds.left) * 16.0 / bounds.size));
		int row = Math.min(15, (int) ((y - bounds.top) * 16.0 / bounds.size));
		return row * 16 + col;
	}

	private ScrollDrawingMode() {}
}
