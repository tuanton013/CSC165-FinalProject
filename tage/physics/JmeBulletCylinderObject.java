package tage.physics;

import com.jme3.bullet.collision.shapes.CylinderCollisionShape;
import com.jme3.math.Vector3f;

/**
* Cylinder physics object TAGE wrapper for libbulletjme CylinbderCollisionShape (used internally by the TAGE engine).
* <br>
* Cylinder size is specified in a float array of size 3, containing three half extents.<br>
* There is also an integer "axis" parameter that signifies which axis this cylinder aligns with (0=X, 1=Y, 2=Z).<br>
* Note that in TAGE, physics objects should be created using the methods in the TAGE Scenegraph class.<br>
* This constructor therefore should not be called directly.
*/

public class JmeBulletCylinderObject extends PhysicsObject
{	
	private float[] halfExtents;

	public JmeBulletCylinderObject(int uid, float mass, org.joml.Vector3f loc, org.joml.Quaternionf rot, int axis, float[] halfExtents)
	{	super(uid, mass, loc, rot, new CylinderCollisionShape(new Vector3f(halfExtents[0],halfExtents[1],halfExtents[2]), axis));
		this.halfExtents = halfExtents;
	}

	/** Returns a float array of size 3, containing the dimensions of this cylinder object, expressed as three half extents */
	public float[] halfExtents() { return halfExtents; }
}
