// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

public final class NPC extends Entity
{

	private Model method450()
	{
		if(super.anim >= 0 && super.anInt1529 == 0)
		{
			int k = Entity.frameId(super.anim, super.anInt1527);
			int i1 = -1;
			if(super.anInt1517 >= 0 && super.anInt1517 != super.anInt1511)
				i1 = Entity.frameId(super.anInt1517, super.anInt1518);
			int[] morph = null;
			if (super.anim < Animation.anims.length && Animation.anims[super.anim] != null)
				morph = Animation.anims[super.anim].anIntArray357;
			return desc.method164(i1, k, morph,
					Entity.frameId(super.anim, super.nextAnimFrame),
					Entity.frameDuration(super.anim, super.anInt1527), super.anInt1528);
		}
		int l = -1;
		if(super.anInt1517 >= 0)
			l = Entity.frameId(super.anInt1517, super.anInt1518);
		return desc.method164(-1, l, null,
				Entity.frameId(super.anInt1517, super.nextIdleFrame),
				Entity.frameDuration(super.anInt1517, super.anInt1518), super.anInt1519);
	}

	public Model getRotatedModel()
	{
		if(desc == null)
			return null;
		Model model = method450();
		if(model == null)
			return null;
		super.height = model.modelHeight;
		if(super.anInt1520 != -1 && super.anInt1521 != -1
				&& SpotAnim.cache != null && super.anInt1520 >= 0 && super.anInt1520 < SpotAnim.cache.length)
		{
			SpotAnim spotAnim = SpotAnim.cache[super.anInt1520];
			if(spotAnim == null || spotAnim.aAnimation_407 == null
					|| spotAnim.aAnimation_407.anIntArray353 == null
					|| super.anInt1521 < 0 || super.anInt1521 >= spotAnim.aAnimation_407.anIntArray353.length)
			{
				if(desc.aByte68 == 1)
					model.aBoolean1659 = true;
				return model;
			}
			Model gfxBase = spotAnim.getModel();
			if(gfxBase != null)
			{
				int frameId = spotAnim.aAnimation_407.anIntArray353[super.anInt1521];
				// Frame archive not ready — show body without gfx this tick.
				if (frameId != -1 && Class36.method531(frameId) == null)
				{
					if(desc.aByte68 == 1)
						model.aBoolean1659 = true;
					return model;
				}
				try {
					/*
					 * Detach body from shared scratch (aModel_1621) only when
					 * layering a spotanim (e.g. Soul Split 2264), and keep height.
					 */
					Model body = new Model(true, true, false, model);
					body.modelHeight = model.modelHeight;
					Model gfx = new Model(true, Class36.method532(frameId), false, gfxBase);
					gfx.method475(0, -super.anInt1524, 0);
					gfx.method469();
					if (frameId != -1)
						gfx.method470(frameId);
					gfx.anIntArrayArray1658 = null;
					gfx.anIntArrayArray1657 = null;
					if(spotAnim.anInt410 != 128 || spotAnim.anInt411 != 128)
						gfx.method478(spotAnim.anInt410, spotAnim.anInt410, spotAnim.anInt411);
					gfx.method479(64 + spotAnim.anInt413, 850 + spotAnim.anInt414, -30, -50, -30, true);
					if (gfx.anInt1626 > 0 && body.anInt1626 > 0) {
						Model layered[] = {
								body, gfx
						};
						model = new Model(layered);
						model.modelHeight = body.modelHeight;
					} else {
						model = body;
					}
				} catch (RuntimeException ignored) {
					// Keep original body if curse/667 gfx merge fails.
				}
			}
		}
		if(desc.aByte68 == 1)
			model.aBoolean1659 = true;
		return model;
	}

	public boolean isVisible()
	{
		return desc != null;
	}

	NPC()
	{
	}

	public EntityDef desc;
}
