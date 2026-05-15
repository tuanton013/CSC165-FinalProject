package myGame;

import java.util.UUID;
import org.joml.*;
import tage.*;
import tage.shapes.AnimatedShape;
import tage.shapes.AnimatedShape.EndType;

/**
 * Represents another player's avatar in the game world (a "ghost").
 * Each ghost tracks the remote player's UUID and mirrors their position,
 * rotation, and animation state.
 */
public class GhostAvatar extends GameObject
{
	private UUID id;
	private AnimatedShape animShape;
	private boolean wasMoving = false;
	private String modelName;
	private static final float GHOST_WALK_ANIM_SPEED = 0.85f;

	public GhostAvatar(UUID id, ObjShape shape, TextureImage texture, Vector3f position)
	{	super(GameObject.root(), shape, texture);
		this.id = id;
		setLocalLocation(position);
	}

	// ---- accessors ----

	public UUID getID() { return id; }

	public Vector3f getPosition() { return getWorldLocation(); }

	public AnimatedShape getAnimShape() { return animShape; }

	public String getModelName() { return modelName; }

	// ---- mutators ----

	public void setPosition(Vector3f position) { setLocalLocation(position); }

	public void setAnimShape(AnimatedShape anim) { this.animShape = anim; }

	public void setModelName(String name) { this.modelName = name; }

	/**
	 * Starts or stops the ghost's animation based on whether it is moving.
	 * Should be called each time a move update is received for this ghost.
	 */
	public void updateAnimationState(boolean isMoving)
	{	if (animShape == null) return;
		if (isMoving && !wasMoving)
		{	animShape.stopAnimation();
			animShape.playAnimation("WALK", GHOST_WALK_ANIM_SPEED, EndType.LOOP, 0);
		}
		else if (!isMoving && wasMoving)
		{	animShape.stopAnimation();
			if ("newHuman.obj".equals(modelName))
				animShape.playAnimation("IDLE", 0.2f, EndType.LOOP, 0);
		}
		wasMoving = isMoving;
	}
}
