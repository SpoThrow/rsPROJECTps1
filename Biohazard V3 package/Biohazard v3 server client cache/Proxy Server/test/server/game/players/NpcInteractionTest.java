package server.game.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link NpcInteraction} (§4.20).
 *
 * <p>These pin the two things the extraction could silently have broken: the "nothing clicked"
 * sentinel, and the fact that the bag is a plain mutable holder with no behaviour of its own.
 */
public class NpcInteractionTest {

	@Test
	public void freshInteractionIsEmpty() {
		NpcInteraction interaction = new NpcInteraction();
		assertEquals(0, interaction.npcType, "no NPC clicked yet");
		assertEquals(0, interaction.npcClickIndex, "no slot occupied");
		assertEquals(0, interaction.clickNpcType, "no menu option chosen");
	}

	@Test
	public void holdsTheClickedNpcSlotAndType() {
		NpcInteraction interaction = new NpcInteraction();
		interaction.npcClickIndex = 42;
		interaction.npcType = 1974;
		assertEquals(42, interaction.npcClickIndex);
		assertEquals(1974, interaction.npcType);
	}

	@Test
	public void clickNpcTypeIsAMenuOptionIndexNotAType() {
		// The name is a lie: ClickNPC writes 1..4 for the four right-click options.
		NpcInteraction interaction = new NpcInteraction();
		interaction.clickNpcType = 3;
		assertTrue(interaction.clickNpcType > 0,
				"PlayerAssistant treats \"talking\" as clickNpcType > 0");
	}

	@Test
	public void clearingTheClickResetsTheOptionSentinel() {
		NpcInteraction interaction = new NpcInteraction();
		interaction.npcClickIndex = 7;
		interaction.clickNpcType = 2;
		// This is the shape of the reset code between clicks.
		interaction.npcClickIndex = 0;
		interaction.clickNpcType = 0;
		interaction.npcType = 0;
		assertEquals(0, interaction.npcClickIndex);
		assertEquals(0, interaction.clickNpcType);
		assertEquals(0, interaction.npcType);
	}
}
