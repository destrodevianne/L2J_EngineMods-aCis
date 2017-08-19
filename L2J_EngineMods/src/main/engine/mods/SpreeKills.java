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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import main.data.ConfigData;
import main.engine.AbstractMods;
import main.util.Util;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.util.Broadcast;

/**
 * Class responsible for managing ads deaths according to consecutive kills amount.
 * @author fissban
 */
public class SpreeKills extends AbstractMods
{
	/** MISC ---------------------------------------------------------------------------------------------- */
	private static final Map<Integer, Integer> players = new HashMap<>();
	
	/**
	 * Constructor.
	 */
	public SpreeKills()
	{
		registerMod(ConfigData.ENABLE_SpreeKills);
	}
	
	@Override
	public void onModState()
	{
		//
	}
	
	@Override
	public void onDeath(Creature player)
	{
		if (players.containsKey(player.getObjectId()))
		{
			players.remove(player.getObjectId());
		}
	}
	
	@Override
	public void onKill(Creature killer, Creature victim, boolean isPet)
	{
		if (!Util.areObjectType(Player.class, victim) || killer.getActingPlayer() == null)
		{
			return;
		}
		
		Player activeChar = killer.getActingPlayer();
		
		int count = 1;
		if (players.containsKey(activeChar.getObjectId()))
		{
			count = players.get(activeChar.getObjectId());
			count++;
		}
		
		players.put(activeChar.getObjectId(), count);
		
		// animation Lightning Strike(279)
		activeChar.broadcastPacket(new MagicSkillUse(activeChar, victim, 279, 1, 500, 500));
		
		announcements(activeChar, count);
	}
	
	public static boolean announcements(Player player, int count)
	{
		String announcement = "";
		
		// the ad is derived from the number of character assassinations achieved.
		for (Entry<Integer, String> kill : ConfigData.ANNOUNCEMENTS_KILLS.entrySet())
		{
			announcement = kill.getValue();
			
			if (kill.getKey() == count)
			{
				break;
			}
		}
		
		// the announcement is sent to all game characters.
		Broadcast.toAllOnlinePlayers(new CreatureSay(0, Say2.TELL, "", announcement.replace("%s1", player.getName())));
		
		return true;
	}
}
