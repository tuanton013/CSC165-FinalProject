package myGame;

import tage.*;
import tage.shapes.*;
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
	private Light        light1;

	// Available assets (add more as you expand the project)
	private static final String[] MODEL_NAMES   = { "dolphinHighPoly.obj" };
	private static final String[] TEXTURE_NAMES = { "Dolphin_HighPolyUV.jpg", "ice.jpg", "brick1.jpg" };

	// Shared asset caches so ghosts re-use already-loaded resources
	private Map<String, ObjShape>     shapeCache   = new HashMap<>();
	private Map<String, TextureImage> textureCache = new HashMap<>();

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

		// Pre-load every available model into the cache
		for (String name : MODEL_NAMES)
			shapeCache.put(name, new ImportedModel(name));
	}

	@Override
	public void loadTextures()
	{	for (String name : TEXTURE_NAMES)
			textureCache.put(name, new TextureImage(name));
	}

	@Override
	public void buildObjects()
	{	avatar = new GameObject(
			GameObject.root(),
			shapeCache.get(avatarModelName),
			textureCache.get(avatarTextureName));
		avatar.setLocalTranslation((new Matrix4f()).translation(0, 0, 0));
		avatar.setLocalScale((new Matrix4f()).scaling(3.0f));
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
		(engine.getRenderSystem().getViewport("MAIN").getCamera())
				.setLocation(new Vector3f(0, 0, 5));

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

		(engine.getHUDmanager()).setHUD1(
				"Time = " + Math.round((float) elapsTime),
				new Vector3f(1, 0, 0), 15, 15);

		// Poll input devices so MoveAction etc. fire
		engine.getInputManager().update((float) elapsTime);

		processNetworking((float) elapsTime);
	}

	// ------------------------------------------------------------------
	// Networking
	// ------------------------------------------------------------------

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
	}

	@Override
	public void keyPressed(KeyEvent e)
	{	switch (e.getKeyCode())
		{	case KeyEvent.VK_1:
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