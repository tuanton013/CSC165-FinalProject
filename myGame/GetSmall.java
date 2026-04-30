package myGame;

import tage.ai.behaviortrees.BTAction;
import tage.ai.behaviortrees.BTStatus;

/**
 * Behavior-tree action that sets the NPC to its normal/small state.
 */
public class GetSmall extends BTAction
{
	private NPC npc;

	/**
	 * Creates the GetSmall action.
	 *
	 * @param n target NPC
	 */
	public GetSmall(NPC n)
	{
		npc = n;
	}

	@Override
	protected BTStatus update(float elapsedTime)
	{
		npc.getSmall();
		return BTStatus.BH_SUCCESS;
	}
}
