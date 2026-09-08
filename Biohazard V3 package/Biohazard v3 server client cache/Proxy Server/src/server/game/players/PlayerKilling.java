package server.game.players;

public class PlayerKilling {

	private Client c;
	
	/**
	* Constructor class
	*/
	
	public PlayerKilling(Client Client) {
		this.c = Client;
	}
	
	/**
	* How many people you have to kill before getting points again
	* for killing the same person.
	*/
	
	public final int NEEDED_KILLS = 10;
	
	/**
	* First the method checks if the array list contains the person
	* and if it doesn't then add there name but if it does then
	* return the method false.
	*/
	
	public boolean addPlayer(String i) {
		if(!c.killedPlayers.contains(i)) {
			c.killedPlayers.add(i);
			return true;
		}
		return false;
	}
	public static boolean addHostToList(Client client, String host) {
		if(client != null) {
			return client.lastKilledPlayers.add(host);
		}
		return false;
	}
	
	/**
	 * Checks if the host is already on the players array.
	 * @param client Player that is adding the killed players host.
	 * @param host Host address of the killed player.
	 * @return True if the host is on the players array.
	 */
	
	public static boolean hostOnList(Client client, String host) {
		if(client != null) {
			if(client.lastKilledPlayers.indexOf(host) >= KILL_WAIT_MAX) {
				removeHostFromList(client, host);
				return false;
			}
			return client.lastKilledPlayers.contains(host);
		}
		return false;
	}
	
	/**
	 * Removes the host from the players array.
	 * @param client Player that is removing the host.
	 * @param host Host that is being removed.
	 * @return True if host is successfully removed.
	 */
	
	public static boolean removeHostFromList(Client client, String host) {
		if(client != null) {
			return client.lastKilledPlayers.remove(host);
		}
		return false;
	}
	
	/*
	 * Amount of kills you have to wait before the host is deleted.
	 */
	
	public static final int KILL_WAIT_MAX = 3;

	
	/**
	* Checking if the array list contains the player and if
	* the person has killed 20 or more people since that person.
	*/
	
	public void checkForPlayer(String i) {
		if(c.killedPlayers.contains(i) && c.killedPlayers.indexOf(i) >= NEEDED_KILLS) {
			c.killedPlayers.remove(i);
		}
	}

}