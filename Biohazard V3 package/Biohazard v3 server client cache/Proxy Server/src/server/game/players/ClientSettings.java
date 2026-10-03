package server.game.players;

/**
 * Per-player client sound and display preferences.
 *
 * <p>Six fields that used to sit at the very top of {@link Player}. They are grouped here
 * because they are the settings the client's config frames (166 brightness, 168 music,
 * 169 sound) and the settings sliders read and write, and nothing else touches them.
 * Reached as {@code player.settings}.
 *
 * <p><b>The leaf names were deliberately NOT renamed</b>, unlike the POS and smelting
 * clusters. Four of the six ({@code musicVolume}, {@code soundEffectVolume},
 * {@code musicEnabled}, {@code brightness}) are also the literal save-file keys in
 * {@code PlayerSave}, so keeping the Java names identical means the file and the code read
 * the same way and a future reader can compare them at a glance. The other two
 * ({@code isLoopingMusic}, {@code auto}) carry no redundant prefix worth stripping once
 * they sit under {@code settings}.
 *
 * <p>This is a deliberate data bag -- the fields stay public, exactly as they were on
 * {@link Player}. Extracting the cluster is one step; encapsulating it is another, so
 * that a behaviour change cannot hide inside a mechanical move.
 */
public final class ClientSettings {

	/**
	 * Music is looping rather than playing once. Toggled by button 38197 in the music tab.
	 * <em>Not persisted</em> -- it resets to {@code true} on every login.
	 */
	public boolean isLoopingMusic = true;

	/**
	 * Music auto-play mode: {@code 1} = AUTO, {@code 0} = MANUAL (the comment on the old
	 * declaration said so, and {@code MusicTab.setToManual} sends {@code 0}). Read by
	 * {@code Music.playMusic}, which returns immediately when it is {@code 0}.
	 * <em>Not persisted</em> -- it resets to {@code 1} on every login.
	 */
	public int auto = 1;

	/** 0-4, where 0 is loudest (matches client config 168). Persisted as {@code musicVolume}. */
	public int musicVolume = 0;

	/** 0-4, where 0 is loudest (matches client config 169). Persisted as {@code soundEffectVolume}. */
	public int soundEffectVolume = 0;

	/** Whether music is on at all. Persisted as {@code musicEnabled}. */
	public boolean musicEnabled = true;

	/** Screen brightness, 1-4. Persisted as {@code brightness}. */
	public int brightness = 3;
}
