package tage;
import org.joml.*;
import java.lang.Math;

/**
 * A 3rd-person orbit camera controller for TAGE.
 *
 * <p>The camera is positioned using three parameters relative to a target
 * {@link GameObject} (typically the player avatar):
 * <ul>
 *   <li><b>azimuth</b>  – horizontal orbit angle in radians (world Y-axis).
 *       Changing azimuth orbits the camera around the avatar without altering
 *       the avatar's heading.</li>
 *   <li><b>elevation</b> – vertical angle above the horizontal plane, in
 *       radians.  Clamped so the camera can never flip over the top or go
 *       below the target.</li>
 *   <li><b>distance</b>  – zoom distance from the target.  Clamped to the
 *       range [{@value #MIN_DISTANCE}, {@value #MAX_DISTANCE}].</li>
 * </ul>
 *
 * <p>Camera position is computed from spherical coordinates each frame:
 * <pre>
 *   camX = targetX + distance * cos(elevation) * sin(azimuth)
 *   camY = targetY + distance * sin(elevation)
 *   camZ = targetZ + distance * cos(elevation) * cos(azimuth)
 * </pre>
 * The camera is then pointed at the target's world location via
 * {@link Camera#lookAt(Vector3f)}.
 *
 * <p>Typical per-frame usage:
 * <pre>
 *   // In MyGame.update():
 *   cameraOrbit.updateCameraPosition(avatar);
 * </pre>
 *
 * <p>Requirements satisfied:
 * <ol>
 *   <li>Orbit camera without altering avatar heading – {@link #orbit(float)}</li>
 *   <li>Adjust camera elevation angle – {@link #elevate(float)}</li>
 *   <li>Zoom in / out – {@link #zoom(float)}</li>
 *   <li>Move / turn avatar while maintaining camera relative position –
 *       {@link #updateCameraPosition(GameObject)} recomputes the offset from
 *       the avatar's current world location every frame.</li>
 * </ol>
 */
public class CameraOrbit3D
{
	// ---------------------------------------------------------------
	// Constants
	// ---------------------------------------------------------------

	/** Minimum allowed elevation angle (radians) – just above horizontal. */
	public static final float MIN_ELEVATION = 0.05f;

	/** Maximum allowed elevation angle (radians) – just below straight up. */
	public static final float MAX_ELEVATION = (float)(Math.PI / 2.0 - 0.05);

	/** Minimum camera-to-target distance (units). */
	public static final float MIN_DISTANCE  = 1.0f;

	/** Maximum camera-to-target distance (units). */
	public static final float MAX_DISTANCE  = 30.0f;

	// ---------------------------------------------------------------
	// State
	// ---------------------------------------------------------------

	private Camera camera;
	private float  azimuth;          // horizontal orbit angle in radians (world Y-axis)
	private float  elevation;        // vertical angle above horizontal in radians
	private float  distance;         // zoom distance from target
	private float  lookHeightOffset; // Y offset added to the look-at target (e.g. 3.2 = chest height)

	// ---------------------------------------------------------------
	// Constructor
	// ---------------------------------------------------------------

	/**
	 * Creates a CameraOrbit3D controller bound to the given {@link Camera}.
	 *
	 * @param cam           the TAGE Camera to control (e.g. viewport "MAIN" camera)
	 * @param initAzimuth   starting horizontal orbit angle in radians
	 *                      (0 = directly behind avatar in +Z world direction)
	 * @param initElevation starting elevation angle in radians
	 * @param initDistance  starting distance from the target
	 */
	public CameraOrbit3D(Camera cam, float initAzimuth, float initElevation, float initDistance)
	{	camera          = cam;
		azimuth         = initAzimuth;
		elevation       = clampElevation(initElevation);
		distance        = clampDistance(initDistance);
		lookHeightOffset = 0f;
	}

	// ---------------------------------------------------------------
	// Orbit / elevate / zoom mutators
	// ---------------------------------------------------------------

	/**
	 * Orbits the camera horizontally by {@code deltaAngle} radians around the
	 * world Y-axis.  The avatar's heading is never touched by this method.
	 *
	 * @param deltaAngle positive = orbit right, negative = orbit left
	 */
	public void orbit(float deltaAngle)
	{	azimuth += deltaAngle;
	}

	/**
	 * Adjusts the camera elevation by {@code deltaAngle} radians.
	 * The result is clamped to [{@value #MIN_ELEVATION}, {@value #MAX_ELEVATION}]
	 * to prevent gimbal-lock flipping.
	 *
	 * @param deltaAngle positive = raise camera, negative = lower camera
	 */
	public void elevate(float deltaAngle)
	{	elevation = clampElevation(elevation + deltaAngle);
	}

	/**
	 * Zooms the camera by {@code delta} world units.
	 * The result is clamped to [{@value #MIN_DISTANCE}, {@value #MAX_DISTANCE}].
	 *
	 * @param delta positive = zoom out (farther), negative = zoom in (closer)
	 */
	public void zoom(float delta)
	{	distance = clampDistance(distance + delta);
	}

	// ---------------------------------------------------------------
	// Absolute setters
	// ---------------------------------------------------------------

	/** Sets the horizontal orbit angle to an absolute value in radians. */
	public void setAzimuth(float a)
	{	azimuth = a;
	}

	/** Sets the elevation angle in radians, clamped to the valid range. */
	public void setElevation(float e)
	{	elevation = clampElevation(e);
	}

	/** Sets the zoom distance, clamped to [{@value #MIN_DISTANCE}, {@value #MAX_DISTANCE}]. */
	public void setDistance(float d)
	{	distance = clampDistance(d);
	}

	// ---------------------------------------------------------------
	// Accessors
	// ---------------------------------------------------------------

	/** Returns the current horizontal orbit angle in radians. */
	public float getAzimuth()   { return azimuth; }

	/** Returns the current elevation angle in radians. */
	public float getElevation() { return elevation; }

	/** Returns the current zoom distance. */
	public float getDistance()  { return distance; }

	/**
	 * Sets the Y offset added to the look-at target each frame.
	 * Use this to match the original chase-camera look height (e.g. 3.2 = chest/head).
	 */
	public void  setLookHeightOffset(float h) { lookHeightOffset = h; }

	/** Returns the current look-height offset. */
	public float getLookHeightOffset() { return lookHeightOffset; }

	// ---------------------------------------------------------------
	// Per-frame update
	// ---------------------------------------------------------------

	/**
	 * Recomputes and applies the camera position and look-at direction so
	 * the camera orbits the given {@link GameObject} at the current azimuth,
	 * elevation, and distance.
	 *
	 * <p>Call this once per frame from the game's {@code update()} method
	 * <em>after</em> any avatar movement / turning has been applied.
	 * The camera's U, V, and N vectors are updated via
	 * {@link Camera#lookAt(Vector3f)} so that the audio ear orientation
	 * (which reads camera N) remains correct.
	 *
	 * @param target the GameObject the camera orbits (typically the avatar)
	 */
	public void updateCameraPosition(GameObject target)
	{	Vector3f targetPos = target.getWorldLocation();

		float cosEl = (float) Math.cos(elevation);
		float sinEl = (float) Math.sin(elevation);
		float cosAz = (float) Math.cos(azimuth);
		float sinAz = (float) Math.sin(azimuth);

		// Spherical → Cartesian offset in world space
		float offsetX = distance * cosEl * sinAz;
		float offsetY = distance * sinEl;
		float offsetZ = distance * cosEl * cosAz;

		Vector3f camPos = new Vector3f(
				targetPos.x() + offsetX,
				targetPos.y() + offsetY,
				targetPos.z() + offsetZ);

		camera.setLocation(camPos);
		camera.lookAt(new Vector3f(targetPos.x(), targetPos.y() + lookHeightOffset, targetPos.z()));
	}

	// ---------------------------------------------------------------
	// Private helpers
	// ---------------------------------------------------------------

	private float clampElevation(float e)
	{	return Math.max(MIN_ELEVATION, Math.min(MAX_ELEVATION, e));
	}

	private float clampDistance(float d)
	{	return Math.max(MIN_DISTANCE, Math.min(MAX_DISTANCE, d));
	}
}
