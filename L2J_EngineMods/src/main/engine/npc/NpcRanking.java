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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import main.engine.AbstractMods;
import main.util.Util;
import main.util.builders.html.Html;
import main.util.builders.html.HtmlBuilder;
import main.util.builders.html.HtmlBuilder.HtmlType;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.instance.Player;

/**
 * @author fissban
 */
public class NpcRanking extends AbstractMods
{
	public class RankingHolder
	{
		String name;
		int kills;
	}
	
	private static final int NPC = 60008;
	// SQL
	private static final String SQL_PVP = "SELECT char_name,pvpkills FROM characters WHERE pvpkills>0 AND accesslevel=0 ORDER BY pvpkills DESC LIMIT 20";
	private static final String SQL_PK = "SELECT char_name,pkkills FROM characters WHERE pkkills>0 AND accesslevel=0 ORDER BY pkkills DESC LIMIT 20";
	// Rank
	public static final List<RankingHolder> _rankingPvP = new ArrayList<>();
	public static final List<RankingHolder> _rankingPk = new ArrayList<>();
	
	public NpcRanking()
	{
		registerMod(true);// TODO missing config
	}
	
	@Override
	public void onModState()
	{
		switch (getState())
		{
			case START:
				// pvp
				startTimer("loadRankingPvP", 0, null, null, false); // 0secs :P
				startTimer("loadRankingPvP", 60000, null, null, true); // 1min
				// pk
				startTimer("loadRankingPk", 0, null, null, false); // 0secs :P
				startTimer("loadRankingPk", 60000, null, null, true); // 1min
				break;
			case END:
				_rankingPvP.clear();
				_rankingPk.clear();
				cancelTimers("loadRankingPvP");
				cancelTimers("loadRankingPk");
				break;
		}
	}
	
	@Override
	public void onTimer(String timerName, Npc npc, Player player)
	{
		switch (timerName)
		{
			case "loadRankingPvP":
			{
				_rankingPvP.clear();
				
				try (Connection con = L2DatabaseFactory.getInstance().getConnection();
					PreparedStatement statement = con.prepareStatement(SQL_PVP);
					ResultSet rset = statement.executeQuery())
				{
					while (rset.next())
					{
						RankingHolder rh = new RankingHolder();
						rh.name = rset.getString("char_name");
						rh.kills = rset.getInt("pvpkills");
						
						_rankingPvP.add(rh);
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				break;
			}
			case "loadRankingPk":
			{
				_rankingPk.clear();
				
				try (Connection con = L2DatabaseFactory.getInstance().getConnection();
					PreparedStatement statement = con.prepareStatement(SQL_PK);
					ResultSet rset = statement.executeQuery())
				{
					while (rset.next())
					{
						RankingHolder rh = new RankingHolder();
						rh.name = rset.getString("char_name");
						rh.kills = rset.getInt("pkkills");
						
						_rankingPk.add(rh);
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				break;
			}
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
		
		HtmlBuilder hb = new HtmlBuilder(HtmlType.HTML_TYPE);
		hb.append(Html.HTML_START);
		hb.append(Html.headHtml("RANKING"));
		hb.append("<br>");
		hb.append("Welcome my name is ", npc.getName(), " and take care to meet the most famous players in the world.<br>");
		hb.append("You probably want to know who it is!<br>");
		hb.append("I actually have a list, I can show it to you if you want.<br>");
		hb.append("What would you like to see?<br>");
		hb.append("<center>");
		hb.append("<table width=280>");
		hb.append("<tr>");
		hb.append("<td>", Html.newImage("L2UI.bbs_folder", 32, 32), "</td>");
		hb.append("<td><button value=\"Top PvP\" action=\"bypass -h Engine NpcRanking pvp\" width=216 height=32 back=L2UI_CH3.refinegrade3_21 fore=L2UI_CH3.refinegrade3_21></td>");
		hb.append("<td>", Html.newImage("L2UI.bbs_folder", 32, 32), "</td>");
		hb.append("</tr>");
		hb.append("<tr>");
		hb.append("<td>", Html.newImage("L2UI.bbs_folder", 32, 32), "</td>");
		hb.append("<td><button value=\"Top PK\" action=\"bypass -h Engine NpcRanking pk\" width=216 height=32 back=L2UI_CH3.refinegrade3_21 fore=L2UI_CH3.refinegrade3_21></td>");
		hb.append("<td>", Html.newImage("L2UI.bbs_folder", 32, 32), "</td>");
		hb.append("</tr>");
		hb.append("</table>");
		hb.append("</center>");
		hb.append(Html.HTML_END);
		
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
		
		switch (command)
		{
			case "pvp":
				sendHtml(player, null, getRanking(_rankingPvP, "PVP"));
				break;
			
			case "pk":
				sendHtml(player, null, getRanking(_rankingPk, "PK"));
				break;
		}
	}
	
	private static HtmlBuilder getRanking(List<RankingHolder> ranking, String rankingName)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.HTML_TYPE);
		hb.append("<html><body>");
		hb.append(Html.headHtml("RANKING " + rankingName));
		hb.append("<br>");
		hb.append("<table width=280>");
		hb.append("<tr>");
		hb.append("<td fixwidth=40><button value=\"Pos\" action=\"\" width=40 height=19 back=L2UI_CH3.amountbox2 fore=L2UI_CH3.amountbox2></td>");
		hb.append("<td fixwidth=120><button value=\"Player\" action=\"\" width=120 height=19 back=L2UI_CH3.amountbox2 fore=L2UI_CH3.amountbox2></td>");
		hb.append("<td fixwidth=120><button value=\"Kills\" action=\"\" width=120 height=19 back=L2UI_CH3.amountbox2 fore=L2UI_CH3.amountbox2></td>");
		hb.append("</tr>");
		hb.append("</table>");
		
		int pos = 1;
		for (RankingHolder rh : ranking)
		{
			hb.append(Html.newImage("L2UI.SquareGray", 280, 1));
			hb.append("<table width=280>");
			hb.append("<tr>");
			hb.append("<td fixwidth=40><center><font color=F7D358>" + pos + "</font></td>");
			hb.append("<td fixwidth=120><center> ", rh.name, " </center></td>");
			hb.append("<td fixwidth=120><center> ", rh.kills, " </center></td>");
			hb.append("</tr>");
			hb.append("</table>");
			
			pos++;
		}
		
		hb.append(Html.newImage("L2UI.SquareGray", 280, 1));
		hb.append("</center>");
		hb.append("</body></html>");
		
		return hb;
	}
}
