package myGame;

import java.io.IOException;
import tage.networking.IGameConnection.ProtocolType;

/**
 * Stand-alone networking server driver.
 *
 * Usage:
 *   java myGame.NetworkingServer <port> <protocol>
 *
 * Example:
 *   java myGame.NetworkingServer 6000 UDP
 */
public class NetworkingServer
{
	private GameAIServerUDP thisUDPServer;
	private NPCcontroller npcCtrl;

	/**
	 * Creates and starts the UDP networking server with NPC AI support.
	 *
	 * @param serverPort server listen port
	 * @param protocol protocol name (UDP expected)
	 */
	public NetworkingServer(int serverPort, String protocol)
	{	try
		{	if (protocol.toUpperCase().compareTo("UDP") == 0)
			{	npcCtrl = new NPCcontroller();
				thisUDPServer = new GameAIServerUDP(serverPort, npcCtrl);
				npcCtrl.start(thisUDPServer);
				System.out.println("UDP game + AI server started on port " + serverPort);
			}
			else
			{	System.out.println("Only UDP is supported in this implementation.");
			}
		}
		catch (IOException e) { e.printStackTrace(); }
	}

	/**
	 * Program entry point for the networking server process.
	 *
	 * @param args expected as <port> <protocol>
	 */
	public static void main(String[] args)
	{	if (args.length < 2)
		{	System.out.println("Usage: NetworkingServer <port> <protocol>");
			return;
		}
		new NetworkingServer(Integer.parseInt(args[0]), args[1]);

		// Keep the server alive
		System.out.println("Server is running. Press Ctrl-C to stop.");
		try
		{	Thread.currentThread().join();
		}
		catch (InterruptedException e) { e.printStackTrace(); }
	}
}
