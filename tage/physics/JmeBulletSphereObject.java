package tage.physics;

import com.jme3.bullet.collision.shapes.SphereCollisionShape;

/**
* Sphere physics object TAGE wrapper for libbulletjme SphereCollisionShape (used internally by the TAGE engine).
* <br>
* Sphere size is determined by a float value for radius. <br>
* Note that in TAGE, physics objects should be created using the methods in the TAGE Scenegraph class.<br>
* This constructor therefore should not be called directly.
*/

public class JmeBulletSphereObject extends PhysicsObject
{	
	float radius;
	
	public JmeBulletSphereObject(int uid, float mass, org.joml.Vector3f loc, org.joml.Quaternionf rot, float radius)
	{	super(uid, mass, loc, rot, new SphereCollisionShape(radius));
		this.radius = radius;
	}
	
	/** Returns the radius of this sphere */
	public float radius() { return radius; }
}
