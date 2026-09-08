import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Floating XP drops drawn over the 3D viewport.
 */
final class XpDrops {

	private static final int MAX_DROPS = 12;
	private static final int LIFETIME = 80;

	private static final int[] SKILL_COLORS = {
			0xFFFF00, 0xC0C0C0, 0xFF7000, 0xFF0000, 0x00FF00,
			0xFFFFFF, 0x0000FF, 0xFF7F00, 0x80FF00, 0xC8A000,
			0x0000C8, 0xFF7F00, 0xFF00FF, 0xC0C0C0, 0x808080,
			0x00FF00, 0x0000FF, 0xFFFF00, 0x800080, 0x00C000,
			0xC800C8, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF
	};

	private final List<Drop> drops = new ArrayList<Drop>();

	void add(int skill, int amount) {
		if (amount <= 0 || skill < 0 || skill >= Skills.skillsCount)
			return;
		if (!drops.isEmpty()) {
			Drop last = drops.get(drops.size() - 1);
			if (last.skill == skill && last.age < 8) {
				last.amount += amount;
				return;
			}
		}
		if (drops.size() >= MAX_DROPS)
			drops.remove(0);
		drops.add(new Drop(skill, amount));
	}

	void reset() {
		drops.clear();
	}

	void processAndDraw(RSFont font) {
		if (drops.isEmpty() || font == null)
			return;
		int stack = 0;
		Iterator<Drop> it = drops.iterator();
		while (it.hasNext()) {
			Drop drop = it.next();
			drop.age++;
			drop.y--;
			if (drop.age > LIFETIME) {
				it.remove();
				continue;
			}
			int alphaFade = drop.age > LIFETIME - 15 ? (LIFETIME - drop.age) * 12 : 256;
			if (alphaFade < 40)
				alphaFade = 40;
			int color = skillColor(drop.skill);
			int drawX = 256;
			int drawY = 90 + drop.y + stack * 16;
			if (drawY < 20)
				drawY = 20;
			String skill = Skills.skillNames[drop.skill];
			if (skill.startsWith("-"))
				skill = "xp";
			String text = capitalize(skill) + " +" + format(drop.amount);
			font.drawCenteredString(text, drawX + 1, drawY + 1, 0, 0);
			font.drawCenteredString(text, drawX, drawY, color, 0);
			stack++;
		}
	}

	private static int skillColor(int skill) {
		if (skill < 0 || skill >= SKILL_COLORS.length)
			return 0xFFFF00;
		return SKILL_COLORS[skill];
	}

	private static String capitalize(String s) {
		if (s == null || s.length() == 0)
			return s;
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	private static String format(int n) {
		String raw = String.valueOf(n);
		StringBuilder sb = new StringBuilder();
		int len = raw.length();
		for (int i = 0; i < len; i++) {
			if (i > 0 && (len - i) % 3 == 0)
				sb.append(',');
			sb.append(raw.charAt(i));
		}
		return sb.toString();
	}

	private static final class Drop {
		final int skill;
		int amount;
		int y;
		int age;

		Drop(int skill, int amount) {
			this.skill = skill;
			this.amount = amount;
			this.y = 0;
			this.age = 0;
		}
	}
}
