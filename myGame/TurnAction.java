package myGame;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
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

	public TurnAction(MyGame game, ProtocolClient protClient, float direction)
	{	this.game       = game;
		this.protClient = protClient;
		this.direction  = direction;
	}

	@Override
	public void performAction(float time, Event e)
	{	var av    = game.getAvatar();
		float yaw = direction * 0.02f * getSpeed();
		av.setLocalRotation(new Matrix4f().rotationY(yaw).mul(av.getWorldRotation()));

		// Broadcast updated position + rotation so remote ghosts face the correct direction
		if (protClient != null && game.getIsConnected())
			protClient.sendMoveMessage(av.getWorldLocation(), av.getWorldRotation(), false);
	}
}
