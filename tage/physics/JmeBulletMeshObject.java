package tage.physics;

import com.jme3.bullet.collision.shapes.MeshCollisionShape;
import com.jme3.bullet.collision.shapes.infos.IndexedMesh;

/**
* Mesh physics object TAGE wrapper for libbulletjme MeshCollisionShape (used internally by the TAGE engine for height-mapped terrain).
* <br>
* It is designed specifically to build a height-mapped terrain object.<br>
* The height-map is specified with a JME IndexedMesh.<br>
* Mesh size is determined by float values for scale, height scale, and resolution, where resolution is the dimensionality of the mesh.<br>
* For example, a resolution of 100 would signify a 100x100 grid.<br>
* Note that in TAGE, physics objects should be created using the methods in the TAGE Scenegraph class.<br>
* This constructor therefore should not be called directly.
*/

public class JmeBulletMeshObject extends PhysicsObject
{
	private int resolution;
	private float scale, heightScale;
	
	public JmeBulletMeshObject(int uid, org.joml.Vector3f loc, org.joml.Quaternionf rot, IndexedMesh arr, float scale, float heightScale, int resolution)
	{	super(uid, 0f, loc, rot, new MeshCollisionShape(true, arr));
		this.resolution = resolution;
		this.scale = scale;
		this.heightScale = heightScale;
	}
	
	/** Returns the resolution of this mesh object */
	public int getResolution() { return resolution; }
	
	/** Returns the scale of this mesh object (pertains to axes X and Y) */
	public float getScale() { return scale; }
	
	/** Returns the height scale of this mesh object (maximum height after height mapping) */
	public float getHeightScale() { return heightScale; }
}
