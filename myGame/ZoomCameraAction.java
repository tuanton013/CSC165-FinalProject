package myGame;

import tage.CameraOrbit3D;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

/**
 * Zooms the {@link CameraOrbit3D} controller in or out relative to the avatar.
 *
 * <p>Keyboard usage: bind with {@code -1f} (zoom in / numpad +) or
 * {@code +1f} (zoom out / numpad -) and {@code REPEAT_WHILE_DOWN}.
 */
public class ZoomCameraAction extends AbstractInputAction
{
	private static final float ZOOM_SPEED = 5.0f;   // units per second

	private CameraOrbit3D orbit;
	private float         direction;   // -1 = zoom in, +1 = zoom out

	public ZoomCameraAction(CameraOrbit3D orbit, float direction)
	{	this.orbit     = orbit;
		this.direction = direction;
	}

	@Override
	public void performAction(float time, Event e)
	{	float delta = Math.max(0f, Math.min(0.1f, time));
		orbit.zoom(direction * ZOOM_SPEED * delta * getSpeed());
	}
}
