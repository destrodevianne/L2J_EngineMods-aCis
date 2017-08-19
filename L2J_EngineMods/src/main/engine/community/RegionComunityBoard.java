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
package main.engine.community;

import java.util.StringTokenizer;

import main.data.ConfigData;
import main.data.PlayerData;
import main.engine.AbstractMods;
import main.util.builders.html.Html;
import main.util.builders.html.HtmlBuilder;
import main.util.builders.html.HtmlBuilder.HtmlType;
import net.sf.l2j.gameserver.data.MapRegionTable;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.base.Sex;

/**
 * @author fissban
 */
public class RegionComunityBoard extends AbstractMods
{
	public RegionComunityBoard()
	{
		registerMod(ConfigData.ENABLE_BBS_REGION);
	}
	
	@Override
	public void onModState()
	{
		//
	}
	
	@Override
	public boolean onCommunityBoard(Player player, String command)
	{
		if (command.startsWith("_bbsloc"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			// _bbsloc
			st.nextToken();
			// page
			int page = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 1;
			
			HtmlBuilder hb = new HtmlBuilder(HtmlType.COMUNITY_TYPE);
			hb.append(Html.HTML_START);
			hb.append("<br>");
			// head
			hb.append(Html.htmlHeadCommunity("TOTAL ONLINE: " + World.getInstance().getPlayers().size()));
			hb.append("<br>");
			// body
			hb.append("<center>");
			hb.append("<table border=0 cellspacing=0 cellpadding=0>");
			hb.append("<tr>");
			hb.append("<td width=16 valign=top align=center height=22>", Html.newImage("L2UI_CH3.FrameBackLeft", 16, 22), "</td>");
			// hb.append(topMenuList("", 16));
			hb.append(topMenuList("Name", 100));
			hb.append(topMenuList("Lvl", 30));
			
			hb.append(topMenuList("aio", 30));
			hb.append(topMenuList("vip", 30));
			
			hb.append(topMenuList("Class", 100));
			hb.append(topMenuList("Clan (Lvl.)", 100));
			hb.append(topMenuList("Town Region", 84));
			hb.append("<td width=16 valign=top align=center height=22>", Html.newImage("L2UI_CH3.FrameBackRight", 16, 22), "</td>");
			
			hb.append("</tr>");
			hb.append("</table>");
			
			// 582 MAX! de largo del html
			int MAX_PER_PAGE = 15;
			int searchPage = MAX_PER_PAGE * (page - 1);
			int count = 0;
			
			for (Player pc : World.getInstance().getPlayers())
			{
				if (pc == null)
				{
					continue;
				}
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
				
				hb.append("<table height=22 border=0 cellspacing=0 cellpadding=0>");
				hb.append("<tr>");
				hb.append("<td fixwidth=16 height=22 align=center>", getIconSex(pc.getAppearance().getSex()), "</td>");
				hb.append("<td fixwidth=100 align=center>", pc.getName(), pc.getName().equals("fissban") ? Html.newFontColor("FF0000", "   ADMIN") : pc.getAccessLevel().getLevel() > 0 ? Html.newFontColor("LEVEL", "   GM") : "", "</td>");
				hb.append("<td fixwidth=30 align=center>", getColorLevel(pc.getLevel()), "</td>");
				hb.append("<td fixwidth=30 align=center>", getIconStatus(PlayerData.get(pc).isAio()), "</td>");
				hb.append("<td fixwidth=30 align=center> ", getIconStatus(PlayerData.get(pc).isVip()), "</td>");
				hb.append("<td fixwidth=100 align=center> ", pc.getClassId().toString(), "</td>");
				hb.append("<td fixwidth=100 align=center> ", pc.getClan() != null ? pc.getClan().getName() + Html.newFontColor("LEVEL", "  (" + pc.getClan().getLevel() + ")") : "No Clan", "</td>");
				hb.append("<td fixwidth=100 align=center> ", MapRegionTable.getInstance().getClosestTownName(pc.getX(), pc.getY()), "</td>");
				hb.append("</tr>");
				hb.append("</table>");
				hb.append(Html.newImage("L2UI.SquareGray", 506, 1));
				count++;
			}
			
			hb.append("<br>");
			hb.append("<table border=0 cellspacing=0 cellpadding=0>");
			hb.append("<tr>");
			
			int currentPage = 1;
			int size = World.getInstance().getPlayers().size();
			for (int i = 0; i < size; i++)
			{
				if (i % MAX_PER_PAGE == 0)
				{
					if (currentPage == page)
					{
						hb.append("<td width=20>", Html.newFontColor("LEVEL", currentPage), "</td>");
					}
					else
					{
						hb.append("<td width=20><a action=\"bypass _bbsloc ", currentPage, "\">", currentPage, "</a></td>");
					}
					currentPage++;
				}
			}
			hb.append("</tr>");
			hb.append("</table>");
			
			hb.append("</center>");
			hb.append(Html.HTML_END);
			sendCommunity(player, hb.toString());
			
			return true;
		}
		return false;
		
	}
	
	// XXX MISC ----------------------------------------------------------------------------------------------------------------------
	
	private static String getIconStatus(boolean status)
	{
		if (status)
		{
			return Html.newImage("L2UI_CH3.QuestWndInfoIcon_5", 16, 16);
		}
		return "X";
	}
	
	/**
	 * 
	 */
	private static String getIconSex(Sex sex)
	{
		return sex == Sex.MALE ? Html.newImage("L2UI_CH3.msnicon1", 16, 16) : Html.newImage("L2UI_CH3.msnicon4", 16, 16);
	}
	
	/**
	 * Asignamos un color segun el lvl del personaje
	 * @param lvl
	 * @return
	 */
	private static String getColorLevel(int lvl)
	{
		HtmlBuilder hb = new HtmlBuilder();
		
		if (lvl >= 20 && lvl < 40)
		{
			hb.append(Html.newFontColor("LEVEL", lvl)); // amarillo
		}
		else if (lvl >= 40 && lvl < 76)
		{
			hb.append(Html.newFontColor("9A5C00", lvl)); // naranja oscuro
		}
		else if (lvl >= 76)
		{
			hb.append(Html.newFontColor("FF0000", lvl));// rojo
		}
		else
		{
			hb.append(lvl);
		}
		
		return hb.toString();
	}
	
	private static String topMenuList(String text, int widthMid)
	{
		return "<td fixwidth=" + widthMid + " align=center><button value=\"" + text + "\" width=" + widthMid + " height=21 back=L2UI_CH3.FrameBackMid fore=L2UI_CH3.FrameBackMid></td>";
	}
}
