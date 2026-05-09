package myGame;

import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import tage.*;
import tage.shapes.AnimatedShape;

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
	 * the remote player selected at startup.  An optional initial rotation
	 * matrix may be supplied (pass {@code null} to use the default facing direction).
	 */
	public void createOrUpdateGhost(UUID id, Vector3f position,
	                                 String modelName, String textureName,
	                                 Matrix4f rotation)
	        throws IOException
	{	GhostAvatar existing = findAvatar(id);
		if (existing != null)
		{	existing.setPosition(position);
			if (rotation != null)
				existing.setLocalRotation(rotation);
			return;
		}

		// Obtain a per-ghost AnimatedShape for animated models, or the shared ObjShape otherwise.
		ObjShape shape;
		AnimatedShape ghostAnim = null;
		if ("HumanFinal".equals(modelName))
		{	ghostAnim = game.getNextGhostHumanShape();
			shape = ghostAnim;
		}
		else if ("newHuman.obj".equals(modelName))
		{	ghostAnim = game.getNextGhostRobotShape();
			shape = ghostAnim;
		}
		else
		{	shape = game.getGhostShape(modelName);
		}

		TextureImage texture = game.getGhostTexture(textureName);
		GhostAvatar  ghost   = new GhostAvatar(id, shape, texture, position);
		ghost.setModelName(modelName);
		if (ghostAnim != null)
			ghost.setAnimShape(ghostAnim);

		// Scale ghost to match the local avatar scale (model-dependent)
		float s = "HumanFinal".equals(modelName) ? 0.01f : 0.2f;
		ghost.setLocalScale(new Matrix4f().scaling(s));

		// Apply rotation: use the supplied rotation if available, otherwise default facing
		if (rotation != null)
			ghost.setLocalRotation(rotation);
		else
			ghost.setLocalRotation(new Matrix4f().rotationY((float) Math.PI));

		// Start robot ghosts in IDLE animation
		if ("newHuman.obj".equals(modelName) && ghostAnim != null)
			ghostAnim.playAnimation("IDLE", 0.2f, tage.shapes.AnimatedShape.EndType.LOOP, 0);

		ghostAvatars.add(ghost);
		System.out.println("Ghost created for " + id);
	}

	/** Overload without rotation – uses default facing direction. */
	public void createOrUpdateGhost(UUID id, Vector3f position,
	                                 String modelName, String textureName)
	        throws IOException
	{	createOrUpdateGhost(id, position, modelName, textureName, null);
	}

	/** Old signature kept for backwards compatibility with lecture sample code. */
	public void createGhost(UUID id, Vector3f position) throws IOException
	{	createOrUpdateGhost(id, position,
			game.getAvatarModelName(), game.getAvatarTextureName(), null);
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

	/** Updates position, rotation, and animation state of an existing ghost. */
	public void updateGhostAvatar(UUID id, Vector3f position, Matrix4f rotation, boolean isMoving)
	{	GhostAvatar ghost = findAvatar(id);
		if (ghost != null)
		{	ghost.setPosition(position);
			if (rotation != null)
				ghost.setLocalRotation(rotation);
			ghost.updateAnimationState(isMoving);
		}
		else
		{	System.out.println("updateGhostAvatar: unable to find ghost " + id);
		}
	}

	/** Legacy overload – updates only the position of an existing ghost. */
	public void updateGhostAvatar(UUID id, Vector3f position)
	{	GhostAvatar ghost = findAvatar(id);
		if (ghost != null)
			ghost.setPosition(position);
		else
			System.out.println("updateGhostAvatar: unable to find ghost " + id);
	}

	/**
	 * Advances the skeletal animation for every ghost that uses an animated model.
	 * Should be called once per frame from {@code MyGame.update()}.
	 */
	public void updateAllAnimations()
	{	for (GhostAvatar g : ghostAvatars)
		{	AnimatedShape anim = g.getAnimShape();
			if (anim != null)
				anim.updateAnimation();
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
