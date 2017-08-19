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
package main.engine.npc;

import java.util.ArrayList;
import java.util.List;

import main.engine.AbstractMods;
import main.util.Util;
import main.util.builders.html.Html;
import main.util.builders.html.HtmlBuilder;
import net.sf.l2j.gameserver.data.CharTemplateTable;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.TutorialShowQuestionMark;

/**
 * @author fissban
 */
public class NpcClassMaster extends AbstractMods
{
	private static final int NPC = 60007;
	
	// price (PriceItemId/PriceItemCount/ReardItemId/RewardItemCount)
	// Usar el 0 para no definir nungun valor
	// FIXME el primer valor es nulo
	private static final List<ClassMasterList> ITEM_LIST = new ArrayList<>();
	{
		ITEM_LIST.add(new ClassMasterList(0, 0, 0, 0));
		ITEM_LIST.add(new ClassMasterList(0, 0, 0, 0));// Job lvl 1
		ITEM_LIST.add(new ClassMasterList(0, 0, 0, 0));// Job lvl 2
		ITEM_LIST.add(new ClassMasterList(0, 0, 0, 0));// Job lvl 3
	}
	// Spawns
	private static final List<Location> SPAWNS = new ArrayList<>();
	{
		SPAWNS.add(new Location(11283, 15951, -4584));// ClassChange_DE
		SPAWNS.add(new Location(115774, -178666, -958));// ClassChange_DW
		SPAWNS.add(new Location(45036, 48384, -3060));// ClassChange_E
		SPAWNS.add(new Location(-44747, -113865, -208));// ClassChange_ORC
		SPAWNS.add(new Location(-84466, 243171, -3729));// ClassChange_TI
	}
	// Html
	private static final String HTML_PATH = "data/html/engine/npc/classmaster/";
	
	public NpcClassMaster()
	{
		registerMod(true);
	}
	
	@Override
	public void onModState()
	{
		switch (getState())
		{
			case START:
				for (Location loc : SPAWNS)
				{
					addSpawn(NPC, loc, false, 0);
				}
				break;
			case END:
				
				break;
		}
	}
	
	@Override
	public boolean onInteract(Player player, Creature npc)
	{
		if (!Util.areObjectType(Npc.class, npc))
		{
			return false;
		}
		
		if (((Npc) npc).getNpcId() != NPC)
		{
			return false;
		}
		
		HtmlBuilder hb = new HtmlBuilder();
		hb.append("<html><body>");
		hb.append(Html.headHtml("CLASS MASTER"));
		hb.append("<br>");
		hb.append("Welcome my name is ", npc.getName(), " and take care to meet the most famous players in the world.<br>");
		hb.append("You probably want to know who it is!<br>");
		hb.append("I actually have a list, I can show it to you if you want.<br>");
		hb.append("What would you like to see?<br>");
		hb.append("<center>");
		hb.append("<table width=280>");
		hb.append("<tr>");
		hb.append("<td align=center>", Html.newImage("L2UI.bbs_folder", 32, 32), "</td>");
		hb.append("<td><button value=\"Complete the first class transfer\" action=\"bypass -h Engine NpcClassMaster 1stClass\" width=216 height=32 back=L2UI_CH3.refinegrade3_21 fore=L2UI_CH3.refinegrade3_21></td>");
		hb.append("<td align=center>", Html.newImage("L2UI.bbs_folder", 32, 32), "</td>");
		hb.append("</tr>");
		hb.append("<tr>");
		hb.append("<td align=center>", Html.newImage("L2UI.bbs_folder", 32, 32), "</td>");
		hb.append("<td><button value=\"Complete the second class transfer\" action=\"bypass -h Engine NpcClassMaster 2ndClass\" width=216 height=32 back=L2UI_CH3.refinegrade3_21 fore=L2UI_CH3.refinegrade3_21></td>");
		hb.append("<td align=center>", Html.newImage("L2UI.bbs_folder", 32, 32), "</td>");
		hb.append("</tr>");
		hb.append("<tr>");
		hb.append("<td align=center>", Html.newImage("L2UI.bbs_folder", 32, 32), "</td>");
		hb.append("<td><button value=\"Complete the third class transfer\" action=\"bypass -h Engine NpcClassMaster 3rdClass\" width=216 height=32 back=L2UI_CH3.refinegrade3_21 fore=L2UI_CH3.refinegrade3_21></td>");
		hb.append("<td align=center>", Html.newImage("L2UI.bbs_folder", 32, 32), "</td>");
		hb.append("</tr>");
		hb.append("</table>");
		hb.append("</center>");
		hb.append("</body></html>");
		
		sendHtml(player, (Npc) npc, hb);
		
		return true;
	}
	
	@Override
	public void onEvent(Player player, Creature npc, String command)
	{
		if (((Npc) npc).getNpcId() != NPC)
		{
			return;
		}
		
		if (command.startsWith("1stClass"))
		{
			showHtmlMenu(player, npc.getObjectId(), 1);
		}
		else if (command.startsWith("2ndClass"))
		{
			showHtmlMenu(player, npc.getObjectId(), 2);
		}
		else if (command.startsWith("3rdClass"))
		{
			showHtmlMenu(player, npc.getObjectId(), 3);
		}
		else if (command.startsWith("change_class"))
		{
			int val = Integer.parseInt(command.substring(13));
			
			if (checkAndChangeClass(player, val))
			{
				player.sendPacket(new PlaySound("ItemSound.quest_fanfare_2"));
				player.sendPacket(new PlaySound("Systemmsg_e.char_change"));
				NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
				html.setFile(HTML_PATH + "ok.htm");
				html.replace("%name%", CharTemplateTable.getInstance().getClassNameById(val));
				player.sendPacket(html);
			}
		}
	}
	
	private static final void showHtmlMenu(Player player, int objectId, int level)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(objectId);
		
		final ClassId currentClassId = player.getClassId();
		if (currentClassId.level() >= level)
		{
			html.setFile(HTML_PATH + "nomore.htm");
		}
		else
		{
			final int minLevel = getMinLevel(currentClassId.level());
			if (player.getLevel() >= minLevel)
			{
				final StringBuilder menu = new StringBuilder(100);
				for (ClassId cid : ClassId.values())
				{
					if (validateClassId(currentClassId, cid) && cid.level() == level)
					{
						menu.append("<button value=\"");
						menu.append(CharTemplateTable.getInstance().getClassNameById(cid.getId()) + "\" ");
						menu.append("action=\"bypass -h Engine NpcClassMaster change_class ");
						menu.append(String.valueOf(cid.getId()) + "\"");
						menu.append(" width=204 height=20 back=\"sek.cbui77\" fore=\"sek.cbui75\"><br>");
					}
				}
				
				if (menu.length() > 0)
				{
					html.setFile(HTML_PATH + "template.htm");
					html.replace("%name%", CharTemplateTable.getInstance().getClassNameById(currentClassId.getId()));
					html.replace("%menu%", menu.toString());
				}
				else
				{
					html.setFile(HTML_PATH + "comebacklater.htm");
					html.replace("%level%", String.valueOf(getMinLevel(level - 1)));
				}
			}
			else
			{
				if (minLevel < Integer.MAX_VALUE)
				{
					html.setFile(HTML_PATH + "comebacklater.htm");
					html.replace("%level%", String.valueOf(minLevel));
				}
				else
				{
					html.setFile(HTML_PATH + "nomore.htm");
				}
			}
		}
		
		html.replace("%objectId%", String.valueOf(objectId));
		html.replace("%req_items%", getRequiredItems(level));
		player.sendPacket(html);
	}
	
	private static final void showQuestionMark(Player player)
	{
		final ClassId classId = player.getClassId();
		if (getMinLevel(classId.level()) > player.getLevel())
		{
			return;
		}
		
		player.sendPacket(new TutorialShowQuestionMark(1001));
	}
	
	/**
	 * Returns minimum player level required for next class transfer
	 * @param level - current skillId level (0 - start, 1 - first, etc)
	 * @return
	 */
	private static final int getMinLevel(int level)
	{
		switch (level)
		{
			case 0:
				return 20;
			case 1:
				return 40;
			case 2:
				return 76;
			default:
				return Integer.MAX_VALUE;
		}
	}
	
	/**
	 * Returns true if class change is possible
	 * @param oldCID current player ClassId
	 * @param val new class index
	 * @return
	 */
	private static final boolean validateClassId(ClassId oldCID, int val)
	{
		try
		{
			return validateClassId(oldCID, ClassId.values()[val]);
		}
		catch (Exception e)
		{
			// possible ArrayOutOfBoundsException
		}
		return false;
	}
	
	/**
	 * Returns true if class change is possible
	 * @param oldCID current player ClassId
	 * @param newCID new ClassId
	 * @return true if class change is possible
	 */
	private static final boolean validateClassId(ClassId oldCID, ClassId newCID)
	{
		if (newCID == null || newCID.getRace() == null)
		{
			return false;
		}
		
		if (oldCID.equals(newCID.getParent()))
		{
			return true;
		}
		
		// if (Config.ALLOW_ENTIRE_TREE && newCID.childOf(oldCID))
		// {
		// return true;
		// }
		
		return false;
	}
	
	private static String getRequiredItems(int level)
	{
		StringBuilder sb = new StringBuilder();
		if (ITEM_LIST.get(level).getPriceItemId() != 0 && ITEM_LIST.get(level).getPriceItemCount() != 0)
		{
			int count = ITEM_LIST.get(level).getPriceItemCount();
			String itemName = ItemTable.getInstance().getTemplate(ITEM_LIST.get(level).getPriceItemId()).getName();
			sb.append("<tr><td><img src=\"Icon.Item_System04\" width=32 height=32></td>");
			sb.append("<td><font color=\"LEVEL\">[" + count + "]</font></td>");
			sb.append("<td>[" + itemName + "]</td>");
			sb.append("<td><img src=\"Icon.Item_System05\" width=32 height=32></td></tr>");
		}
		else
		{
			sb.append("<tr><td>Free</td></tr>");
		}
		return sb.toString();
	}
	
	private static final boolean checkAndChangeClass(Player player, int val)
	{
		final ClassId currentClassId = player.getClassId();
		if (getMinLevel(currentClassId.level()) > player.getLevel())
		{
			return false;
		}
		
		if (!validateClassId(currentClassId, val))
		{
			return false;
		}
		
		int newJobLevel = currentClassId.level() + 1;
		
		// Weight/Inventory check
		if (ITEM_LIST.get(newJobLevel).getRewardItemId() != 0)
		{
			if (player.getWeightPenalty() >= 3 || player.getInventoryLimit() * 0.8 <= player.getInventory().getSize())
			{
				player.sendPacket(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT);
				return false;
			}
		}
		
		// get all required items for class transfer
		int priceCount = ITEM_LIST.get(newJobLevel).getPriceItemCount();
		int priceItemId = ITEM_LIST.get(newJobLevel).getPriceItemId();
		
		if (!player.destroyItemByItemId("ClassMaster", priceItemId, priceCount, player, true))
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
			return false;
		}
		
		// reward player with items
		int rewardCount = ITEM_LIST.get(newJobLevel).getRewardItemCount();
		int rewardItemId = ITEM_LIST.get(newJobLevel).getRewardItemId();
		player.addItem("ClassMaster", rewardItemId, rewardCount, player, true);
		
		player.setClassId(val);
		
		if (player.isSubClassActive())
		{
			player.getSubClasses().get(player.getClassIndex()).setClassId(player.getActiveClass());
		}
		else
		{
			player.setBaseClass(player.getActiveClass());
		}
		
		player.broadcastUserInfo();
		
		if (player.getClassId().level() == 1 && player.getLevel() >= 40 || player.getClassId().level() == 2 && player.getLevel() >= 76)
		{
			showQuestionMark(player);
		}
		
		return true;
	}
	
	public class ClassMasterList
	{
		private final int _priceItemId;
		private final int _priceItemCount;
		
		private final int _rewardItemId;
		private final int _rewardItemCount;
		
		public ClassMasterList(int priceItemId, int priceItemCount, int rewardItemId, int rewardItemCount)
		{
			_priceItemId = priceItemId;
			_priceItemCount = priceItemCount;
			
			_rewardItemId = rewardItemId;
			_rewardItemCount = rewardItemCount;
		}
		
		public int getPriceItemId()
		{
			return _priceItemId;
		}
		
		public int getPriceItemCount()
		{
			return _priceItemCount;
		}
		
		public int getRewardItemId()
		{
			return _rewardItemId;
		}
		
		public int getRewardItemCount()
		{
			return _rewardItemCount;
		}
	}
}
