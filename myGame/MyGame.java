package myGame;

import tage.*;
import tage.shapes.*;
import tage.shapes.AnimatedShape.EndType;
import tage.input.*;
import tage.input.action.*;
import tage.networking.IGameConnection.ProtocolType;

import net.java.games.input.Component.Identifier.Key;

import java.lang.Math;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import org.joml.*;

/**
 * Main game class.  Supports both single-player and networked multi-player.
 *
 * Command-line usage:
 *   Single-player : java myGame.MyGame
 *   Multi-player  : java myGame.MyGame <serverIP> <serverPort> UDP
 *
 * At startup the player picks their avatar model and texture via a dialog.
 * The chosen names are included in the CREATE message so every remote client
 * renders the correct ghost avatar for this player.
 */
public class MyGame extends VariableFrameRateGame
{
	// ------------------------------------------------------------------
	// Engine / scene
	// ------------------------------------------------------------------
	private static Engine engine;

	private boolean paused = false;
	private double  lastFrameTime, currFrameTime, elapsTime;

	private GameObject   avatar;
	private GameObject   terrain;
	private GameObject   mazeVisible;   // top + wall faces  -> maze.png texture
	private GameObject   mazeHidden;    // bottom faces      -> no texture
	private TerrainPlane terrainShape;
	private ObjShape     mazeVisibleShape;
	private ObjShape     mazeHiddenShape;
	private Light        light1;

	// Maze geometry constants (from maze.obj bounding box)
	// The maze is shifted +9 in Z so it sits in front of the camera.
	private static final float MAZE_CENTER_X  =  0.16f;   // (xMin+xMax)/2
	private static final float MAZE_FLOOR_Y   = 15.0f;    // maze floats above terrain peaks
	private static final float MAZE_OFFSET_Z  =  9.0f;    // world-space Z shift applied to maze
	private static final float MAZE_START_Z   =  9.80f;   // 0.80 + MAZE_OFFSET_Z
	private static final float MAZE_CENTER_Z  = -0.07f;   // -9.07 + MAZE_OFFSET_Z
	// Maze exit: the opening at the far (negative-Z) end of the maze
	private static final float MAZE_EXIT_Z    = -9.5f;    // trigger zone just before end wall

	// Indoor / outdoor state
	private boolean isOutdoor = false;

	// Available assets (add more as you expand the project)
	private static final String[] MODEL_NAMES   = { "HumanFinal", "dolphinHighPoly.obj", "kir.obj", "newHuman.obj" };
	private static final String[] TEXTURE_NAMES = { "Dolphin_HighPolyUV.jpg", "ice.jpg", "brick1.jpg", "human.png", "new_character_texture.png" };

	// Shared asset caches so ghosts re-use already-loaded resources
	private Map<String, ObjShape>     shapeCache   = new HashMap<>();
	private Map<String, TextureImage> textureCache = new HashMap<>();

	// Animated shape for the HumanFinal model
	private AnimatedShape humanShape;

	// The avatar choices made at startup
	private String avatarModelName;
	private String avatarTextureName;

	// ------------------------------------------------------------------
	// Networking
	// ------------------------------------------------------------------
	private GhostManager   gm;
	private String         serverAddress;
	private int            serverPort;
	private ProtocolType   serverProtocol;
	private ProtocolClient protClient;
	private boolean        isClientConnected = false;

	// ------------------------------------------------------------------
	// Constructors
	// ------------------------------------------------------------------

	/** Single-player (no networking) */
	public MyGame()
	{	super();
		gm = new GhostManager(this);
	}

	/** Multi-player */
	public MyGame(String serverAddress, int serverPort, String protocol)
	{	super();
		gm = new GhostManager(this);
		this.serverAddress  = serverAddress;
		this.serverPort     = serverPort;
		this.serverProtocol = protocol.toUpperCase().compareTo("TCP") == 0
				? ProtocolType.TCP : ProtocolType.UDP;
	}

	// ------------------------------------------------------------------
	// Entry point
	// ------------------------------------------------------------------

	public static void main(String[] args)
	{	MyGame game;
		if (args.length >= 3)
			game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);
		else
			game = new MyGame();

		engine = new Engine(game);
		engine.initializeSystem();
		game.buildGame();
		game.startGame();
	}

	// ------------------------------------------------------------------
	// Avatar-selection dialog (shown before the game window opens)
	// ------------------------------------------------------------------

	private void showAvatarSelectionDialog()
	{	JDialog dialog = new JDialog((Frame) null, "Choose Your Avatar", true);
		dialog.setLayout(new BorderLayout(10, 10));
		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

		JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
		panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));

		panel.add(new JLabel("Avatar model:"));
		JComboBox<String> modelBox = new JComboBox<>(MODEL_NAMES);
		panel.add(modelBox);

		panel.add(new JLabel("Avatar texture:"));
		JComboBox<String> textureBox = new JComboBox<>(TEXTURE_NAMES);
		panel.add(textureBox);

		dialog.add(panel, BorderLayout.CENTER);

		JButton ok = new JButton("Play!");
		ok.addActionListener(e -> dialog.dispose());
		JPanel btnPanel = new JPanel();
		btnPanel.add(ok);
		dialog.add(btnPanel, BorderLayout.SOUTH);

		dialog.pack();
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);   // blocks until dismissed

		avatarModelName   = (String) modelBox.getSelectedItem();
		avatarTextureName = (String) textureBox.getSelectedItem();
	}

	// ------------------------------------------------------------------
	// TAGE lifecycle
	// ------------------------------------------------------------------

	@Override
	public void loadShapes()
	{	// Show selection dialog first so we know which model to highlight
		showAvatarSelectionDialog();

		// Load the animated HumanFinal model
		humanShape = new AnimatedShape("HumanFinal.rkm", "HumanFinal.rks");
		humanShape.loadAnimation("WALK", "HumanFinal.rka");
		shapeCache.put("HumanFinal", humanShape);

		// Pre-load every other OBJ model into the cache
		for (String name : MODEL_NAMES)
			if (!name.equals("HumanFinal"))
				shapeCache.put(name, new ImportedModel(name));

		// Terrain shape (100x100 vertex grid)
		terrainShape = new TerrainPlane(100);

		// Maze split meshes (visible = tops+walls, hidden = undersides)
		mazeVisibleShape = new ImportedModel("maze2_visible.obj");
		mazeHiddenShape  = new ImportedModel("maze2_hidden.obj");
	}

	@Override
	public void loadTextures()
	{	for (String name : TEXTURE_NAMES)
			textureCache.put(name, new TextureImage(name));
			textureCache.put("gridTerrain.jpg",   new TextureImage("gridTerrain.jpg"));
			textureCache.put("trainHeightMap.jpg", new TextureImage("trainHeightMap.jpg"));
	}

	@Override
	public void buildObjects()
	{	// Terrain – Tron grid ground with subtle height variation
		terrain = new GameObject(GameObject.root(), terrainShape, textureCache.get("gridTerrain.jpg"));
		terrain.setLocalScale(new Matrix4f().scaling(100.0f, 22.0f, 100.0f));
		terrain.setLocalTranslation(new Matrix4f().translation(0f, 0f, 0f));
		terrain.setHeightMap(textureCache.get("trainHeightMap.jpg"));
		terrain.setIsTerrain(true);
		terrain.getRenderStates().setTiling(1);
		terrain.getRenderStates().setTileFactor(10);

		// Maze visible faces (tops + walls) – brick texture (swap to maze.png once UV-painted)
		mazeVisible = new GameObject(GameObject.root(), mazeVisibleShape, textureCache.get("brick1.jpg"));
		mazeVisible.setLocalTranslation(new Matrix4f().translation(0f, MAZE_FLOOR_Y, MAZE_OFFSET_Z));
		mazeVisible.setLocalScale(new Matrix4f().scaling(1.0f));

		// Maze hidden faces (undersides) – plain material, closes the geometry
		mazeHidden = new GameObject(GameObject.root(), mazeHiddenShape);
		mazeHidden.setLocalTranslation(new Matrix4f().translation(0f, MAZE_FLOOR_Y, MAZE_OFFSET_Z));
		mazeHidden.setLocalScale(new Matrix4f().scaling(1.0f));

		// Avatar – placed at the mid-point of the maze start edge
		avatar = new GameObject(
			GameObject.root(),
			shapeCache.get(avatarModelName),
			textureCache.get(avatarTextureName));
		if ("HumanFinal".equals(avatarModelName))
		{	avatar.setLocalScale(new Matrix4f().scaling(0.01f));
			avatar.setLocalTranslation(new Matrix4f().translation(MAZE_CENTER_X, 15f, MAZE_START_Z));
		}
		else if ("newHuman.obj".equals(avatarModelName))
		{	// newHuman.obj is ~6.71 units tall in Blender space (Y: -4.49 to +2.22).
			// Lift by maze floor (15) plus the foot offset (4.49 * scale) so feet land on the floor.
			float s = 0.2f;
			avatar.setLocalScale(new Matrix4f().scaling(s));
			avatar.setLocalTranslation(new Matrix4f().translation(MAZE_CENTER_X, 15f + 4.49f * s, MAZE_START_Z));
		}
		else
		{	avatar.setLocalScale(new Matrix4f().scaling(0.2f));
			avatar.setLocalTranslation(new Matrix4f().translation(MAZE_CENTER_X, 15f, MAZE_START_Z));
		}
	}

	@Override
	public void loadSkyBoxes()
	{	tage.SceneGraph sg = engine.getSceneGraph();
		int skyTex = sg.loadCubeMap("fluffyClouds");
		sg.setActiveSkyBoxTexture(skyTex);
		sg.setSkyBoxEnabled(true);
	}

	@Override
	public void initializeLights()
	{	Light.setGlobalAmbient(0.5f, 0.5f, 0.5f);
		light1 = new Light();
		light1.setLocation(new Vector3f(5.0f, 4.0f, 2.0f));
		(engine.getSceneGraph()).addLight(light1);
	}

	@Override
	public void initializeGame()
	{	lastFrameTime = System.currentTimeMillis();
		currFrameTime = System.currentTimeMillis();
		elapsTime     = 0.0;

		(engine.getRenderSystem()).setWindowDimensions(1900, 1000);
		// Camera: elevated above the start edge, looking at the full maze center
		// Maze after shift: Z from 9.8 (start) to -10.1 (end), center = -0.07
		tage.Camera cam = engine.getRenderSystem().getViewport("MAIN").getCamera();
		cam.setLocation(new Vector3f(MAZE_CENTER_X, 30f, 25f));
		cam.lookAt(new Vector3f(MAZE_CENTER_X, 15f, MAZE_CENTER_Z));

		// Networking (only when a server address was supplied)
		if (serverAddress != null)
			setupNetworking();

		// Input bindings
		setupInput();
	}

	@Override
	public void update()
	{	lastFrameTime = currFrameTime;
		currFrameTime = System.currentTimeMillis();
		if (!paused) elapsTime += (currFrameTime - lastFrameTime) / 1000.0;

		if (!isOutdoor)
			(engine.getHUDmanager()).setHUD1(
					"Time = " + Math.round((float) elapsTime) + "s  |  Reach the far end to escape!",
					new Vector3f(1, 0, 0), 15, 15);

		// Poll input devices so MoveAction etc. fire
		engine.getInputManager().update((float) elapsTime);

		// Update skeleton animation if the HumanFinal model is the active avatar
		if (humanShape != null && "HumanFinal".equals(avatarModelName))
			humanShape.updateAnimation();

		// Detect when the player reaches the maze exit and step outside
		if (!isOutdoor && avatar.getWorldLocation().z() < MAZE_EXIT_Z)
			transitionToOutdoor();

		// Terrain-following: keep avatar on the heightmap surface when outdoors
		if (isOutdoor)
		{	Vector3f loc = avatar.getWorldLocation();
			float terrainY = terrain.getHeight(loc.x(), loc.z());
			avatar.setLocalLocation(new Vector3f(loc.x(), terrainY, loc.z()));
		}

		// Follow-camera for outdoor exploration
		if (isOutdoor)
			updateOutdoorCamera();

		processNetworking((float) elapsTime);
	}

	// ------------------------------------------------------------------
	// Networking
	// ------------------------------------------------------------------

	// ------------------------------------------------------------------
	// Outdoor transition
	// ------------------------------------------------------------------

	/** Called once when the avatar crosses the maze exit threshold. */
	private void transitionToOutdoor()
	{	isOutdoor = true;

		// Hide the maze geometry so only sky + terrain are visible
		mazeVisible.getRenderStates().disableRendering();
		mazeHidden.getRenderStates().disableRendering();

		// Snap the avatar to the terrain surface just outside the exit
		float snapZ = MAZE_EXIT_Z - 2f;
		avatar.setLocalLocation(new Vector3f(MAZE_CENTER_X, terrain.getHeight(MAZE_CENTER_X, snapZ), snapZ));

		(engine.getHUDmanager()).setHUD1(
			"You escaped! Explore outside (W/S = move, A/D = turn)",
			new Vector3f(0, 1, 0), 15, 15);

		System.out.println("[MyGame] Player exited the maze – outdoor mode active.");
	}

	/** Third-person follow camera used during outdoor exploration. */
	private void updateOutdoorCamera()
	{	tage.Camera cam = engine.getRenderSystem().getViewport("MAIN").getCamera();
		Vector3f avatarPos = avatar.getWorldLocation();

		// 6 units behind the avatar's facing direction, 3 units above
		Vector4f back = new Vector4f(0f, 0f, 6f, 1f);
		back.mul(avatar.getWorldRotation());
		Vector3f camPos = new Vector3f(
				avatarPos.x() + back.x(),
				avatarPos.y() + back.y() + 3f,
				avatarPos.z() + back.z());
		cam.setLocation(camPos);
		cam.lookAt(new Vector3f(avatarPos.x(), avatarPos.y() + 1f, avatarPos.z()));
	}

	private void setupNetworking()
	{	isClientConnected = false;
		try
		{	protClient = new ProtocolClient(
					InetAddress.getByName(serverAddress),
					serverPort,
					serverProtocol,
					this);
		}
		catch (UnknownHostException e) { e.printStackTrace(); }
		catch (IOException e)          { e.printStackTrace(); }

		if (protClient == null)
			System.out.println("setupNetworking: missing protocol host");
		else
			protClient.sendJoinMessage();
	}

	protected void processNetworking(float elapsTime)
	{	if (protClient != null)
			protClient.processPackets();
	}

	// ------------------------------------------------------------------
	// Input
	// ------------------------------------------------------------------

	private void setupInput()
	{	InputManager im = engine.getInputManager();
		im.associateActionWithAllKeyboards(
				Key.W,
				new MoveAction(this, protClient, +1f),
				IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
				Key.S,
				new MoveAction(this, protClient, -1f),
				IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		// A/D: rotate avatar left/right (used for outdoor exploration)
		im.associateActionWithAllKeyboards(
				Key.A,
				new TurnAction(this, +1f),
				IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
				Key.D,
				new TurnAction(this, -1f),
				IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
	}

	@Override
	public void keyPressed(KeyEvent e)
	{	switch (e.getKeyCode())
		{	case KeyEvent.VK_W:
				if (humanShape != null && "HumanFinal".equals(avatarModelName))
				{	humanShape.stopAnimation();
					humanShape.playAnimation("WALK", 0.15f, EndType.LOOP, 0);
				}
				break;
			case KeyEvent.VK_S:
				if (humanShape != null && "HumanFinal".equals(avatarModelName))
					humanShape.stopAnimation();
				break;
			case KeyEvent.VK_1:
				paused = !paused;
				break;
			case KeyEvent.VK_2:
				avatar.getRenderStates().setWireframe(true);
				break;
			case KeyEvent.VK_3:
				avatar.getRenderStates().setWireframe(false);
				break;
			case KeyEvent.VK_4:
				(engine.getRenderSystem().getViewport("MAIN").getCamera())
						.setLocation(new Vector3f(0, 0, 0));
				break;
			case KeyEvent.VK_5:
				(engine.getRenderSystem().getViewport("MAIN").getCamera())
						.setLocation(new Vector3f(0, 0, 5));
				break;
			case KeyEvent.VK_ESCAPE:
				if (protClient != null && isClientConnected)
					protClient.sendByeMessage();
				System.exit(0);
				break;
		}
		super.keyPressed(e);
	}

	// ------------------------------------------------------------------
	// Accessors used by networking / ghost classes / input actions
	// ------------------------------------------------------------------

	public GameObject   getAvatar()            { return avatar; }
	public Engine       getEngine()            { return engine; }
	public GhostManager getGhostManager()      { return gm; }
	public Vector3f     getPlayerPosition()    { return avatar.getWorldLocation(); }
	public String       getAvatarModelName()   { return avatarModelName; }
	public String       getAvatarTextureName() { return avatarTextureName; }

	public void    setIsConnected(boolean b)   { isClientConnected = b; }
	public boolean getIsConnected()            { return isClientConnected; }

	/** Returns (loading if necessary) the ObjShape for the given filename. */
	public ObjShape getGhostShape(String modelName)
	{	return shapeCache.computeIfAbsent(modelName, ImportedModel::new);
	}

	/** Returns (loading if necessary) the TextureImage for the given filename. */
	public TextureImage getGhostTexture(String textureName)
	{	return textureCache.computeIfAbsent(textureName, TextureImage::new);
	}

	/** Convenience overloads – return the local player's own assets. */
	public ObjShape     getGhostShape()   { return getGhostShape(avatarModelName); }
	public TextureImage getGhostTexture() { return getGhostTexture(avatarTextureName); }
}