package myGame;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

/**
 * Gamepad button action: toggles the game pause state.
 * Bound to Button 0 (A / Cross).
 * Ignores the release event (value < 0.5) to fire only once per press.
 */
public class TogglePauseAction extends AbstractInputAction
{
	private MyGame game;

	public TogglePauseAction(MyGame game)
	{	this.game = game;
	}

	@Override
	public void performAction(float time, Event e)
	{	if (e.getValue() < 0.5f) return;
		game.togglePause();
	}
}
