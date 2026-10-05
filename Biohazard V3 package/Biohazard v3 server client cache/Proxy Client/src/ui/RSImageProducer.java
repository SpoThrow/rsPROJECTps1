// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

package ui;

import java.awt.*;
import java.awt.image.*;

public final class RSImageProducer
		implements ImageProducer, ImageObserver
{

	public RSImageProducer(int i, int j, Component component)
	{
		anInt316 = i;
		anInt317 = j;
		anIntArray315 = new int[i * j];
		aColorModel318 = new DirectColorModel(32, 0xff0000, 65280, 255);
		anImage320 = createImageBuffer(component);
		if (!(anImage320 instanceof BufferedImage)) {
			method239();
			component.prepareImage(anImage320, this);
			method239();
			component.prepareImage(anImage320, this);
			method239();
			component.prepareImage(anImage320, this);
		}
		initDrawingArea();
	}

	private Image createImageBuffer(Component component)
	{
		try {
			return GlPresent.wrapPixels(anIntArray315, anInt316, anInt317, (DirectColorModel) aColorModel318);
		} catch (Exception e) {
		}
		return component.createImage(this);
	}

	public void initDrawingArea()
	{
		DrawingArea.initDrawingArea(anInt317, anInt316, anIntArray315);
	}

	/**
	 * Package-private since Phase 4.1a: {@link Renderer} is now the only way to
	 * present a buffer, so the seam is enforced rather than merely documented.
	 */
	void drawGraphics(int i, Graphics g, int k)
	{
		if (!(anImage320 instanceof BufferedImage)) {
			method239();
		}
		GlPresent.blit(g, anImage320, k, i);
	}

	public synchronized void addConsumer(ImageConsumer imageconsumer)
	{
		anImageConsumer319 = imageconsumer;
		imageconsumer.setDimensions(anInt316, anInt317);
		imageconsumer.setProperties(null);
		imageconsumer.setColorModel(aColorModel318);
		imageconsumer.setHints(14);
	}

	public synchronized boolean isConsumer(ImageConsumer imageconsumer)
	{
		return anImageConsumer319 == imageconsumer;
	}

	public synchronized void removeConsumer(ImageConsumer imageconsumer)
	{
		if(anImageConsumer319 == imageconsumer)
			anImageConsumer319 = null;
	}

	public void startProduction(ImageConsumer imageconsumer)
	{
		addConsumer(imageconsumer);
	}

	public void requestTopDownLeftRightResend(ImageConsumer imageconsumer)
	{
		System.out.println("TDLR");
	}

	private synchronized void method239()
	{
		if(anImageConsumer319 != null)
		{
			anImageConsumer319.setPixels(0, 0, anInt316, anInt317, aColorModel318, anIntArray315, 0, anInt316);
			anImageConsumer319.imageComplete(2);
		}
	}

	public boolean imageUpdate(Image image, int i, int j, int k, int l, int i1)
	{
		return true;
	}

	public final int[] anIntArray315;
	public final int anInt316;
	public final int anInt317;
	private final ColorModel aColorModel318;
	private ImageConsumer anImageConsumer319;
	private final Image anImage320;
}
