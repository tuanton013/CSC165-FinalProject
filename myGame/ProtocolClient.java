package myGame;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import tage.networking.client.GameConnectionClient;
import tage.networking.IGameConnection.ProtocolType;

/**
 * Client-side protocol handler for the multiplayer game.
 *
 * Message formats (all comma-separated):
 *   SEND   join,localId
 *   RECV   join,success | join,failure
 *
 *   SEND   create,localId,x,y,z,modelName,textureName
 *   RECV   create,remoteId,x,y,z,modelName,textureName
 *
 *   SEND   bye,localId
 *   RECV   bye,remoteId
 *
 *   RECV   wsds,remoteId   (wants-details – another client wants our info)
 *   SEND   dsfr,localId,remoteId,x,y,z,modelName,textureName
 *
 *   RECV   dsfr,remoteId,x,y,z,modelName,textureName
 *
 *   SEND   move,localId,x,y,z
 *   RECV   move,remoteId,x,y,z
 */
public class ProtocolClient extends GameConnectionClient
{
	private MyGame game;
	private UUID id;
	private GhostManager ghostManager;
	private GhostNPC ghostNPC;

	public ProtocolClient(InetAddress remAddr, int remPort,
	                      ProtocolType pType, MyGame game) throws IOException
	{	super(remAddr, remPort, pType);
		this.game = game;
		this.id   = UUID.randomUUID();
		ghostManager = game.getGhostManager();
	}

	// -----------------------------------------------------------------------
	// Incoming packet handler
	// -----------------------------------------------------------------------

	@Override
	protected void processPacket(Object msg)
	{
		if (msg == null) return;   // null arrives when the UDP receive loop catches a corrupt packet
		String strMessage = (String) msg;
		String[] t = strMessage.split(",");

		if (t.length == 0) return;

		// ---- join ack ----
		if (t[0].compareTo("join") == 0)
		{	if (t.length >= 2 && t[1].compareTo("success") == 0)
			{	game.setIsConnected(true);
				sendCreateMessage(
					game.getPlayerPosition(),
					game.getAvatarModelName(),
					game.getAvatarTextureName());
				sendNeedNPCmsg();
			}
			else
			{	game.setIsConnected(false);
			}
		}

		// ---- remote player left ----
		if (t[0].compareTo("bye") == 0 && t.length >= 2)
		{	UUID ghostID = UUID.fromString(t[1]);
			ghostManager.removeGhostAvatar(ghostID);
		}

		// ---- create or dsfr – spawn / update a ghost ----
		if ((t[0].compareTo("create") == 0 || t[0].compareTo("dsfr") == 0)
		    && t.length >= 6)
		{	UUID ghostID = UUID.fromString(t[1]);
			Vector3f pos  = new Vector3f(
				Float.parseFloat(t[2]),
				Float.parseFloat(t[3]),
				Float.parseFloat(t[4]));
			String modelName   = t[5];
			String textureName = (t.length >= 7) ? t[6] : t[5];
			// Optional rotation quaternion (fields 7-10)
			Matrix4f rotation = null;
			if (t.length >= 11)
			{	Quaternionf q = new Quaternionf(
					Float.parseFloat(t[7]),
					Float.parseFloat(t[8]),
					Float.parseFloat(t[9]),
					Float.parseFloat(t[10]));
				rotation = new Matrix4f().rotation(q);
			}
			try
			{	ghostManager.createOrUpdateGhost(ghostID, pos, modelName, textureName, rotation);
			}
			catch (IOException e)
			{	System.out.println("error creating ghost avatar: " + e.getMessage());
			}
		}

		// ---- wants details – a new client wants our position & avatar ----
		if (t[0].compareTo("wsds") == 0 && t.length >= 2)
		{	UUID remoteID = UUID.fromString(t[1]);
			sendDetailsForMessage(remoteID, game.getPlayerPosition());
		}

		// ---- move – update ghost position, rotation, and animation state ----
		if (t[0].compareTo("move") == 0 && t.length >= 5)
		{	UUID ghostID = UUID.fromString(t[1]);
			Vector3f pos = new Vector3f(
				Float.parseFloat(t[2]),
				Float.parseFloat(t[3]),
				Float.parseFloat(t[4]));
			// Optional rotation quaternion (fields 5-8) + isMoving flag (field 9)
			if (t.length >= 10)
			{	Quaternionf q = new Quaternionf(
					Float.parseFloat(t[5]),
					Float.parseFloat(t[6]),
					Float.parseFloat(t[7]),
					Float.parseFloat(t[8]));
				Matrix4f rotation = new Matrix4f().rotation(q);
				boolean isMoving = t[9].equals("1");
				ghostManager.updateGhostAvatar(ghostID, pos, rotation, isMoving);
			}
			else
			{	ghostManager.updateGhostAvatar(ghostID, pos);
			}
		}

		// ---- createNPC / mnpc ----
		if ((t[0].compareTo("createNPC") == 0 || t[0].compareTo("mnpc") == 0)
			&& t.length >= 5)
		{	Vector3f npcPos = new Vector3f(
				Float.parseFloat(t[1]),
				Float.parseFloat(t[2]),
				Float.parseFloat(t[3]));
			double size = Double.parseDouble(t[4]);
			updateGhostNPC(npcPos, size);
		}

		// ---- isnr ----
		if (t[0].compareTo("isnr") == 0 && t.length >= 5)
		{	Vector3f npcPos = new Vector3f(
				Float.parseFloat(t[1]),
				Float.parseFloat(t[2]),
				Float.parseFloat(t[3]));
			double criteria = Double.parseDouble(t[4]);
			if (game.getPlayerPosition().distance(npcPos) <= (float) criteria)
				sendIsNearMessage();
		}
	}

	private void createGhostNPC(Vector3f position) throws IOException
	{
		if (ghostNPC == null)
		{	ghostNPC = new GhostNPC(0, game.getNPCshape(), game.getNPCtexture(), position);
			ghostNPC.setLocalScale(new org.joml.Matrix4f().scaling(0.2f));
		}
	}

	private void updateGhostNPC(Vector3f position, double gsize)
	{
		if (ghostNPC == null)
		{	try
			{	createGhostNPC(position);
			}
			catch (IOException e)
			{	System.out.println("error creating npc");
			}
		}
		if (ghostNPC != null)
		{	ghostNPC.setPosition(position);
			ghostNPC.setSize(gsize > 1.0);
		}
	}

	// -----------------------------------------------------------------------
	// Outbound message helpers
	// -----------------------------------------------------------------------

	/** Sends initial join request.  format: join,localId */
	public void sendJoinMessage()
	{	try
		{	sendPacket("join," + id.toString());
		}
		catch (IOException e) { e.printStackTrace(); }
	}

	/** format: needNPC,localId */
	public void sendNeedNPCmsg()
	{	try
		{	sendPacket("needNPC," + id.toString());
		}
		catch (IOException e) { e.printStackTrace(); }
	}

	/** format: isnear,localId */
	public void sendIsNearMessage()
	{	try
		{	sendPacket("isnear," + id.toString());
		}
		catch (IOException e) { e.printStackTrace(); }
	}

	/**
	 * Announces this client's avatar to the server.
	 * format: create,localId,x,y,z,modelName,textureName,qx,qy,qz,qw
	 */
	public void sendCreateMessage(Vector3f pos, String modelName, String textureName)
	{	try
		{	Quaternionf q = new Quaternionf().setFromUnnormalized(game.getAvatarRotation());
			String msg = "create," + id.toString()
				+ "," + pos.x() + "," + pos.y() + "," + pos.z()
				+ "," + modelName
				+ "," + textureName
				+ "," + q.x() + "," + q.y() + "," + q.z() + "," + q.w();
			sendPacket(msg);
		}
		catch (IOException e) { e.printStackTrace(); }
	}

	/**
	 * Responds to a wants-details request.
	 * format: dsfr,localId,remoteId,x,y,z,modelName,textureName,qx,qy,qz,qw
	 */
	public void sendDetailsForMessage(UUID remoteID, Vector3f pos)
	{	try
		{	Quaternionf q = new Quaternionf().setFromUnnormalized(game.getAvatarRotation());
			String msg = "dsfr," + id.toString()
				+ "," + remoteID.toString()
				+ "," + pos.x() + "," + pos.y() + "," + pos.z()
				+ "," + game.getAvatarModelName()
				+ "," + game.getAvatarTextureName()
				+ "," + q.x() + "," + q.y() + "," + q.z() + "," + q.w();
			sendPacket(msg);
		}
		catch (IOException e) { e.printStackTrace(); }
	}

	/** format: move,localId,x,y,z,qx,qy,qz,qw,isMoving */
	public void sendMoveMessage(Vector3f pos, Matrix4f rotation, boolean isMoving)
	{	try
		{	Quaternionf q = new Quaternionf().setFromUnnormalized(rotation);
			String msg = "move," + id.toString()
				+ "," + pos.x() + "," + pos.y() + "," + pos.z()
				+ "," + q.x() + "," + q.y() + "," + q.z() + "," + q.w()
				+ "," + (isMoving ? 1 : 0);
			sendPacket(msg);
		}
		catch (IOException e) { e.printStackTrace(); }
	}

	/** format: bye,localId */
	public void sendByeMessage()
	{	try
		{	sendPacket("bye," + id.toString());
		}
		catch (IOException e) { e.printStackTrace(); }
	}

	/**
	 * Returns this client's unique network identifier.
	 *
	 * @return local client UUID
	 */
	public UUID getID() { return id; }

	/**
	 * Returns the client-side GhostNPC, or null if it has not been spawned yet.
	 */
	public GhostNPC getGhostNPC() { return ghostNPC; }
}
