package server.game.players;

/**
 * Per-player music-unlock state, reached as {@code player.music}.
 *
 * <p>Holds the song-unlock flags. These lived in a {@code public static boolean[]} on
 * {@code Music}, which every player shared: one player unlocking a song lit it up in
 * everybody's music tab, and each save wrote the same shared array back. Song definitions
 * are indexed by {@code Music.array}, up to 383, which is why the array is 384 wide.
 */
public final class MusicState {

	/** Which songs this player has unlocked, indexed by {@code Music.array}. */
	public boolean[] unlocked = new boolean[384];
}
