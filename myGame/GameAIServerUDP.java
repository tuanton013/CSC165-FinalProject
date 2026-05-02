package myGame;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

/**
 * UDP game server with additional AI/NPC networking protocol.
 */
public class GameAIServerUDP extends GameServerUDP
{
	private NPCcontroller npcCtrl;

	/**
	 * Constructs an AI-enabled UDP game server.
	 *
	 * @param localPort port to bind
	 * @param npc NPC controller used by this server
	 * @throws IOException if socket initialization fails
	 */
	public GameAIServerUDP(int localPort, NPCcontroller npc) throws IOException
	{
		super(localPort);
		npcCtrl = npc;
	}

	/**
	 * Broadcasts a proximity-check request for all clients against the NPC.
	 */
	public void sendCheckForAvatarNear()
	{
		try
		{
			NPC npc = npcCtrl.getNPC();
			if (npc == null)
				return;
			String message = "isnr"
				+ "," + npc.getX()
				+ "," + npc.getY()
				+ "," + npc.getZ()
				+ "," + npcCtrl.getCriteria();
			sendPacketToAll(message);
		}
		catch (IOException e)
		{
			System.out.println("couldnt send isnr msg");
			e.printStackTrace();
		}
	}

	/**
	 * Broadcasts NPC position and size updates to all clients.
	 */
	public void sendNPCinfo()
	{
		try
		{
			NPC npc = npcCtrl.getNPC();
			if (npc == null)
				return;
			String message = "mnpc"
				+ "," + npc.getX()
				+ "," + npc.getY()
				+ "," + npc.getZ()
				+ "," + npc.getSize();
			sendPacketToAll(message);
		}
		catch (IOException e)
		{
			System.out.println("couldnt send mnpc msg");
			e.printStackTrace();
		}
	}

	/**
	 * Sends initial NPC spawn data to a specific client.
	 *
	 * @param clientID target client identifier
	 */
	public void sendNPCstart(UUID clientID)
	{
		try
		{
			NPC npc = npcCtrl.getNPC();
			if (npc == null)
				return;
			String message = "createNPC"
				+ "," + npc.getX()
				+ "," + npc.getY()
				+ "," + npc.getZ()
				+ "," + npc.getSize();
			sendPacket(message, clientID);
		}
		catch (IOException e)
		{
			System.out.println("couldnt send createNPC msg");
			e.printStackTrace();
		}
	}

	/**
	 * Processes incoming packets including base player protocol and NPC messages.
	 *
	 * @param o packet payload
	 * @param senderIP sender IP address
	 * @param port sender port
	 */
	@Override
	public void processPacket(Object o, InetAddress senderIP, int port)
	{
		super.processPacket(o, senderIP, port);

		String message = (String) o;
		String[] messageTokens = message.split(",");
		if (messageTokens.length == 0)
			return;

		if (messageTokens[0].compareTo("needNPC") == 0)
		{
			System.out.println("server got a needNPC message");
			if (messageTokens.length >= 2)
			{
				UUID clientID = UUID.fromString(messageTokens[1]);
				sendNPCstart(clientID);
			}
		}

		if (messageTokens[0].compareTo("isnear") == 0)
		{
			if (messageTokens.length >= 2)
			{
				UUID clientID = UUID.fromString(messageTokens[1]);
				handleNearTiming(clientID);
			}
		}
	}

	/**
	 * Handles a near-event report from a client.
	 *
	 * @param clientID reporting client ID
	 */
	public void handleNearTiming(UUID clientID)
	{
		npcCtrl.setNearFlag(true);
	}
}
