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
package main.engine.mods;

import main.data.ConfigData;
import main.engine.AbstractMods;
import main.util.Util;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.instance.GrandBoss;
import net.sf.l2j.gameserver.model.actor.instance.RaidBoss;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.util.Broadcast;

/**
 * @author fissban
 */
public class AnnounceKillBoss extends AbstractMods
{
	public AnnounceKillBoss()
	{
		registerMod(ConfigData.ENABLE_AnnounceKillBoss);
	}
	
	@Override
	public void onModState()
	{
		//
	}
	
	@Override
	public void onKill(Creature killer, Creature victim, boolean isPet)
	{
		if (!Util.areObjectType(Playable.class, killer))
		{
			return;
		}
		
		if (Util.areObjectType(RaidBoss.class, victim))
		{
			Broadcast.toAllOnlinePlayers(new CreatureSay(0, Say2.TELL, "", ConfigData.ANNOUNCE_KILL_BOSS.replace("%s1", killer.getActingPlayer().getName()).replace("%s2", victim.getName())));
			return;
		}
		
		if (Util.areObjectType(GrandBoss.class, victim))
		{
			Broadcast.toAllOnlinePlayers(new CreatureSay(0, Say2.TELL, "", ConfigData.ANNOUNCE_KILL_GRANDBOSS.replace("%s1", killer.getActingPlayer().getName()).replace("%s2", victim.getName())));
			return;
		}
	}
}
