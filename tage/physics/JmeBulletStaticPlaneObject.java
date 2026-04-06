package tage.physics;

import com.jme3.bullet.collision.shapes.PlaneCollisionShape;
import com.jme3.math.Plane;
import com.jme3.math.Vector3f;

/**
* Plane physics object TAGE wrapper for libbulletjme PlaneCollisionShape (used internally by the TAGE engine).
* <br>
* The plane object is specified by a world "up" vector (specified as a JME Vector3f) and a "plane constant".<br>
* The plane constant indicated the dimension along the up vector that the plane is shifted.<br>
* The size of the plane is infinite, and it is static (cannot move).<br>
* Note that in TAGE, physics objects should be created using the methods in the TAGE Scenegraph class.<br>
* This constructor therefore should not be called directly.
*/

public class JmeBulletStaticPlaneObject extends PhysicsObject
{
    public JmeBulletStaticPlaneObject(int uid, org.joml.Vector3f loc, org.joml.Quaternionf rot, float[] up_vector, float plane_constant)
	{	super(uid, 0, loc, rot, new PlaneCollisionShape(new Plane(new Vector3f(up_vector[0],up_vector[1],up_vector[2]), plane_constant)));
    }
}
