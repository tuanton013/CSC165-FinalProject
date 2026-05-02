package myGame;

import tage.ai.behaviortrees.BTCondition;

/**
 * Behavior-tree condition that checks whether any player avatar is near the NPC.
 */
public class AvatarNear extends BTCondition
{
	private NPCcontroller npcController;
	private GameAIServerUDP server;

	/**
	 * Creates the AvatarNear condition node.
	 *
	 * @param s active AI server used to query clients
	 * @param c NPC controller containing near-state cache
	 * @param toNegate true to invert condition result
	 */
	public AvatarNear(GameAIServerUDP s, NPCcontroller c, boolean toNegate)
	{
		super(toNegate);
		server = s;
		npcController = c;
	}

	@Override
	protected boolean check()
	{
		server.sendCheckForAvatarNear();
		return npcController.getNearFlag();
	}
}
