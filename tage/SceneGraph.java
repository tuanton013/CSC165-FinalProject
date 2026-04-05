package tage;
import java.util.*;
import tage.shapes.*;
import tage.nodeControllers.*;
import tage.physics.*;
import org.joml.*;

/**
* Tools for building a scene graph tree, and building and applying the associated node controllers.
* A game application should use the tools here for adding game objects, lights, and node controllers.
* The game objects and node controllers are stored in ArrayLists.
* The renderer also uses methods here for applying node controllers at each frame.
* <p>
* The functions here that are useful for the game application are:
* <ul>
* <li> addLight()
* <li> removeLight()
* <li> addNodeController()
* <li> getRoot()
* <li> loadCubeMap()
* <li> removeGameObject()
* <li> getNumGameObjects()
* <li> addPhysicsXXX() - where XXX is the desired physics object shape
* <li> removePhysicsObject()
* </ul>
* <p>
* It is important to understand that adding a game object doesn't require calling addGameObject().
* That function is called by the engine.  All that is necessary is to use one of the GameObject constructors.
* They will call addGameObject().  Similarly it isn't necessary to call buildSkyBox(), that happens automatically.
* However, adding a Light or a Node Controller does necessitate calling addLight() and addNodeController().
* <p>
* Loading a skybox texture into an OpenGL CubeMap using loadCubeMap() is done as follows.
* The game application needs to supply the name of a folder containing the 6 textures.
* The folder is assumed to be in the assets/skyboxes folder.
* The resulting OpenGL CubeMap is referenced by the returned integer.
* The game application should store that integer to refer to the cubemap if it wishes to swap
* between multiple cubemaps.  That can be done by calling setActiveSkyBoxTexture() with the integer
* skybox reference provided as a parameter. All cubemaps should be loaded before starting the game loop.
* <p>
* Creation of physics objects is also done using the functions in this SceneGraph class.
* @author Scott Gordon
*/

public class SceneGraph
{	private static GameObject root;
	private ArrayList<GameObject> gameObjects = new ArrayList<GameObject>();
	private ArrayList<NodeController> nodeControllers = new ArrayList<NodeController>();
	private Vector<GameObject> physicsRenderables = new Vector<GameObject>();
	private Engine engine;

	private RenderSystem rs;
	private PhysicsEngine pe;
	private PhysicsObject po;
	private GameObject go;
	private NodeController nc, nci;
	private GameObject skybox;
	private boolean skyboxEnabled = false;
	private int activeSkyBoxTexture;
	private RenderStates physicsVisualizationState;
	
	private GameObject physicsRoot;
	private ObjShape physicsBox, physicsSphere, physicsCylinder, physicsCone, physicsCapsule, physicsPlane;
	private float halfExtents[] = new float[3];

	protected SceneGraph(Engine e)
	{	engine = e;
		root = GameObject.createRoot();
		preparePhysicsDisplayObjects();
	}

	// -------------- LIGHT SECTION ------------------------

	/** adds the specified Light object to the LightManager for rendering. */
	public void addLight(Light light) { (engine.getLightManager()).addLight(light); }
	
	/** removes the specified Light object, using the LightManager, if the light is installed, but doesn't delete the Light object. */
	public void removeLight(Light light) { (engine.getLightManager()).removeLight(light); }

	// -------------- NODE CONTROLLER SECTION -------------------

	/** adds the specified node controller for use in the game. */
	public void addNodeController(NodeController nc) { nodeControllers.add(nc); }

	// Apply the node controllers to their attached objects - for engine use only.
	// Called by RenderSystem, should not be called by the game application directly.

	protected void applyNodeControllers()
	{	for (int i = 0; i < nodeControllers.size(); i++)
		{	nci = nodeControllers.get(i);
			if (nci.isEnabled()) nci.applyController();
	}	}

	// -------------- GAME OBJECT SECTION ---------------------

	/** returns the current number of GameObjects. */
	public int getNumGameObjects() { return gameObjects.size(); }

	/** returns a reference to the entire ArrayList of GameObjects - not likely to be useful to the game application. */
	public ArrayList<GameObject> getGameObjects() { return gameObjects; }

	protected GameObject getGameObject(int i) { return gameObjects.get(i); }
	protected GameObject getRoot() { return root; }
	protected void updateAllObjectTransforms() { root.update(); }

	/** removes the specified GameObject from the scenegraph. */
	public void removeGameObject(GameObject go)
	{	if (go.hasChildren())
		{	System.out.println("attempted deletion of game object with children");
		}
		else
		{	// first delete any NodeController references to the game object
			for (int i = 0; i < nodeControllers.size(); i++)
			{	nci = nodeControllers.get(i);
				if (nci.hasTarget(go)) nci.removeTarget(go);
			}
			// then remove the object, also removing the parent reference
			if (go.getParent() != null) (go.getParent()).removeChild(go);
			if (gameObjects.contains(go)) gameObjects.remove(go);
		}
	}

	protected void addGameObject(GameObject g) { gameObjects.add(g); }

	//------------- SKYBOX SECTION ---------------------

	/** loads a set of six skybox images into an OpenGL cubemap so that it can be used in an OpenGL skybox. */
	public int loadCubeMap(String foldername)
	{	int skyboxTexture = Utils.loadCubeMap("assets/skyboxes/"+foldername);
		return skyboxTexture;
	}

	/** returns a boolean that is true if skybox rendering has been enabled */
	public boolean isSkyboxEnabled() { return skyboxEnabled; }

	/** sets whether or not to render a skybox */
	public void setSkyBoxEnabled(boolean sbe) { skyboxEnabled = sbe; }

	/** specifies which loaded skybox should be rendered */
	public void setActiveSkyBoxTexture(int tex) { activeSkyBoxTexture = tex; }

	/** returns an integer reference to the current active skybox */
	public int getActiveSkyBoxTexture() { return activeSkyBoxTexture; }

	protected GameObject getSkyBoxObject() { return skybox; }

	protected void buildSkyBox()
	{	skybox = new GameObject(new SkyBoxShape());
	}

	//------------------- PHYSICS SECTION -----------------------------

	protected void setPhysicsEngine(PhysicsEngine peng) { pe = peng; }

	/** returns the physics engine object */
	public PhysicsEngine getPhysicsEngine() { return pe; }
	
	// returns the GameObjects that are physics renderables - used by the renderer.
	protected Vector<GameObject> getPhysicsRenderables() { return physicsRenderables; }
	
	// prepares the physics objects and shapes for displaying the physics world if enabled
	protected void preparePhysicsDisplayObjects()
	{	physicsRoot = new GameObject();
		physicsBox = new Cube();
		physicsSphere = new Sphere(8);
		physicsCone = new ImportedModel("cone.obj", "assets/defaultAssets/");
		physicsCylinder = new ImportedModel("cylinder.obj", "assets/defaultAssets/");
		physicsCapsule = new ImportedModel("capsule.obj", "assets/defaultAssets/");
		physicsPlane = new ImportedModel("plane.obj", "assets/defaultAssets/");
		physicsVisualizationState = new RenderStates();
		physicsVisualizationState.hasLighting(false);
		physicsVisualizationState.setHasSolidColor(true);
		physicsVisualizationState.setColor(new Vector3f(.5f,.5f,.5f));
		physicsVisualizationState.setWireframe(true);
	}
	
	/** Accessor for the RenderState object used by the physics engine while rendering the physics world visualization. */
	public RenderStates getPhysicsVisualizationState() { return physicsVisualizationState.makeCopy(); }
	
	// sets relevant parameters for physics object and corresponding renderable (if physics visualization enabled).
	protected void createPhysicsObjectRenderable(ObjShape shape, Matrix4f m)
	{	go = new GameObject();
		go.setShape(shape);
		go.setFirstParent(physicsRoot);
		go.setLocalScale(m);
		go.setRenderStates(physicsVisualizationState.makeCopy());
		go.setPhysicsObject(po);
		physicsRenderables.add(go);
	}
	
	protected void createPhysicsTerrainMeshRenderable(PhysicsObject po, TextureImage heightMap)
	{	GameObject goPR = new GameObject();
		ObjShape terrPR = new TerrainPlane(((JmeBulletMeshObject)po).getResolution());
		goPR.setShape(terrPR);
		goPR.setHeightMap(heightMap);
		goPR.setFirstParent(physicsRoot);
		
		float scale = ((JmeBulletMeshObject)po).getScale();
		float heightScale = ((JmeBulletMeshObject)po).getHeightScale();
		goPR.setLocalScale(new Matrix4f().scaling(scale, heightScale, scale));

		goPR.setRenderStates(physicsVisualizationState.makeCopy());
		(goPR.getRenderStates()).setColor(new Vector3f(.5f,.5f,0f));
		goPR.setPhysicsObject(po);
		physicsRenderables.add(goPR);
	}

	/** 
	* Adds a box physics object in the physics world, with specified mass, location and rotation information, and size.
	* <br>
	* Location and rotation are specified using JOML Vector3f and Quaternionf, respectfully.<br>
	* The size is specified using a 3-element array of type double, with desired dimensions in X, Y, and Z.<br>
	* Also adds a corresponding renderable object if displaying the physics world has been enabled.
	*/
	public PhysicsObject addPhysicsBox(float mass, Vector3f loc, Quaternionf rot, float[] size)
	{	po = pe.addBoxObject(pe.nextUID(), mass, loc, rot, size);
		createPhysicsObjectRenderable(physicsBox, (new Matrix4f()).scaling(size[0]/2f, size[1]/2f, size[2]/2f));
		return po;
	}
	
	/**
	* Adds a sphere physics object in the physics world, with specified mass, location and rotation information, and radius.
	* <br>
	* Location and rotation are specified using JOML Vector3f and Quaternionf, respectfully.<br>
	* The size is specified using a float value for the sphere's radius.<br>
	* Also adds a corresponding renderable object if displaying the physics world has been enabled.
	*/
	public PhysicsObject addPhysicsSphere(float mass, Vector3f loc, Quaternionf rot, float radius)
	{	po = pe.addSphereObject(pe.nextUID(), mass, loc, rot, radius);
		createPhysicsObjectRenderable(physicsSphere, (new Matrix4f()).scaling(radius));
		return po;
	}

	/**
	* Adds a cone physics object in the physics world, with specified mass, location and rotation information, dimensions, and axis alignment.
	* <br>
	* Location and rotation are specified using JOML Vector3f and Quaternionf, respectfully.<br>
	* The dimensions are specified with float values for radius of the circle base, and height of the cone.<br>
	* The cone aligns with either the X, Y, or Z world axis, depending on the specified float value of axis (0=X, 1=Y, 2=Z).<br>
	* Also adds a corresponding renderable object if displaying the physics world has been enabled.
	*/
	public PhysicsObject addPhysicsCone(float mass, Vector3f loc, Quaternionf rot, int axis, float radius, float height)
	{	po = pe.addConeObject(pe.nextUID(), mass, loc, rot, axis, radius, height);
		createPhysicsObjectRenderable(physicsCone, (new Matrix4f()).scaling(radius, height/2f, radius));
		
		if (axis == 0)
		{	go.getRenderStates().setModelOrientationCorrection(
			(new Matrix4f()).rotationZ((float)java.lang.Math.toRadians(270.0f)));
		}
		else if (axis == 1)
		{	// no need for axis correction
		}
		else
		{	go.getRenderStates().setModelOrientationCorrection(
			(new Matrix4f()).rotationZ((float)java.lang.Math.toRadians(90.0f)));
		}
		return po;
	}

	/**
	* Adds a capsule physics object in the physics world, with specified mass, location and rotation information, dimensions, and axis alignment.
	* <br>
	* A capsule has cylindrical sides, but with rounded semi-spherical ends.<br>
	* The dimensions of a capsule are specified with radius and height, where radius is for the semi-spheres on each end, and height is the connecting cylinder.<br>
	* The total length of the capsule is height + 2 * radius.<br>
	* Location and rotation are specified using JOML Vector3f and Quaternionf, respectfully.<br>
	* The capsule aligns with either the X, Y, or Z world axis, depending on the specified float value of axis (0=X, 1=Y, 2=Z).<br>
	* Also adds a corresponding renderable object if displaying the physics world has been enabled.
	*/
	public PhysicsObject addPhysicsCapsule(float mass, Vector3f loc, Quaternionf rot, int axis, float radius, float height)
	{	po = pe.addCapsuleObject(pe.nextUID(), mass, loc, rot, axis, radius, height);
		createPhysicsObjectRenderable(physicsCapsule, (new Matrix4f()).scaling(radius, (height+radius*2f)/4f, radius));
		
		if (axis == 0)
		{	go.getRenderStates().setModelOrientationCorrection(
			(new Matrix4f()).rotationZ((float)java.lang.Math.toRadians(270.0f)));
		}
		else if (axis == 1)
		{	// no need for axis correction
		}
		else
		{	go.getRenderStates().setModelOrientationCorrection(
			(new Matrix4f()).rotationZ((float)java.lang.Math.toRadians(90.0f)));
		}
		return po;
	}

	/**
	* Adds a cylinder physics object in the physics world, with specified mass, location and rotation information, dimensions, and axis alignment.
	* <br>
	* The dimensions of a cylinder are specified with float values for radius and height.<br>
	* Location and rotation are specified using JOML Vector3f and Quaternionf, respectfully.<br>
	* The capsule aligns with either the X, Y, or Z world axis, depending on the specified float value of axis (0=X, 1=Y, 2=Z).<br>
	* Also adds a corresponding renderable object if displaying the physics world has been enabled.
	*/
	public PhysicsObject addPhysicsCylinder(float mass, Vector3f loc, Quaternionf rot, int axis, float radius, float height)
	{	halfExtents[0] = radius; halfExtents[1] = height; halfExtents[2] = radius;
		po = pe.addCylinderObject(pe.nextUID(), mass, loc, rot, axis, halfExtents);
		createPhysicsObjectRenderable(physicsCylinder, (new Matrix4f()).scaling(radius, height, radius));
		
		if (axis == 0)
		{	go.getRenderStates().setModelOrientationCorrection(
			(new Matrix4f()).rotationZ((float)java.lang.Math.toRadians(270.0f)));
		}
		else if (axis == 1)
		{	// no need for axis correction
		}
		else
		{	go.getRenderStates().setModelOrientationCorrection(
			(new Matrix4f()).rotationZ((float)java.lang.Math.toRadians(90.0f)));
		}
		return po;
	}

	/**
	* Adds a a static plane object in the physics world, with specified location and rotation information, up vector and location offset.
	* <br>
	* Location and rotation are specified using JOML Vector3f and Quaternionf, respectfully.<br>
	* The up vector, usually set to (0,1,0), defines a normal to the plane, and is opposite the pull of gravity.<br>
	* The plane_constant, usually set to 0, is an offset (positive or negative) for moving the plane up and down along its up vector.<br>
	* Also adds a corresponding renderable object if displaying the physics world has been enabled.<br>
	* The renderable object is a 100x100 rectangle, although the actual physics object is infinite.
	*/
	public PhysicsObject addPhysicsStaticPlane(Vector3f loc, Quaternionf rot, float[] up_vector, float plane_constant)
	{	po = pe.addStaticPlaneObject(pe.nextUID(), loc, rot, up_vector, plane_constant);
		createPhysicsObjectRenderable(physicsPlane, (new Matrix4f()).scaling(100));
		return po;
	}

	/**
	* Adds a a static terrain mesh object in the physics world, with specified location and rotation information, height map texture image, scale, and resolution.
	* <br>
	* Location and rotation are specified using JOML Vector3f and Quaternionf, respectfully.<br>
	* The height map is of type TextureImage and must have been already specified in loadTextures();<br>
	* Scale is specified with two float values, one that is applied to the X and Z dimensions, and one the is applied in the Y dimension (to the height mapping).<br>
	* Resolution is an integer value that determines the number of subdivisions on each side. For example, a resolution of 100 would result in a 100x100 mesh.<br>
	* Also adds a corresponding renderable object if displaying the physics world has been enabled.<br>
	* The renderable object is the same dimensionality and resolution as the physics object.<br>
	* Since creation of a terrain mesh necessitates creating a TAGE Shape, this function should only be called during initializePhysicsObject().
	*/
	public PhysicsObject addPhysicsStaticTerrainMesh(Vector3f loc, Quaternionf rot, TextureImage heightMapTexture,
			float scale, float heightScale, int resolution)
	{	float[] vertices = new float[resolution * resolution * 3];
		int[] indices = new int[(resolution-1) * (resolution-1) * 6];

		// use same spacing calculation as TerrainPlane (distance between vertices)
		float spacing = 1.0f/(float)(resolution-1);
		
		// generate vertices using TerrainPlane's coordinate system
		for (int i = 0; i < resolution; i++)
		{	for (int j = 0; j < resolution; j++)
			{	int baseIndex = (i * resolution + j) * 3;
				
				// same positioning logic as TerrainPlane
				float worldX = (i*2.0f*spacing-1.0f) * scale;
				float worldZ = (j*2.0f*spacing-1.0f) * scale;
				
				// same texture coordinate calculation as TerrainPlane
				float u = i*spacing;
				float v = 1.0f-j*spacing; // flip here to match TerrainPlane
				
				// get height from texture using visual terrain's texture coordinates
				float height = engine.getRenderSystem().getHeightAt(
					heightMapTexture.getTexture(), u, v) * heightScale;
					
				// store vertex
				vertices[baseIndex + 0] = worldX;
				vertices[baseIndex + 1] = height;
				vertices[baseIndex + 2] = worldZ;
		}	}
		
		// generate indices for triangles (same pattern as TerrainPlane)
		int index = 0;
		for (int i = 0; i < resolution - 1; i++)
		{	for (int j = 0; j < resolution - 1; j++)
			{	int bottomLeft = i*resolution+j;
				int bottomRight = bottomLeft+1;
				int topLeft = (i+1)*resolution+j;
				int topRight = topLeft+1;
				
				// use the same triangle indices as TerrainPlane
				indices[index++] = bottomLeft;
				indices[index++] = bottomRight;
				indices[index++] = topLeft;
				
				indices[index++] = bottomRight;
				indices[index++] = topRight;
				indices[index++] = topLeft;
		}	}
		
		// Create the physics object
		PhysicsObject po = pe.addStaticTerrainMesh(pe.nextUID(), loc, rot, vertices, indices, scale, heightScale, resolution);
		createPhysicsTerrainMeshRenderable(po, heightMapTexture);
		return po;
	}

	/** removes the specified PhysicsObject from the physics world, and its associated renderable, if one exists. */
	public void removePhysicsObject(PhysicsObject po)
	{	GameObject toRemove = null;
	
		// look for it in the renderables queue
		for (int i = 0; i < physicsRenderables.size(); i++)
		{	GameObject go = physicsRenderables.get(i);
			if (go.getPhysicsObject() == po) toRemove = go;
		}
		// if it is found in the renderables queue, remove it from the root list and the renderables list
		if (toRemove != null)
		{	if (toRemove.getParent() != null) (toRemove.getParent()).removeChild(toRemove);
			physicsRenderables.remove(toRemove);
		}
		// finally, tell the physics engine to remove the physics object itself
		pe.removeObject(po.getUID());
	}
}










