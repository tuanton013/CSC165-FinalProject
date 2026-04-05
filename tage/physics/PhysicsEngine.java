package tage.physics;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import com.jme3.math.Matrix4f;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.SolverType;
import com.jme3.bullet.collision.PersistentManifolds;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.bullet.collision.PhysicsSweepTestResult;
import com.jme3.bullet.collision.shapes.ConvexShape;
import com.jme3.bullet.collision.shapes.infos.IndexedMesh;
import com.jme3.bullet.PhysicsSpace.BroadphaseType;
import com.jme3.bullet.collision.shapes.infos.IndexedMesh;
import com.jme3.system.NativeLibraryLoader;

/**
 * This is the TAGE wrapper for the LibBulletJme physics engine.
 * <br>
 * If using TAGE, this physics engine is automatically instantiated.
 *
 * @author Scott Gordon
 * @author Roxanne Campbell
 * @author Alexander Bass (terrain mesh)
 */

public class PhysicsEngine
{
	private static int nextUID;

	private ArrayList<PhysicsObject> objects;
	private PhysicsSpace physicsSpace;
	
	public static final float DEFAULT_GRAVITY_X = 0;
	public static final float DEFAULT_GRAVITY_Y = -10;
	public static final float DEFAULT_GRAVITY_Z = 0;
	
	public PhysicsEngine() { }

	/** Sets up the physics engine - automatically called by the TAGE engine. */
	public void initSystem()
	{	com.jme3.bullet.PhysicsSpace.logger.setLevel(java.util.logging.Level.SEVERE);
		com.jme3.bullet.objects.PhysicsRigidBody.logger2.setLevel(java.util.logging.Level.SEVERE);
		com.jme3.system.NativeLibraryLoader.logger.setLevel(java.util.logging.Level.SEVERE);
		
		NativeLibraryLoader.loadLibbulletjme(true, new File(System.getProperty("user.dir")+"\\tage"+"\\physics"), "Release", "Sp");

		Vector3f worldAabbMin = new Vector3f(-10000, -10000, -10000);
		Vector3f worldAabbMax = new Vector3f(10000, 10000, 10000);

		BroadphaseType broadphaseType = BroadphaseType.AXIS_SWEEP_3;
		SolverType solverType = SolverType.SI;

		physicsSpace = new PhysicsSpace(worldAabbMin, worldAabbMax, broadphaseType);
		physicsSpace.setAccuracy(1/100f);
		physicsSpace.setMaxSubSteps(5);

		float[] gravity_vector = { 0, 0, 0 };
		setGravity(gravity_vector);

		objects = new ArrayList<PhysicsObject>(/* 50, 25 */);
	}
	
	/** Sets the gravity vector, using a float array of size 3 representing x, y, z */
	public void setGravity(float[] grav)
	{	physicsSpace.setGravity(new Vector3f(grav[0], grav[1], grav[2]));
	}

	/** Used by TAGE to create a box physics object - the application should call the corresponding function in SceneGraph. */
	public PhysicsObject addBoxObject(int uid, float mass, org.joml.Vector3f loc, org.joml.Quaternionf rot, float[] size)
	{	// PhysicsEngine asks for dimensions, JBullet uses halfExtents
		float[] temp = new float[size.length];
		for (int i = 0; i < size.length; i++)
		{	temp[i] = size[i] / 2f;
		}
		JmeBulletBoxObject boxObject = new JmeBulletBoxObject(uid, mass, loc, rot, temp);
		this.physicsSpace.addCollisionObject(boxObject.getRigidBody());
		this.objects.add(boxObject);
		return boxObject;
	}

	/** Used by TAGE to create a sphere physics object - the application should call the corresponding function in SceneGraph. */
	public PhysicsObject addSphereObject(int uid, float mass, org.joml.Vector3f loc, org.joml.Quaternionf rot, float radius)
	{	JmeBulletSphereObject sphereObject = new JmeBulletSphereObject(uid, mass, loc, rot, radius);
		this.physicsSpace.addCollisionObject(sphereObject.getRigidBody());
		this.objects.add(sphereObject);
		return sphereObject;
	}

	/** Used by TAGE to create a capsule physics object - the application should call the corresponding function in SceneGraph. */	
	public PhysicsObject addCapsuleObject(int uid, float mass, org.joml.Vector3f loc, org.joml.Quaternionf rot, int axis, float radius, float height)
	{	JmeBulletCapsuleObject capsuleObject = new JmeBulletCapsuleObject(uid, mass, loc, rot, axis, radius, height);
		this.physicsSpace.addCollisionObject(capsuleObject.getRigidBody());
		this.objects.add(capsuleObject);
		return capsuleObject;
	}

	/** Used by TAGE to create a cylinder physics object - the application should call the corresponding function in SceneGraph. */
	public PhysicsObject addCylinderObject(int uid, float mass, org.joml.Vector3f loc, org.joml.Quaternionf rot, int axis, float[] halfExtents)
	{	JmeBulletCylinderObject cylinderObject = new JmeBulletCylinderObject(uid, mass, loc, rot, axis, halfExtents);
		this.physicsSpace.addCollisionObject(cylinderObject.getRigidBody());
		this.objects.add(cylinderObject);
		return cylinderObject;
	}

	/** Used by TAGE to create a cone physics object - the application should call the corresponding function in SceneGraph. */
	public PhysicsObject addConeObject(int uid, float mass, org.joml.Vector3f loc, org.joml.Quaternionf rot, int axis, float radius, float height)
	{	JmeBulletConeObject coneObject = new JmeBulletConeObject(uid, mass, loc, rot, axis, radius, height);
		this.physicsSpace.addCollisionObject(coneObject.getRigidBody());
		this.objects.add(coneObject);
		return coneObject;
	}

	/** Used by TAGE to create a static plane physics object - the application should call the corresponding function in SceneGraph. */
	public PhysicsObject addStaticPlaneObject(int uid, org.joml.Vector3f loc, org.joml.Quaternionf rot, float[] up_vector, float plane_constant)
	{	JmeBulletStaticPlaneObject planeObject = new JmeBulletStaticPlaneObject(uid, loc, rot, up_vector, plane_constant);
		this.physicsSpace.addCollisionObject(planeObject.getRigidBody());
		this.objects.add(planeObject);
		return planeObject;
	}

	/** Used by TAGE to create a static terrain mesh physics object - the application should call the corresponding function in SceneGraph. */
	public PhysicsObject addStaticTerrainMesh(int uid, org.joml.Vector3f loc, org.joml.Quaternionf rot, float[] vertices, int[] indices, float scale, float heightScale, int resolution)
	{	Vector3f[] v = new Vector3f[(vertices.length)/3];
		for (int i=0; i<vertices.length; i=i+3)
		{	v[i/3] = new Vector3f(vertices[i],vertices[i+1],vertices[i+2]);
		}
		IndexedMesh mesh = new IndexedMesh(v, indices);
		JmeBulletMeshObject meshObject = new JmeBulletMeshObject(uid, loc, rot, mesh, scale, heightScale, resolution);
		this.physicsSpace.addCollisionObject(meshObject.getRigidBody());
		this.objects.add(meshObject);
		return meshObject;
	}

	/** Used by TAGE to remove a physics object - the application should call the corresponding function in SceneGraph. */
	public void removeObject(int uid)
	{	PhysicsObject target_object = null;
		for (PhysicsObject object : objects)
		{	if (object.getUID() == uid)
			{	target_object = object;
			}
		}
		if (target_object != null)
		{	physicsSpace.removeCollisionObject(target_object.getRigidBody());
		}
	}

	/** The application should normally call this function once each frame, such as from the TAGE game's update() function. */
	public void update(float seconds)
	{	if (physicsSpace != null)
		{	physicsSpace.update(seconds);
		}
	}

	/** Used by TAGE to generate IDs for physics objects. */
	public int nextUID()
	{	int temp = PhysicsEngine.nextUID;
		PhysicsEngine.nextUID++;
		return temp;
	}
	
	// hinge constraint
	// ball socket constraint
	
	/**
	* The application typically calls this function once every frame to enable accessing collision information for all objects.
	* Results are stored in two data structures accessible from the physics objects:
	* <ul>
	* <li> an ArrayList of manifolds
	* <li> a HashSet of physics objects that the particular object collided with
	* </ul>
	* The application should use the functions in PhysicsObject for accessing the collision results.
	*/
	public void detectCollisions()
	{
		for (PhysicsObject obj : objects) { obj.clearCollisionLists(); }
		
		long[] manifolds = physicsSpace.listManifoldIds();
		for (long manifold : manifolds)
		{	if (PersistentManifolds.countPoints(manifold) > 0)
			{	PhysicsObject obj0 = PhysicsObject.getPhysicsObject(PersistentManifolds.getBodyAId(manifold));
				PhysicsObject obj1 = PhysicsObject.getPhysicsObject(PersistentManifolds.getBodyBId(manifold));
				if (obj0 != null)
				{	obj0.addManifold(manifold);
					obj0.addCollidedObject(obj1);
				}
				if (obj1 != null)
				{	obj1.addManifold(manifold);
					obj1.addCollidedObject(obj0);
	}	}	}	}
}