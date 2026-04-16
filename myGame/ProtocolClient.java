package myGame;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

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
			try
			{	ghostManager.createOrUpdateGhost(ghostID, pos, modelName, textureName);
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

		// ---- move – update ghost position ----
		if (t[0].compareTo("move") == 0 && t.length >= 5)
		{	UUID ghostID = UUID.fromString(t[1]);
			Vector3f pos = new Vector3f(
				Float.parseFloat(t[2]),
				Float.parseFloat(t[3]),
				Float.parseFloat(t[4]));
			ghostManager.updateGhostAvatar(ghostID, pos);
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

	/**
	 * Announces this client's avatar to the server.
	 * format: create,localId,x,y,z,modelName,textureName
	 */
	public void sendCreateMessage(Vector3f pos, String modelName, String textureName)
	{	try
		{	String msg = "create," + id.toString()
				+ "," + pos.x() + "," + pos.y() + "," + pos.z()
				+ "," + modelName
				+ "," + textureName;
			sendPacket(msg);
		}
		catch (IOException e) { e.printStackTrace(); }
	}

	/**
	 * Responds to a wants-details request.
	 * format: dsfr,localId,remoteId,x,y,z,modelName,textureName
	 */
	public void sendDetailsForMessage(UUID remoteID, Vector3f pos)
	{	try
		{	String msg = "dsfr," + id.toString()
				+ "," + remoteID.toString()
				+ "," + pos.x() + "," + pos.y() + "," + pos.z()
				+ "," + game.getAvatarModelName()
				+ "," + game.getAvatarTextureName();
			sendPacket(msg);
		}
		catch (IOException e) { e.printStackTrace(); }
	}

	/** format: move,localId,x,y,z */
	public void sendMoveMessage(Vector3f pos)
	{	try
		{	String msg = "move," + id.toString()
				+ "," + pos.x() + "," + pos.y() + "," + pos.z();
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

	public UUID getID() { return id; }
}
