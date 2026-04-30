package myGame;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import tage.GameObject;
import tage.ObjShape;
import tage.TextureImage;

/**
 * Client-side scene object that represents the networked NPC.
 */
public class GhostNPC extends GameObject
{
	private int id;

	/**
	 * Constructs a ghost NPC object.
	 *
	 * @param id local NPC identifier
	 * @param shape render shape
	 * @param texture render texture
	 * @param position initial world position
	 */
	public GhostNPC(int id, ObjShape shape, TextureImage texture, Vector3f position)
	{
		super(GameObject.root(), shape, texture);
		this.id = id;
		setPosition(position);
	}

	/**
	 * Returns the local NPC identifier.
	 *
	 * @return NPC ID
	 */
	public int getID()
	{
		return id;
	}

	/**
	 * Updates the NPC world position.
	 *
	 * @param position new world position
	 */
	public void setPosition(Vector3f position)
	{
		setLocalLocation(position);
	}

	/**
	 * Applies visual size state sent by the server.
	 *
	 * @param big true for large scale, false for small scale
	 */
	public void setSize(boolean big)
	{
		if (big)
			setLocalScale((new Matrix4f()).scaling(1.0f));
		else
			setLocalScale((new Matrix4f()).scaling(0.5f));
	}
}
