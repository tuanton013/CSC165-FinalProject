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
	private GameServerUDP thisUDPServer;

	public NetworkingServer(int serverPort, String protocol)
	{	try
		{	if (protocol.toUpperCase().compareTo("UDP") == 0)
			{	thisUDPServer = new GameServerUDP(serverPort);
				System.out.println("UDP game server started on port " + serverPort);
			}
			else
			{	System.out.println("Only UDP is supported in this implementation.");
			}
		}
		catch (IOException e) { e.printStackTrace(); }
	}

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
