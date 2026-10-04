// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

package ui;

import java.util.Arrays;import node.NodeSub;



public class DrawingArea extends NodeSub {

	public static void initDrawingArea(int i, int j, int ai[])
	{
		pixels = ai;
		width = j;
		height = i;
		setDrawingArea(i, 0, j, 0);
	}

	public static void defaultDrawingAreaSize()
	{
			topX = 0;
			topY = 0;
			bottomX = width;
			bottomY = height;
			centerX = bottomX;
			centerY = bottomX / 2;
	}

	public static void setDrawingArea(int i, int j, int k, int l)
	{
		if(j < 0)
			j = 0;
		if(l < 0)
			l = 0;
		if(k > width)
			k = width;
		if(i > height)
			i = height;
		topX = j;
		topY = l;
		bottomX = k;
		bottomY = i;
		centerX = bottomX;
		centerY = bottomX / 2;
		anInt1387 = bottomY / 2;
	}

	public static void setAllPixelsToZero()
	{
		setAllPixels(0);
	}

	public static void setAllPixels(int color)
	{
		if (pixels == null) {
			return;
		}
		Arrays.fill(pixels, 0, width * height, color);
	}
	
	public static void drawHorizontalLine(int yPos, int lineColor, int lineWidth, int xPos) {//method339
		if(yPos < topY || yPos >= bottomY)
			return;
		if(xPos < topX) {
			lineWidth -= topX - xPos;
			xPos = topX;
		}
		if(xPos + lineWidth > bottomX)
			lineWidth = bottomX - xPos;
		int i1 = xPos + yPos * width;
		for(int j1 = 0; j1 < lineWidth; j1++)
			pixels[i1 + j1] = lineColor;
	}

	public static void method335(int i, int j, int k, int l, int i1, int k1)
	{
		if(k1 < topX)
		{
			k -= topX - k1;
			k1 = topX;
		}
		if(j < topY)
		{
			l -= topY - j;
			j = topY;
		}
		if(k1 + k > bottomX)
			k = bottomX - k1;
		if(j + l > bottomY)
			l = bottomY - j;
		int l1 = 256 - i1;
		int i2 = (i >> 16 & 0xff) * i1;
		int j2 = (i >> 8 & 0xff) * i1;
		int k2 = (i & 0xff) * i1;
		int k3 = width - k;
		int l3 = k1 + j * width;
		for(int i4 = 0; i4 < l; i4++)
		{
			for(int j4 = -k; j4 < 0; j4++)
			{
				int l2 = (pixels[l3] >> 16 & 0xff) * l1;
				int i3 = (pixels[l3] >> 8 & 0xff) * l1;
				int j3 = (pixels[l3] & 0xff) * l1;
				int k4 = ((i2 + l2 >> 8) << 16) + ((j2 + i3 >> 8) << 8) + (k2 + j3 >> 8);
				pixels[l3++] = k4;
			}

			l3 += k3;
		}
	}

	public static void drawPixels(int i, int j, int k, int l, int i1)
	{
		if(k < topX)
		{
			i1 -= topX - k;
			k = topX;
		}
		if(j < topY)
		{
			i -= topY - j;
			j = topY;
		}
		if(k + i1 > bottomX)
			i1 = bottomX - k;
		if(j + i > bottomY)
			i = bottomY - j;
		int k1 = width - i1;
		int l1 = k + j * width;
		for(int i2 = -i; i2 < 0; i2++)
		{
			for(int j2 = -i1; j2 < 0; j2++)
				pixels[l1++] = l;

			l1 += k1;
		}

	}

	public static void fillPixels(int i, int j, int k, int l, int i1)
	{
		method339(i1, l, j, i);
		method339((i1 + k) - 1, l, j, i);
		method341(i1, l, k, i);
		method341(i1, l, k, (i + j) - 1);
	}

	public static void method338(int i, int j, int k, int l, int i1, int j1)
	{
		method340(l, i1, i, k, j1);
		method340(l, i1, (i + j) - 1, k, j1);
		if(j >= 3)
		{
			method342(l, j1, k, i + 1, j - 2);
			method342(l, (j1 + i1) - 1, k, i + 1, j - 2);
		}
	}

	public static void method339(int i, int j, int k, int l)
	{
		if(i < topY || i >= bottomY)
			return;
		if(l < topX)
		{
			k -= topX - l;
			l = topX;
		}
		if(l + k > bottomX)
			k = bottomX - l;
		int i1 = l + i * width;
		for(int j1 = 0; j1 < k; j1++)
			pixels[i1 + j1] = j;

	}

	private static void method340(int i, int j, int k, int l, int i1)
	{
		if(k < topY || k >= bottomY)
			return;
		if(i1 < topX)
		{
			j -= topX - i1;
			i1 = topX;
		}
		if(i1 + j > bottomX)
			j = bottomX - i1;
		int j1 = 256 - l;
		int k1 = (i >> 16 & 0xff) * l;
		int l1 = (i >> 8 & 0xff) * l;
		int i2 = (i & 0xff) * l;
		int i3 = i1 + k * width;
		for(int j3 = 0; j3 < j; j3++)
		{
			int j2 = (pixels[i3] >> 16 & 0xff) * j1;
			int k2 = (pixels[i3] >> 8 & 0xff) * j1;
			int l2 = (pixels[i3] & 0xff) * j1;
			int k3 = ((k1 + j2 >> 8) << 16) + ((l1 + k2 >> 8) << 8) + (i2 + l2 >> 8);
			pixels[i3++] = k3;
		}

	}

	public static void method341(int i, int j, int k, int l)
	{
		if(l < topX || l >= bottomX)
			return;
		if(i < topY)
		{
			k -= topY - i;
			i = topY;
		}
		if(i + k > bottomY)
			k = bottomY - i;
		int j1 = l + i * width;
		for(int k1 = 0; k1 < k; k1++)
			pixels[j1 + k1 * width] = j;

	}

	private static void method342(int i, int j, int k, int l, int i1) {
		if(j < topX || j >= bottomX)
			return;
		if(l < topY) {
			i1 -= topY - l;
			l = topY;
		}
		if(l + i1 > bottomY)
			i1 = bottomY - l;
		int j1 = 256 - k;
		int k1 = (i >> 16 & 0xff) * k;
		int l1 = (i >> 8 & 0xff) * k;
		int i2 = (i & 0xff) * k;
		int i3 = j + l * width;
		for(int j3 = 0; j3 < i1; j3++) {
			int j2 = (pixels[i3] >> 16 & 0xff) * j1;
			int k2 = (pixels[i3] >> 8 & 0xff) * j1;
			int l2 = (pixels[i3] & 0xff) * j1;
			int k3 = ((k1 + j2 >> 8) << 16) + ((l1 + k2 >> 8) << 8) + (i2 + l2 >> 8);
			pixels[i3] = k3;
			i3 += width;
		}
	}
	
	 public static void method336(int i, int j, int k, int l, int i1)
	    {
	        if(k < topX)
	        {
	            i1 -= topX - k;
	            k = topX;
	        }
	        if(j < topY)
	        {
	            i -= topY - j;
	            j = topY;
	        }
	        if(k + i1 > bottomX)
	            i1 = bottomX - k;
	        if(j + i > bottomY)
	            i = bottomY - j;
	        int k1 = width - i1;
	        int l1 = k + j * width;
	        for(int i2 = -i; i2 < 0; i2++)
	        {
	            for(int j2 = -i1; j2 < 0; j2++)
	                pixels[l1++] = l;

	            l1 += k1;
	        }

	    }

	public static void drawLine(int x1, int y1, int x2, int y2, int color) {
		int dx = x2 - x1;
		if (dx < 0) {
			dx = -dx;
		}
		int dy = y2 - y1;
		if (dy < 0) {
			dy = -dy;
		}
		if (dx > dy) {
			if (x1 > x2) {
				int t = x1;
				x1 = x2;
				x2 = t;
				t = y1;
				y1 = y2;
				y2 = t;
			}
			int y = y1;
			int err = 0;
			int step = y2 >= y1 ? 1 : -1;
			int span = x2 - x1;
			if (span == 0) {
				plot(x1, y1, color);
				return;
			}
			for (int x = x1; x <= x2; x++) {
				plot(x, y, color);
				err += dy;
				if (err + err >= span) {
					y += step;
					err -= span;
				}
			}
		} else {
			if (y1 > y2) {
				int t = x1;
				x1 = x2;
				x2 = t;
				t = y1;
				y1 = y2;
				y2 = t;
			}
			int x = x1;
			int err = 0;
			int step = x2 >= x1 ? 1 : -1;
			int span = y2 - y1;
			if (span == 0) {
				plot(x1, y1, color);
				return;
			}
			for (int y = y1; y <= y2; y++) {
				plot(x, y, color);
				err += dx;
				if (err + err >= span) {
					x += step;
					err -= span;
				}
			}
		}
	}

	public static void blendHLine(int y, int x1, int x2, int color, int alpha) {
		if (y < topY || y >= bottomY || pixels == null) {
			return;
		}
		if (x1 > x2) {
			int t = x1;
			x1 = x2;
			x2 = t;
		}
		if (x1 < topX) {
			x1 = topX;
		}
		if (x2 >= bottomX) {
			x2 = bottomX - 1;
		}
		if (x1 > x2) {
			return;
		}
		int inv = 256 - alpha;
		int sr = (color >> 16 & 0xff) * alpha;
		int sg = (color >> 8 & 0xff) * alpha;
		int sb = (color & 0xff) * alpha;
		int index = x1 + y * width;
		for (int x = x1; x <= x2; x++) {
			if (index >= 0 && index < pixels.length) {
				int dest = pixels[index];
				int r = (sr + (dest >> 16 & 0xff) * inv) >> 8;
				int g = (sg + (dest >> 8 & 0xff) * inv) >> 8;
				int b = (sb + (dest & 0xff) * inv) >> 8;
				pixels[index] = (r << 16) + (g << 8) + b;
			}
			index++;
		}
	}

	private static void plot(int x, int y, int color) {
		if (x < topX || x >= bottomX || y < topY || y >= bottomY || pixels == null) {
			return;
		}
		int i = x + y * width;
		if (i >= 0 && i < pixels.length) {
			pixels[i] = color;
		}
	}

	public DrawingArea() {}

	public static int pixels[];
	public static int width;
	public static int height;
	public static int topY;
	public static int bottomY;
	public static int topX;
	public static int bottomX;
	public static int centerX;
	public static int centerY;
	public static int anInt1387;

}
