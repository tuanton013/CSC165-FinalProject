package tage.physics;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.math.Vector3f;

/**
* Box physics object TAGE wrapper for libbulletjme BoxCollisionShape (used internally by the TAGE engine).
* <br>
* Box size is determined by a float array containing X, Y, and Z dimensions.<br>
* Note that in TAGE, physics objects should be created using the methods in the TAGE Scenegraph class.<br>
* This constructor therefore should not be called directly.
*/

public class JmeBulletBoxObject extends PhysicsObject
{
	private float[] size;

	public JmeBulletBoxObject(int uid, float mass, org.joml.Vector3f loc, org.joml.Quaternionf rot, float[] size)
	{	super(uid, mass, loc, rot, new BoxCollisionShape(new Vector3f(size[0],size[1],size[2])));
		this.size = size;
	}

	/** Returns the dimensions of the box physics object in a float array containing X, Y, Z dimensions. */
	public float[] size() { return size; }
}
