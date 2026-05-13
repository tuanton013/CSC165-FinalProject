package myGame;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

/**
 * Gamepad button action: toggles background music on/off.
 * Bound to Button 1 (B / Circle).
 * Uses pause/resume to preserve playback position.
 */
public class ToggleMusicAction extends AbstractInputAction
{
	private MyGame game;

	public ToggleMusicAction(MyGame game)
	{	this.game = game;
	}

	@Override
	public void performAction(float time, Event e)
	{	if (e.getValue() < 0.5f) return;
		game.toggleMusic();
	}
}
