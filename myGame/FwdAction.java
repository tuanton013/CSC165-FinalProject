package myGame;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
import org.joml.*;

/**
 * Moves the player avatar forward (along its local Z axis) and
 * broadcasts the new position to all other connected players.
 *
 * Assign to a key or gamepad axis via InputManager.
 */
public class FwdAction extends AbstractInputAction
{
	private MyGame         game;
	private ProtocolClient protClient;

	public FwdAction(MyGame game, ProtocolClient protClient)
	{	this.game       = game;
		this.protClient = protClient;
	}

	@Override
	public void performAction(float time, Event e)
	{	// Move avatar in the direction it is facing
		var av          = game.getAvatar();
		var oldPosition = av.getWorldLocation();
		var fwdDir      = new Vector4f(0f, 0f, 1f, 1f);
		fwdDir.mul(av.getWorldRotation());
		fwdDir.mul(0.01f * getSpeed());
		var newPosition = oldPosition.add(fwdDir.x(), fwdDir.y(), fwdDir.z());
		av.setLocalLocation(newPosition);

		// Notify other players
		if (protClient != null && game.getIsConnected())
			protClient.sendMoveMessage(av.getWorldLocation());
	}
}
