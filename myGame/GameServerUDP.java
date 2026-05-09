package myGame;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import tage.networking.server.GameConnectionServer;
import tage.networking.server.IClientInfo;

/**
 * UDP game server. Handles JOIN, CREATE, MOVE, DETAILS-FOR (dsfr), and BYE
 * messages from connected clients.
 *
 * CREATE message format is extended to carry the avatar model and texture names
 * so every remote client can render the correct ghost avatar:
 *   create,localId,x,y,z,modelName,textureName
 */
public class GameServerUDP extends GameConnectionServer<UUID>
{
	public GameServerUDP(int localPort) throws IOException
	{	super(localPort, ProtocolType.UDP);	}

	@Override
	public void processPacket(Object o, InetAddress senderIP, int senderPort)
	{
		String message = (String) o;
		String[] msgTokens = message.split(",");

		if (msgTokens.length == 0) return;

		// ------------------------------------------------------------------
		// JOIN  –  format: join,localId
		// ------------------------------------------------------------------
		if (msgTokens[0].compareTo("join") == 0)
		{
			if (msgTokens.length >= 2)
			{	try
				{	IClientInfo ci = getServerSocket().createClientInfo(senderIP, senderPort);
					UUID clientID = UUID.fromString(msgTokens[1]);
					addClient(ci, clientID);
					sendJoinedMessage(clientID, true);
				}
				catch (IOException e) { e.printStackTrace(); }
			}
		}

		// ------------------------------------------------------------------
		// CREATE  –  format: create,localId,x,y,z,modelName,textureName[,qx,qy,qz,qw]
		// ------------------------------------------------------------------
		if (msgTokens[0].compareTo("create") == 0)
		{
			if (msgTokens.length >= 7)
			{	UUID clientID = UUID.fromString(msgTokens[1]);
				// Collect ALL fields after the sender ID so rotation tokens are preserved.
				String[] details = new String[msgTokens.length - 2];
				for (int i = 2; i < msgTokens.length; i++)
					details[i - 2] = msgTokens[i];
				sendCreateMessages(clientID, details);
				sendWantsDetailsMessages(clientID);
			}
			else if (msgTokens.length >= 6)
			{	// legacy: create,localId,x,y,z,modelName (no texture token)
				UUID clientID = UUID.fromString(msgTokens[1]);
				String[] details = { msgTokens[2], msgTokens[3], msgTokens[4],
				                     msgTokens[5], msgTokens[5] };
				sendCreateMessages(clientID, details);
				sendWantsDetailsMessages(clientID);
			}
		}

		// ------------------------------------------------------------------
		// BYE  –  format: bye,localId
		// ------------------------------------------------------------------
		if (msgTokens[0].compareTo("bye") == 0)
		{
			if (msgTokens.length >= 2)
			{	UUID clientID = UUID.fromString(msgTokens[1]);
				sendByeMessages(clientID);
				removeClient(clientID);
			}
		}

		// ------------------------------------------------------------------
		// DETAILS-FOR (dsfr)
		// format: dsfr,localId,remoteId,x,y,z,modelName,textureName[,qx,qy,qz,qw]
		// ------------------------------------------------------------------
		if (msgTokens[0].compareTo("dsfr") == 0)
		{
			if (msgTokens.length >= 8)
			{	UUID senderID = UUID.fromString(msgTokens[1]);
				UUID remoteID = UUID.fromString(msgTokens[2]);
				// Collect ALL fields after remoteId so rotation tokens are preserved.
				String[] details = new String[msgTokens.length - 3];
				for (int i = 3; i < msgTokens.length; i++)
					details[i - 3] = msgTokens[i];
				sendDetailsForMessage(senderID, remoteID, details);
			}
		}

		// ------------------------------------------------------------------
		// MOVE  –  format: move,localId,x,y,z[,qx,qy,qz,qw,isMoving]
		// ------------------------------------------------------------------
		if (msgTokens[0].compareTo("move") == 0)
		{
			if (msgTokens.length >= 5)
			{	UUID clientID = UUID.fromString(msgTokens[1]);
				// Collect ALL fields after the sender ID so rotation and isMoving are preserved.
				String[] data = new String[msgTokens.length - 2];
				for (int i = 2; i < msgTokens.length; i++)
					data[i - 2] = msgTokens[i];
				sendMoveMessages(clientID, data);
			}
		}
	}

	// -----------------------------------------------------------------------
	// Outbound message helpers
	// -----------------------------------------------------------------------

	/** format: join,success  or  join,failure */
	public void sendJoinedMessage(UUID clientID, boolean success)
	{	try
		{	String msg = "join," + (success ? "success" : "failure");
			sendPacket(msg, clientID);
		}
		catch (IOException e) { e.printStackTrace(); }
	}

	/**
	 * Forwards the CREATE message to all OTHER clients.
	 * format: create,remoteId,x,y,z,modelName,textureName[,qx,qy,qz,qw]
	 */
	public void sendCreateMessages(UUID clientID, String[] details)
	{	try
		{	StringBuilder msg = new StringBuilder("create,").append(clientID.toString());
			for (String d : details) msg.append(',').append(d);
			forwardPacketToAll(msg.toString(), clientID);
		}
		catch (IOException e) { e.printStackTrace(); }
	}

	/**
	 * Tells the newly joined client to ask every existing client for their
	 * current details.
	 * format: wsds,remoteId
	 */
	public void sendWantsDetailsMessages(UUID clientID)
	{	try
		{	String msg = "wsds," + clientID.toString();
			forwardPacketToAll(msg, clientID);
		}
		catch (IOException e) { e.printStackTrace(); }
	}

	/**
	 * Sends a DETAILS-FOR packet to the target client (remoteID) telling them
	 * what senderID looks like and where they are.
	 * format: dsfr,senderId,x,y,z,modelName,textureName[,qx,qy,qz,qw]
	 */
	public void sendDetailsForMessage(UUID senderID, UUID remoteID, String[] details)
	{	try
		{	StringBuilder msg = new StringBuilder("dsfr,").append(senderID.toString());
			for (String d : details) msg.append(',').append(d);
			sendPacket(msg.toString(), remoteID);
		}
		catch (IOException e) { e.printStackTrace(); }
	}

	/**
	 * Forwards a MOVE update to all clients except the mover.
	 * format: move,remoteId,x,y,z[,qx,qy,qz,qw,isMoving]
	 */
	public void sendMoveMessages(UUID clientID, String[] data)
	{	try
		{	StringBuilder msg = new StringBuilder("move,").append(clientID.toString());
			for (String d : data) msg.append(',').append(d);
			forwardPacketToAll(msg.toString(), clientID);
		}
		catch (IOException e) { e.printStackTrace(); }
	}

	/**
	 * Tells all remaining clients that a player has left.
	 * format: bye,remoteId
	 */
	public void sendByeMessages(UUID clientID)
	{	try
		{	String msg = "bye," + clientID.toString();
			forwardPacketToAll(msg, clientID);
		}
		catch (IOException e) { e.printStackTrace(); }
	}
}
