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
import main.data.SkillData;
import main.engine.AbstractMods;
import main.holders.PlayerHolder;
import main.util.Util;
import main.util.builders.html.Html;
import main.util.builders.html.HtmlBuilder;
import main.util.builders.html.HtmlBuilder.HtmlType;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.MapRegionTable.TeleportType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.instance.Gatekeeper;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage.SMPOS;
import net.sf.l2j.gameserver.skills.Stats;

/**
 * @author fissban
 */
public class SystemAio extends AbstractMods
{
	public SystemAio()
	{
		registerMod(true);// TODO missing config
	}
	
	@Override
	public void onModState()
	{
		switch (getState())
		{
			case START:
				readAllAios();
				break;
			case END:
				//
				break;
		}
	}
	
	@Override
	public boolean onInteract(Player player, Creature npc)
	{
		if (PlayerData.get(player.getObjectId()).isAio())
		{
			if (!Util.areObjectType(Gatekeeper.class, npc))
			{
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public void onExitZone(Creature player, L2ZoneType zone)
	{
		if (!Util.areObjectType(Player.class, player))
		{
			return;
		}
		
		if (!PlayerData.get(player.getObjectId()).isAio())
		{
			return;
		}
		
		ThreadPool.schedule(() -> new CheckZone((Player) player), 3000);
		
	}
	
	private static class CheckZone implements Runnable
	{
		Player _player;
		
		public CheckZone(Player player)
		{
			_player = player;
		}
		
		@Override
		public void run()
		{
			if (!_player.isInsideZone(ZoneId.PEACE))
			{
				_player.teleToLocation(TeleportType.TOWN);
			}
		}
		
	}
	
	@Override
	public void onEvent(Player player, Creature npc, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		
		String event = st.nextToken();
		switch (event)
		{
			case "allAio":
			{
				if (player.getAccessLevel().getLevel() < 1)
				{
					break;
				}
				
				getAllPlayerAios(player, Integer.parseInt(st.nextToken()));
				break;
			}
			case "aioInfo":
			{
				informeExpireAio(player, Integer.parseInt(st.nextToken()));
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
			case "allaio":
			{
				getAllPlayerAios(player, 1);
				return true;
			}
			// only for admins
			// format: setAio days
			case "removeaio":
			{
				if (!checkTarget(player))
				{
					return true;
				}
				
				removeAio((Player) player.getTarget());
				return true;
			}
			case "setaio":
			{
				if (!checkTarget(player))
				{
					return true;
				}
				
				if (!st.hasMoreTokens())
				{
					player.sendMessage("Correct command:");
					player.sendMessage(".setAio days");
					return true;
				}
				
				String days = st.nextToken();
				
				if (!Util.isNumber(days))
				{
					player.sendMessage("Correct command:");
					player.sendMessage(".setAio days");
					return true;
				}
				
				Player aio = (Player) player.getTarget();
				
				// Create calendar
				Calendar time = new GregorianCalendar();
				time.add(Calendar.DAY_OF_YEAR, Integer.parseInt(days));
				// save values in DB
				setValueDB(aio, "aio", time.getTimeInMillis() + "");
				// saved state in memory
				PlayerData.get(aio).setAio(true);
				PlayerData.get(aio).setAioExpireDate(time.getTimeInMillis());
				
				addAio(aio, time.getTimeInMillis());
				
				// Informed admin
				player.sendPacket(new ExShowScreenMessage("player: " + aio.getName() + "is Aio now", 10000, SMPOS.TOP_CENTER, false));
				// Informed player
				aio.sendPacket(new ExShowScreenMessage("Dear " + aio.getName() + " your are now Aio", 10000, SMPOS.TOP_CENTER, false));
				informeExpireAio(aio, 1);
				
				// give duals
				ItemInstance item = ItemTable.getInstance().createItem("aio", ConfigData.AIO_ITEM_ID, 1, aio, aio);
				aio.addItem("aio", item, aio, true);
				
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public void onEnterWorld(Player player)
	{
		if (PlayerData.get(player).isAio())
		{
			if (PlayerData.get(player).getAioExpireDate() < System.currentTimeMillis())
			{
				removeAio(player);
				return;
			}
			
			addAio(player, PlayerData.get(player).getAioExpireDate());
			informeExpireAio(player, 1);
		}
	}
	
	@Override
	public double onStats(Stats stat, Creature character, double value)
	{
		if (!Util.areObjectType(Player.class, character))
		{
			return value;
		}
		
		if (!PlayerData.get(character.getObjectId()).isAio())
		{
			return value;
		}
		
		if (ConfigData.AIO_STATS.containsKey(stat))
		{
			return value *= ConfigData.AIO_STATS.get(stat);
		}
		
		return value;
	}
	
	/**
	 * Send the character html informing the time expire AIO. (format: dd-MMM-yyyy)
	 * @param player
	 * @param dayTime
	 */
	private static void informeExpireAio(Player player, int page)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.HTML_TYPE);
		
		hb.append("<html><body>");
		hb.append("<br>");
		hb.append(Html.headHtml("AIO"));
		hb.append("<br>");
		
		hb.append("<font color=9900CC>AIO Expire Date: </font>", PlayerData.get(player).getAioExpireDateFormat(), "<br>");
		hb.append("<font color=9900CC>The AIO have the skills:</font><br>");
		
		hb.append("<table>");
		int MAX_PER_PAGE = 12;
		int searchPage = MAX_PER_PAGE * (page - 1);
		int count = 0;
		for (IntIntHolder bh : ConfigData.AIO_LIST_SKILLS)
		{
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
			
			hb.append("<tr>");
			hb.append("<td width=32><img src=", SkillData.getSkillIcon(bh.getId()), " width=32 height=16></td>");
			hb.append("<td width=200><font color=LEVEL>", bh.getSkill().getName(), "</font></td>");
			hb.append("</tr>");
			count++;
		}
		hb.append("</table>");
		
		hb.append("<center>");
		hb.append("<img src=L2UI.SquareGray width=264 height=1>");
		hb.append("<table bgcolor=CC99FF>");
		hb.append("<tr>");
		
		int currentPage = 1;
		for (int i = 0; i < ConfigData.AIO_LIST_SKILLS.size(); i++)
		{
			if (i % MAX_PER_PAGE == 0)
			{
				hb.append("<td width=18 align=center><a action=\"bypass -h Engine SystemAio aioInfo ", currentPage, "\">" + currentPage, "</a></td>");
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
	
	public void addAio(Player player, long dayTime)
	{
		// Shedule to end AIO
		ThreadPool.schedule(() ->
		{
			// TODO missing onExitWorld
			if (player == null)
			{
				return;
			}
			
			informeExpireAio(player, 1);
			removeAio(player);
			
		}, dayTime - System.currentTimeMillis());
		// Set Max Lvl
		if (ConfigData.AIO_SET_MAX_LVL)
		{
			player.getStat().addExp(player.getStat().getExpForLevel(81));
		}
		// clear karma
		if (player.getKarma() > 0)
		{
			player.setKarma(0);
		}
		// teleport to city
		if (!player.isInsideZone(ZoneId.PEACE))
		{
			player.teleToLocation(TeleportType.TOWN);
		}
		
		// set custom tile
		player.setTitle(ConfigData.AIO_TITLE);
		// add skills for aio
		for (IntIntHolder bh : ConfigData.AIO_LIST_SKILLS)
		{
			player.addSkill(bh.getSkill(), false);
		}
		
		if (ConfigData.ALLOW_AIO_NCOLOR)
		{
			player.getAppearance().setNameColor(ConfigData.AIO_NCOLOR);
		}
		
		if (ConfigData.ALLOW_AIO_TCOLOR)
		{
			player.getAppearance().setTitleColor(ConfigData.AIO_TCOLOR);
		}
		
		player.broadcastUserInfo();
	}
	
	public void removeAio(Player player)
	{
		// remove state in memory
		PlayerData.get(player).setAio(false);
		// init title
		player.setTitle("");
		player.broadcastUserInfo();
	}
	
	public void getAllPlayerAios(Player player, int page)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.HTML_TYPE);
		
		hb.append("<html><body>");
		hb.append("<br>");
		hb.append(Html.headHtml("All AIO Players"));
		hb.append("<br>");
		
		hb.append("<table>");
		hb.append("<tr>");
		hb.append("<td width=64><font color=LEVEL>Player:</font></td><td width=200><font color=LEVEL>ExpireDate:</font></td>");
		hb.append("</tr>");
		hb.append("</table>");
		
		int MAX_PER_PAGE = 12;
		int searchPage = MAX_PER_PAGE * (page - 1);
		int count = 0;
		int countAio = 0;
		
		for (PlayerHolder ph : PlayerData.getAllPlayers())
		{
			if (ph.isAio())
			{
				countAio++;
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
				hb.append("<td width=64>", ph.getName(), "</td><td width=200>", ph.getAioExpireDateFormat(), "</td>");
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
		
		for (int i = 0; i < countAio; i++)
		{
			if (i % MAX_PER_PAGE == 0)
			{
				hb.append("<td width=18 align=center><a action=\"bypass -h Engine SystemAio allAio ", currentPage, "\">", currentPage, "</a></td>");
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
	
	private void readAllAios()
	{
		for (PlayerHolder ph : PlayerData.getAllPlayers())
		{
			String timeInMillis = getValueDB(ph.getObjectId(), "aio");
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
			PlayerData.get(ph.getObjectId()).setAio(true);
			PlayerData.get(ph.getObjectId()).setAioExpireDate(dayTime);
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
