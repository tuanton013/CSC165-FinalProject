package myGame;

import tage.ai.behaviortrees.BTCondition;

/**
 * Behavior-tree condition that succeeds once per second.
 */
public class OneSecPassed extends BTCondition
{
	private NPCcontroller npcController;

	/**
	 * Creates the one-second gate condition.
	 *
	 * @param c NPC controller owning the timer state
	 * @param toNegate true to invert condition result
	 */
	public OneSecPassed(NPCcontroller c, boolean toNegate)
	{
		super(toNegate);
		npcController = c;
	}

	@Override
	protected boolean check()
	{
		return npcController.oneSecondPassed();
	}
}
