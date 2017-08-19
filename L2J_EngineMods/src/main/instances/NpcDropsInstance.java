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

import java.util.HashMap;
import java.util.Map;

import main.enums.ItemDropType;
import main.holders.DropBonusHolder;
import net.sf.l2j.Config;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.data.xml.HerbDropData;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.DropCategory;
import net.sf.l2j.gameserver.model.item.DropData;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * @author fissban
 */
public class NpcDropsInstance
{
	private final Map<ItemDropType, DropBonusHolder> _dropsSettings = new HashMap<>();
	{
		_dropsSettings.put(ItemDropType.NORMAL, new DropBonusHolder());
		_dropsSettings.put(ItemDropType.SPOIL, new DropBonusHolder());
		_dropsSettings.put(ItemDropType.SEED, new DropBonusHolder());
		_dropsSettings.put(ItemDropType.HERB, new DropBonusHolder());
	}
	
	public NpcDropsInstance()
	{
		//
	}
	
	public void increaseDrop(ItemDropType type, double chance, double amount)
	{
		_dropsSettings.get(type).increaseAmountBonus(amount);
		_dropsSettings.get(type).increaseChanceBonus(chance);
	}
	
	public boolean hasSettings()
	{
		for (DropBonusHolder holder : _dropsSettings.values())
		{
			if (holder.getAmountBonus() > 1.0 || holder.getChanceBonus() > 1.0)
			{
				return true;
			}
		}
		return false;
	}
	
	public void init(Attackable npc, Creature mainDamageDealer)
	{
		// -------------------------------------------------------------------------------------------------------------------
		
		if (mainDamageDealer == null)
		{
			return;
		}
		
		// Don't drop anything if the last attacker or owner isn't Player
		Player player = mainDamageDealer.getActingPlayer();
		if (player == null)
		{
			return;
		}
		
		CursedWeaponsManager.getInstance().checkDrop(npc, player);
		
		// level modifier in %'s (will be subtracted from drop chance)
		int levelModifier = calculateLevelModifierForDrop(npc, player);
		
		// now throw all categorized drops and handle spoil.
		for (DropCategory cat : npc.getTemplate().getDropData())
		{
			IntIntHolder item = null;
			if (cat.isSweep())
			{
				if (npc.getSpoilerId() != 0)
				{
					for (DropData drop : cat.getAllDrops())
					{
						item = calculateRewardItem(npc, player, drop, levelModifier, true);
						if (item == null)
						{
							continue;
						}
						
						npc.getSweepItems().add(item);
					}
				}
			}
			else
			{
				if (npc.isSeeded())
				{
					DropData drop = cat.dropSeedAllowedDropsOnly();
					if (drop == null)
					{
						continue;
					}
					
					item = calculateRewardItem(npc, player, drop, levelModifier, false);
				}
				else
				{
					item = calculateCategorizedRewardItem(npc, player, cat, levelModifier);
				}
				
				if (item != null)
				{
					// Check if the autoLoot mode is active
					if (npc.isRaid() && Config.AUTO_LOOT_RAID || !npc.isRaid() && Config.AUTO_LOOT)
					{
						player.doAutoLoot(npc, item); // Give this or these Item(s) to the Player that has killed the Attackable
					}
					else
					{
						npc.dropItem(player, item); // drop the item on the ground
					}
					
					// Broadcast message if RaidBoss was defeated
					if (npc.isRaid() && !npc.isRaidMinion())
					{
						npc.broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DIED_DROPPED_S3_S2).addCharName(npc).addItemName(item.getId()).addNumber(item.getValue()));
					}
				}
			}
		}
		
		// Herbs.
		if (npc.getTemplate().getDropHerbGroup() > 0)
		{
			for (DropCategory cat : HerbDropData.getInstance().getHerbDroplist(npc.getTemplate().getDropHerbGroup()))
			{
				final IntIntHolder item = calculateCategorizedHerbItem(cat, levelModifier);
				if (item != null)
				{
					if (Config.AUTO_LOOT_HERBS)
					{
						player.addItem("Loot", item.getId(), 1, npc, true);
					}
					else
					{
						// If multiple similar herbs drop, split them and make a unique drop per item.
						final int count = item.getValue();
						if (count > 1)
						{
							item.setValue(1);
							for (int i = 0; i < count; i++)
							{
								npc.dropItem(player, item);
							}
						}
						else
						{
							npc.dropItem(player, item);
						}
					}
				}
			}
		}
	}
	
	// ----------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Calculates quantity of items for specific drop according to current situation.
	 * @param drop The L2DropData count is being calculated for
	 * @param lastAttacker The Player that has killed the Attackable
	 * @param levelModifier level modifier in %'s (will be subtracted from drop chance)
	 * @param isSweep if true, use spoil drop chance.
	 * @return the ItemHolder.
	 */
	private IntIntHolder calculateRewardItem(Npc npc, Player lastAttacker, DropData drop, int levelModifier, boolean isSweep)
	{
		// Get default drop chance
		double dropChance = drop.getChance();
		
		if (Config.DEEPBLUE_DROP_RULES)
		{
			int deepBlueDrop = 1;
			if (levelModifier > 0)
			{
				// We should multiply by the server's drop rate, so we always get a low chance of drop for deep blue mobs.
				// NOTE: This is valid only for adena drops! Others drops will still obey server's rate
				deepBlueDrop = 3;
				if (drop.getItemId() == 57)
				{
					deepBlueDrop *= npc.isRaid() && !npc.isRaidMinion() ? (int) Config.RATE_DROP_ITEMS_BY_RAID : (int) Config.RATE_DROP_ITEMS;
					if (deepBlueDrop == 0)
					{
						deepBlueDrop = 1;
					}
				}
			}
			
			// Check if we should apply our maths so deep blue mobs will not drop that easy
			dropChance = (drop.getChance() - drop.getChance() * levelModifier / 100) / deepBlueDrop;
		}
		
		// Applies Drop rates
		if (drop.getItemId() == 57)
		{
			dropChance *= Config.RATE_DROP_ADENA;
		}
		else if (isSweep)
		{
			dropChance *= Config.RATE_DROP_SPOIL;
		}
		else
		{
			dropChance *= npc.isRaid() && !npc.isRaidMinion() ? Config.RATE_DROP_ITEMS_BY_RAID : Config.RATE_DROP_ITEMS;
		}
		
		// Custom for all mods
		dropChance *= _dropsSettings.get(isSweep ? ItemDropType.SPOIL : ItemDropType.SEED).getChanceBonus();
		
		if (dropChance < 1)
		{
			dropChance = 1;
		}
		
		// Get min and max Item quantity that can be dropped in one time
		final int minCount = drop.getMinDrop();
		final int maxCount = drop.getMaxDrop();
		
		// Get the item quantity dropped
		int itemCount = 0;
		
		// Check if the Item must be dropped
		int random = Rnd.get(DropData.MAX_CHANCE);
		while (random < dropChance)
		{
			// Get the item quantity dropped
			if (minCount < maxCount)
			{
				itemCount += Rnd.get(minCount, maxCount);
			}
			else if (minCount == maxCount)
			{
				itemCount += minCount;
			}
			else
			{
				itemCount++;
			}
			
			// Prepare for next iteration if dropChance > L2DropData.MAX_CHANCE
			dropChance -= DropData.MAX_CHANCE;
		}
		
		// Custom for all mods
		itemCount *= _dropsSettings.get(ItemDropType.SPOIL).getAmountBonus();
		
		if (itemCount > 0)
		{
			return new IntIntHolder(drop.getItemId(), itemCount);
		}
		
		return null;
	}
	
	/**
	 * Calculates quantity of items for specific drop CATEGORY according to current situation <br>
	 * Only a max of ONE item from a category is allowed to be dropped.
	 * @param lastAttacker The Player that has killed the Attackable
	 * @param categoryDrops The category to make checks on.
	 * @param levelModifier level modifier in %'s (will be subtracted from drop chance)
	 * @return the ItemHolder.
	 */
	private IntIntHolder calculateCategorizedRewardItem(Npc npc, Player lastAttacker, DropCategory categoryDrops, int levelModifier)
	{
		if (categoryDrops == null)
		{
			return null;
		}
		
		// Get default drop chance for the category (that's the sum of chances for all items in the category)
		// keep track of the base category chance as it'll be used later, if an item is drop from the category.
		// for everything else, use the total "categoryDropChance"
		int basecategoryDropChance = categoryDrops.getCategoryChance();
		int categoryDropChance = basecategoryDropChance;
		
		if (Config.DEEPBLUE_DROP_RULES)
		{
			int deepBlueDrop = levelModifier > 0 ? 3 : 1;
			
			// Check if we should apply our maths so deep blue mobs will not drop that easy
			categoryDropChance = (categoryDropChance - categoryDropChance * levelModifier / 100) / deepBlueDrop;
		}
		
		// Applies Drop rates
		categoryDropChance *= npc.isRaid() && !npc.isRaidMinion() ? Config.RATE_DROP_ITEMS_BY_RAID : Config.RATE_DROP_ITEMS;
		
		// Custom for all mods
		categoryDropChance *= _dropsSettings.get(ItemDropType.NORMAL).getChanceBonus();
		
		// Set our limits for chance of drop
		if (categoryDropChance < 1)
		{
			categoryDropChance = 1;
		}
		
		// Check if an Item from this category must be dropped
		if (Rnd.get(DropData.MAX_CHANCE) < categoryDropChance)
		{
			DropData drop = categoryDrops.dropOne(npc.isRaid() && !npc.isRaidMinion());
			if (drop == null)
			{
				return null;
			}
			
			// Now decide the quantity to drop based on the rates and penalties. To get this value
			// simply divide the modified categoryDropChance by the base category chance. This
			// results in a chance that will dictate the drops amounts: for each amount over 100
			// that it is, it will give another chance to add to the min/max quantities.
			//
			// For example, If the final chance is 120%, then the item should drop between
			// its min and max one time, and then have 20% chance to drop again. If the final
			// chance is 330%, it will similarly give 3 times the min and max, and have a 30%
			// chance to give a 4th time.
			// At least 1 item will be dropped for sure. So the chance will be adjusted to 100%
			// if smaller.
			
			double dropChance = drop.getChance();
			if (drop.getItemId() == 57)
			{
				dropChance *= Config.RATE_DROP_ADENA;
			}
			else
			{
				dropChance *= npc.isRaid() && !npc.isRaidMinion() ? Config.RATE_DROP_ITEMS_BY_RAID : Config.RATE_DROP_ITEMS;
			}
			
			// Custom for all mods
			dropChance *= _dropsSettings.get(ItemDropType.NORMAL).getChanceBonus();
			
			if (dropChance < DropData.MAX_CHANCE)
			{
				dropChance = DropData.MAX_CHANCE;
			}
			
			// Get min and max Item quantity that can be dropped in one time
			final int min = drop.getMinDrop();
			final int max = drop.getMaxDrop();
			
			// Get the item quantity dropped
			int itemCount = 0;
			
			// Check if the Item must be dropped
			int random = Rnd.get(DropData.MAX_CHANCE);
			while (random < dropChance)
			{
				// Get the item quantity dropped
				if (min < max)
				{
					itemCount += Rnd.get(min, max);
				}
				else if (min == max)
				{
					itemCount += min;
				}
				else
				{
					itemCount++;
				}
				
				// Prepare for next iteration if dropChance > L2DropData.MAX_CHANCE
				dropChance -= DropData.MAX_CHANCE;
			}
			
			// Custom for all mods
			itemCount *= _dropsSettings.get(ItemDropType.NORMAL).getAmountBonus();
			
			if (itemCount > 0)
			{
				return new IntIntHolder(drop.getItemId(), itemCount);
			}
		}
		return null;
	}
	
	/**
	 * @param lastAttacker The Player that has killed the Attackable
	 * @return the level modifier for drop
	 */
	private int calculateLevelModifierForDrop(Npc npc, Player lastAttacker)
	{
		if (Config.DEEPBLUE_DROP_RULES)
		{
			int highestLevel = lastAttacker.getLevel();
			
			// Check to prevent very high level player to nearly kill mob and let low level player do the last hit.
			for (Creature atkChar : ((Attackable) npc).getAttackByList())
			{
				if (atkChar.getLevel() > highestLevel)
				{
					highestLevel = atkChar.getLevel();
				}
			}
			
			// According to official data (Prima), deep blue mobs are 9 or more levels below players
			if (highestLevel - 9 >= npc.getLevel())
			{
				return (highestLevel - (npc.getLevel() + 8)) * 9;
			}
		}
		return 0;
	}
	
	private IntIntHolder calculateCategorizedHerbItem(DropCategory categoryDrops, int levelModifier)
	{
		if (categoryDrops == null)
		{
			return null;
		}
		
		int categoryDropChance = categoryDrops.getCategoryChance();
		
		// Applies Drop rates
		switch (categoryDrops.getCategoryType())
		{
			case 1:
				categoryDropChance *= Config.RATE_DROP_HP_HERBS;
				break;
			case 2:
				categoryDropChance *= Config.RATE_DROP_MP_HERBS;
				break;
			case 3:
				categoryDropChance *= Config.RATE_DROP_SPECIAL_HERBS;
				break;
			default:
				categoryDropChance *= Config.RATE_DROP_COMMON_HERBS;
		}
		
		// Custom for all mods
		categoryDropChance *= _dropsSettings.get(ItemDropType.HERB).getChanceBonus();
		
		// Drop chance is affected by deep blue drop rule.
		if (Config.DEEPBLUE_DROP_RULES)
		{
			int deepBlueDrop = levelModifier > 0 ? 3 : 1;
			
			// Check if we should apply our maths so deep blue mobs will not drop that easy
			categoryDropChance = (categoryDropChance - categoryDropChance * levelModifier / 100) / deepBlueDrop;
		}
		
		// Check if an Item from this category must be dropped
		if (Rnd.get(DropData.MAX_CHANCE) < Math.max(1, categoryDropChance))
		{
			final DropData drop = categoryDrops.dropOne(false);
			if (drop == null)
			{
				return null;
			}
			
			/*
			 * Now decide the quantity to drop based on the rates and penalties. To get this value, simply divide the modified categoryDropChance by the base category chance. This results in a chance that will dictate the drops amounts : for each amount over 100 that it is, it will give another
			 * chance to add to the min/max quantities. For example, if the final chance is 120%, then the item should drop between its min and max one time, and then have 20% chance to drop again. If the final chance is 330%, it will similarly give 3 times the min and max, and have a 30% chance to
			 * give a 4th time. At least 1 item will be dropped for sure. So the chance will be adjusted to 100% if smaller.
			 */
			double dropChance = drop.getChance();
			
			switch (categoryDrops.getCategoryType())
			{
				case 1:
					dropChance *= Config.RATE_DROP_HP_HERBS;
					break;
				case 2:
					dropChance *= Config.RATE_DROP_MP_HERBS;
					break;
				case 3:
					dropChance *= Config.RATE_DROP_SPECIAL_HERBS;
					break;
				default:
					dropChance *= Config.RATE_DROP_COMMON_HERBS;
			}
			
			// Custom for all mods
			dropChance *= _dropsSettings.get(ItemDropType.HERB).getChanceBonus();
			
			if (dropChance < DropData.MAX_CHANCE)
			{
				dropChance = DropData.MAX_CHANCE;
			}
			
			// Get min and max Item quantity that can be dropped in one time
			final int min = drop.getMinDrop();
			final int max = drop.getMaxDrop();
			
			// Get the item quantity dropped
			int itemCount = 0;
			
			// Check if the Item must be dropped
			int random = Rnd.get(DropData.MAX_CHANCE);
			while (random < dropChance)
			{
				// Get the item quantity dropped
				if (min < max)
				{
					itemCount += Rnd.get(min, max);
				}
				else if (min == max)
				{
					itemCount += min;
				}
				else
				{
					itemCount++;
				}
				
				// Prepare for next iteration if dropChance > L2DropData.MAX_CHANCE
				dropChance -= DropData.MAX_CHANCE;
			}
			
			// Custom for all mods
			itemCount *= _dropsSettings.get(ItemDropType.HERB).getAmountBonus();
			
			if (itemCount > 0)
			{
				return new IntIntHolder(drop.getItemId(), itemCount);
			}
		}
		return null;
	}
}
