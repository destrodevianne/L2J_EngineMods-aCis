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

import main.engine.AbstractMods;
import main.util.Util;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.skills.Stats;

/**
 * @author fissban
 */
public class StatsNpc extends AbstractMods
{
	public StatsNpc()
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
		if (!Util.areObjectType(Monster.class, character))
		{
			return value;
		}
		
		// bonus generales para todos.
		switch (stat)
		{
			case POWER_DEFENCE:
				value /= 1.0;
				break;
			case MAGIC_DEFENCE:
				value /= 1.0;
				break;
			default:
				break;
		}
		
		return value;
	}
}
