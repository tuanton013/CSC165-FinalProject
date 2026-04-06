package myGame;

import java.util.UUID;
import org.joml.*;
import tage.*;

/**
 * Represents another player's avatar in the game world (a "ghost").
 * Each ghost tracks the remote player's UUID and mirrors their position.
 */
public class GhostAvatar extends GameObject
{
	private UUID id;

	public GhostAvatar(UUID id, ObjShape shape, TextureImage texture, Vector3f position)
	{	super(GameObject.root(), shape, texture);
		this.id = id;
		setLocalLocation(position);
	}

	// ---- accessors ----

	public UUID getID() { return id; }

	public Vector3f getPosition() { return getWorldLocation(); }

	// ---- mutator ----

	public void setPosition(Vector3f position) { setLocalLocation(position); }
}
