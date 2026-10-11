import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import ui.MakeX;
import ui.RSInterface;
import ui.Sprite;

	/**
	 * Draws each quantity-first chatbox to a PNG so the layout can be looked at without launching the
	 * client. A diagram, not a screenshot.
	 *
	 * <p><b>Why a diagram and not a real render.</b> The client's own draw path for an interface is
	 * {@code client.drawInterface}, an instance method that reaches for the media archive, the item
	 * model cache, the chat area's own sprites and a live framebuffer. None of that exists headless, so
	 * driving it would mean standing up most of the client. What this draws instead is the widget list
	 * the probe just checked: the real positions and sizes, the real label text, the real quantity pills
	 * taken out of the loaded sprite, a box where each model frame will show an item, and the chat area
	 * art itself underneath. Positions, sizes and text are the client's; the item artwork is a
	 * placeholder because only the server knows what it is.
	 *
	 * <p>Positions are drawn where the client draws them, offset by ({@code DRAW_X},
	 * {@code DRAW_OFFSET}) like {@code ChatArea}'s call to {@code drawInterface} - which is the whole
	 * point: the earlier version drew each child at its raw child coordinate, 20px up and left of the
	 * panel it lives on, so it could show a pill at x 496 while the game drew it at 516, past the
	 * chatbox edge, and called it a fit. The click region and the scrollbar's strip are drawn over the
	 * top so anything that leaves either is visible rather than inferred.
	 *
	 * <p>So it answers "is this laid out like the screenshots" and "does anything overlap or run off the
	 * 519x142 dialog", and deliberately does not answer "is the item icon drawn correctly".
	 */
final class MakeXPreview {

	/** Nearest-neighbour blow-up, because a 519px diagram is unreadable at 1:1. */
	private static final int SCALE = 2;

	/** A strip above the dialog for the caption that says which family this is. */
	private static final int HEADER = 14;

	/** Drawn over the real chat area art when it can be read; the chatbox tan otherwise. */
	private static final Color PAPER = new Color(0xC8, 0xB5, 0x91);

	private static final Color BOX = new Color(0x70, 0x60, 0x44);
	private static final Color TITLE_BAND = new Color(0xA0, 0x8B, 0x6A);
	private static final Color LABEL_INK = new Color(0x00, 0x00, 0x00);
	private static final Color HELP_INK = new Color(0x60, 0x40, 0x20);
	private static final Color TITLE_INK = new Color(0x80, 0x00, 0x00);
	private static final Color NOTE_INK = new Color(0x55, 0x55, 0x55);
	private static final Color CLICK = new Color(0x1E, 0x6B, 0x1E);
	private static final Color CLICK_WASH = new Color(0x1E, 0x6B, 0x1E, 0x30);
	/** The strip the message scrollbar owns even while a dialog is up: x 496..512. */
	private static final Color SCROLL_STRIP = new Color(0x8B, 0x1E, 0x1E, 0x40);
	/** The scrollbar's 16px strip, from {@code client.mouseOverChatScrollbar}. */
	private static final int SCROLL_LEFT = 496;
	private static final int SCROLL_W = 16;

	static void render(String cacheRoot) throws Exception {
		File dir = new File(MakeXProbe.previewDir());
		if (!dir.isDirectory() && !dir.mkdirs()) {
			System.out.println("preview: could not create " + dir);
			return;
		}
		BufferedImage paper = readPaper(cacheRoot);
		System.out.println();
		System.out.println("previews (diagram, not a screenshot)");
		for (int family = 0; family < MakeXProbe.LEGACY.length; family++) {
			BufferedImage image = draw(paper, family, MakeXProbe.SLOTS[family]);
			File out = new File(dir, "makex-" + MakeXProbe.FAMILY_NAMES[family] + ".png");
			ImageIO.write(image, "png", out);
			System.out.println("  wrote " + out.getPath());
		}
		// And the two shapes the row is really read in: the one-product case that was reported as
		// five boxes when the player had one uncut gem, and smelting's eight, which is the widest
		// row the click region has to give room to. Drawn from the live count, not from a table.
		MakeXProbe.fill(0, 1);
		MakeX.onModelUpdate(MakeXProbe.modelIds[0][0]);
		File one = new File(dir, "makex-creation-one-product.png");
		ImageIO.write(draw(paper, 0, 1), "png", one);
		System.out.println("  wrote " + one.getPath());
		MakeXProbe.allFilled(0);
	}

	private static BufferedImage draw(BufferedImage paper, int family, int count) {
		int width = MakeXProbe.WIDTH * SCALE;
		// The whole chat area, not just the dialog: the art starts 20px above where the dialog does,
		// so cropping to the dialog would hide the top of the title row.
		int paperHeight = paper != null ? paper.getHeight() : MakeXProbe.HEIGHT;
		int height = (paperHeight + HEADER) * SCALE;
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		g.setColor(Color.WHITE);
		g.fillRect(0, 0, width, height);
		g.setFont(new Font("Dialog", Font.PLAIN, 8 * SCALE));
		g.setColor(NOTE_INK);
		g.drawString("layout diagram - item art is a placeholder, everything else is the real widget",
				4 * SCALE, 10 * SCALE);
		g.translate(0, HEADER * SCALE);

		if (paper != null) {
			g.drawImage(paper, 0, 0, width, paperHeight * SCALE, null);
		} else {
			g.setColor(PAPER);
			g.fillRect(0, 0, width, paperHeight * SCALE);
		}

		// The scrollbar's strip, so a pill that reaches into it is visible as such rather than
		// looking like a margin.
		g.setColor(SCROLL_STRIP);
		g.fillRect(SCROLL_LEFT * SCALE, MakeXProbe.CLICK_TOP * SCALE, SCROLL_W * SCALE,
				(MakeXProbe.CLICK_BOTTOM - MakeXProbe.CLICK_TOP) * SCALE);

		int containerId = MakeX.CONTAINER_FIRST + family;
		RSInterface menu = RSInterface.interfaceCache[containerId];
		int slots = MakeXProbe.SLOTS[family];
		// The width the row is really drawn at: the live count's box, not the widest one. The two
		// agree for every full family and differ exactly for the one-product case drawn separately.
		int box = MakeXProbe.slotWidth(count);

		// The title band and the help line, both hard against the panel's top-left corner. That is
		// what the reference layout does and what was asked for: the question is not centred, and
		// it is not centred on the room left of the pills either. The band is drawn only as wide as
		// the caption's own column, so it does not read as running under the pills.
		int titleBand = MakeXProbe.quantityLeft() - MakeXProbe.TITLE_GAP - MakeXProbe.TITLE_X;
		g.setColor(TITLE_BAND);
		g.fillRect((MakeXProbe.DRAW_X + MakeXProbe.TITLE_X) * SCALE,
				(MakeXProbe.DRAW_OFFSET + MakeXProbe.TITLE_Y) * SCALE, titleBand * SCALE,
				MakeXProbe.TITLE_H * SCALE);
		g.setColor(TITLE_INK);
		g.setFont(new Font("Dialog", Font.BOLD, 11 * SCALE));
		g.drawString("title frame: the server writes the menu name here",
				(MakeXProbe.DRAW_X + MakeXProbe.TITLE_X + 2) * SCALE,
				(MakeXProbe.DRAW_OFFSET + MakeXProbe.TITLE_Y + 12) * SCALE);
		g.setColor(HELP_INK);
		g.setFont(new Font("Dialog", Font.PLAIN, 9 * SCALE));
		g.drawString("Choose a quantity, then click an item to begin.",
				(MakeXProbe.DRAW_X + MakeXProbe.TITLE_X) * SCALE,
				(MakeXProbe.DRAW_OFFSET + MakeXProbe.HELP_Y + 9) * SCALE);

		// The dialog's own widgets: models, the armed click target, and the product labels. Drawn
		// where the client draws them, which is the child coordinate plus the dialog's own offset.
		for (int i = 0; i < menu.children.length; i++) {
			int id = menu.children[i];
			RSInterface child = RSInterface.interfaceCache[id];
			if (child == null || child.width <= 0 || child.height <= 0) {
				// A parked slot: no size at all, and drawn nowhere. Nothing to show.
				continue;
			}
			int x = (menu.childX[i] + MakeXProbe.DRAW_X) * SCALE;
			int y = (menu.childY[i] + MakeXProbe.DRAW_OFFSET) * SCALE;
			if (MakeXProbe.isHitbox(menu, i, slots)) {
				// The armed amount's click target: real, and invisible in the game.
				g.setColor(CLICK_WASH);
				g.fillRect(x, y, child.width * SCALE, child.height * SCALE);
				g.setColor(CLICK);
				g.drawRect(x, y, child.width * SCALE - 1, child.height * SCALE - 1);
				g.setFont(new Font("Dialog", Font.PLAIN, 8 * SCALE));
				g.drawString("click reports " + id, x + 4, y + child.height * SCALE - 5);
			} else if (child.type == 5 && child.height == MakeXProbe.SLOT_H) {
				// The slot frame, drawn from the real sprite so the preview shows the actual art.
				if (child.sprite1 != null) {
					blit(g, child.sprite1, x, y);
				}
			} else if (child.width == box && child.height == MakeXProbe.SLOT_H) {
				g.setColor(BOX);
				g.drawRect(x, y, child.width * SCALE - 1, child.height * SCALE - 1);
				g.setFont(new Font("Dialog", Font.PLAIN, 8 * SCALE));
				g.setColor(NOTE_INK);
				g.drawString("item model, centred in the slot", x + 4, y + 12 * SCALE);
				g.drawString("frame " + id, x + 4, y + 20 * SCALE);
			}
		}

		// The quantity row, drawn last so its pills sit over the label row the way they will.
		g.setFont(new Font("Dialog", Font.BOLD, 8 * SCALE));
		for (int amount = 0; amount < MakeXProbe.AMOUNTS; amount++) {
			RSInterface button = RSInterface.interfaceCache[MakeX.QUANTITY_FIRST + amount];
			RSInterface caption = RSInterface.interfaceCache[
					MakeXProbe.QUANTITY_TEXT_FIRST + amount];
			int x = MakeXProbe.drawnXOf(containerId, MakeX.QUANTITY_FIRST + amount) * SCALE;
			int y = MakeXProbe.drawnYOf(containerId, MakeX.QUANTITY_FIRST + amount) * SCALE;
			Sprite pill = button.sprite1;
			if (pill != null) {
				blit(g, pill, x, y);
			} else {
				g.setColor(BOX);
				g.drawRect(x, y, MakeXProbe.QUANT_W * SCALE - 1, MakeXProbe.QUANT_H * SCALE - 1);
			}
			g.setColor(new Color(caption.textColor));
			center(g, caption.message, x + MakeXProbe.QUANT_W * SCALE / 2,
					y + MakeXProbe.QUANT_H * SCALE / 2 + 5);
		}

		// The region the client builds hitboxes in, last so it frames everything: anything outside
		// this is drawn but dead, which is exactly the bug this diagram exists to catch.
		g.setColor(CLICK);
		g.drawRect(MakeXProbe.CLICK_LEFT * SCALE, MakeXProbe.CLICK_TOP * SCALE,
				(MakeXProbe.CLICK_RIGHT - MakeXProbe.CLICK_LEFT) * SCALE - 1,
				(MakeXProbe.CLICK_BOTTOM - MakeXProbe.CLICK_TOP) * SCALE - 1);

		g.dispose();
		return image;
	}

	/** Copies a sprite's pixels, one source pixel per SCALE x SCALE block. */
	private static void blit(Graphics2D g, Sprite sprite, int x, int y) {
		for (int sy = 0; sy < sprite.myHeight; sy++) {
			for (int sx = 0; sx < sprite.myWidth; sx++) {
				int pixel = sprite.myPixels[sy * sprite.myWidth + sx];
				if ((pixel >>> 24) == 0) {
					continue;
				}
				g.setColor(new Color(pixel, true));
				g.fillRect(x + sx * SCALE, y + sy * SCALE, SCALE, SCALE);
			}
		}
	}

	/** Rough JS5-font kerning at AWT's mercy: a short name would otherwise look far too wide. */
	private static String equalise(String text) {
		if (text == null) {
			return "";
		}
		String flat = text.replace("\\n", " ").trim();
		return flat.length() > 11 ? flat.substring(0, 11) : flat;
	}

	private static void center(Graphics2D g, String text, int cx, int baseline) {
		if (text == null) {
			return;
		}
		int width = g.getFontMetrics().stringWidth(text);
		g.drawString(text, cx - width / 2, baseline);
	}

	/**
	 * The panel the interfaces are drawn over.
	 *
	 * <p>A creation chatbox is drawn on its own art - {@code ItemCraftingChatInterface/
	 * Chatboxbackground.png}, 519x142, which is what {@code ChatArea.drawCreationPanel} paints - and
	 * every other dialog on {@code Gameframe/chatarea.png}, which the chat area draws and leaves
	 * visible. The new art is preferred when it is present, because it is what the player will
	 * actually see behind these five.
	 */
	private static BufferedImage readPaper(String cacheRoot) {
		File[] candidates = {
				new File(cacheRoot, "Sprites/ItemCraftingChatInterface/Chatboxbackground.png"),
				new File(cacheRoot, "Sprites/ItemCraftingChatInterface/Chatboxbackground.PNG"),
				new File(cacheRoot, "Sprites/Gameframe/chatarea.png"),
				new File(cacheRoot, "Sprites/Gameframe/chatarea.PNG"),
		};
		for (int i = 0; i < candidates.length; i++) {
			if (candidates[i].isFile()) {
				try {
					return ImageIO.read(candidates[i]);
				} catch (Exception e) {
					System.out.println("preview: could not read " + candidates[i] + ": " + e);
					return null;
				}
			}
		}
		System.out.println("preview: " + candidates[0] + " not found, filling with the chatbox tan");
		return null;
	}

	private MakeXPreview() {
	}
}
