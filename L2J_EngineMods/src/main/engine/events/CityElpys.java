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

import java.util.ArrayList;
import java.util.List;

import main.data.ConfigData;
import main.engine.AbstractMods;
import main.holders.RewardHolder;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.MapRegionTable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.util.Broadcast;

/**
 * @author fissban
 */
public class CityElpys extends AbstractMods
{
	// lista de los elpys que se spawnean en el evento
	private static final List<Npc> _mobs = new ArrayList<>(ConfigData.ELPY_COUNT);
	
	public CityElpys()
	{
		registerMod(ConfigData.ELPY_Enabled, ConfigData.ELPY_ENABLE_DAY);
	}
	
	@Override
	public void onModState()
	{
		switch (getState())
		{
			case START:
				startTimer("spawnElpys", ConfigData.ELPY_EVENT_TIME * 60 * 1000, null, null, true);// 1hs
				break;
			case END:
				// removemos todos los elpys del evento anterior
				unspawnElpys();
				cancelTimers("spawnElpys");
				break;
		}
	}
	
	@Override
	public void onTimer(String timerName, Npc npc, Player player)
	{
		switch (timerName)
		{
			case "spawnElpys":
			{
				// removemos todos los elpys del evento anterior
				unspawnElpys();
				// obtenemos un lugar random para el evento
				Location loc = ConfigData.ELPY_LOC.get(Rnd.get(ConfigData.ELPY_LOC.size()));
				// anunciamos donde se generaran los spawns
				String locName = MapRegionTable.getInstance().getClosestTownName(loc.getX(), loc.getY());
				Broadcast.announceToOnlinePlayers("Elpys spawn near " + locName, true);
				// generamos los nuevos spawns
				for (int i = 0; i < ConfigData.ELPY_COUNT; i++)
				{
					int x = loc.getX() + Rnd.get(-ConfigData.ELPY_RANGE_SPAWN, ConfigData.ELPY_RANGE_SPAWN);
					int y = loc.getY() + Rnd.get(-ConfigData.ELPY_RANGE_SPAWN, ConfigData.ELPY_RANGE_SPAWN);
					int z = loc.getZ();
					
					Npc spawn = addSpawn(ConfigData.ELPY, new Location(x, y, z), false, 0);
					_mobs.add(spawn);
				}
				break;
			}
		}
	}
	
	@Override
	public void onKill(Creature killer, Creature victim, boolean isPet)
	{
		if (_mobs.contains(victim))
		{
			_mobs.remove(victim);
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
	
	private static void unspawnElpys()
	{
		for (Npc mob : _mobs)
		{
			mob.deleteMe();
		}
		// limpiamos la variable
		_mobs.clear();
	}
}
