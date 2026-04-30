package myGame;

import java.util.Random;

import tage.ai.behaviortrees.BTCompositeType;
import tage.ai.behaviortrees.BTSequence;
import tage.ai.behaviortrees.BehaviorTree;

/**
 * Controls server-side NPC behavior using a behavior tree and tick/think loop.
 */
public class NPCcontroller
{
	private NPC npc;
	private Random random = new Random();
	private BehaviorTree bt = new BehaviorTree(BTCompositeType.SELECTOR);

	private long thinkStartTime;
	private long tickStartTime;
	private long lastThinkUpdateTime;
	private long lastTickUpdateTime;
	private long lastOneSecondCheck;
	private volatile long lastNearEventTime;
	private GameAIServerUDP server;
	private double criteria = 2.0;
	private volatile boolean running;

	/**
	 * Starts the AI controller and launches the NPC update loop thread.
	 *
	 * @param s active AI-capable UDP game server
	 */
	public void start(GameAIServerUDP s)
	{
		thinkStartTime = System.nanoTime();
		tickStartTime = thinkStartTime;
		lastThinkUpdateTime = thinkStartTime;
		lastTickUpdateTime = tickStartTime;
		lastOneSecondCheck = thinkStartTime;
		lastNearEventTime = 0L;
		server = s;
		setupNPCs();
		setupBehaviorTree();

		running = true;
		Thread npcThread = new Thread(this::npcLoop, "NPC-AI-Thread");
		npcThread.setDaemon(true);
		npcThread.start();
	}

	/**
	 * Stops the controller loop.
	 */
	public void stop()
	{
		running = false;
	}

	/**
	 * Creates and initializes NPC state.
	 */
	public void setupNPCs()
	{
		npc = new NPC();
		npc.randomizeLocation(random.nextInt(40), random.nextInt(40));
	}

	/**
	 * Main AI loop that runs tick updates and think updates at different rates.
	 */
	public void npcLoop()
	{
		while (running)
		{
			long currentTime = System.nanoTime();
			float elapsedThinkMilliSecs =
				(currentTime - lastThinkUpdateTime) / 1000000.0f;
			float elapsedTickMilliSecs =
				(currentTime - lastTickUpdateTime) / 1000000.0f;

			if (elapsedTickMilliSecs >= 25.0f)
			{
				lastTickUpdateTime = currentTime;
				npc.updateLocation();
				server.sendNPCinfo();
			}

			if (elapsedThinkMilliSecs >= 250.0f)
			{
				lastThinkUpdateTime = currentTime;
				bt.update(elapsedThinkMilliSecs);
			}

			Thread.yield();
		}
	}

	/**
	 * Builds the NPC behavior tree.
	 */
	public void setupBehaviorTree()
	{
		bt.insertAtRoot(new BTSequence(10));
		bt.insertAtRoot(new BTSequence(20));
		bt.insert(10, new OneSecPassed(this, false));
		bt.insert(10, new GetSmall(npc));
		bt.insert(20, new AvatarNear(server, this, false));
		bt.insert(20, new GetBig(npc));
	}

	/**
	 * Time-gate condition helper for behavior tree logic.
	 *
	 * @return true when at least one second has passed since last success
	 */
	public boolean oneSecondPassed()
	{
		long now = System.nanoTime();
		if ((now - lastOneSecondCheck) >= 1000000000L)
		{
			lastOneSecondCheck = now;
			return true;
		}
		return false;
	}

	/**
	 * Returns the currently controlled NPC.
	 *
	 * @return NPC instance
	 */
	public NPC getNPC()
	{
		return npc;
	}

	/**
	 * Updates near-avatar state based on client proximity reports.
	 *
	 * @param near true when any client reports being near the NPC
	 */
	public void setNearFlag(boolean near)
	{
		if (near)
			lastNearEventTime = System.nanoTime();
	}

	/**
	 * Returns whether the NPC is currently considered near any avatar.
	 *
	 * @return true if recent near-event is within active window
	 */
	public boolean getNearFlag()
	{
		long ageNanos = System.nanoTime() - lastNearEventTime;
		return ageNanos <= 600000000L;
	}

	/**
	 * Returns the distance threshold used for near checks.
	 *
	 * @return near-check radius criteria
	 */
	public double getCriteria()
	{
		return criteria;
	}
}
