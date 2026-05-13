package myGame;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

/**
 * Gamepad button action: toggles the free-walk (unwalkable path) restriction.
 * Bound to Button 4 (LB / L1).
 */
public class ToggleFreeWalkAction extends AbstractInputAction
{
	private MyGame game;

	public ToggleFreeWalkAction(MyGame game)
	{	this.game = game;
	}

	@Override
	public void performAction(float time, Event e)
	{	if (e.getValue() < 0.5f) return;
		game.toggleFreeWalk();
	}
}
