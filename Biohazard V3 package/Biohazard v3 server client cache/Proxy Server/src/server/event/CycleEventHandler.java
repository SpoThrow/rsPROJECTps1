package server.event;

import java.util.ArrayList;
import java.util.List;

public class CycleEventHandler {

    private static final List<CycleEventContainer> events = new ArrayList<CycleEventContainer>();

    public static void addEvent(final Object owner, final CycleEvent event, final int cycles) {
        events.add(new CycleEventContainer(owner, event, cycles));
    }
    
	public static void addEvent(int id, Object owner, CycleEvent event, int cycles) {
		events.add(new CycleEventContainer(id, owner, event, cycles));
	}

    public static void addEvent(Object owner, CycleEvent event, int cycles, String name) {
        events.add(new CycleEventContainer(owner, event, cycles, name));
    }

    /**
     * Execute and remove events
     */
    public static void process() {
    		List<CycleEventContainer> eventsCopy = new ArrayList<CycleEventContainer>(events);
    		for (CycleEventContainer c : eventsCopy) {
	            if (!c.isRunning())
	                events.remove(c);
	            if (c.needsExecution() && c.isRunning()) {
	                try {
	                    c.execute();
	                } catch (Exception e) {
	                    e.printStackTrace();
	                    c.setRunning(false);
	                }
	            }
    		}
    }

    /**
     * Returns the amount of events currently running
     *
     * @return amount
     */
    public static int getEventsCount() {
        return events.size();
    }

    /**
     * Stops all events which are being ran by the given owner
     *
     * @param owner
     */
    public static void stopEvents(Object owner) {
        for (CycleEventContainer c : events) {
            if (c.getOwner() == owner) {
                c.stop();
            }
        }
    }
    
	public static void stopEvents(Object owner, int id) {
		for (CycleEventContainer c : events) {
			if(c.getOwner() == owner && id == c.getID()) {
				c.stop();
			}
		}
	}

    /**
     * Stop an event by its name
     *
     * @param name name of the event to stop
     */
    public static void stopEvents(String name) {
        for (CycleEventContainer c : events) {
            if (c.getName() != null && c.getName().equalsIgnoreCase(name)) {
                c.stop();
            }
        }
    }

}