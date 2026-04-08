package myGame;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
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

	public MoveAction(MyGame game, ProtocolClient protClient, float direction)
	{	this.game       = game;
		this.protClient = protClient;
		this.direction  = direction;
	}

	@Override
	public void performAction(float time, Event e)
	{	var av          = game.getAvatar();
		var oldPosition = av.getWorldLocation();
		var moveDir     = new Vector4f(0f, 0f, 1f, 1f);
		moveDir.mul(av.getWorldRotation());
		moveDir.mul(direction * 0.01f * getSpeed());
		var newPosition = oldPosition.add(moveDir.x(), moveDir.y(), moveDir.z());
		av.setLocalLocation(newPosition);

		if (protClient != null && game.getIsConnected())
			protClient.sendMoveMessage(av.getWorldLocation());
	}
}
