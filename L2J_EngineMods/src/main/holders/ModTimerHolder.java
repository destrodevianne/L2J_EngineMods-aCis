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
package main.holders;

import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import main.EngineModsManager;
import main.engine.AbstractMods;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.instance.Player;

public class ModTimerHolder
{
	protected static final Logger LOG = Logger.getLogger(ModTimerHolder.class.getName());
	
	protected final Integer _mod;
	protected final String _name;
	protected final Npc _npc;
	protected final Player _player;
	protected final boolean _isRepeating;
	
	private ScheduledFuture<?> _schedular;
	
	public ModTimerHolder(AbstractMods mod, String name, Npc npc, Player player, long time, boolean repeating)
	{
		_mod = mod.hashCode();
		_name = name;
		_npc = npc;
		_player = player;
		_isRepeating = repeating;
		
		if (repeating)
		{
			_schedular = ThreadPool.scheduleAtFixedRate(new ScheduleTimerTask(), time, time);
		}
		else
		{
			_schedular = ThreadPool.schedule(new ScheduleTimerTask(), time);
		}
	}
	
	protected final class ScheduleTimerTask implements Runnable
	{
		@Override
		public void run()
		{
			if (!_isRepeating)
			{
				cancel();
			}
			
			if (!EngineModsManager.getMod(_mod).isStarting())
			{
				return;
			}
			
			EngineModsManager.getMod(_mod).onTimer(_name, _npc, _player);
		}
	}
	
	public final void cancel()
	{
		if (_schedular != null)
		{
			_schedular.cancel(false);
		}
		
		EngineModsManager.getMod(_mod).removeTimer(this);
	}
	
	/**
	 * public method to compare if this timer matches with the key attributes passed.
	 * @param mod : Mod instance to which the timer is attached
	 * @param name : Name of the timer
	 * @param npc : Npc instance attached to the desired timer (null if no npc attached)
	 * @param player : Player instance attached to the desired timer (null if no player attached)
	 * @return boolean
	 */
	public final boolean equals(AbstractMods mod, String name, Npc npc, Player player)
	{
		if (mod == null || mod.hashCode() != _mod)
		{
			return false;
		}
		
		if (name == null || !name.equals(_name))
		{
			return false;
		}
		
		return npc == _npc && player == _player;
	}
	
	public String getName()
	{
		return _name;
	}
}