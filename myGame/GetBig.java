package myGame;

import tage.ai.behaviortrees.BTAction;
import tage.ai.behaviortrees.BTStatus;

/**
 * Behavior-tree action that sets the NPC to its large state.
 */
public class GetBig extends BTAction
{
	private NPC npc;

	/**
	 * Creates the GetBig action.
	 *
	 * @param n target NPC
	 */
	public GetBig(NPC n)
	{
		npc = n;
	}

	@Override
	protected BTStatus update(float elapsedTime)
	{
		npc.getBig();
		return BTStatus.BH_SUCCESS;
	}
}
