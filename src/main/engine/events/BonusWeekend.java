/*
 * L2J_EngineMods
 * Engine developed by Fissban.
 *
 * This software is not free and you do not have permission
 * to distribute without the permission of its owner.
 *
 * This software is distributed only under the rule
 * of www.devsadmins.com.
 * 
 * Contact us with any questions by the media
 * provided by our web or email marco.faccio@gmail.com
 */
package main.engine.events;

import main.data.ConfigData;
import main.engine.AbstractMods;
import main.enums.ExpSpType;
import main.enums.ItemDropType;
import main.instances.NpcDropsInstance;
import main.instances.NpcExpInstance;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.instance.Player;

/**
 * @author fissban
 */
public class BonusWeekend extends AbstractMods
{
	public BonusWeekend()
	{
		registerMod(ConfigData.ENABLE_BonusWeekend, ConfigData.BONUS_WEEKEND_ENABLE_DAY);
	}
	
	@Override
	public void onModState()
	{
		//
	}
	
	@Override
	public void onNpcExpSp(Player killer, Attackable npc, NpcExpInstance instance)
	{
		// ExpSpBonusHolder (bonusType, amountBonus)
		// Example: 1.1 -> 110%
		// if you use 100% exp will be normal, to earn bonus use values greater than 100%.
		// increase normal exp/sp amount
		instance.increaseRate(ExpSpType.EXP, ConfigData.BONUS_WEEKEND_RATE_EXP);
		instance.increaseRate(ExpSpType.SP, ConfigData.BONUS_WEEKEND_RATE_SP);
		return;
	}
	
	@Override
	public void onNpcDrop(Player killer, Attackable npc, NpcDropsInstance instance)
	{
		// DropBonusHolder (dropType, amountBonus, chanceBonus)
		// Example: 110 -> 110%
		// if you use 100% drop will be normal, to earn bonus use values greater than 100%.
		
		// increase normal drop amount and chance
		instance.increaseDrop(ItemDropType.NORMAL, ConfigData.BONUS_WEEKEND_DROP, ConfigData.BONUS_WEEKEND_DROP);
		// increase spoil drop amount and chance
		instance.increaseDrop(ItemDropType.SPOIL, ConfigData.BONUS_WEEKEND_SPOIL, ConfigData.BONUS_WEEKEND_SPOIL);
		// increase herb drop amount and chance
		instance.increaseDrop(ItemDropType.HERB, ConfigData.BONUS_WEEKEND_HERB, ConfigData.BONUS_WEEKEND_HERB);
		// increase seed drop amount and chance
		instance.increaseDrop(ItemDropType.SEED, ConfigData.BONUS_WEEKEND_SEED, ConfigData.BONUS_WEEKEND_SEED);
	}
}
