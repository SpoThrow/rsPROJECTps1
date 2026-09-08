package server.world;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import server.clip.region.Region;
import server.game.objects.Object;
import server.game.objects.Objects;
import server.game.players.Client;
import server.game.players.Player;
import server.game.players.PlayerHandler;
import core.util.Misc;

/**
 * @author MrClassic
 */

public class ObjectManager {

	public CopyOnWriteArrayList<Object> object = new CopyOnWriteArrayList<Object>();
	//private ArrayList<Object> toRemove = new ArrayList<Object>();
	/*public void process() {
		for (Object o : objects) {
			if (o.tick > 0)
				o.tick--;
			else {
				updateObject(o);
				toRemove.add(o);
			}		
		}
		for (Object o : toRemove) {
			if (isObelisk(o.newId)) {
				int index = getObeliskIndex(o.newId);
				if (activated[index]) {
					activated[index] = false;
					teleportObelisk(index);
				}
			}
			objects.remove(o);	
		}
		toRemove.clear();
	}*/
	
	public void removeObject(int x, int y) {
		for (int j = 0; j < PlayerHandler.players.length; j++) {
			if (PlayerHandler.players[j] != null) {
				Client c = (Client)PlayerHandler.players[j];
				c.getPA().object(-1, x, y, 0, 10);			
			}	
		}	
	}
	
	public void process() {
		Iterator<Object> itr = object.iterator();
		while(itr.hasNext()) {
			Object o = itr.next();
			if(o.tick > 0) {
				o.tick--;
				continue;
			}
			if(o.tick == 0) {
				object.remove(o);
				loadObject(o);
			}
			if((o.getId() == 0 || o.getId() == -1) && o.isFire) {
				object.remove(o);
				loadObject(o);
			}
		}
		
	}
	
	public void addObject(Object o) {
		Object o2 = getObject(o.getX(), o.getY(), o.getHeight());
		if(o2 != null) {
			object.set(object.indexOf(o2), o);
		} else {
			object.add(o);
		}
		loadObject(o);
		Region r = Region.getRegion(o.getX(), o.getY());
		if (r != null)
			r.realObjects.add(new Objects(o.objectId, o.objectX, o.objectY, 0, o.face, o.type));
	}
	
	public void loadObject(Object o) {
		for(Player p : PlayerHandler.players) {
			if(p == null)
				continue;
			Client c = (Client) p;
			if(loadForPlayer(c, o)) {
				c.getPA().object((o.tick > 0) ? o.getNewId() : o.getId(), o.getX(), o.getY(), o.getFace(), o.getType());
				continue;
			}
			if(withinDistance(c, o) && o.tick <= 0)
				c.objectToRemove.add(o);
			
		}
	}
	
	public void loadObjects(Client c) {
		for(Object o : object) {
			if(loadForPlayer(c, o))
				c.getPA().object(o.getNewId(), o.getX(), o.getY(), o.getFace(), o.getType());
		}
		loadCustomSpawns(c);
		for(Object o2 : c.objectToRemove) {
			if(loadForPlayer(c, o2)) {
				c.getPA().object(o2.getNewId(), o2.getX(), o2.getY(), o2.getFace(), o2.getType());
				c.objectToRemove.remove(o2);
			}
		}
	}
	
	public boolean loadForPlayer(Client c, Object o) {
		return c.distanceToPoint(o.getX(), o.getY()) <= 60 && o.getHeight() == c.heightLevel;
	}
	
	public boolean withinDistance(Client c, Object o) {
		return c.distanceToPoint(o.getX(), o.getY()) <= 60;
	}
	
	public Object getObject(int x, int y, int height) {
		for (Object o : object) {
			if (o.objectX == x && o.objectY == y && o.height == height)
				return o;
		}	
		return null;
	}
	
	/**
	 * Removing Minigame Objects
	 * @param objectClass - (1 = CastleWars objects)
	 */
	public void removeGameObjects(int objectClass) {
		for(Object o: object) {
			if(o.objectClass == objectClass)
				object.remove(o);
		}
	}
	
	public void updateObject(Object o) {
		for (int j = 0; j < PlayerHandler.players.length; j++) {
			if (PlayerHandler.players[j] != null) {
				Client c = (Client)PlayerHandler.players[j];
				c.getPA().object(o.newId, o.objectX, o.objectY, o.face, o.type);			
			}	
		}	
	}
	
	public void placeObject(Object o) {
		for (int j = 0; j < PlayerHandler.players.length; j++) {
			if (PlayerHandler.players[j] != null) {
				Client c = (Client)PlayerHandler.players[j];
				if (c.distanceToPoint(o.objectX, o.objectY) <= 60)
					c.getPA().object(o.objectId, o.objectX, o.objectY, o.face, o.type);
			}	
		}
	}
	
	/*public Object getObject(int x, int y, int height) {
		for (Object o : objects) {
			if (o.objectX == x && o.objectY == y && o.height == height)
				return o;
		}	
		return null;
	}
	
	public void loadObjects(Client c) {
		if (c == null)
			return;
		for (Object o : objects) {
			if (loadForPlayer(o,c))
				c.getPA().object(o.objectId, o.objectX, o.objectY, o.face, o.type);
		}
		loadCustomSpawns(c);
		if (c.distanceToPoint(2813, 3463) <= 60) {
			Farming.updateHerbPatch(c);
		}
	}*/
	
	@SuppressWarnings("unused")
	private int[][] customObjects = {{}};
	public void loadCustomSpawns(Client c) {
		c.getPA().checkObjectSpawn(1596, 3008, 3850, 1, 0);
		c.getPA().checkObjectSpawn(1596, 3008, 3849, -1, 0);
		c.getPA().checkObjectSpawn(1596, 3040, 10307, -1, 0);
		c.getPA().checkObjectSpawn(1596, 3040, 10308, 1, 0);
		c.getPA().checkObjectSpawn(1596, 3022, 10311, -1, 0);
		c.getPA().checkObjectSpawn(1596, 3022, 10312, 1, 0);
		c.getPA().checkObjectSpawn(1596, 3044, 10341, -1, 0);
		c.getPA().checkObjectSpawn(1596, 3044, 10342, 1, 0);
		c.getPA().checkObjectSpawn(6552, 3094, 3506, 2, 10);
		c.getPA().checkObjectSpawn(409, 3091, 3506, 2, 10);
		c.getPA().checkObjectSpawn(410, 3101, 3504, 2, 10);
		c.getPA().checkObjectSpawn(2213, 3047, 9779, 1, 10);
		c.getPA().checkObjectSpawn(2213, 3080, 9502, 1, 10);
		c.getPA().checkObjectSpawn(409, 2853, 3348, 1, 10);
		c.getPA().checkObjectSpawn(2, 2862, 3373, 1, 10);
		c.getPA().checkObjectSpawn(2781, 3272, 3185, 1, 10);
		c.getPA().checkObjectSpawn(409, 2797, 2794, 2, 10);
		c.getPA().checkObjectSpawn(2287, 2552, 3559, 1, 10);
		c.getPA().checkObjectSpawn(2823, 2903, 3732, 1, 10); //hole gwd
		c.getPA().checkObjectSpawn(2067, 2894, 3720, 2, 10); //fence gwd
		c.getPA().checkObjectSpawn(2067, 2894, 3721, 2, 10); //fence gwd
		c.getPA().checkObjectSpawn(2067, 2894, 3722, 2, 10); //fence gwd
		c.getPA().checkObjectSpawn(2067, 2894, 3723, 2, 10); //fence gwd
		c.getPA().checkObjectSpawn(2067, 2894, 3724, 2, 10); //fence gwd
		c.getPA().checkObjectSpawn(2067, 2894, 3725, 2, 10); //fence gwd
		c.getPA().checkObjectSpawn(2067, 2908, 3736, 1, 10); //fence gwd
		c.getPA().checkObjectSpawn(2067, 2907, 3736, 1, 10); //fence gwd
		c.getPA().checkObjectSpawn(2067, 2906, 3736, 1, 10); //fence gwd
		c.getPA().checkObjectSpawn(2067, 2905, 3736, 1, 10); //fence gwd
		c.getPA().checkObjectSpawn(2067, 2904, 3736, 1, 10); //fence gwd
		c.getPA().checkObjectSpawn(2067, 2903, 3736, 1, 10); //fence gwd
		c.getPA().checkObjectSpawn(2067, 2902, 3736, 1, 10); //fence gwd
		c.getPA().checkObjectSpawn(2067, 2901, 3736, 1, 10); //fence gwd
		c.getPA().checkObjectSpawn(2067, 2900, 3736, 1, 10); //fence gwd
		c.getPA().checkObjectSpawn(2067, 2899, 3736, 1, 10); //fence gwd
		c.getPA().checkObjectSpawn(2067, 2898, 3736, 1, 10); //fence gwd
		c.getPA().checkObjectSpawn(2067, 2897, 3736, 1, 10); //fence gwd
		c.getPA().checkObjectSpawn(-1, 2543, 10143, 1, 10); //delete dagganoth wall
		c.getPA().checkObjectSpawn(-1, 2545, 10141, 1, 10); //delete dagganoth wall
		c.getPA().checkObjectSpawn(-1, 2545, 10145, 1, 10); //delete dagganoth wall
		c.getPA().checkObjectSpawn(172, 3083, 3506, 2, 10); //crystal key chest
		c.getPA().checkObjectSpawn(3044, 2999, 3140, 2, 10); //smelting furnace
		c.getPA().checkObjectSpawn(2213, 2996, 3139, 0, 10); //bank
		c.getPA().checkObjectSpawn(2213, 3206, 3217, 0, 10); //bank
		c.getPA().checkObjectSpawn(2213, 2933, 3286, 0, 10); //bank
		c.getPA().checkObjectSpawn(2213, 2900, 3428, 0, 10); //bank
		c.getPA().checkObjectSpawn(2213, 2664, 3162, 0, 10); //bank
		c.getPA().checkObjectSpawn(2213, 2897, 4821, 0, 10); //bank
		c.getPA().checkObjectSpawn(2213, 2536, 4717, 0, 10); //bank
		c.getPA().checkObjectSpawn(2213, 2809, 3344, 1, 10); //bank
		c.getPA().checkObjectSpawn(2213, 2329, 3692, 0, 10); //bank
		
		//empty bulding spaces
				//1
				c.getPA().checkObjectSpawn(11214, 2069+156, 3247+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2065+156, 3247+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2061+156, 3247+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2057+156, 3247+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2053+156, 3247+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2049+156, 3247+52, 0, 10);
				//2
				c.getPA().checkObjectSpawn(11214, 2067+156, 3248+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2063+156, 3248+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2059+156, 3248+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2055+156, 3248+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2051+156, 3248+52, 0, 10);
				//1
				c.getPA().checkObjectSpawn(11214, 2069+156, 3249+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2065+156, 3249+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2061+156, 3249+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2057+156, 3249+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2053+156, 3249+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2049+156, 3249+52, 0, 10);
				//2
				c.getPA().checkObjectSpawn(11214, 2067+156, 3250+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2063+156, 3250+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2059+156, 3250+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2055+156, 3250+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2051+156, 3250+52, 0, 10);
				//1
				c.getPA().checkObjectSpawn(11214, 2069+156, 3251+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2065+156, 3251+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2061+156, 3251+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2057+156, 3251+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2053+156, 3251+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2049+156, 3251+52, 0, 10);
				//2
				c.getPA().checkObjectSpawn(11214, 2067+156, 3252+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2063+156, 3252+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2059+156, 3252+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2055+156, 3252+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2051+156, 3252+52, 0, 10);
				//1
				c.getPA().checkObjectSpawn(11214, 2069+156, 3253+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2065+156, 3253+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2061+156, 3253+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2057+156, 3253+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2053+156, 3253+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2049+156, 3253+52, 0, 10);
				//2
				c.getPA().checkObjectSpawn(11214, 2067+156, 3254+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2063+156, 3254+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2059+156, 3254+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2055+156, 3254+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2051+156, 3254+52, 0, 10);
				//1
				c.getPA().checkObjectSpawn(11214, 2069+156, 3255+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2065+156, 3255+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2061+156, 3255+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2057+156, 3255+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2053+156, 3255+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2049+156, 3255+52, 0, 10);
				//2
				c.getPA().checkObjectSpawn(11214, 2067+156, 3256+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2063+156, 3256+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2059+156, 3256+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2055+156, 3256+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2051+156, 3256+52, 0, 10);
				//1
				c.getPA().checkObjectSpawn(11214, 2069+156, 3257+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2065+156, 3257+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2061+156, 3257+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2057+156, 3257+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2053+156, 3257+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2049+156, 3257+52, 0, 10);
				//2
				c.getPA().checkObjectSpawn(11214, 2067+156, 3258+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2063+156, 3258+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2059+156, 3258+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2055+156, 3258+52, 0, 10);
				c.getPA().checkObjectSpawn(11214, 2051+156, 3258+52, 0, 10);
	}
	
	public final int IN_USE_ID = 14825;
	public boolean isObelisk(int id) {
		for (int j = 0; j < obeliskIds.length; j++) {
			if (obeliskIds[j] == id)
				return true;
		}
		return false;
	}
	public int[] obeliskIds = {14829,14830,14827,14828,14826,14831};
	public int[][] obeliskCoords = {{3154,3618},{3225,3665},{3033,3730},{3104,3792},{2978,3864},{3305,3914}};
	public boolean[] activated = {false,false,false,false,false,false};
	
	public void startObelisk(int obeliskId) {
		int index = getObeliskIndex(obeliskId);
		if (index >= 0) {
			if (!activated[index]) {
				activated[index] = true;
				addObject(new Object(14825, obeliskCoords[index][0], obeliskCoords[index][1], 0, -1, 10, obeliskId,16));
				addObject(new Object(14825, obeliskCoords[index][0] + 4, obeliskCoords[index][1], 0, -1, 10, obeliskId,16));
				addObject(new Object(14825, obeliskCoords[index][0], obeliskCoords[index][1] + 4, 0, -1, 10, obeliskId,16));
				addObject(new Object(14825, obeliskCoords[index][0] + 4, obeliskCoords[index][1] + 4, 0, -1, 10, obeliskId,16));
			}
		}	
	}
	
	public int getObeliskIndex(int id) {
		for (int j = 0; j < obeliskIds.length; j++) {
			if (obeliskIds[j] == id)
				return j;
		}
		return -1;
	}
	
	public void teleportObelisk(int port) {
		int random = Misc.random(5);
		while (random == port) {
			random = Misc.random(5);
		}
		for (int j = 0; j < PlayerHandler.players.length; j++) {
			if (PlayerHandler.players[j] != null) {
				Client c = (Client)PlayerHandler.players[j];
				int xOffset = c.absX - obeliskCoords[port][0];
				int yOffset = c.absY - obeliskCoords[port][1];
				if (c.goodDistance(c.getX(), c.getY(), obeliskCoords[port][0] + 2, obeliskCoords[port][1] + 2, 1)) {
					c.getPA().startTeleport2(obeliskCoords[random][0] + xOffset, obeliskCoords[random][1] + yOffset, 0);
				}
			}		
		}
	}
	
	/*public boolean loadForPlayer(Object o, Client c) {
		if (o == null || c == null)
			return false;
		return c.distanceToPoint(o.objectX, o.objectY) <= 60 && c.heightLevel == o.height;
	}
	
	public void addObject(Object o) {
		if (getObject(o.objectX, o.objectY, o.height) == null) {
			objects.add(o);
			placeObject(o);
		}	
	}*/




}