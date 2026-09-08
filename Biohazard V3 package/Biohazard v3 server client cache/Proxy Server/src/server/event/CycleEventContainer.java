package server.event;

import server.game.players.Player;

public class CycleEventContainer {

    /**
     * Event owner
     */
    private final Object owner;

    /**
     * Is the event running or not
     */
    private boolean isRunning;

    /**
     * The amount of cycles per event execution
     */
    private int tick;

    /**
     * The actual event
     */
    private final CycleEvent event;

    /**
     * The current amount of cycles passed
     */
    private int cyclesPassed;

    /**
     * Event name
     */
    private String name;
    
	/**
	 * The event ID
	 */
	private int eventID;

	/**
	 * Sets the event containers details
	 * 
	 * @param owner
	 *            , the owner of the event
	 * @param event
	 *            , the actual event to run
	 * @param tick
	 *            , the cycles between execution of the event
	 */
	public CycleEventContainer(int id, Object owner, CycleEvent event, int tick) {
		this.eventID = id;
		this.owner = owner;
		this.event = event;
		this.isRunning = true;
		this.cyclesPassed = 0;
		this.tick = tick;
	}

    /**
     * Sets the event containers details
     *
     * @param owner , the owner of the event
     * @param event , the actual event to run
     * @param tick  , the cycles between execution of the event
     */
    public CycleEventContainer(final Object owner, final CycleEvent event, final int tick) {
        this.owner = owner;
        this.event = event;
        this.isRunning = true;
        this.cyclesPassed = 0;
        this.tick = tick;
    }

    /**
     * Sets the event containers details
     *
     * @param owner the owner of the event
     * @param event the actual even to run
     * @param tick  the cycles between execution of the event
     * @param name  a name given to the event
     */
    public CycleEventContainer(final Object owner, final CycleEvent event, final int tick, final String name) {
        this.owner = owner;
        this.event = event;
        this.isRunning = true;
        this.cyclesPassed = 0;
        this.tick = tick;
        this.name = name;
    }

    /**
     * Execute the contents of the event
     */
    public void execute() {
        event.execute(this);
    }

    /**
     * Stop the event from running
     */
    public void stop() {
    	if (isRunning()) {
	        isRunning = false;
	        event.stop();
    	}
    }

    /**
     * Does the event need to be ran?
     *
     * @return true yes false no
     */
    public boolean needsExecution() {
        if (++this.cyclesPassed >= tick) {
            this.cyclesPassed = 0;
            return true;
        }
        return false;
    }

    /**
     * Returns the owner of the event
     *
     * @return
     */
    public Object getOwner() {
        return owner;
    }

    /**
     * Returns the owner of this event as a <code>Player</code>
     * @return
     */
    public Player getPlayer() {
        return (Player) owner;
    }

    /**
     * Is the event running?
     *
     * @return true yes false no
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Set if or not this event is running
     *
     * @param running
     */
    public void setRunning(boolean running) {
        this.isRunning = running;
    }

    /**
     * Set the amount of cycles between the execution
     *
     * @param tick
     */
    public void setTick(final int tick) {
        this.tick = tick;
    }

    /**
     * Get the event name
     *
     * @return
     */
    public String getName() {
        return name;
    }
    
	/**
	 * Returns the event id
	 *
	 * @return id
	 */
	public int getID() {
		return eventID;
	}
}