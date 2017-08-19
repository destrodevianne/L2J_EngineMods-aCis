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
package main.engine.stats;

import main.data.PlayerData;
import main.engine.AbstractMods;
import main.util.Util;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.skills.Stats;

/**
 * @author fissban
 */
public class StatsFake extends AbstractMods
{
	public StatsFake()
	{
		registerMod(true);
	}
	
	@Override
	public void onModState()
	{
		//
	}
	
	@Override
	public double onStats(Stats stat, Creature character, double value)
	{
		if (!Util.areObjectType(Player.class, character))
		{
			return value;
		}
		
		if (PlayerData.get((Player) character).isFake())
		{
			// bonus generales para todos los fakes
			switch (stat)
			{
				case MAX_MP:
					value *= 10.0;
					break;
				case REGENERATE_MP_RATE:
					value *= 10.0;
					break;
				case REGENERATE_HP_RATE:
					value *= 10.0;
					break;
				case MAGIC_DEFENCE:
					value *= 4.0;
					break;
				case POWER_DEFENCE:
					value *= 4.0;
					break;
				case MAGIC_REUSE_RATE:
					value *= 0.8;
					break;
				case POWER_ATTACK_SPEED:
					value *= 3.0;
					break;
				case MAGIC_ATTACK_SPEED:
					value *= 3.0;
					break;
				case POWER_ATTACK:
					value *= 4.5;
					break;
				case MAGIC_ATTACK:
					value *= 4.5;
					break;
				default:
					break;
			}
		}
		
		return value;
	}
}
