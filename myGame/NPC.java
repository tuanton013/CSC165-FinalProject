package myGame;

/**
 * Simple server-side NPC data model.
 */
public class NPC
{
	private double locationX;
	private double locationY;
	private double locationZ;
	private double dir = 0.1;
	private double size = 1.0;

	/**
	 * Constructs a default NPC at the origin.
	 */
	public NPC()
	{
		locationX = 0.0;
		locationY = 0.0;
		locationZ = 0.0;
	}

	/**
	 * Places the NPC in the maze entrance corridor.
	 * The maze sits at world Y=15.0; the entrance corridor runs along Z
	 * from roughly 9.8 (start) toward the center, so the NPC is placed
	 * a few units ahead of the player spawn to be encountered early.
	 * The seed parameters are intentionally unused but kept for compatibility.
	 *
	 * @param seedX unused – retained for API compatibility
	 * @param seedZ unused – retained for API compatibility
	 */
	public void randomizeLocation(int seedX, int seedZ)
	{
		// Fixed position: maze center X, maze floor Y, halfway down entrance corridor
		locationX = 0.16;
		locationY = 15.0;
		locationZ = 5.0;
	}

	/** @return NPC world X position */
	public double getX() { return locationX; }
	/** @return NPC world Y position */
	public double getY() { return locationY; }
	/** @return NPC world Z position */
	public double getZ() { return locationZ; }

	/** Sets NPC visual size to the large state. */
	public void getBig() { size = 2.0; }
	/** Sets NPC visual size to the normal/small state. */
	public void getSmall() { size = 1.0; }
	/** @return current NPC size scale flag value */
	public double getSize() { return size; }

	/**
	 * Updates NPC movement each tick using Z-axis patrol along the entrance corridor.
	 * Bounces between Z=1.5 (near maze midpoint) and Z=8.5 (near player spawn).
	 */
	public void updateLocation()
	{
		if (locationZ > 8.5) dir = -0.1;
		if (locationZ < 1.5) dir = 0.1;
		locationZ += dir;
	}
}
