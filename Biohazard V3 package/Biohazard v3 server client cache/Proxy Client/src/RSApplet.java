import java.applet.Applet;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class RSApplet extends Applet implements Runnable, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener, FocusListener, WindowListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1473917011474991756L;
	final void createClientFrame(int i, int j) {
		myWidth = j;
		myHeight = i;
		gameFrame = new RSFrame(this, myWidth, myHeight);
		graphics = getGameComponent().getGraphics();
		fullGameScreen = new RSImageProducer(myWidth, myHeight, getGameComponent());
		startRunnable(this, 1);
	}

	final void initClientFrame(int i, int j) {
		myWidth = j;
		myHeight = i;
		graphics = getGameComponent().getGraphics();
		fullGameScreen = new RSImageProducer(myWidth, myHeight, getGameComponent());
		startRunnable(this, 1);
	}

	public void mouseWheelMoved(MouseWheelEvent event) {
		int rotation = event.getWheelRotation();
		if (client.instance == null) {
			return;
		}
		int chatX = client.instance.chatDrawX();
		int chatY = client.instance.chatDrawY();
		int chatW = client.instance.chatWidth();
		if (mouseX >= chatX && mouseX < chatX + chatW && mouseY >= chatY && mouseY < chatY + 165) {
			int scrollPos = client.anInt1089;
			scrollPos -= rotation * 30;
			if (scrollPos < 0) {
				scrollPos = 0;
			}
			if (scrollPos > client.anInt1211 - 110) {
				scrollPos = client.anInt1211 - 110;
			}
			if (client.anInt1089 != scrollPos) {
				client.anInt1089 = scrollPos;
				client.inputTaken = true;
			}
			return;
		}
		if (handleInterfaceScrolling(event)) {
			return;
		}
		if (mouseX >= client.instance.tabDrawX() && mouseY >= client.instance.tabDrawY()) {
			return;
		}
		if (overOpenInterface()) {
			return;
		}
		client.adjustCameraZoom(rotation);
	}

	public boolean handleInterfaceScrolling(MouseWheelEvent event) {
		int rotation = event.getWheelRotation();
		int tabInterfaceID = client.tabInterfaceIDs[client.tabID];
		if (tabInterfaceID != -1 && RSInterface.interfaceCache != null && tabInterfaceID < RSInterface.interfaceCache.length) {
			RSInterface tab = RSInterface.interfaceCache[tabInterfaceID];
			int offsetX = client.instance != null ? client.instance.tabDrawX() : 553;
			int offsetY = client.instance != null ? client.instance.tabDrawY() : 205;
			if (scrollInterfaceTree(tab, offsetX, offsetY, rotation)) {
				client.tabAreaAltered = true;
				client.needDrawTabArea = true;
				return true;
			}
		}
		if (client.openInterfaceID != -1 && RSInterface.interfaceCache != null
				&& client.openInterfaceID < RSInterface.interfaceCache.length) {
			RSInterface rsi = RSInterface.interfaceCache[client.openInterfaceID];
			int offsetX = client.instance != null ? client.instance.interfaceMenuX() : 4;
			int offsetY = client.instance != null ? client.instance.interfaceMenuY() : 4;
			if (scrollInterfaceTree(rsi, offsetX, offsetY, rotation)) {
				return true;
			}
		}
		return false;
	}

	private boolean overOpenInterface() {
		if (client.openInterfaceID == -1 || client.instance == null || RSInterface.interfaceCache == null) {
			return false;
		}
		if (client.openInterfaceID < 0 || client.openInterfaceID >= RSInterface.interfaceCache.length) {
			return false;
		}
		RSInterface rsi = RSInterface.interfaceCache[client.openInterfaceID];
		if (rsi == null) {
			return false;
		}
		int x = client.instance.interfaceMenuX();
		int y = client.instance.interfaceMenuY();
		int w = rsi.width > 0 ? rsi.width : 512;
		int h = rsi.height > 0 ? rsi.height : 334;
		return mouseX >= x && mouseY >= y && mouseX < x + w && mouseY < y + h;
	}

	private boolean scrollInterfaceTree(RSInterface rsi, int offsetX, int offsetY, int rotation) {
		if (rsi == null) {
			return false;
		}
		if (rsi.scrollMax > 0 && mouseOver(offsetX, offsetY, rsi.width, rsi.height)) {
			int max = rsi.scrollMax - rsi.height;
			if (max < 0) {
				max = rsi.scrollMax;
			}
			rsi.scrollPosition += rotation * 30;
			if (rsi.scrollPosition < 0) {
				rsi.scrollPosition = 0;
			}
			if (rsi.scrollPosition > max) {
				rsi.scrollPosition = max;
			}
			return true;
		}
		if (rsi.children == null) {
			return false;
		}
		for (int index = 0; index < rsi.children.length; index++) {
			int childId = rsi.children[index];
			if (childId < 0 || RSInterface.interfaceCache == null || childId >= RSInterface.interfaceCache.length) {
				continue;
			}
			RSInterface child = RSInterface.interfaceCache[childId];
			if (child == null) {
				continue;
			}
			int childX = offsetX + rsi.childX[index];
			int childY = offsetY + rsi.childY[index];
			if (rsi.scrollMax > 0) {
				childY -= rsi.scrollPosition;
			}
			if (scrollInterfaceTree(child, childX, childY, rotation)) {
				return true;
			}
		}
		return false;
	}

	private boolean mouseOver(int x, int y, int width, int height) {
		return mouseX > x && mouseY > y && mouseX < x + width && mouseY < y + height;
	}

	public void run()
	{
		getGameComponent().addMouseListener(this);
		getGameComponent().addMouseMotionListener(this);
		getGameComponent().addKeyListener(this);
		getGameComponent().addFocusListener(this);
		getGameComponent().addMouseWheelListener(this);
		if(gameFrame != null)
			gameFrame.addWindowListener(this);
		getGameComponent().setFocusable(true);
		getGameComponent().requestFocus();
		//drawLoadingText(0, "Loading...");
		startUp();
		int i = 0;
		int j = 256;
		int k = 1;
		int i1 = 0;
		int j1 = 0;
		for(int k1 = 0; k1 < 10; k1++)
			aLongArray7[k1] = System.currentTimeMillis();

		System.currentTimeMillis();
		long lastLogicTime = System.currentTimeMillis();
		int fpsFrames = 0;
		long fpsSecond = lastLogicTime;
		while(anInt4 >= 0) 
		{
			if(anInt4 > 0)
			{
				anInt4--;
				if(anInt4 == 0)
				{
					exit();
					return;
				}
			}
			if (client.fpsUnlocked) {
				long now = System.currentTimeMillis();
				int ticks = 0;
				while (now - lastLogicTime >= 20L && ticks < 8) {
					clickMode3 = clickMode1;
					saveClickX = clickX;
					saveClickY = clickY;
					aLong29 = clickTime;
					clickMode1 = 0;
					processGameLoop();
					readIndex = writeIndex;
					lastLogicTime += 20L;
					ticks++;
					now = System.currentTimeMillis();
				}
				if (ticks >= 8) {
					lastLogicTime = now;
				}
				long drawStart = now;
				processDrawing();
				long after = System.currentTimeMillis();
				fpsFrames++;
				lastFrameMs = (int) (after - drawStart);
				if (after - fpsSecond >= 1000L) {
					fps = fpsFrames;
					if (fps < fpsMin) {
						fpsMin = fps;
					}
					if (fps > fpsMax) {
						fpsMax = fps;
					}
					fpsFrames = 0;
					fpsSecond = after;
				}
				if (after - fpsWindowAt > 5000L) {
					fpsMin = fps;
					fpsMax = fps;
					fpsWindowAt = after;
				}
				continue;
			}
			lastLogicTime = System.currentTimeMillis();
			int i2 = j;
			int j2 = k;
			j = 300;
			k = 1;
			long l1 = System.currentTimeMillis();
			if(aLongArray7[i] == 0L)
			{
				j = i2;
				k = j2;
			} else
			if(l1 > aLongArray7[i])
				j = (int)((long)(2560 * delayTime) / (l1 - aLongArray7[i]));
			if(j < 25)
				j = 25;
			if(j > 256)
			{
				j = 256;
				k = (int)((long) delayTime - (l1 - aLongArray7[i]) / 10L);
			}
			if(k > delayTime)
				k = delayTime;
			aLongArray7[i] = l1;
			i = (i + 1) % 10;
			if(k > 1)
			{
				for(int k2 = 0; k2 < 10; k2++)
					if(aLongArray7[k2] != 0L)
						aLongArray7[k2] += k;

			}
			if(k < minDelay)
				k = minDelay;
			try
			{
				Thread.sleep(k);
			}
			catch(InterruptedException _ex)
			{
				j1++;
			}
			for(; i1 < 256; i1 += j)
			{
				clickMode3 = clickMode1;
				saveClickX = clickX;
				saveClickY = clickY;
				aLong29 = clickTime;
				clickMode1 = 0;
				processGameLoop();
				readIndex = writeIndex;
			}

			i1 &= 0xff;
			if(delayTime > 0)
				fps = (1000 * j) / (delayTime * 256);
			if (fps < fpsMin)
				fpsMin = fps;
			if (fps > fpsMax)
				fpsMax = fps;
			if (System.currentTimeMillis() - fpsWindowAt > 5000L) {
				fpsMin = fps;
				fpsMax = fps;
				fpsWindowAt = System.currentTimeMillis();
			}
			lastFrameMs = k;
			processDrawing();
			if(shouldDebug)
			{
				System.out.println("ntime:" + l1);
				for(int l2 = 0; l2 < 10; l2++)
				{
					int i3 = ((i - l2 - 1) + 20) % 10;
					System.out.println("otim" + i3 + ":" + aLongArray7[i3]);
				}

				System.out.println("fps:" + fps + " ratio:" + j + " count:" + i1);
				System.out.println("del:" + k + " deltime:" + delayTime + " mindel:" + minDelay);
				System.out.println("intex:" + j1 + " opos:" + i);
				shouldDebug = false;
				j1 = 0;
			}
		}
		if(anInt4 == -1)
			exit();
	}

	
	
	private void exit()
	{
		anInt4 = -2;
		cleanUpForQuit();
		if(gameFrame != null)
		{
			try
			{
				Thread.sleep(1000L);
			}
			catch(Exception _ex) { }
			try
			{
				System.exit(0);
			}
			catch(Throwable _ex) { }
		}
	}

	final void method4(int i)
	{
			delayTime = 1000 / i;
	}

	public final void start()
	{
		if(anInt4 >= 0)
			anInt4 = 0;
	}

	public final void stop()
	{
		if(anInt4 >= 0)
			anInt4 = 4000 / delayTime;
	}

	public final void destroy()
	{
		anInt4 = -1;
		try
		{
			Thread.sleep(5000L);
		}
		catch(Exception _ex) { }
		if(anInt4 == -1)
			exit();
	}

	public final void update(Graphics g)
	{
		if(graphics == null)
			graphics = g;
		shouldClearScreen = true;
		raiseWelcomeScreen();
	}

	public final void paint(Graphics g)
	{
		if(graphics == null)
			graphics = g;
		shouldClearScreen = true;
		raiseWelcomeScreen();
	}

	public final void mousePressed(MouseEvent mouseevent)
	{
		int i = mouseevent.getX();
		int j = mouseevent.getY();
		shiftIsDown = mouseevent.isShiftDown();
		if(gameFrame != null)
		{
			i -= 4;//4
			j -= 22;//22
		}
		idleTime = 0;
		clickX = i;
		clickY = j;
		clickTime = System.currentTimeMillis();
		int button = mouseevent.getButton();
		if(button == MouseEvent.BUTTON1)
		{
			clickMode1 = 1;
			clickMode2 = 1;
		} else if(button == MouseEvent.BUTTON2)
		{
			mouseWheelDown = true;
			mouseWheelX = i;
			mouseWheelY = j;
			middleClickDragged = false;
		} else if(button == MouseEvent.BUTTON3)
		{
			clickMode1 = 2;
			clickMode2 = 2;
		} else if(mouseevent.isMetaDown())
		{
			clickMode1 = 2;
			clickMode2 = 2;
		} else
		{
			clickMode1 = 1;
			clickMode2 = 1;
		}
		mouseevent.consume();
	}

	public final void mouseReleased(MouseEvent mouseevent)
	{
		idleTime = 0;
		int button = mouseevent.getButton();
		if(button == MouseEvent.BUTTON1)
		{
			if(clickMode2 == 1)
				clickMode2 = 0;
		} else if(button == MouseEvent.BUTTON3)
		{
			if(clickMode2 == 2)
				clickMode2 = 0;
		} else if(button == MouseEvent.BUTTON2)
		{
			if (!middleClickDragged) {
				middleClickQueued = true;
			}
			mouseWheelDown = false;
		} else
		{
			clickMode2 = 0;
			mouseWheelDown = false;
		}
	}

	public final void mouseClicked(MouseEvent mouseevent)
	{
	}

	public final void mouseEntered(MouseEvent mouseevent)
	{
	}

	public final void mouseExited(MouseEvent mouseevent)
	{
		idleTime = 0;
		mouseX = -1;
		mouseY = -1;
	}
 public final void mouseDragged(MouseEvent mouseevent)
    {
        int i = mouseevent.getX();
        int j = mouseevent.getY();
        if(gameFrame != null)
        {
            i -= 4;
            j -= 22;
        }
        if (mouseWheelDown) {
            int dx = mouseWheelX - i;
            int dy = mouseWheelY - j;
            if (Math.abs(dx) > 4 || Math.abs(dy) > 4) {
                middleClickDragged = true;
            }
            mouseWheelDragged(dx, -dy);
            mouseWheelX = i;
            mouseWheelY = j;
            return;
        }
        if (System.currentTimeMillis() - clickTime >= 250L
            || Math.abs(saveClickX - i) > 5 || Math.abs(saveClickY - j) > 5) {
            idleTime = 0;
            mouseX = i;
            mouseY = j;
        }
    }

	void mouseWheelDragged(int i, int j) {
	}
 public final void mouseMoved(MouseEvent mouseevent)
    {
        int i = mouseevent.getX();
        int j = mouseevent.getY();
        if(gameFrame != null)
        {
            i -= 4;
            j -= 22;
        }
      if (System.currentTimeMillis() - clickTime >= 250L
            || Math.abs(saveClickX - i) > 5 || Math.abs(saveClickY - j) > 5) {
            idleTime = 0;
            mouseX = i;
            mouseY = j;
      }
    }
	public static int hotKey = 508;
	public final void keyPressed(KeyEvent keyevent)
	{
		idleTime = 0;
		int i = keyevent.getKeyCode();
		int j = keyevent.getKeyChar();
		if (i == KeyEvent.VK_SHIFT) {
			shiftIsDown = true;
		}
		if (i == KeyEvent.VK_PRINTSCREEN) {
			Jframe.takeScreenshot(client.silentScreenshots);
			return;
		}
		if (KeyRemapper.isCapturing()) {
			KeyRemapper.captureKey(i);
			return;
		}
		boolean remap = client.keyRemapping;
		boolean typing = client.chatTypeFocused || (client.instance != null && client.instance.isTypingOverlay());
		if (remap && !typing) {
			int tab = KeyRemapper.tabForKey(i);
			if (tab >= 0) {
				client.setTab(tab);
				return;
			}
		}
		if (!remap) {
		    if (i == KeyEvent.VK_ESCAPE) {
			client.setTab(3);
		    }
		    if (i == KeyEvent.VK_F1) {
			client.setTab(0);
		    }
		    if (i == KeyEvent.VK_F2) {
			client.setTab(1);
		    }
		    if (i == KeyEvent.VK_F3) {
			client.setTab(2);
		    }
		    if (i == KeyEvent.VK_F4) {
			client.setTab(4);
		    }
		    if (i == KeyEvent.VK_F5) {
			client.setTab(5);
		    }
		    if (i == KeyEvent.VK_F6) {
			client.setTab(6);
		    }
		    if (i == KeyEvent.VK_F7) {
			client.setTab(7);
		    }
		    if (i == KeyEvent.VK_F8) {
			client.setTab(8);
		    }
		    if (i == KeyEvent.VK_F9) {
			client.setTab(9);
		    }
		    if (i == KeyEvent.VK_F10) {
			client.setTab(11);
		    }
		    if (i == KeyEvent.VK_F11) {
			client.setTab(12);
		    }
		if (i == KeyEvent.VK_F12) {
			client.setTab(13);
		    }
		}
		if(j < 30)
			j = 0;
		if(i == 37)
			j = 1;
		if(i == 39)
			j = 2;
		if(i == 38)
			j = 3;
		if(i == 40)
			j = 4;
		if (remap && client.wasdCamera && !typing) {
			if (i == KeyEvent.VK_A)
				j = 1;
			else if (i == KeyEvent.VK_D)
				j = 2;
			else if (i == KeyEvent.VK_W)
				j = 3;
			else if (i == KeyEvent.VK_S)
				j = 4;
		}
		if(i == 17)
			j = 5;
		if(i == 8)
			j = 8;
		if(i == 127)
			j = 8;
		if(i == 9)
			j = 9;
		if(i == 10)
			j = 10;
		if(i >= 112 && i <= 123)
			j = (1008 + i) - 112;
		if(i == 36)
			j = 1000;
		if(i == 35)
			j = 1001;
		if(i == 33)
			j = 1002;
		if(i == 34)
			j = 1003;
		if(j > 0 && j < 128)
			keyArray[j] = 1;
		if(j > 4)
		{
			charQueue[writeIndex] = j;
			writeIndex = writeIndex + 1 & 0x7f;
		}
	}

	public final void keyReleased(KeyEvent keyevent)
	{
		idleTime = 0;
		int i = keyevent.getKeyCode();
		char c = keyevent.getKeyChar();
		if (i == KeyEvent.VK_SHIFT) {
			shiftIsDown = false;
		}
		if(c < '\036')
			c = '\0';
		if(i == 37)
			c = '\001';
		if(i == 39)
			c = '\002';
		if(i == 38)
			c = '\003';
		if(i == 40)
			c = '\004';
		if(client.keyRemapping && client.wasdCamera) {
			if(i == KeyEvent.VK_A)
				c = '\001';
			if(i == KeyEvent.VK_D)
				c = '\002';
			if(i == KeyEvent.VK_W)
				c = '\003';
			if(i == KeyEvent.VK_S)
				c = '\004';
		}
		if(i == 17)
			c = '\005';
		if(i == 8)
			c = '\b';
		if(i == 127)
			c = '\b';
		if(i == 9)
			c = '\t';
		if(i == 10)
			c = '\n';
		if(c > 0 && c < '\200')
			keyArray[c] = 0;
	}

	public final void keyTyped(KeyEvent keyevent)
	{
	}

	final int readChar(int dummy)
	{
		while(dummy >= 0)
		{
			for(int j = 1; j > 0; j++);
		}
		int k = -1;
		if(writeIndex != readIndex)
		{
			k = charQueue[readIndex];
			readIndex = readIndex + 1 & 0x7f;
		}
		return k;
	}

	public final void focusGained(FocusEvent focusevent)
	{
		awtFocus = true;
		shouldClearScreen = true;
		raiseWelcomeScreen();
	}

	public final void focusLost(FocusEvent focusevent)
	{
		awtFocus = false;
		for(int i = 0; i < 128; i++)
			keyArray[i] = 0;

	}

	public final void windowActivated(WindowEvent windowevent)
	{
	}

	public final void windowClosed(WindowEvent windowevent)
	{
	}

	public final void windowClosing(WindowEvent windowevent)
	{
		destroy();
	}

	public final void windowDeactivated(WindowEvent windowevent)
	{
	}

	public final void windowDeiconified(WindowEvent windowevent)
	{
	}

	public final void windowIconified(WindowEvent windowevent)
	{
	}

	public final void windowOpened(WindowEvent windowevent)
	{
	}

	void startUp()
	{
	}

	void processGameLoop()
	{
	}

	void cleanUpForQuit()
	{
	}

	void processDrawing()
	{
	}

	void raiseWelcomeScreen()
	{
	}

	Component getGameComponent()
	{
		if(gameFrame != null)
			return gameFrame;
		else
			return this;
	}

	public void startRunnable(Runnable runnable, int priority)
	{
		Thread thread = new Thread(runnable);
		thread.start();
		thread.setPriority(priority);
	}

	void drawLoadingText(int i, String s)
	{
		while(graphics == null)
		{
			graphics = getGameComponent().getGraphics();
			try
			{
				getGameComponent().repaint();
			}
			catch(Exception _ex) { }
			try
			{
				Thread.sleep(1000L);
			}
			catch(Exception _ex) { }
		}
		Font font = new Font("Helvetica", 1, 13);
		FontMetrics fontmetrics = getGameComponent().getFontMetrics(font);
		Font font1 = new Font("Helvetica", 0, 13);
		getGameComponent().getFontMetrics(font1);
		if(shouldClearScreen)
		{
			graphics.setColor(Color.black);
			graphics.fillRect(0, 0, myWidth, myHeight);
			shouldClearScreen = false;
		}
		Color color = new Color(140, 17, 17);
		int j = myHeight / 2 - 18;
		graphics.setColor(color);
		graphics.drawRect(myWidth / 2 - 152, j, 304, 34);
		graphics.fillRect(myWidth / 2 - 150, j + 2, i * 3, 30);
		graphics.setColor(Color.black);
			graphics.fillRect((myWidth / 2 - 150) + i * 3, j + 2, 300 - i * 3, 30);
			graphics.setFont(font);
			graphics.setColor(Color.white);
			graphics.drawString(s, (myWidth - fontmetrics.stringWidth(s)) / 2, j + 22);
	}

	RSApplet()
	{
		delayTime = 20;
		minDelay = 1;
		aLongArray7 = new long[10];
		shouldDebug = false;
		shouldClearScreen = true;
		awtFocus = true;
		keyArray = new int[128];
		charQueue = new int[128];
		fpsMin = 999;
		fpsMax = 0;
	}

	private int anInt4;
	private int delayTime;
	int minDelay;
	private final long[] aLongArray7;
	int fps;
	int fpsMin;
	int fpsMax;
	int lastFrameMs;
	long fpsWindowAt;
	static boolean shiftIsDown;
	boolean shouldDebug;
	int myWidth;
	int myHeight;
	Graphics graphics;
	RSImageProducer fullGameScreen;
	RSFrame gameFrame;
	private boolean shouldClearScreen;
	boolean awtFocus;
	int idleTime;
	int clickMode2;
	public int mouseX;
	public int mouseY;
	private int clickMode1;
	private int clickX;
	private int clickY;
	private long clickTime;
	int clickMode3;
	int saveClickX;
	int saveClickY;
	long aLong29;
	boolean mouseWheelDown;
	int mouseWheelX;
	int mouseWheelY;
	boolean middleClickQueued;
	boolean middleClickDragged;
	final int[] keyArray;
	private final int[] charQueue;
	private int readIndex;
	private int writeIndex;
	public static int anInt34;
}
