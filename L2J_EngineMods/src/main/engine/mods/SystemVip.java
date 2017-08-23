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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;

import main.data.ConfigData;
import main.data.PlayerData;
import main.engine.AbstractMods;
import main.enums.ExpSpType;
import main.enums.ItemDropType;
import main.holders.PlayerHolder;
import main.instances.NpcDropsInstance;
import main.instances.NpcExpInstance;
import main.util.Util;
import main.util.builders.html.Html;
import main.util.builders.html.HtmlBuilder;
import main.util.builders.html.HtmlBuilder.HtmlType;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage.SMPOS;
import net.sf.l2j.gameserver.skills.Stats;

/**
 * @author fissban
 */
public class SystemVip extends AbstractMods
{
	public SystemVip()
	{
		registerMod(true);// TODO missing config
	}
	
	@Override
	public void onModState()
	{
		switch (getState())
		{
			case START:
				readAllVips();
				break;
			case END:
				//
				break;
		}
	}
	
	@Override
	public void onNpcExpSp(Player killer, Attackable npc, NpcExpInstance instance)
	{
		if (!PlayerData.get(killer).isVip())
		{
			return;
		}
		
		// ExpSpBonusHolder (bonusType, amountBonus)
		// Example: 110 -> 110%
		// if you use 100% exp will be normal, to earn bonus use values greater than 100%.
		// increase normal exp/sp amount
		instance.increaseRate(ExpSpType.EXP, ConfigData.VIP_BONUS_XP);
		instance.increaseRate(ExpSpType.SP, ConfigData.VIP_BONUS_SP);
		return;
	}
	
	@Override
	public void onNpcDrop(Player killer, Attackable npc, NpcDropsInstance instance)
	{
		if (!PlayerData.get(killer).isVip())
		{
			return;
		}
		// DropBonusHolder (dropType, amountBonus, chanceBonus)
		// Example: 110 -> 110%
		// if you use 100% drop will be normal, to earn bonus use values greater than 100%.
		
		// increase normal drop amount and chance
		instance.increaseDrop(ItemDropType.NORMAL, ConfigData.VIP_BONUS_DROP_NORMAL_AMOUNT, ConfigData.VIP_BONUS_DROP_NORMAL_CHANCE);
		// increase spoil drop amount and chance
		instance.increaseDrop(ItemDropType.SPOIL, ConfigData.VIP_BONUS_DROP_SPOIL_AMOUNT, ConfigData.VIP_BONUS_DROP_SPOIL_CHANCE);
		// increase herb drop amount and chance
		instance.increaseDrop(ItemDropType.HERB, ConfigData.VIP_BONUS_DROP_HERB_AMOUNT, ConfigData.VIP_BONUS_DROP_HERB_CHANCE);
		// increase seed drop amount and chance
		instance.increaseDrop(ItemDropType.SEED, ConfigData.VIP_BONUS_DROP_SEED_AMOUNT, ConfigData.VIP_BONUS_DROP_SEED_CHANCE);
	}
	
	@Override
	public void onEvent(Player player, Creature npc, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		
		String event = st.nextToken();
		switch (event)
		{
			case "allVip":
			{
				if (player.getAccessLevel().getLevel() < 1)
				{
					break;
				}
				
				getAllPlayerVips(player, Integer.parseInt(st.nextToken()));
				break;
			}
		}
	}
	
	@Override
	public boolean onAdminCommand(Player player, String chat)
	{
		StringTokenizer st = new StringTokenizer(chat, " ");
		
		String command = st.nextToken().toLowerCase();
		switch (command)
		{
			case "allvip":
			{
				getAllPlayerVips(player, 1);
				return true;
			}
			case "removevip":
			{
				if (!checkTarget(player))
				{
					return true;
				}
				
				removeVip((Player) player.getTarget());
				return true;
			}
			// only for admins
			// format: setVip days
			case "setvip":
			{
				if (!checkTarget(player))
				{
					return true;
				}
				
				if (!st.hasMoreTokens())
				{
					player.sendMessage("Correct command:");
					player.sendMessage(".setVip days");
					return true;
				}
				
				String days = st.nextToken();
				
				if (!Util.isNumber(days))
				{
					player.sendMessage("Correct command:");
					player.sendMessage(".setVip days");
					return true;
				}
				
				Player vip = (Player) player.getTarget();
				
				// Create calendar
				Calendar time = new GregorianCalendar();
				time.add(Calendar.DAY_OF_YEAR, Integer.parseInt(days));
				// save values in DB
				setValueDB(vip, "vip", time.getTimeInMillis() + "");
				// saved state in memory
				PlayerData.get(vip).setVip(true);
				PlayerData.get(vip).setVipExpireDate(time.getTimeInMillis());
				
				addVip(vip, time.getTimeInMillis());
				
				// Informed admin
				player.sendPacket(new ExShowScreenMessage("player: " + vip.getName() + " is Vip now", 10000, SMPOS.TOP_CENTER, false));
				// Informed player
				vip.sendPacket(new ExShowScreenMessage("player: " + vip.getName() + " is Vip now", 10000, SMPOS.TOP_CENTER, false));
				informeExpireVip(vip);
				
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public void onEnterWorld(Player player)
	{
		if (PlayerData.get(player).isVip())
		{
			if (PlayerData.get(player).getVipExpireDate() < System.currentTimeMillis())
			{
				removeVip(player);
				return;
			}
			
			addVip(player, PlayerData.get(player).getVipExpireDate());
			informeExpireVip(player);
		}
	}
	
	@Override
	public double onStats(Stats stat, Creature character, double value)
	{
		if (!Util.areObjectType(Player.class, character))
		{
			return value;
		}
		
		if (!PlayerData.get(character.getObjectId()).isVip())
		{
			return value;
		}
		
		if (ConfigData.VIP_STATS.containsKey(stat))
		{
			return value *= ConfigData.VIP_STATS.get(stat);
		}
		
		return value;
	}
	
	/**
	 * Send the character html informing the time expire VIP. (format: dd-MMM-yyyy)
	 * @param player
	 * @param dayTime
	 */
	private static void informeExpireVip(Player player)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.HTML_TYPE);
		
		hb.append("<html><body>");
		hb.append(Html.headHtml("VIP"));
		hb.append("<br>");
		// date
		hb.append("<font color=9900CC>VIP Expire Date: </font>", PlayerData.get(player).getVipExpireDateFormat(), "<br>");
		
		hb.append("<font color=LEVEL>The VIP have exp/sp rate:</font><br>");
		
		hb.append("<img src=L2UI.SquareGray width=264 height=1>");
		hb.append("<table fixwidth=264 border=0 cellspacing=0 cellpadding=0 bgcolor=CC99FF>");
		hb.append("<tr>");
		hb.append("<td fixwidth=100 align=center><button value=\"Type\" action=\"\" width=100 height=19 back=L2UI_CH3.amountbox2 fore=L2UI_CH3.amountbox2></td>");
		hb.append("<td fixwidth=164 align=center><button value=\"Bonus\" action=\"\" width=164 height=19 back=L2UI_CH3.amountbox2 fore=L2UI_CH3.amountbox2></td>");
		hb.append("</tr>");
		hb.append("</table>");
		hb.append("<img src=L2UI.SquareGray width=264 height=1>");
		hb.append("<table fixwidth=264>");
		hb.append("<tr>");
		hb.append("<td fixwidth=100 align=center><font color=3366FF>EXP:</font></td>");
		hb.append("<td fixwidth=164 align=center><font color=LEVEL>", (int) ConfigData.VIP_BONUS_XP + 100, "%</font></td>");
		hb.append("</tr>");
		hb.append("</table>");
		hb.append("<img src=L2UI.SquareGray width=264 height=1>");
		hb.append("<table fixwidth=264>");
		hb.append("<tr>");
		hb.append("<td fixwidth=100 align=center><font color=3366FF>SP:</font></td>");
		hb.append("<td fixwidth=164 align=center><font color=LEVEL>", (int) ConfigData.VIP_BONUS_SP + 100, "%</font></td>");
		hb.append("</tr>");
		hb.append("</table>");
		hb.append("<img src=L2UI.SquareGray width=264 height=1>");
		
		hb.append("<br><br><font color=LEVEL>The VIP have drop rate:</font><br>");
		
		hb.append("<img src=L2UI.SquareGray width=264 height=1>");
		hb.append("<table fixwidth=264 border=0 cellspacing=0 cellpadding=0 bgcolor=CC99FF>");
		hb.append("<tr>");
		hb.append("<td fixwidth=100><button value=\"Type\" action=\"\" width=100 height=19 back=L2UI_CH3.amountbox2 fore=L2UI_CH3.amountbox2></td>");
		hb.append("<td fixwidth=82><button value=\"Bonus Amount\" action=\"\" width=82 height=19 back=L2UI_CH3.amountbox2 fore=L2UI_CH3.amountbox2></td>");
		hb.append("<td fixwidth=82><button value=\"Bonus Chance\" action=\"\" width=82 height=19 back=L2UI_CH3.amountbox2 fore=L2UI_CH3.amountbox2></td>");
		hb.append("</tr>");
		hb.append("</table>");
		hb.append("<img src=L2UI.SquareGray width=264 height=1>");
		hb.append("<table fixwidth=264 border=0 cellspacing=0 cellpadding=0>");
		hb.append("<tr>");
		hb.append("<td fixwidth=100 align=center><font color=3366FF>Normal:</font></td>");
		hb.append("<td fixwidth=82 align=center><font color=LEVEL>", (int) ConfigData.VIP_BONUS_DROP_NORMAL_AMOUNT + 100, "%</font></td>");
		hb.append("<td fixwidth=82 align=center><font color=LEVEL>", (int) ConfigData.VIP_BONUS_DROP_NORMAL_AMOUNT + 100, "%</font></td>");
		hb.append("</tr>");
		hb.append("</table>");
		hb.append("<img src=L2UI.SquareGray width=264 height=1>");
		hb.append("<table fixwidth=264 border=0 cellspacing=0 cellpadding=0>");
		hb.append("<tr>");
		hb.append("<td fixwidth=100 align=center><font color=3366FF>Spoil:</font></td>");
		hb.append("<td fixwidth=82 align=center><font color=LEVEL>", (int) ConfigData.VIP_BONUS_DROP_SPOIL_AMOUNT + 100, "%</font></td>");
		hb.append("<td fixwidth=82 align=center><font color=LEVEL>", (int) ConfigData.VIP_BONUS_DROP_SPOIL_CHANCE + 100, "%</font></td>");
		hb.append("</tr>");
		hb.append("</table>");
		hb.append("<img src=L2UI.SquareGray width=264 height=1>");
		hb.append("<table fixwidth=264 border=0 cellspacing=0 cellpadding=0>");
		hb.append("<tr>");
		hb.append("<td fixwidth=100 align=center><font color=3366FF>Seed:</font></td>");
		hb.append("<td fixwidth=82 align=center><font color=LEVEL>", (int) ConfigData.VIP_BONUS_DROP_SEED_AMOUNT + 100, "%</font></td>");
		hb.append("<td fixwidth=82 align=center><font color=LEVEL>", (int) ConfigData.VIP_BONUS_DROP_SEED_AMOUNT + 100, "%</font></td>");
		hb.append("</tr>");
		hb.append("</table>");
		hb.append("<img src=L2UI.SquareGray width=264 height=1>");
		hb.append("<table fixwidth=264 border=0 cellspacing=0 cellpadding=0>");
		hb.append("<tr>");
		hb.append("<td fixwidth=100 align=center><font color=3366FF>Herb:</font></td>");
		hb.append("<td fixwidth=82 align=center><font color=LEVEL>", (int) ConfigData.VIP_BONUS_DROP_HERB_AMOUNT + 100, "%</font></td>");
		hb.append("<td fixwidth=82 align=center><font color=LEVEL>", (int) ConfigData.VIP_BONUS_DROP_HERB_AMOUNT + 100, "%</font></td>");
		hb.append("</tr>");
		hb.append("</table>");
		hb.append("<img src=L2UI.SquareGray width=264 height=1>");
		
		hb.append("</body></html>");
		sendHtml(player, null, hb);
	}
	
	public void addVip(Player player, long dayTime)
	{
		// Shedule to end VIP
		ThreadPool.schedule(() ->
		{
			// TODO missing onExitWorld
			if (player == null)
			{
				return;
			}
			
			informeExpireVip(player);
			removeVip(player);
			
		}, dayTime - System.currentTimeMillis());
		
		if (ConfigData.ALLOW_VIP_NCOLOR)
		{
			player.getAppearance().setNameColor(ConfigData.VIP_NCOLOR);
		}
		
		if (ConfigData.ALLOW_VIP_TCOLOR)
		{
			player.getAppearance().setTitleColor(ConfigData.VIP_TCOLOR);
		}
		
		player.broadcastUserInfo();
	}
	
	public static void removeVip(Player player)
	{
		// remove state in memory
		PlayerData.get(player).setVip(false);
		player.broadcastUserInfo();
	}
	
	public void getAllPlayerVips(Player player, int page)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.HTML_TYPE);
		
		hb.append("<html><body>");
		hb.append("<br>");
		hb.append(Html.headHtml("All VIP Players"));
		hb.append("<br>");
		
		hb.append("<table>");
		hb.append("<tr>");
		hb.append("<td width=64><font color=LEVEL>Player:</font></td><td width=200><font color=LEVEL>ExpireDate:</font></td>");
		hb.append("</tr>");
		hb.append("</table>");
		
		int MAX_PER_PAGE = 12;
		int searchPage = MAX_PER_PAGE * (page + 100);
		int count = 0;
		int countVip = 0;
		
		for (PlayerHolder ph : PlayerData.getAllPlayers())
		{
			if (ph.isVip())
			{
				countVip++;
				// min
				if (count < searchPage)
				{
					count++;
					continue;
				}
				// max
				if (count >= searchPage + MAX_PER_PAGE)
				{
					continue;
				}
				
				hb.append("<table", count % 2 == 0 ? " bgcolor=000000>" : ">");
				hb.append("<tr>");
				hb.append("<td width=64>" + ph.getName(), "</td><td width=200>" + ph.getVipExpireDateFormat(), "</td>");
				hb.append("</tr>");
				hb.append("</table>");
				count++;
			}
		}
		
		hb.append("<center>");
		hb.append("<img src=L2UI.SquareGray width=264 height=1>");
		hb.append("<table bgcolor=CC99FF>");
		hb.append("<tr>");
		
		int currentPage = 1;
		
		for (int i = 0; i < countVip; i++)
		{
			if (i % MAX_PER_PAGE == 0)
			{
				hb.append("<td width=18><center><a action=\"bypass -h Engine SystemVip allVip ", currentPage, "\">" + currentPage, "</center></a></td>");
				currentPage++;
			}
		}
		
		hb.append("</tr>");
		hb.append("</table>");
		hb.append("<img src=L2UI.SquareGray width=264 height=1>");
		hb.append("</center>");
		
		hb.append("</body></html>");
		sendHtml(player, null, hb);
	}
	
	private void readAllVips()
	{
		for (PlayerHolder ph : PlayerData.getAllPlayers())
		{
			String timeInMillis = getValueDB(ph.getObjectId(), "vip");
			// Don't has value in db
			if (timeInMillis == null)
			{
				continue;
			}
			
			long dayTime = Long.parseLong(timeInMillis);
			
			if (dayTime < System.currentTimeMillis())
			{
				continue;
			}
			
			// saved state in memory
			PlayerData.get(ph.getObjectId()).setVip(true);
			PlayerData.get(ph.getObjectId()).setVipExpireDate(dayTime);
		}
	}
	
	private static boolean checkTarget(Player ph)
	{
		if (ph.getTarget() == null)
		{
			ph.sendMessage("this command need target");
			return false;
		}
		
		if (!Util.areObjectType(Player.class, ph.getTarget()))
		{
			ph.sendMessage("this command need player target");
			return false;
		}
		
		return true;
	}
}
