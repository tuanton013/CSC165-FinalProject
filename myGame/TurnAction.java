package myGame;

import tage.input.action.AbstractInputAction;
import org.joml.Matrix4f;
import net.java.games.input.Event;
import net.java.games.input.Component;
import org.joml.*;

/**
 * Rotates the player avatar around the Y axis and broadcasts the updated
 * rotation to other connected players.
 * Pass {@code +1f} to turn left (A key) or {@code -1f} to turn right (D key).
 */
public class TurnAction extends AbstractInputAction
{
	private MyGame game;
	private ProtocolClient protClient;
	private float  direction;   // +1 = left, -1 = right
	private static final float TURN_SPEED_RAD_PER_SEC = 4f;
	private static final float GAMEPAD_DEADZONE = 0.12f;

	public TurnAction(MyGame game, ProtocolClient protClient, float direction)
	{	this.game       = game;
		this.protClient = protClient;
		this.direction  = direction;
	}

	@Override
	public void performAction(float time, Event e)
	{	var av    = game.getAvatar();
		float deltaSeconds = time;
		if (deltaSeconds < 0.0f) deltaSeconds = 0.0f;
		if (deltaSeconds > 0.1f) deltaSeconds = 0.1f;
		
		float speedMultiplier = 1f;
		// Check if this is a gamepad axis event and scale speed based on axis value
		if (e != null && e.getComponent() != null)
		{	net.java.games.input.Component.Identifier comp = e.getComponent().getIdentifier();
			if (comp instanceof net.java.games.input.Component.Identifier.Axis)
			{	float rawAxis = e.getValue();
				float absAxis = java.lang.Math.abs(rawAxis);
				if (absAxis > GAMEPAD_DEADZONE)
				{	// Remap deadzone: rescale so the range (deadzone..1) becomes (0..1)
					float normalized = (absAxis - GAMEPAD_DEADZONE) / (1.0f - GAMEPAD_DEADZONE);
					// Quadratic curve: fine control near center, full speed at edges
					float curved = normalized * normalized;
					speedMultiplier = java.lang.Math.signum(rawAxis) * curved;
				}
				else
					speedMultiplier = 0f;
			}
		}
		
		float yaw = direction * TURN_SPEED_RAD_PER_SEC * deltaSeconds * getSpeed() * speedMultiplier;
		av.setLocalRotation(new Matrix4f().rotationY(yaw).mul(av.getWorldRotation()));

		// Broadcast updated position + rotation so remote ghosts face the correct direction
		if (protClient != null && game.getIsConnected())
			protClient.sendMoveMessage(av.getWorldLocation(), av.getWorldRotation(), false);
	}
}
