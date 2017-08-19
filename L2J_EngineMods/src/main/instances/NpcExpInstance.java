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
package main.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import main.enums.ExpSpType;
import net.sf.l2j.Config;
import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.gameserver.model.AggroInfo;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.RewardInfo;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.instance.Servitor;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.network.SystemMessageId;

/**
 * @author fissban
 */
public class NpcExpInstance
{
	private Map<ExpSpType, Double> _expSettings = new HashMap<>();
	{
		_expSettings.put(ExpSpType.EXP, 1.0);
		_expSettings.put(ExpSpType.SP, 1.0);
	}
	
	public NpcExpInstance()
	{
		//
	}
	
	public void increaseRate(ExpSpType type, double bonus)
	{
		double oldValue = _expSettings.get(type);
		_expSettings.put(type, oldValue + bonus - 1);
	}
	
	public boolean hasSettings()
	{
		for (Double value : _expSettings.values())
		{
			if (value > 1.0)
			{
				return true;
			}
		}
		
		return false;
	}
	
	public void init(Attackable npc, Creature lastAttacker)
	{
		if (npc.getAggroList().isEmpty())
		{
			return;
		}
		
		// Creates an empty list of rewards.
		final Map<Creature, RewardInfo> rewards = new ConcurrentHashMap<>();
		
		Player maxDealer = null;
		int maxDamage = 0;
		long totalDamage = 0;
		
		// Go through the npc.getAggroList() of the Attackable.
		for (AggroInfo info : npc.getAggroList().values())
		{
			if (info == null)
			{
				continue;
			}
			
			// Get the Character corresponding to this attacker.
			final Player attacker = info.getAttacker().getActingPlayer();
			if (attacker == null)
			{
				continue;
			}
			
			// Get damages done by this attacker.
			final int damage = info.getDamage();
			if (damage <= 1)
			{
				continue;
			}
			
			// Check if attacker isn't too far from this.
			if (!MathUtil.checkIfInRange(Config.PARTY_RANGE, npc, attacker, true))
			{
				continue;
			}
			
			totalDamage += damage;
			
			// Calculate real damages (Summoners should get own damage plus summon's damage).
			RewardInfo reward = rewards.get(attacker);
			if (reward == null)
			{
				reward = new RewardInfo(attacker);
				rewards.put(attacker, reward);
			}
			reward.addDamage(damage);
			
			if (reward.getDamage() > maxDamage)
			{
				maxDealer = attacker;
				maxDamage = reward.getDamage();
			}
		}
		
		// Manage Base, Quests and Sweep drops of the Attackable.
		npc.doItemDrop(maxDealer != null && maxDealer.isOnline() ? maxDealer : lastAttacker);
		
		for (RewardInfo reward : rewards.values())
		{
			if (reward == null)
			{
				continue;
			}
			
			// Attacker to be rewarded.
			final Player attacker = reward.getAttacker().getActingPlayer();
			
			// Total amount of damage done.
			final int damage = reward.getDamage();
			
			// Get party.
			final Party attackerParty = attacker.getParty();
			
			// Penalty applied to the attacker's XP
			final float penalty = attacker.hasServitor() ? ((Servitor) attacker.getPet()).getExpPenalty() : 0;
			
			// If there's NO party in progress.
			if (attackerParty == null)
			{
				// Calculate Exp and SP rewards.
				if (!attacker.isDead() && attacker.getKnownType(Attackable.class).contains(npc))
				{
					// Calculate the difference of level between this attacker and the Attackable.
					final int levelDiff = attacker.getLevel() - npc.getLevel();
					
					final int[] expSp = calculateExpAndSp(npc, levelDiff, damage, totalDamage);
					long exp = expSp[0];
					int sp = expSp[1];
					
					exp *= _expSettings.get(ExpSpType.EXP);
					sp *= _expSettings.get(ExpSpType.SP);
					
					// if (npc.isChampion())
					// {
					// exp *= Config.CHAMPION_REWARDS;
					// sp *= Config.CHAMPION_REWARDS;
					// }
					
					exp *= 1 - penalty;
					
					if (npc.isOverhit() && npc.getOverhitAttacker() != null && npc.getOverhitAttacker().getActingPlayer() != null && attacker == npc.getOverhitAttacker().getActingPlayer())
					{
						attacker.sendPacket(SystemMessageId.OVER_HIT);
						exp += npc.calculateOverhitExp(exp);
					}
					
					// Set new karma.
					attacker.updateKarmaLoss(exp);
					
					// Distribute the Exp and SP between the Player and its L2Summon.
					attacker.addExpAndSp(exp, sp);
				}
			}
			// Share with party members.
			else
			{
				int partyDmg = 0;
				float partyMul = 1;
				int partyLvl = 0;
				
				// Get all Character that can be rewarded in the party.
				final List<Player> rewardedMembers = new ArrayList<>();
				
				// Go through all Player in the party.
				final List<Player> groupMembers = attackerParty.isInCommandChannel() ? attackerParty.getCommandChannel().getMembers() : attackerParty.getMembers();
				
				final Map<Creature, RewardInfo> playersWithPets = new HashMap<>();
				
				for (Player partyPlayer : groupMembers)
				{
					if (partyPlayer == null || partyPlayer.isDead())
					{
						continue;
					}
					
					// Get the RewardInfo of this Player from Attackable rewards
					final RewardInfo reward2 = rewards.get(partyPlayer);
					
					// If the Player is in the Attackable rewards add its damages to party damages
					if (reward2 != null)
					{
						if (MathUtil.checkIfInRange(Config.PARTY_RANGE, npc, partyPlayer, true))
						{
							partyDmg += reward2.getDamage(); // Add Player damages to party damages
							rewardedMembers.add(partyPlayer);
							
							if (partyPlayer.getLevel() > partyLvl)
							{
								partyLvl = attackerParty.isInCommandChannel() ? attackerParty.getCommandChannel().getLevel() : partyPlayer.getLevel();
							}
						}
						rewards.remove(partyPlayer); // Remove the Player from the Attackable rewards
						
						playersWithPets.put(partyPlayer, reward2);
						if (partyPlayer.hasPet() && rewards.containsKey(partyPlayer.getPet()))
						{
							playersWithPets.put(partyPlayer.getPet(), rewards.get(partyPlayer.getPet()));
						}
					}
					// Add Player of the party (that have attacked or not) to members that can be rewarded and in range of the monster.
					else
					{
						if (MathUtil.checkIfInRange(Config.PARTY_RANGE, npc, partyPlayer, true))
						{
							rewardedMembers.add(partyPlayer);
							if (partyPlayer.getLevel() > partyLvl)
							{
								partyLvl = attackerParty.isInCommandChannel() ? attackerParty.getCommandChannel().getLevel() : partyPlayer.getLevel();
							}
						}
					}
				}
				
				// If the party didn't killed this Attackable alone
				if (partyDmg < totalDamage)
				{
					partyMul = (float) partyDmg / totalDamage;
				}
				
				// Calculate the level difference between Party and Attackable
				final int levelDiff = partyLvl - npc.getLevel();
				
				// Calculate Exp and SP rewards
				final int[] expSp = calculateExpAndSp(npc, levelDiff, partyDmg, totalDamage);
				long exp = expSp[0];
				int sp = expSp[1];
				
				exp *= _expSettings.get(ExpSpType.EXP);
				sp *= _expSettings.get(ExpSpType.SP);
				
				// if (isChampion())
				// {
				// exp *= Config.CHAMPION_REWARDS;
				// sp *= Config.CHAMPION_REWARDS;
				// }
				
				exp *= partyMul;
				sp *= partyMul;
				
				// Check for an over-hit enabled strike
				// (When in party, the over-hit exp bonus is given to the whole party and splitted proportionally through the party members)
				if (npc.isOverhit() && npc.getOverhitAttacker() != null && npc.getOverhitAttacker().getActingPlayer() != null && attacker == npc.getOverhitAttacker().getActingPlayer())
				{
					attacker.sendPacket(SystemMessageId.OVER_HIT);
					exp += npc.calculateOverhitExp(exp);
				}
				
				// Distribute Experience and SP rewards to Player Party members in the known area of the last attacker
				if (partyDmg > 0)
				{
					attackerParty.distributeXpAndSp(exp, sp, rewardedMembers, partyLvl, playersWithPets);
				}
			}
		}
		
	}
	
	/**
	 * Calculate the Experience and SP to distribute to attacker (Player, L2SummonInstance or L2Party) of the Attackable.
	 * @param diff The difference of level between attacker (Player, L2SummonInstance or L2Party) and the Attackable
	 * @param damage The damages given by the attacker (Player, L2SummonInstance or L2Party)
	 * @param totalDamage The total damage done.
	 * @return an array consisting of xp and sp values.
	 */
	private static int[] calculateExpAndSp(Attackable npc, int diff, int damage, long totalDamage)
	{
		if (diff < -5)
		{
			diff = -5;
		}
		
		double xp = (double) npc.getExpReward() * damage / totalDamage;
		double sp = (double) npc.getSpReward() * damage / totalDamage;
		
		final L2Skill hpSkill = npc.getSkill(4408);
		if (hpSkill != null)
		{
			xp *= hpSkill.getPower();
			sp *= hpSkill.getPower();
		}
		
		if (diff > 5) // formula revised May 07
		{
			double pow = Math.pow((double) 5 / 6, diff - 5);
			xp = xp * pow;
			sp = sp * pow;
		}
		
		if (xp <= 0)
		{
			xp = 0;
			sp = 0;
		}
		else if (sp <= 0)
		{
			sp = 0;
		}
		
		int[] tmp =
		{
			(int) xp,
			(int) sp
		};
		
		return tmp;
	}
}
