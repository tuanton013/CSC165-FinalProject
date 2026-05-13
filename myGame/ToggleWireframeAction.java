package myGame;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

/**
 * Gamepad button action: toggles avatar wireframe rendering.
 * Bound to Button 2 (X / Square).
 */
public class ToggleWireframeAction extends AbstractInputAction
{
	private MyGame game;

	public ToggleWireframeAction(MyGame game)
	{	this.game = game;
	}

	@Override
	public void performAction(float time, Event e)
	{	if (e.getValue() < 0.5f) return;
		game.toggleWireframe();
	}
}
