package myGame;

import tage.CameraOrbit3D;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Component;
import net.java.games.input.Event;

/**
 * Adjusts the {@link CameraOrbit3D} elevation angle (vertical tilt).
 *
 * <p>Keyboard usage: bind with {@code +1f} (raise camera) or {@code -1f}
 * (lower camera) and {@code REPEAT_WHILE_DOWN}.
 * <p>Gamepad usage: bind to the right-stick vertical axis; the action
 * scales by the raw axis value for analogue feel.
 */
public class ElevateCameraAction extends AbstractInputAction
{
	private static final float ELEVATE_SPEED = 1.2f;   // radians per second (keyboard)
	private static final float GAMEPAD_DEADZONE = 0.10f;

	private CameraOrbit3D orbit;
	private float         direction;   // +1 = raise, -1 = lower

	public ElevateCameraAction(CameraOrbit3D orbit, float direction)
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

		orbit.elevate(ELEVATE_SPEED * delta * getSpeed() * scale);
	}
}
