package myGame;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

/**
 * Gamepad button action: toggles the physics debug visualization.
 * Bound to Button 3 (Y / Triangle).
 */
public class TogglePhysicsAction extends AbstractInputAction
{
	private MyGame game;

	public TogglePhysicsAction(MyGame game)
	{	this.game = game;
	}

	@Override
	public void performAction(float time, Event e)
	{	if (e.getValue() < 0.5f) return;
		game.togglePhysicsViz();
	}
}
