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
package main.engine.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Future;

import main.data.PlayerData;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.data.MapRegionTable;
import net.sf.l2j.gameserver.data.MapRegionTable.TeleportType;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.ShotType;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.ai.Desire;
import net.sf.l2j.gameserver.model.actor.ai.type.CreatureAI;
import net.sf.l2j.gameserver.model.actor.instance.Gatekeeper;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.instance.StaticObject;
import net.sf.l2j.gameserver.model.item.type.CrystalType;
import net.sf.l2j.gameserver.model.item.type.WeaponType;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.util.Broadcast;

/**
 * @author fissban
 */
public class FakePlayerAI extends CreatureAI implements Runnable
{
	private boolean _thinking = false; // to prevent recursive thinking
	private Desire _nextIntention = null;
	
	private Location _posToFarm = null;
	private Future<?> _taskReturnFarm = null;
	
	/** The L2Attackable AI task executed every 1s (call onEvtThink method) */
	protected Future<?> _aiTask = null;
	
	// ACTIVE
	/** usado para evitar q todos los fakes vallan a hablar con el mismo. */
	private static List<Npc> _lastTalk = new ArrayList<>(5);
	private boolean _isNeedBackToFarm = false;
	
	private int _randomGoToCityTime = 0;
	
	// IDLE
	
	public enum CityTimeType
	{
		SEARCH_NEXT_POS,
	
	}
	
	// tiempo de demora entre un objetivo y otro al caminar en segundos
	private int _randomWalkTime = 0;
	
	@Override
	public Player getActor()
	{
		return (Player) _actor;
	}
	
	public FakePlayerAI(Player player)
	{
		super(player);
		
		startTasks();
		System.out.println("FakePlayerAI");
	}
	
	@Override
	public void run()
	{
		// Launch actions corresponding to the Event Think
		onEvtThink();
	}
	
	private void startTasks()
	{
		try
		{
			ThreadPool.schedule(() ->
			{
				// getActor().revalidateZone(false);
				
				_posToFarm = PlayerData.get(getActor()).getPosToFarm();
				
				if (isInsideCity())
				{
					// System.out.println("startTasks 2");
					// solo caminaran de un npc al otro dentro de la ciudad asignada.
					// activamos el Ai
					startAITask(1000);
					// setIntention(CtrlIntention.IDLE);
				}
				else
				{
					// System.out.println("startTasks 3");
					_isNeedBackToFarm = true;
					// hago que se paren los sentados parados
					getActor().standUp();
					// hago q corran
					getActor().setRunning();
					// me aseguro de q no tengan target definido
					getActor().setTarget(null);
					
					_randomGoToCityTime = Rnd.get(2, 16) * 60 * 1000;
					// entregamos los buffs al player segun si es amgo o warrior
					// giveBuffsTask();
					// activamos el ai
					setIntention(CtrlIntention.ACTIVE);
				}
			}, Rnd.get(10, 30) * 1000);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void attackTarget()
	{
		// evitamos de que el fake se quede sin mp
		if (getActor().getCurrentMp() < getActor().getMaxMp() * 0.5)
		{
			getActor().setCurrentMp(getActor().getMaxMp());
		}
		
		if (getActor().isMageClass())
		{
			int skillId = -1;
			
			switch (getActor().getRace())
			{
				case HUMAN:
					skillId = 1230;// Prominence
					break;
				case DARK_ELF:
					skillId = 1239;// Hurricane
					break;
				case ELF:
					skillId = 1235;// Hydro Blast
					break;
				case ORC:
					skillId = 1245;// Steal Essence
				default:
					break;
			}
			
			if (skillId != -1)
			{
				int level = SkillTable.getInstance().getMaxLevel(skillId);
				L2Skill skill = SkillTable.getInstance().getInfo(skillId, level);
				
				// me aseguro de q el personaje tenga el skill o se lo doy yo
				if (getActor().getSkill(skillId) == null)
				{
					getActor().addSkill(skill, true);
				}
				
				if (maybeMoveToPawn(getTarget(), skill.getCastRange()))
				{
					return;
				}
				
				// clientStopMoving(null);
				// .useMagic(skill, true, true);
				getActor().useMagic(skill, true, true);
			}
		}
		else
		{
			// chequeamos q tipo de arma tiene
			if (getActor().getActiveWeaponItem() != null && getActor().getActiveWeaponItem().getItemType() == WeaponType.BOW)
			{
				CrystalType bow = getActor().getActiveWeaponItem().getCrystalType();
				int arrowId = 17;
				
				switch (bow)
				{
					case S:
						arrowId = 1345;
						break;
					case A:
						arrowId = 1344;
						break;
					case B:
						arrowId = 1343;
						break;
					case C:
						arrowId = 1342;
						break;
					case D:
						arrowId = 1341;
						break;
					case NONE:
						arrowId = 17;
						break;
					default:
						break;
				}
				
				if (getActor().getInventory().getInventoryItemCount(arrowId, -1) < 10)
				{
					getActor().getInventory().addItem("arrow fakes", arrowId, 500, getActor(), getActor());
				}
			}
			
			setIntention(CtrlIntention.ATTACK, getActor().getTarget());
		}
	}
	
	/**
	 * Buscamos dentro del knowlist del player<br>
	 * Si tenemos un player con karma o pvpflag devolvemos ese como target<br>
	 * en caso contrario devolvemos el monster mas cercano al fake
	 */
	private Creature searchNextAttack()
	{
		// obtenemos todos los player que tengan karma o esten flag
		for (Player player : getActor().getKnownTypeInRadius(Player.class, 1000))
		{
			if (player.getKarma() > 0 || player.getPvpFlag() > 0)
			{
				return player;
			}
		}
		
		// mapa ordenado por key
		Map<Double, Monster> listAttack = new TreeMap<>();
		
		// obtenemos todos los monster en un area de 2000
		for (Monster monster : getActor().getKnownTypeInRadius(Monster.class, 2000))
		{
			// nos aseguramos de q el player pueda ir hasta ese punto
			if (!GeoEngine.getInstance().canMoveToTarget(getActor().getX(), getActor().getY(), getActor().getZ(), monster.getX(), monster.getY(), monster.getZ()))
			{
				continue;
			}
			// nos aseguramos de poder ver ese target
			if (!GeoEngine.getInstance().canSeeTarget(getActor(), monster))
			{
				continue;
			}
			// calculamos la distancia a la q estamos del objetivo
			double distance = MathUtil.calculateDistance(getActor(), monster, false);
			
			listAttack.put(distance, monster);
		}
		
		// ahora buscamos el objetivo por distancia
		for (Monster monster : listAttack.values())
		{
			if (monster != null && !monster.isDead())
			{
				return monster;
			}
		}
		return null;
	}
	
	private synchronized void searchCityRandomLoc()
	{
		boolean searchNewLoc = true;
		while (searchNewLoc)
		{
			int ox = getActor().getX();
			int oy = getActor().getY();
			int oz = getActor().getZ();
			
			int tx = Rnd.get(ox - 2000, ox + 2000);
			int ty = Rnd.get(oy - 2000, oy + 2000);
			int tz = getActor().getZ();
			
			// chequeamos que el punto random este dentro de la ciudad y que se pueda mover a ese punto.
			if (MapRegionTable.getTown(tx, ty, tz) != null && GeoEngine.getInstance().canMoveToTarget(ox, oy, oz, tx, ty, tz))
			{
				searchNewLoc = false;
				setIntention(CtrlIntention.MOVE_TO, new Location(tx, ty, tz));
			}
		}
	}
	
	@Override
	protected void clientActionFailed()
	{
		_actor.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	void setNextIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		_nextIntention = new Desire(intention, arg0, arg1);
	}
	
	@Override
	public Desire getNextIntention()
	{
		return _nextIntention;
	}
	
	/**
	 * Saves the current Intention for this L2PlayerAI if necessary and calls changeIntention in AbstractAI.<BR>
	 * <BR>
	 * @param intention The new Intention to set to the AI
	 * @param arg0 The first parameter of the Intention
	 * @param arg1 The second parameter of the Intention
	 */
	@Override
	public synchronized void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		// do nothing if next intention is same as current one.
		if (intention == _intention && arg0 == _intentionArg0 && arg1 == _intentionArg1)
		{
			super.changeIntention(intention, arg0, arg1);
			return;
		}
		
		// Check if actor is not dead
		if (!getActor().isAlikeDead() && intention == CtrlIntention.IDLE)
		{
			// intention = CtrlIntention.ACTIVE;
			// If its _knownPlayer isn't empty set the Intention to ACTIVE
			if (!getActor().getKnownTypeInRadius(Monster.class, 500).isEmpty())
			{
				intention = CtrlIntention.ACTIVE;
			}
		}
		
		// Set the Intention of this L2AttackableAI to intention
		super.changeIntention(intention, arg0, arg1);
		
		startAITask(500);
	}
	
	/**
	 * Launch actions corresponding to the Event ReadyToAct.<BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <li>Launch actions corresponding to the Event Think</li><BR>
	 */
	@Override
	protected void onEvtReadyToAct()
	{
		// Launch actions corresponding to the Event Think
		if (_nextIntention != null)
		{
			setIntention(_nextIntention.getIntention(), _nextIntention.getFirstParameter(), _nextIntention.getSecondParameter());
			_nextIntention = null;
		}
		super.onEvtReadyToAct();
	}
	
	@Override
	protected void onEvtCancel()
	{
		_nextIntention = null;
		super.onEvtCancel();
	}
	
	/**
	 * Finalize the casting of a skill. Drop latest intention before the actual CAST.
	 */
	@Override
	protected void onEvtFinishCasting()
	{
		//
	}
	
	@Override
	protected void onIntentionRest()
	{
		if (getIntention() != CtrlIntention.REST)
		{
			changeIntention(CtrlIntention.REST, null, null);
			setTarget(null);
			clientStopMoving(null);
		}
	}
	
	@Override
	protected void onIntentionActive()
	{
		super.onIntentionActive();
	}
	
	/**
	 * Manage the Move To Intention : Stop current Attack and Launch a Move to Location Task.<BR>
	 * <BR>
	 * <B><U> Actions</U> : </B><BR>
	 * <BR>
	 * <li>Stop the actor auto-attack server side AND client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Set the Intention of this AI to MOVE_TO</li>
	 * <li>Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet MoveToLocation (broadcast)</li><BR>
	 * <BR>
	 */
	@Override
	protected void onIntentionMoveTo(Location loc)
	{
		if (getIntention() == CtrlIntention.REST)
		{
			// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor
			clientActionFailed();
			return;
		}
		
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow() || _actor.isAttackingNow())
		{
			clientActionFailed();
			setNextIntention(CtrlIntention.MOVE_TO, loc, null);
			return;
		}
		
		// Set the Intention of this AbstractAI to MOVE_TO
		changeIntention(CtrlIntention.MOVE_TO, loc, null);
		
		// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
		clientStopAutoAttack();
		
		// Abort the attack of the Character and send Server->Client ActionFailed packet
		_actor.abortAttack();
		
		// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet MoveToLocation (broadcast)
		moveTo(loc.getX(), loc.getY(), loc.getZ());
	}
	
	@Override
	protected void clientNotifyDead()
	{
		_clientMovingToPawnOffset = 0;
		_clientMoving = false;
		
		super.clientNotifyDead();
	}
	
	private void thinkAttack()
	{
		final Creature target = (Creature) getTarget();
		if (target == null)
		{
			setTarget(null);
			setIntention(CtrlIntention.ACTIVE);
			return;
		}
		
		if (maybeMoveToPawn(target, _actor.getPhysicalAttackRange()))
		{
			return;
		}
		
		if (target.isAlikeDead())
		{
			if (target instanceof Player && ((Player) target).isFakeDeath())
			{
				target.stopFakeDeath(true);
			}
			else
			{
				setIntention(CtrlIntention.ACTIVE);
				return;
			}
		}
		
		chargeShots();
		
		clientStopMoving(null);
		_actor.doAttack(target);
	}
	
	private void thinkCast()
	{
		if (checkTargetLost(getTarget()))
		{
			setTarget(null);
			return;
		}
		
		if (maybeMoveToPawn(getTarget(), _skill.getCastRange()))
		{
			return;
		}
		
		clientStopMoving(null);
		setIntention(CtrlIntention.ACTIVE);
		_actor.doCast(_skill);
	}
	
	/**
	 * Manage the Idle Intention : Stop Attack, Movement and Stand Up the actor.
	 * <ul>
	 * <li>Set the AI Intention to IDLE</li>
	 * <li>Init cast and attack target</li>
	 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * <li>Stand up the actor server side AND client side by sending Server->Client packet ChangeWaitType (broadcast)</li>
	 * </ul>
	 */
	@Override
	protected void onIntentionIdle()
	{
		super.onIntentionIdle();
	}
	
	private void thinkPickUp()
	{
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
		{
			return;
		}
		
		final WorldObject target = getTarget();
		if (checkTargetLost(target))
		{
			return;
		}
		
		if (maybeMoveToPawn(target, 36))
		{
			return;
		}
		
		_actor.getActingPlayer().doPickupItem(target);
		
		setIntention(CtrlIntention.IDLE);
	}
	
	private void thinkInteract()
	{
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
		{
			return;
		}
		
		WorldObject target = getTarget();
		if (checkTargetLost(target))
		{
			return;
		}
		
		if (maybeMoveToPawn(target, 36))
		{
			return;
		}
		
		if (!(target instanceof StaticObject))
		{
			_actor.getActingPlayer().doInteract((Creature) target);
		}
		
		setIntention(CtrlIntention.IDLE);
	}
	
	@Override
	protected void onEvtThink()
	{
		// Check if the actor can't use skills and if a thinking action isn't already in progress
		if (_thinking && getIntention() != CtrlIntention.CAST)
		{
			return;
		}
		
		if (getIntention() == CtrlIntention.MOVE_TO)
		{
			return;
		}
		
		if (isInsideCity())
		{
			// forzamos el cambio de actividad almenos que halla monster cerca.
			changeIntention(CtrlIntention.IDLE, null, null);
		}
		
		// Start thinking action
		_thinking = true;
		
		try
		{
			// Manage AI thoughts
			switch (getIntention())
			{
				case IDLE:
					// aqui haremos q el personaje camine de un lugar a otro.
					thinkIdle();
					break;
				case ACTIVE:
					// Aqui haremos q el personaje se ponga a levelear entre otra acciones
					thinkActive();
					break;
				case ATTACK:
					thinkAttack();
					break;
				case CAST:
					thinkCast();
					break;
				case PICK_UP:
					thinkPickUp();
					break;
				case INTERACT:
					thinkInteract();
					break;
				default:
					break;
			}
		}
		finally
		{
			// Stop thinking action
			_thinking = false;
		}
	}
	
	protected void thinkActive()
	{
		// System.out.println("thinkActive 1");
		if (_taskReturnFarm != null)
		{
			_taskReturnFarm.cancel(true);
			_taskReturnFarm = null;
		}
		
		if (_randomGoToCityTime <= 0)
		{
			// entre 2 y 16 min
			_randomGoToCityTime = Rnd.get(2, 16) * 60 * 2;
			
			// stop all ai action
			getActor().abortCast();
			getActor().abortAttack();
			getActor().setTarget(null);
			// stopAITask();
			// escape skill
			L2Skill skill = SkillTable.getInstance().getInfo(2036, 1);// Blessed Scroll of Escape
			
			// finalmente enviamos al fake a la ciudad
			getActor().useMagic(skill, true, false);
			
			// a los 3 seg hacemos q se mueva como un pj mas dentro de la ciudad
			_randomWalkTime = 3 * 2;
			
			setIntention(CtrlIntention.IDLE);
			return;
		}
		
		_randomGoToCityTime--;
		
		// si ahi algun item lo recojemos
		// if (!getActor().getKnownType(ItemInstance.class).isEmpty())
		// {
		// for (ItemInstance item : getActor().getKnownType(ItemInstance.class))
		// {
		// / if (item==null)
		// {
		// return;
		// }
		// // recojemos la adena.
		// // solo si el duenio del item es el mismo
		// if (item.getItemId() == 57 && item.getOwnerId() == getActor().getObjectId())
		// {
		// setIntention(CtrlIntention.PICK_UP, item);
		// return;
		// }
		// }
		// }
		
		// buscamos un objetivo para matar
		if (!getActor().isCastingNow() && !getActor().isAttackingNow())
		{
			chargeShots();
			
			WorldObject obj = getActor().getTarget();
			
			if (obj == null || ((Creature) obj).isDead())
			{
				// buscamos el siguiente objetivo a matar
				Creature monster = searchNextAttack();
				if (monster != null)
				{
					getActor().setTarget(monster);
					setTarget(monster);// ????
				}
			}
			
			attackTarget();
		}
	}
	
	protected void thinkIdle()
	{
		// System.out.println("thinkIdle");
		if (_randomWalkTime <= 0)
		{
			// System.out.println("thinkIdle 1");
			// chequeamos si requiere volver a farmear a su posicion
			if (isInsideCity() && _isNeedBackToFarm)
			{
				for (Gatekeeper teleport : getActor().getKnownTypeInRadius(Gatekeeper.class, 2000))
				{
					setIntention(CtrlIntention.MOVE_TO, new Location(teleport.getX(), teleport.getY(), teleport.getZ()));
					// System.out.println("thinkIdle 2");
					_isNeedBackToFarm = false;
					return;
				}
			}
			
			// System.out.println("thinkIdle 3");
			// definimos un nuevo tiempo para la proxima accion....luego de q termine de moverse.
			_randomWalkTime = Rnd.get(5, 30);
			
			if (_isNeedBackToFarm)
			{
				_randomWalkTime *= 2;
			}
			
			Npc target = null;
			
			// int cont = 0;
			
			if (Rnd.nextBoolean())
			{
				// while (cont < 5)
				// {
				// buscamos un npc dentro de un determinado rango
				// no usamos el kwnowlist porq no todos los npc de la ciudad estan al alcanze de la vista del player.
				for (Npc npc : getActor().getKnownType(Npc.class))
				// for (L2Spawn spawn : SpawnTable.getInstance().getSpawnTable())
				{
					// Npc npc = spawn.getNpc();
					if (npc == null)
					{
						continue;
					}
					
					if (npc instanceof Monster)
					{
						continue;
					}
					
					// 20%
					if (Rnd.get(5) != 0)
					{
						continue;
					}
					
					// me aseguro de q se pueda ver el objetivo y nos podamos mover al objetivo
					if (GeoEngine.getInstance().canSeeTarget(getActor(), npc) && !GeoEngine.getInstance().canMoveToTarget(getActor().getX(), getActor().getY(), getActor().getZ(), npc.getX(), npc.getY(), npc.getZ()))
					{
						if (_lastTalk.contains(npc))
						{
							continue;
						}
						
						if (_lastTalk.size() > 5)
						{
							_lastTalk.clear();
						}
						_lastTalk.add(npc);
						
						target = npc;
						// cont = 5;
						break;
					}
					// }
					
					// cont++;
				}
			}
			
			getActor().setRunning();
			
			// si no se logro un objetivo nos movemos al azar
			if (target == null)
			{
				searchCityRandomLoc();
			}
			else
			{
				setIntention(CtrlIntention.MOVE_TO, new Location(target.getX() + 10, target.getY() + 10, target.getZ()));
			}
		}
		// System.out.println("thinkIdle 4");
		_randomWalkTime--;
	}
	
	@Override
	protected void onEvtDead()
	{
		super.onEvtDead();
		
		// Stop an AI Tasks
		stopAITask();
		
		// Kill the actor client side by sending Server->Client packet AutoAttackStop, StopMove/StopRotation, Die (broadcast)
		clientNotifyDead();
		
		// 5 seg y lo volvemos a la ciudad
		ThreadPool.schedule(() ->
		{
			getActor().teleToLocation(TeleportType.TOWN);
			getActor().doRevive();
			
		}, 5 * 1000);
		
		ThreadPool.schedule(() ->
		{
			searchCityRandomLoc();
			
		}, 8 * 1000);
		
		// 3 min hacemos q el player valla hasta un teleport
		ThreadPool.schedule(() ->
		{
			for (Gatekeeper teleport : getActor().getKnownTypeInRadius(Gatekeeper.class, 1000))
			{
				setIntention(CtrlIntention.MOVE_TO, new Location(teleport.getX(), teleport.getY(), teleport.getZ()));
				break;
			}
		}, 3 * 60 * 1000);
		
		// 5 min antes de q vuelvan a la accion :P
		ThreadPool.schedule(() ->
		{
			if (_posToFarm != null)
			{
				getActor().teleToLocation(_posToFarm.getX(), _posToFarm.getY(), _posToFarm.getZ(), 1000);
				
				setIntention(CtrlIntention.MOVE_TO, _posToFarm);
				
				// task para verificar la proximidad del pj con la zona de farm
				_taskReturnFarm = ThreadPool.scheduleAtFixedRate(() ->
				{
					// esperamos hasta q el player este cerca de la zona inicial
					if (getActor().isInsideRadius(_posToFarm.getX(), _posToFarm.getY(), 100, false))
					{
						startAITask(500);
					}
				}, 1000, 1000);
			}
		}, 5 * 60 * 1000);
	}
	
	private void chargeShots()
	{
		// carga de los shots
		if (!getActor().isChargedShot(ShotType.BLESSED_SPIRITSHOT))
		{
			Broadcast.toSelfAndKnownPlayersInRadius(getActor(), new MagicSkillUse(getActor(), getActor(), 2061, 1, 0, 0), 600);
			getActor().setChargedShot(ShotType.BLESSED_SPIRITSHOT, true);
		}
		if (!getActor().isChargedShot(ShotType.SOULSHOT))
		{
			Broadcast.toSelfAndKnownPlayersInRadius(getActor(), new MagicSkillUse(getActor(), getActor(), 2155, 1, 0, 0), 600);
			getActor().setChargedShot(ShotType.SOULSHOT, true);
		}
	}
	
	public void startAITask(long time)
	{
		// if (isInsideCity())
		// {
		// return;
		// }
		
		if (_aiTask == null)
		{
			_aiTask = ThreadPool.scheduleAtFixedRate(this, 1000, time);
		}
	}
	
	@Override
	public void stopAITask()
	{
		if (_aiTask != null)
		{
			_aiTask.cancel(false);
			_aiTask = null;
		}
		super.stopAITask();
	}
	// MISC ----------------------------------------------------
	
	private boolean isInsideCity()
	{
		if (MapRegionTable.getTown(getActor().getX(), getActor().getY(), getActor().getZ()) != null)
		{
			return true;
		}
		
		return false;
	}
}