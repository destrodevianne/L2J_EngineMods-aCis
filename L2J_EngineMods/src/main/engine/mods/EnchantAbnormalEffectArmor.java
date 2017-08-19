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
import net.sf.l2j.gameserver.data.xml.ArmorSetData;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.item.ArmorSet;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;

/**
 * Class responsible for giving the character a "custom" effect by having all their set enchanted to xxx
 * @author fissban
 */
public class EnchantAbnormalEffectArmor extends AbstractMods
{
	/**
	 * Constructor
	 */
	public EnchantAbnormalEffectArmor()
	{
		registerMod(ConfigData.ENABLE_EnchantAbnormalEffectArmor);
	}
	
	@Override
	public void onModState()
	{
		//
	}
	
	@Override
	public void onEnchant(Creature player)
	{
		checkSetEffect(player);
	}
	
	@Override
	public void onEquip(Creature player)
	{
		checkSetEffect(player);
	}
	
	@Override
	public void onUnequip(Creature player)
	{
		checkSetEffect(player);
	}
	
	@Override
	public boolean onExitWorld(Player player)
	{
		cancelTimer("customEffectSkill", null, player);
		
		return super.onExitWorld(player);
	}
	
	@Override
	public void onTimer(String timerName, Npc npc, Player player)
	{
		switch (timerName)
		{
			case "customEffectSkill":
				
				if (player != null)
				{
					player.broadcastPacket(new MagicSkillUse(player, player, 4326, 1, 1000, 1000));
				}
				break;
		}
	}
	
	/** MISC --------------------------------------------------------------------------------------------- */
	
	private void checkSetEffect(Creature character)
	{
		if (!Util.areObjectType(Player.class, character))
		{
			return;
		}
		
		Player player = (Player) character;
		
		// We review the positions of the set of the character.
		if (checkItems(player))
		{
			startTimer("customEffectSkill", 2000, null, player, true);
		}
		else
		{
			// if the character has the effect would Cancelled
			cancelTimer("customEffectSkill", null, player);
		}
	}
	
	/**
	 * It checks the character:<br>
	 * <li>Keep all equipment + ENCHANT_EFFECT_LVL except the coat and jewelry</li><br>
	 * <li>You have equipped a complete set according to "ArmorSetsTable"</li> <br>
	 * @param player
	 * @param paperdoll
	 * @return
	 */
	private boolean checkItems(Player player)
	{
		Inventory inv = player.getInventory();
		
		// Checks if player is wearing a chest item
		final ItemInstance chestItem = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		if (chestItem == null)
		{
			return false;
		}
		
		// checks if there is armorset for chest item that player worns
		final ArmorSet armorSet = ArmorSetData.getInstance().getSet(chestItem.getItemId());
		if (armorSet == null)
		{
			return false;
		}
		
		// check enchant lvl
		if (chestItem.getEnchantLevel() < ConfigData.ENCHANT_EFFECT_LVL)
		{
			return false;
		}
		
		int legs = 0;
		int head = 0;
		int gloves = 0;
		int feet = 0;
		
		final ItemInstance legsItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		if (legsItem != null && legsItem.getEnchantLevel() >= ConfigData.ENCHANT_EFFECT_LVL)
		{
			legs = legsItem.getItemId();
		}
		
		if (armorSet.getSetItemsId()[1] != 0 && armorSet.getSetItemsId()[1] != legs)
		{
			return false;
		}
		
		final ItemInstance headItem = inv.getPaperdollItem(Inventory.PAPERDOLL_HEAD);
		if (headItem != null && headItem.getEnchantLevel() >= ConfigData.ENCHANT_EFFECT_LVL)
		{
			head = headItem.getItemId();
		}
		
		if (armorSet.getSetItemsId()[2] != 0 && armorSet.getSetItemsId()[2] != head)
		{
			return false;
		}
		
		final ItemInstance glovesItem = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		if (glovesItem != null && glovesItem.getEnchantLevel() >= ConfigData.ENCHANT_EFFECT_LVL)
		{
			gloves = glovesItem.getItemId();
		}
		
		if (armorSet.getSetItemsId()[3] != 0 && armorSet.getSetItemsId()[3] != gloves)
		{
			return false;
		}
		
		final ItemInstance feetItem = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
		if (feetItem != null && feetItem.getEnchantLevel() >= ConfigData.ENCHANT_EFFECT_LVL)
		{
			feet = feetItem.getItemId();
		}
		
		if (armorSet.getSetItemsId()[4] != 0 && armorSet.getSetItemsId()[4] != feet)
		{
			return false;
		}
		
		return true;
	}
}
