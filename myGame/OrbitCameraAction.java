package myGame;

import tage.CameraOrbit3D;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Component;
import net.java.games.input.Event;

/**
 * Orbits the {@link CameraOrbit3D} controller horizontally around the avatar
 * without altering the avatar's heading.
 *
 * <p>Keyboard usage: bind with {@code +1f} (orbit right) or {@code -1f}
 * (orbit left) and {@code REPEAT_WHILE_DOWN}.
 * <p>Gamepad usage: bind to the right-stick horizontal axis; the action
 * scales by the raw axis value for analogue feel.
 */
public class OrbitCameraAction extends AbstractInputAction
{
	private static final float ORBIT_SPEED = 1.8f;   // radians per second (keyboard)
	private static final float GAMEPAD_DEADZONE = 0.10f;

	private CameraOrbit3D orbit;
	private float         direction;   // +1 = orbit right, -1 = orbit left

	public OrbitCameraAction(CameraOrbit3D orbit, float direction)
	{	this.orbit     = orbit;
		this.direction = direction;
	}

	@Override
	public void performAction(float time, Event e)
	{	float delta = Math.max(0f, Math.min(0.1f, time));

		float scale = direction;

		// Analogue gamepad: scale by axis value; apply deadzone
		if (e != null && e.getComponent() != null)
		{	Component.Identifier id = e.getComponent().getIdentifier();
			if (id instanceof Component.Identifier.Axis)
			{	float raw = e.getValue();
				if (Math.abs(raw) < GAMEPAD_DEADZONE) return;
				scale = direction * raw;
			}
		}

		orbit.orbit(ORBIT_SPEED * delta * getSpeed() * scale);
	}
}
