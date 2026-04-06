package tage.physics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.joml.Math;
import org.joml.Vector4f;

import tage.GameObject;
import tage.Utils;

import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.bullet.collision.PersistentManifolds;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;

/**
* Abstract class containing properties and functions common to all physics objects.
* If using TAGE, physics objects should be created using the methods in the TAGE Scenegraph class.
* Physics objects store their location and rotation information in a libbulletjme class called "Transform",
* which contains a location vector and a rotation quaternion (but not scale).
* Libbulletjme stores them in vector and quaternion types using its own "jme" math library.
* This class provides convenience functions that utilize Vector3f and Quaternionf types from the JOML library.
* <p>
* There are several functions defined here that the application can utilize:
* <ul>
* <li>Set the transform, providing a JOML location vector and a JOML rotation quaternion
* <li>Get the location vector, and the location quaternion as JOML classes
* <li>Get or set the mass, friction, bounciness, damping, or velocity
* <li>Get or set whether the object goes to sleep after it stops moving
* <li>Apply a force, torque, or impulse
* </ul>
* <p>
* There are also functions for accessing collision information, which are applicable if the application
* has called "detectCollisions()" in the PhysicsEngine object:
* <ul>
* <li>get the HashSet containing the list of objects that have just collided with this object
* <li>get the HashSet containing the complete list of all objects that are touching (colliding) with this object
* <li>get the ArrayList containing the complete list of manifolds involved in collision for this object
* </ul>
*/

public abstract class PhysicsObject
{
	public static HashMap<Long, PhysicsObject> lookUpObject = new HashMap<>();
	public static PhysicsObject getPhysicsObject(Long r) { return lookUpObject.get(r); }

	private int uid;
	private float mass;
	private CollisionShape shape;
	private PhysicsRigidBody body;
	private Vector3f v;
	private Quaternion q;
	private boolean isDynamic;
	private ArrayList<Long> manifoldList;
	private HashSet<PhysicsObject> collidedWithSet;
	private HashSet<PhysicsObject> prevCollidedWithSet;

	/**
	* TAGE uses this constructor to assign default values to physics object properties,
	* and it also initializes collision data structures used by the physics engine.
	*/
	public PhysicsObject(int uid, float mass, org.joml.Vector3f loc, org.joml.Quaternionf rot, CollisionShape shape)
	{
		this.uid = uid;
		this.mass = mass;
		this.isDynamic = (mass != 0f);
		this.shape = shape;
		this.body = new PhysicsRigidBody(shape, mass);
		this.setTransform(loc, rot);

		if (mass > 0) //dynamic
		{	body.setCcdMotionThreshold(1f);
			body.setCcdSweptSphereRadius(0.4f);
        }

		manifoldList = new ArrayList<Long>();
		collidedWithSet = new HashSet<PhysicsObject>();
		prevCollidedWithSet = new HashSet<PhysicsObject>();

		PhysicsObject.lookUpObject.put(body.nativeId(),this);
    }

	/** Used by TAGE to access the ID of this physics object. */
	public int getUID() { return uid; }

	/** Sets the location and rotation properties of this physics object by providing a JOML Vector3f and a JOML Quaternionf. */
	public void setTransform(org.joml.Vector3f loc, org.joml.Quaternionf rot)
	{	Vector3f v = new Vector3f(loc.x, loc.y, loc.z);
		Quaternion q = new Quaternion(rot.x, rot.y, rot.z, rot.w);
		Transform transform = new Transform(v,q);
		this.body.setPhysicsTransform(transform);
	}
	
	/** Gets the location of this physics object as a JOML Vector3f */
	public org.joml.Vector3f getLocation()
	{	v = this.body.getPhysicsLocation(v);
		org.joml.Vector3f vj = new org.joml.Vector3f(v.x, v.y, v.z);
		return vj;
	}
	
	/** Gets the rotation of this physics object as a JOML Quaternion */
	public org.joml.Quaternionf getRotation()
	{	q = this.body.getPhysicsRotation(q);
		org.joml.Quaternionf qj = new org.joml.Quaternionf(q.getX(), q.getY(), q.getZ(), q.getW());
		return qj;
	}

	/** Convenience function to set the location of this physics object using a float array representing location (x,y,z) */
	public void setLocation(float[] location)
	{	body.setPhysicsLocation(new Vector3f(location[0],location[1],location[2]));
	}
	
	/** Accessor for this physics object's libbulletjme collision shape */
	public CollisionShape getShape() { return shape; }

	/** Gets the mass of this physics object */
	public float getMass() { return this.mass; }
	
	/** Sets the mass of this physics object */
	public void setMass(float mass)
	{	this.mass = mass;
		this.isDynamic = mass != 0;
	}
	
	/** Accessor for the libbulletjme rigid body object associated with this physics object */
	public PhysicsRigidBody getRigidBody() { return this.body; }

	/** Returns whether or not this object is dynamic (has mass > 0) */
	public boolean isDynamic() { return isDynamic; }

	/** Gets the friction of this object */
	public float getFriction() { return this.body.getFriction(); }

	/** Sets the friction of this object */
	public void setFriction(float friction) { this.body.setFriction(friction); }

	/** Gets the bounciness of this object */
	public float getBounciness() { return this.body.getRestitution(); }
	
	/**
	* Set the bounciness (restitution) coefficient. The value should be kept
	* between 0 and 1. Anything above 1 will make bouncing objects bounce further
	* on each bounce. The true bouncieness value of a collision between any two
	* objects in the physics world is the muplication of the two object's bounciness
	* coefficient.
	*/
	public void setBounciness(float bounciness) { this.body.setRestitution(bounciness); }
	
	/** Gets the current linear velocity of this physics object */
	public float[] getLinearVelocity()
	{	Vector3f out = new Vector3f();
		this.body.getLinearVelocity(out);
		float[] velocity = {out.x, out.y, out.z};
		return velocity;
	}
	
	/** Sets the linear velocity of this physics object directly */
	public void setLinearVelocity(float[] velocity)
	{	this.body.setLinearVelocity(new Vector3f(velocity[0],velocity[1],velocity[2]));
	}
	
	/** Gets the angular velocity of this physics object */
	public float[] getAngularVelocity()
	{	Vector3f out = new Vector3f();
		this.body.getAngularVelocity(out);
		float[] velocity = {out.x, out.y, out.z};
		return velocity;
	}
	
	/** Sets the angular velocity of this physics object directly */
	public void setAngularVelocity(float[] velocity)
	{	this.body.setAngularVelocity(new Vector3f(velocity[0],velocity[1],velocity[2]));
	}

	/**
	* Sets this physics object to never sleep.
	* This is useful if an object may continue to participate in collisions even after it has been resting.
	*/
	public void disableSleeping()
	{	body.setEnableSleep(false);
	}
	
	/**
	* Sets this physics object to sleep after some period of time has elapsed without collision or motion.
	* This is useful if the object is no longer participating in the game, to conserve resources.
	* For example, a bullet that eventually comes to rest may have no further use in the game.
	*/
	public void enableSleeping()
	{	body.setEnableSleep(true);
	}
	
	/**
	* Sets linear and angular threshold values to determine when this physics object is put to sleep (if sleep is enabled).<br>
	* The threshold values represent speeds; if the object speed drops below the threshold for 2 seconds, it sleeps.
	*/
	public void setSleepThresholds(float linearThreshold, float angularThreshold) 
	{	body.setSleepingThresholds(linearThreshold, angularThreshold);
	}

	/** Sets linear threshold value to determine when this physics object is put to sleep (if sleep is enabled) */
	public float getLinearSleepThreshold() 
	{ return body.getLinearSleepingThreshold();
	}

	/** Sets angular threshold value to determine when this physics object is put to sleep (if sleep is enabled) */
	public float getAngularSleepThreshold() 
	{	return body.getAngularSleepingThreshold();
	}

	/** Sets both the linear and angular damping of this physics object */
	public void setDamping(float linearDamping, float angularDamping) 
	{	body.setDamping(linearDamping, angularDamping);
	}

	/** Gets the linear damping setting for this physics object */
	public float getLinearDamping() 
	{	return body.getLinearDamping();
	}

	/** Gets the angular damping setting for this physics object */
	public float getAngularDamping() 
	{	return body.getAngularDamping();
	}
	
	/**
	* Applies a linear force to this object.
	* <br>
	* (fx,fy,fz) is the direction and magnitude of the force. <br>
	* (px,py,pz) is an offset location from the center of the object, to apply the force.
	*/
	public void applyForce(float fx, float fy, float fz, float px, float py, float pz)
	{	if (!body.isActive()) { body.activate(); }
		body.applyForce(new Vector3f(fx, fy, fz), new Vector3f(px, py, pz));
	}
	
	/**
	* Applies a torque to this object.
	* <br>
	* Expressed as an float array (x,y,z), a vector representing mass times distance squared per second squared.
	*/
	public void applyTorque(float fx, float fy, float fz)
	{	if (!body.isActive()) { body.activate(); }
		body.applyTorque(new Vector3f(fx, fy, fz));
	}

	/**
	* Applies an instantaneous impulse to this object.
	* <br>
	* Adds the specified velocity to the object's current velocity.<br>
	* (fx,fy,fz) is the direction and magnitude of the impulse. <br>
	* (px,py,pz) is an offset location from the center of the object, to apply the impulse.
	*/
	public void applyImpulse(float fx, float fy, float fz, float px, float py, float pz)
	{	if (!body.isActive()) { body.activate(); }
		body.applyImpulse(new Vector3f(fx, fy, fz), new Vector3f(px, py, pz));
    }

	/**
	* Alter's the body's angular factors, used to scale applied torques.
	* <br>
	* The vector is the desired angular factor for each axis (default = 1,1,1)
	*/
	public void setAngularFactor(float f) { body.setAngularFactor(0f); }

	/** Get the number of manifolds in the collision manifold list */
	public int getManifoldCount() { return manifoldList.size(); }
	
	/** Add a manifold to the manifold list (used by TAGE collision detection wrapper) */
	public void addManifold(long c) { manifoldList.add(c); }
	
	/** Get the ith manifold in the manifold list */
	public long getManifold(int i) { return manifoldList.get(i); }
	
	/** Get the total number of physics object that are touching (colliding with) this physics object */
	public int getCollidedCount() { return collidedWithSet.size(); }

	/** Add a physics object to the list of objects colliding with this object (used by TAGE collision detection wrapper) */
	public void addCollidedObject(PhysicsObject p) { collidedWithSet.add(p); }

	/** Returns a HashSet containing the full list of objects touching (colliding with) this physics object */
	public HashSet<PhysicsObject> getFullCollidedSet() { return collidedWithSet; }
	
	/**
	* Computes and returns a HashSet containing the list of objects that have just collided with this physics object.<br>
	* This list is computed by TAGE by subtracting the list of colliders in the previous frame from the full list of current colliders.
	*/
	public HashSet<PhysicsObject> getNewlyCollidedSet()
	{	HashSet<PhysicsObject> newCollides = new HashSet<PhysicsObject>(collidedWithSet);
		newCollides.removeAll(prevCollidedWithSet);
		return newCollides;
	}
	
	/** Used by TAGE to initialize collision lists before computing the current collisions */
	public void clearCollisionLists()
	{	manifoldList.clear();
		prevCollidedWithSet = collidedWithSet;
		collidedWithSet = new HashSet<PhysicsObject>();
	}
}
