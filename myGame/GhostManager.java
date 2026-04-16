package myGame;

import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import tage.*;

/**
 * Manages the collection of ghost avatars representing remote players.
 *
 * Ghost avatars are created on demand with the model and texture that the
 * remote player chose at start-up, so every client sees the correct avatar
 * for each player.
 */
public class GhostManager
{
	private MyGame game;
	private Vector<GhostAvatar> ghostAvatars = new Vector<GhostAvatar>();

	public GhostManager(VariableFrameRateGame vfrg)
	{	game = (MyGame) vfrg;	}

	// -----------------------------------------------------------------------
	// Public interface
	// -----------------------------------------------------------------------

	/**
	 * Creates a new ghost if none exists for the given UUID, or updates the
	 * position of the existing one.  The model and texture names are those
	 * the remote player selected at startup.
	 */
	public void createOrUpdateGhost(UUID id, Vector3f position,
	                                 String modelName, String textureName)
	        throws IOException
	{	GhostAvatar existing = findAvatar(id);
		if (existing != null)
		{	existing.setPosition(position);
			return;
		}

		ObjShape     shape   = game.getGhostShape(modelName);
		TextureImage texture = game.getGhostTexture(textureName);
		GhostAvatar  ghost   = new GhostAvatar(id, shape, texture, position);

		// Scale ghost to match the local avatar scale
		Matrix4f scale = (new Matrix4f()).scaling(3.0f);
		ghost.setLocalScale(scale);

		ghostAvatars.add(ghost);
		System.out.println("Ghost created for " + id);
	}

	/** Old signature kept for backwards compatibility with lecture sample code. */
	public void createGhost(UUID id, Vector3f position) throws IOException
	{	createOrUpdateGhost(id, position,
			game.getAvatarModelName(), game.getAvatarTextureName());
	}

	/** Removes the ghost avatar for the given remote UUID from the scene. */
	public void removeGhostAvatar(UUID id)
	{	GhostAvatar ghost = findAvatar(id);
		if (ghost != null)
		{	game.getEngine().getSceneGraph().removeGameObject(ghost);
			ghostAvatars.remove(ghost);
			System.out.println("Ghost removed for " + id);
		}
		else
		{	System.out.println("removeGhostAvatar: unable to find ghost " + id);
		}
	}

	/** Updates only the position of an existing ghost. */
	public void updateGhostAvatar(UUID id, Vector3f position)
	{	GhostAvatar ghost = findAvatar(id);
		if (ghost != null)
		{	ghost.setPosition(position);
		}
		else
		{	System.out.println("updateGhostAvatar: unable to find ghost " + id);
		}
	}

	// -----------------------------------------------------------------------
	// Private helpers
	// -----------------------------------------------------------------------

	private GhostAvatar findAvatar(UUID id)
	{	Iterator<GhostAvatar> it = ghostAvatars.iterator();
		while (it.hasNext())
		{	GhostAvatar g = it.next();
			if (g.getID().compareTo(id) == 0)
				return g;
		}
		return null;
	}
}
