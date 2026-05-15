package myGame;

import tage.input.action.AbstractInputAction;
import tage.shapes.AnimatedShape;
import org.joml.Vector3f;
import org.joml.Vector4f;
import net.java.games.input.Event;
import net.java.games.input.Component;
import org.joml.*;

/**
 * Moves the player avatar along its local Z axis and broadcasts the new
 * position to all other connected players.
 *
 * Pass {@code +1} for forward (W) or {@code -1} for backward (S).
 * Additional directions can follow the same pattern with different axes.
 */
public class MoveAction extends AbstractInputAction
{
	private MyGame         game;
	private ProtocolClient protClient;
	private float          direction;   // +1 = forward, -1 = backward
	private static final float MOVE_SPEED_UNITS_PER_SEC = 3f;

	public MoveAction(MyGame game, ProtocolClient protClient, float direction)
	{	this.game       = game;
		this.protClient = protClient;
		this.direction  = direction;
	}

	@Override
	public void performAction(float time, Event e)
	{	var av          = game.getAvatar();
		float deltaSeconds = time;
		if (deltaSeconds < 0.0f) deltaSeconds = 0.0f;
		if (deltaSeconds > 0.1f) deltaSeconds = 0.1f;
		
		float speedMultiplier = 1f;
		// Check if this is a gamepad axis event and handle animation + speed scaling
		if (e != null && e.getComponent() != null)
		{	Component.Identifier comp = e.getComponent().getIdentifier();
			if (comp instanceof Component.Identifier.Axis)
			{	float rawAxis = e.getValue();
				float absAxis = java.lang.Math.abs(rawAxis);
				// Preserve sign so forward/backward both work; deadzone via abs
				if (absAxis > 0.1f)
					speedMultiplier = rawAxis;
				else
					speedMultiplier = 0f;
				
				// Handle animation based on axis state (abs value)
				handleGamepadAnimation(absAxis);
			}
		}
		
		// Snapshot the pre-move position as a NEW vector – JOML's add() mutates in place,
		// so we must not reuse the same object for oldPosition and newPosition.
		Vector3f oldPosition = new Vector3f(av.getWorldLocation());
		var moveDir = new Vector4f(0f, 0f, 1f, 1f);
		moveDir.mul(av.getWorldRotation());
		moveDir.mul(direction * MOVE_SPEED_UNITS_PER_SEC * deltaSeconds * getSpeed() * speedMultiplier * game.getDangerSpeedMultiplier());
		// Build proposed position in a fresh vector, leaving oldPosition untouched
		Vector3f newPosition = new Vector3f(oldPosition)
				.add(moveDir.x(), moveDir.y(), moveDir.z());
		av.setLocalLocation(newPosition);

		if (protClient != null && game.getIsConnected())
			protClient.sendMoveMessage(av.getWorldLocation(), av.getWorldRotation(), true);
	}
	
	private void handleGamepadAnimation(float axisValue)
	{	AnimatedShape humanShape = game.getHumanShape();
		AnimatedShape robotShape = game.getRobotShape();
		String modelName = game.getAvatarModelName();
		boolean wasMoving = game.isGamepadMoving;
		
		if (axisValue > 0.1f)  // Stick pushed beyond deadzone
		{	if (!game.isGamepadMoving)
			{	game.isGamepadMoving = true;
				// Start walk animation
				if (humanShape != null && "HumanFinal".equals(modelName))
				{	humanShape.stopAnimation();
					humanShape.playAnimation("WALK", game.getHumanWalkAnimSpeed(), 
						AnimatedShape.EndType.LOOP, 0);
				}
				else if (robotShape != null && "newHuman.obj".equals(modelName))
				{	robotShape.stopAnimation();
					robotShape.playAnimation("WALK", game.getRobotWalkAnimSpeed(),
						AnimatedShape.EndType.LOOP, 0);
				}
			}
		}
		else  // Stick at neutral
		{	if (game.isGamepadMoving)
			{	game.isGamepadMoving = false;
				// Stop walk animation and play idle if applicable
				if (humanShape != null && "HumanFinal".equals(modelName))
					humanShape.stopAnimation();
				else if (robotShape != null && "newHuman.obj".equals(modelName))
				{	robotShape.stopAnimation();
					robotShape.playAnimation("IDLE", game.getRobotWalkAnimSpeed(),
						AnimatedShape.EndType.LOOP, 0);
				}
				
				// Notify other clients that this player has stopped moving
				var av = game.getAvatar();
				if (protClient != null && game.getIsConnected())
					protClient.sendMoveMessage(av.getWorldLocation(), av.getWorldRotation(), false);
			}
		}
	}
}
