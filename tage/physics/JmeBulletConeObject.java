package tage.physics;

import com.jme3.bullet.collision.shapes.ConeCollisionShape;

/**
* Cone physics object TAGE wrapper for libbulletjme ConeCollisionShape (used internally by the TAGE engine).
* <br>
* Cone size is determined by float values for radius and height. <br>
* There is also an integer "axis" parameter that signifies which axis this cone aligns with (0=X, 1=Y, 2=Z).<br>
* Note that in TAGE, physics objects should be created using the methods in the TAGE Scenegraph class.<br>
* This constructor therefore should not be called directly.
*/

public class JmeBulletConeObject extends PhysicsObject
{	
	private float radius;
	private float height;
	
	public JmeBulletConeObject(int uid, float mass, org.joml.Vector3f loc, org.joml.Quaternionf rot, int axis,  float radius, float height)
	{	super(uid, mass, loc, rot, new ConeCollisionShape(radius, height, axis));
		this.radius = radius;
		this.height = height;
	}

	/** Returns the radius of this cone object */
	public float radius() { return radius; }
	
	/** Returns the height of this cone object */
	public float height() { return height; }
}
