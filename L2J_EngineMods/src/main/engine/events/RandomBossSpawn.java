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
import main.holders.RewardHolder;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.util.Broadcast;

/**
 * @author fissban
 */
public class RandomBossSpawn extends AbstractMods
{
	private static final String[] LOCATIONS =
	{
		"in the colliseum",
		"near the entrance of the Garden of Eva",
		"close to the western entrance of the Cemetary",
		"at Gludin's Harbor"
	};
	
	private static final Location[] SPAWNS =
	{
		new Location(150086, 46733, -3407),
		new Location(84805, 233832, -3669),
		new Location(161385, 21032, -3671),
		new Location(89199, 149962, -3581),
	};
	
	private static Npc _raid = null;
	
	/**
	 * Constructor
	 */
	public RandomBossSpawn()
	{
		registerMod(ConfigData.ENABLE_RandomBossSpawn);
	}
	
	@Override
	public void onModState()
	{
		switch (getState())
		{
			case START:
				startTimer("spawnRaids", ConfigData.RANDOM_BOSS_SPWNNED_TIME * 1000 * 60, null, null, true);
				break;
			case END:
				cancelTimers("spawnRaids");
				break;
		}
	}
	
	@Override
	public void onTimer(String timerName, Npc npc, Player player)
	{
		switch (timerName)
		{
			case "spawnRaids":
				int random = Rnd.get(4);
				// spawn raid
				_raid = addSpawn(ConfigData.RANDOM_BOSS_NPC_ID.get(Rnd.get(ConfigData.RANDOM_BOSS_NPC_ID.size())), SPAWNS[random], false, ConfigData.RANDOM_BOSS_SPWNNED_TIME * 1000 * 60);
				// anuncio del spawn del raid
				Broadcast.announceToOnlinePlayers("Raid " + _raid.getName() + " spawn " + LOCATIONS[random]);
				// anunciamos el tiempo que tienen para matarlo
				Broadcast.announceToOnlinePlayers("Have " + ConfigData.RANDOM_BOSS_SPWNNED_TIME + " minutes to kill");
				break;
		}
	}
	
	@Override
	public void onKill(Creature killer, Creature victim, boolean isPet)
	{
		if (victim == _raid)
		{
			for (RewardHolder reward : ConfigData.RANDOM_BOSS_REWARDS)
			{
				if (Rnd.get(100) <= reward.getRewardChance())
				{
					killer.sendMessage("Have won " + reward.getRewardCount() + " " + ItemTable.getInstance().getTemplate(reward.getRewardId()).getName());
					killer.getActingPlayer().getInventory().addItem("PvpReward", reward.getRewardId(), reward.getRewardCount(), (Player) killer, victim);
				}
			}
		}
	}
}
