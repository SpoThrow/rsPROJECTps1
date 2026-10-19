package def;

import node.MRUNodes;import cache.StreamLoader;
import game.client;
import model.Frames;
import model.Model;
import net.Stream;



public final class EntityDef {

	public static EntityDef forID(int i) {
		for (int j = 0; j < 20; j++)
			if (cache[j].interfaceType == (long) i)
				return cache[j];

		int id = i;
		int readId = i;
		if (i < 0 || streamIndices == null || i >= streamIndices.length)
			readId = 0;
		anInt56 = (anInt56 + 1) % 20;
		EntityDef entityDef = cache[anInt56] = new EntityDef();
		stream.currentOffset = streamIndices[readId];
		entityDef.interfaceType = id;
		entityDef.readValues(stream);
		switch (id) {
	       case 742:
	       case 6087:
	           // Elvarg, the Dragon Slayer boss. The server files carry her HP and damage; the
	           // level shown on the right-click menu comes from here, so it has to match.
	           entityDef.name = "Elvarg";
	           entityDef.combatLevel = 118;
	           break;
	       case 1625:
	           entityDef.name = "Smoke devil";
	           entityDef.combatLevel = 160;
	           entityDef.anIntArray94[0] = 28302;
	           EntityDef smoke = forID(1624);
	           entityDef.standAnim = smoke.standAnim;
	           entityDef.walkAnim = smoke.walkAnim;
	           entityDef.actions = smoke.actions;
	           entityDef.anInt86 = smoke.anInt86;
	           entityDef.anInt91 = smoke.anInt91;
	    				break;
	       case 105:
	           entityDef.name = "Callisto";
	           entityDef.combatLevel = 470;
	           entityDef.anIntArray94[0] = 28298;
	           EntityDef callisto = forID(105);
	           entityDef.standAnim = callisto.standAnim;
	           entityDef.walkAnim = callisto.walkAnim;
	           entityDef.actions = callisto.actions;
	           entityDef.anInt86 = 70;
	           entityDef.anInt91 = 60;
	    				break;
	       case 650:
	           // World-adventurer Max — 667 Max cape look (npc 3373).
	           // Model 252 remapped to 90252 so Raw preload cannot clobber OG model 252.
	           entityDef.name = "Max";
	           entityDef.combatLevel = 138;
	           entityDef.anIntArray94 = new int[] { 65291, 62746, 62743, 27738, 13307, 65300, 90252 };
	           entityDef.anIntArray73 = new int[] { 16, 38097 };
	           entityDef.standAnim = 813;
	           entityDef.walkAnim = 1205;
	           entityDef.anInt58 = 1205;
	           entityDef.anInt83 = 1205;
	           entityDef.anInt55 = 1205;
	           entityDef.aByte68 = 1;
	           entityDef.actions = new String[5];
	           entityDef.actions[0] = "Talk-to";
	           entityDef.description = "A skillcaper who never stops training.".getBytes();
	           break;
	       case 13447:
	           // Deflect Melee (curse overhead)
	           entityDef.name = "Nex";
	           entityDef.combatLevel = 1001;
	           entityDef.anIntArray94 = new int[] { 62717 };
	           entityDef.standAnim = 6320;
	           entityDef.walkAnim = 6319;
	           entityDef.anInt58 = 6319;
	           entityDef.anInt83 = 6319;
	           entityDef.anInt55 = 6319;
	           entityDef.aByte68 = 3;
	           entityDef.anInt75 = 9;
	           entityDef.actions = new String[5];
	           entityDef.actions[1] = "Attack";
	           entityDef.description = "The empty lord's general.".getBytes();
	           break;
	       case 13448:
	           // Deflect Missiles (curse overhead)
	           entityDef.name = "Nex";
	           entityDef.combatLevel = 1001;
	           entityDef.anIntArray94 = new int[] { 62717 };
	           entityDef.standAnim = 6320;
	           entityDef.walkAnim = 6319;
	           entityDef.anInt58 = 6319;
	           entityDef.anInt83 = 6319;
	           entityDef.anInt55 = 6319;
	           entityDef.aByte68 = 3;
	           entityDef.anInt75 = 11;
	           entityDef.actions = new String[5];
	           entityDef.actions[1] = "Attack";
	           entityDef.description = "The empty lord's general.".getBytes();
	           break;
	       case 13449:
	           // Deflect Magic (curse overhead)
	           entityDef.name = "Nex";
	           entityDef.combatLevel = 1001;
	           entityDef.anIntArray94 = new int[] { 62717 };
	           entityDef.standAnim = 6320;
	           entityDef.walkAnim = 6319;
	           entityDef.anInt58 = 6319;
	           entityDef.anInt83 = 6319;
	           entityDef.anInt55 = 6319;
	           entityDef.aByte68 = 3;
	           entityDef.anInt75 = 10;
	           entityDef.actions = new String[5];
	           entityDef.actions[1] = "Attack";
	           entityDef.description = "The empty lord's general.".getBytes();
	           break;
	       case 13450:
	           // Soul Split (curse overhead — late fight)
	           entityDef.name = "Nex";
	           entityDef.combatLevel = 1001;
	           entityDef.anIntArray94 = new int[] { 62717 };
	           entityDef.standAnim = 6320;
	           entityDef.walkAnim = 6319;
	           entityDef.anInt58 = 6319;
	           entityDef.anInt83 = 6319;
	           entityDef.anInt55 = 6319;
	           entityDef.aByte68 = 3;
	           entityDef.anInt75 = 17;
	           entityDef.actions = new String[5];
	           entityDef.actions[1] = "Attack";
	           entityDef.description = "The empty lord's general.".getBytes();
	           break;
	       case 13451:
	           // Wrath (curse overhead — critical HP)
	           entityDef.name = "Nex";
	           entityDef.combatLevel = 1001;
	           entityDef.anIntArray94 = new int[] { 62717 };
	           entityDef.standAnim = 6320;
	           entityDef.walkAnim = 6319;
	           entityDef.anInt58 = 6319;
	           entityDef.anInt83 = 6319;
	           entityDef.anInt55 = 6319;
	           entityDef.aByte68 = 3;
	           entityDef.anInt75 = 16;
	           entityDef.actions = new String[5];
	           entityDef.actions[1] = "Attack";
	           entityDef.description = "The empty lord's general.".getBytes();
	           break;
	       case 13458:
	           // Blood reaver (667) - model 27823, anims 7000/7001/7004/7008
	           entityDef.name = "Blood reaver";
	           entityDef.combatLevel = 147;
	           entityDef.anIntArray94 = new int[] { 27823 };
	           entityDef.standAnim = 7008;
	           entityDef.walkAnim = 7001;
	           entityDef.anInt58 = 7001;
	           entityDef.anInt83 = 7001;
	           entityDef.anInt55 = 7001;
	           entityDef.aByte68 = 1;
	           entityDef.actions = new String[5];
	           entityDef.actions[1] = "Attack";
	           entityDef.description = "A blood-thirsty servant of Nex.".getBytes();
	           break;
	       case 6605:
	           // Wilderness bandit, level 130. This cache stops at the 474-era bandits (id 1926 is the
	           // highest, level 74), so the OSRS id 6605 is declared here as a head over that body:
	           // same model, same stand/walk, but the level the right-click menu reads and the
	           // "Attack" option a wilderness outlaw should have. The server side lives in
	           // npc.cfg (130 combat, 155 hp) and npc_drops.cfg; see NPCHandler.isAggressive.
	           EntityDef bandit = forID(1926);
	           entityDef.name = "Bandit";
	           entityDef.combatLevel = 130;
	           entityDef.anIntArray94 = bandit.anIntArray94.clone();
	           entityDef.anIntArray73 = bandit.anIntArray73 == null ? null : bandit.anIntArray73.clone();
	           entityDef.anIntArray70 = bandit.anIntArray70 == null ? null : bandit.anIntArray70.clone();
	           entityDef.anIntArray76 = bandit.anIntArray76 == null ? null : bandit.anIntArray76.clone();
	           entityDef.standAnim = bandit.standAnim;
	           entityDef.walkAnim = bandit.walkAnim;
	           entityDef.anInt58 = bandit.anInt58;
	           entityDef.anInt83 = bandit.anInt83;
	           entityDef.anInt55 = bandit.anInt55;
	           entityDef.anInt79 = bandit.anInt79;
	           entityDef.anInt86 = bandit.anInt86;
	           entityDef.anInt91 = bandit.anInt91;
	           entityDef.anInt75 = bandit.anInt75;
	           entityDef.aByte68 = bandit.aByte68;
	           entityDef.actions = new String[5];
	           entityDef.actions[1] = "Attack";
	           entityDef.description = "A wilderness outlaw.".getBytes();
	           break;
	       case 7133:
	           // Bork (667) - models/anims from CursePack-era 667 Data
	           entityDef.name = "Bork";
	           entityDef.combatLevel = 267;
	           entityDef.anIntArray94 = new int[] { 32351, 32352 };
	           entityDef.standAnim = 8753;
	           entityDef.walkAnim = 8752;
	           entityDef.anInt58 = 8752;
	           entityDef.anInt83 = 8752;
	           entityDef.anInt55 = 8752;
	           entityDef.aByte68 = 3;
	           entityDef.actions = new String[5];
	           entityDef.actions[1] = "Attack";
	           entityDef.description = "A huge ork warbeast.".getBytes();
	           break;
	       case 1488:
	       case 1489:
	           // Training dummies. The cache already has these as "Dummy" (model 14035, size 1) but
	           // with no actions, no combat level and no stand animation - so on their own they are
	           // scenery a player can neither click nor recognise. Three things are added here:
	           //
	           //   * the model of object 23921 - the world's own "Dummy" object, model 25177 - so the
	           //     two targets look exactly like the training dummies already in the game rather
	           //     than like whatever the cache's NPC-side dummy art happens to be;
	           //   * an "Attack" action, which is what makes the NPC clickable at all;
	           //   * a combat level, which is what the menu treats as "this is attackable" and what
	           //     decides whether Attack is the left-click or sits behind right-click.
	           //
	           // The name is what tells the two apart in-game, since they share a model. What each
	           // one actually does to a damage roll - and the fact that neither pays XP - is decided
	           // server-side in TrainingDummy.
	           //
	           // anInt79 is the turn speed the client copies into Entity.anInt1504, and it is the one
	           // field here that is not cosmetic. The cache sets it to 0 for these two, which is
	           // deliberate on its part - they are meant to be scenery - but it also silently
	           // disables facing: method100, the only code that applies a direction to an entity,
	           // returns immediately when anInt1504 is 0. That is why the dummies sat at the model's
	           // authored orientation (south) no matter what the server told them to look at.
	           //
	           // 1024 rather than the usual 32: a turn is applied in whole steps of anInt1504, and
	           // every turn is smaller than 1024, so one tick lands it. A dummy is a fixture, not a
	           // person - it should already be facing east the moment it comes into view, not pivot
	           // towards it over the ~10 seconds 32 would take. Only turning reads this field.
	           //
	           // The absent stand animation is deliberate: both are stationary targets, so a still
	           // model is the correct look, and EntityDef.method164 renders a model with no frame
	           // rather than skipping it.
	           entityDef.name = id == 1488 ? "0 Defence Dummy" : "Max Hit Dummy";
	           entityDef.combatLevel = 1;
	           entityDef.anIntArray94 = new int[] { 25177 };
	           entityDef.anInt79 = 1024;
	           entityDef.actions = new String[5];
	           entityDef.actions[1] = "Attack";
	           entityDef.description = (id == 1488
	                   ? "A training dummy with no defence at all - every hit lands."
	                   : "A training dummy that always takes your maximum hit.").getBytes();
	           break;
	       case 8019:
	           // The home teleport portal (667 revision). The 667 def carries this one model and a
	           // stand animation and nothing else, so everything below is stated here rather than
	           // read from npc.dat, whose 474 entry for this id is unrelated.
	           //
	           // Model 41508 is NOT in the packed 474 model archive; it is loaded from the cache's
	           // Raw folder by client.preloadModels, the same path the Nex and CursePack models use.
	           // "Teleport" is the FIRST action, which is what makes it the left-click option.
	           // menuColor is cyan so the name in the menu is drawn like an object's, not an NPC's.
	           entityDef.name = "Portal";
	           entityDef.combatLevel = 0;
	           entityDef.anIntArray94 = new int[] { 41508 };
	           entityDef.standAnim = 10176;
	           // The 474 entry for this id (if there is one) may name a walk/turn animation for a
	           // different NPC; a portal never walks, so all four are cleared rather than inherited.
	           entityDef.walkAnim = -1;
	           entityDef.anInt58 = -1;
	           entityDef.anInt83 = -1;
	           entityDef.anInt55 = -1;
	           entityDef.aByte68 = 1;
	           entityDef.menuColor = "@cya@";
	           entityDef.actions = new String[5];
	           entityDef.actions[0] = "Teleport";
	           entityDef.description = "A shimmering gateway to distant places.".getBytes();
	           break;
	}
		return entityDef;
	}

	public Model method160() {
		if (childrenIDs != null) {
			EntityDef entityDef = method161();
			if (entityDef == null)
				return null;
			else
				return entityDef.method160();
		}
		if (anIntArray73 == null)
			return null;
		boolean flag1 = false;
		for (int i = 0; i < anIntArray73.length; i++)
			if (!Model.method463(anIntArray73[i]))
				flag1 = true;

		if (flag1)
			return null;
		Model aclass30_sub2_sub4_sub6s[] = new Model[anIntArray73.length];
		for (int j = 0; j < anIntArray73.length; j++)
			aclass30_sub2_sub4_sub6s[j] = Model.method462(anIntArray73[j]);

		Model model;
		if (aclass30_sub2_sub4_sub6s.length == 1)
			model = aclass30_sub2_sub4_sub6s[0];
		else
			model = new Model(aclass30_sub2_sub4_sub6s.length,
					aclass30_sub2_sub4_sub6s);
		if (anIntArray76 != null) {
			for (int k = 0; k < anIntArray76.length; k++)
				model.method476(anIntArray76[k], anIntArray70[k]);

		}
		return model;
	}

	public EntityDef method161() {
		int j = -1;
		if (anInt57 != -1 && anInt57 <= 2113) {
			VarBit varBit = VarBit.cache[anInt57];
			int k = varBit.anInt648;
			int l = varBit.anInt649;
			int i1 = varBit.anInt650;
			int j1 = client.anIntArray1232[i1 - l];
			j = clientInstance.variousSettings[k] >> l & j1;
		} else if (anInt59 != -1)
			j = clientInstance.variousSettings[anInt59];
		if (j < 0 || j >= childrenIDs.length || childrenIDs[j] == -1)
			return null;
		else
			return forID(childrenIDs[j]);
	}

	public static void unpackConfig(StreamLoader streamLoader) {
		stream = new Stream(streamLoader.getDataForName("npc.dat"));
		Stream stream2 = new Stream(streamLoader.getDataForName("npc.idx"));
		int totalNPCs = stream2.readUnsignedWord();
		// Room for custom high IDs (Nex + Blood reaver) without remapping to 0.
		int maxId = Math.max(totalNPCs, 13458 + 1);
		streamIndices = new int[maxId];
		int i = 2;
		for (int j = 0; j < totalNPCs; j++) {
			streamIndices[j] = i;
			i += stream2.readUnsignedWord();
		}
		int fallback = totalNPCs > 0 ? streamIndices[0] : 2;
		for (int j = totalNPCs; j < maxId; j++) {
			streamIndices[j] = fallback;
		}

		cache = new EntityDef[20];
		for (int k = 0; k < 20; k++)
			cache[k] = new EntityDef();
		for (int index = 0; index < totalNPCs; index++) {
			EntityDef ed = forID(index);
			if (ed == null)
				continue;
			if (ed.name == null)
				continue;
		}
	}

	public static void nullLoader() {
		mruNodes = null;
		streamIndices = null;
		cache = null;
		stream = null;
	}

	public Model method164(int j, int k, int ai[]) {
		return method164(j, k, ai, -1, 1, 0);
	}

	public Model method164(int j, int k, int ai[], int nextFrame, int duration, int cycle) {
		if (childrenIDs != null) {
			EntityDef entityDef = method161();
			if (entityDef == null)
				return null;
			else
				return entityDef.method164(j, k, ai, nextFrame, duration, cycle);
		}
		Model model = (Model) mruNodes.insertFromCache(interfaceType);
		if (model == null) {
			boolean flag = false;
			for (int i1 = 0; i1 < anIntArray94.length; i1++)
				if (!Model.method463(anIntArray94[i1]))
					flag = true;

			if (flag)
				return null;
			Model aclass30_sub2_sub4_sub6s[] = new Model[anIntArray94.length];
			for (int j1 = 0; j1 < anIntArray94.length; j1++)
				aclass30_sub2_sub4_sub6s[j1] = Model
				.method462(anIntArray94[j1]);

			if (aclass30_sub2_sub4_sub6s.length == 1)
				model = aclass30_sub2_sub4_sub6s[0];
			else
				model = new Model(aclass30_sub2_sub4_sub6s.length,
						aclass30_sub2_sub4_sub6s);
			if (anIntArray76 != null) {
				for (int k1 = 0; k1 < anIntArray76.length; k1++)
					model.method476(anIntArray76[k1], anIntArray70[k1]);

			}
			model.method469();
			//model.method479(64 + anInt85, 850 + anInt92, -30, -50, -30, true);
			model.method479(84 + anInt85, 1000 + anInt92, -90, -580, -90, true);
			mruNodes.removeFromCache(model, interfaceType);
		}
		Model model_1 = Model.aModel_1621;
		model_1.method464(model, Frames.method532(k) & Frames.method532(j));
		if (k != -1 && j != -1)
			model_1.method471(ai, j, k);
		else if (k != -1)
			model_1.applyAnimationFrame(k, nextFrame, duration, cycle);
		if (anInt91 != 128 || anInt86 != 128)
			model_1.method478(anInt91, anInt91, anInt86);
		model_1.method466();
		model_1.anIntArrayArray1658 = null;
		model_1.anIntArrayArray1657 = null;
		if (aByte68 == 1)
			model_1.aBoolean1659 = true;
		return model_1;
	}

	public void readValues(Stream stream) {
		do {
			int i = stream.readUnsignedByte();
			if (i == 0)
				return;
			if (i == 1) {
				int j = stream.readUnsignedByte();
				anIntArray94 = new int[j];
				for (int j1 = 0; j1 < j; j1++)
					anIntArray94[j1] = stream.readUnsignedWord();

			} else if (i == 2)
				name = stream.readNewString();
			else if (i == 3)
				description = stream.readBytes();
			else if (i == 12)
				aByte68 = stream.readSignedByte();
			else if (i == 13)
				standAnim = stream.readUnsignedWord();
			else if (i == 14)
				walkAnim = stream.readUnsignedWord();
			else if (i == 17) {
				walkAnim = stream.readUnsignedWord();
				anInt58 = stream.readUnsignedWord();
				anInt83 = stream.readUnsignedWord();
				anInt55 = stream.readUnsignedWord();
				if (walkAnim == 65535)
					walkAnim = -1;
				if (anInt58 == 65535)
					anInt58 = -1;
				if (anInt83 == 65535)
					anInt83 = -1;
				if (anInt55 == 65535)
					anInt55 = -1;
			} else if (i >= 30 && i < 40) {
				if (actions == null)
					actions = new String[5];
				actions[i - 30] = stream.readNewString();
				if (actions[i - 30].equalsIgnoreCase("hidden"))
					actions[i - 30] = null;
			} else if (i == 40) {
				int k = stream.readUnsignedByte();
				anIntArray76 = new int[k];
				anIntArray70 = new int[k];
				for (int k1 = 0; k1 < k; k1++) {
					anIntArray76[k1] = stream.readUnsignedWord();
					anIntArray70[k1] = stream.readUnsignedWord();
				}

			} else if (i == 60) {
				int l = stream.readUnsignedByte();
				anIntArray73 = new int[l];
				for (int l1 = 0; l1 < l; l1++)
					anIntArray73[l1] = stream.readUnsignedWord();

			} else if (i == 90)
				stream.readUnsignedWord();
			else if (i == 91)
				stream.readUnsignedWord();
			else if (i == 92)
				stream.readUnsignedWord();
			else if (i == 93)
				aBoolean87 = false;
			else if (i == 95)
				combatLevel = stream.readUnsignedWord();
			else if (i == 97)
				anInt91 = stream.readUnsignedWord();
			else if (i == 98)
				anInt86 = stream.readUnsignedWord();
			else if (i == 99)
				aBoolean93 = true;
			else if (i == 100)
				anInt85 = stream.readSignedByte();
			else if (i == 101)
				anInt92 = stream.readSignedByte() * 5;
			else if (i == 102)
				anInt75 = stream.readUnsignedWord();
			else if (i == 103)
				anInt79 = stream.readUnsignedWord();
			else if (i == 106) {
				anInt57 = stream.readUnsignedWord();
				if (anInt57 == 65535)
					anInt57 = -1;
				anInt59 = stream.readUnsignedWord();
				if (anInt59 == 65535)
					anInt59 = -1;
				int i1 = stream.readUnsignedByte();
				childrenIDs = new int[i1 + 1];
				for (int i2 = 0; i2 <= i1; i2++) {
					childrenIDs[i2] = stream.readUnsignedWord();
					if (childrenIDs[i2] == 65535)
						childrenIDs[i2] = -1;
				}

			} else if (i == 107)
				aBoolean84 = false;
		} while (true);
	}

	public EntityDef() {
		anInt55 = -1;
		anInt57 = -1;
		anInt58 = -1;
		anInt59 = -1;
		combatLevel = -1;
		anInt64 = 1834;
		walkAnim = -1;
		aByte68 = 1;
		anInt75 = -1;
		standAnim = -1;
		interfaceType = -1L;
		anInt79 = 32;
		anInt83 = -1;
		aBoolean84 = true;
		anInt86 = 128;
		aBoolean87 = true;
		anInt91 = 128;
		aBoolean93 = false;
	}

	public int anInt55;
	public static int anInt56;
	public int anInt57;
	public int anInt58;
	public int anInt59;
	public static Stream stream;
	public int combatLevel;
	public final int anInt64;
	public String name;
	public String actions[];
	public int walkAnim;
	public byte aByte68;
	public int[] anIntArray70;
	public static int[] streamIndices;
	public int[] anIntArray73;
	public int anInt75;
	public int[] anIntArray76;
	public int standAnim;
	public long interfaceType;
	public int anInt79;
	public static EntityDef[] cache;
	public static client clientInstance;
	public int anInt83;
	public boolean aBoolean84;
	public int anInt85;
	public int anInt86;
	public boolean aBoolean87;
	public int childrenIDs[];
	public byte description[];
	public int anInt91;
	public int anInt92;
	public boolean aBoolean93;
	public int[] anIntArray94;
	/**
	 * Colour tag the menu draws this NPC's name in, or {@code null} for the ordinary NPC yellow.
	 *
	 * <p>Objects have always had their names in cyan ({@code @cya@}) while NPCs use
	 * {@code @yel@}, so a definition that should read like scenery - the home teleport portal
	 * is the case this exists for - would otherwise be indistinguishable from a person in the
	 * right-click menu. {@link #name} is still the plain text; this is presentation only.
	 */
	public String menuColor;
	public static MRUNodes mruNodes = new MRUNodes(30);

}
