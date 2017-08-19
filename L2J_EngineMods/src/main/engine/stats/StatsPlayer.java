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
package main.engine.stats;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import main.engine.AbstractMods;
import main.util.Util;
import main.util.builders.html.Html;
import main.util.builders.html.HtmlBuilder;
import main.util.builders.html.HtmlBuilder.HtmlType;
import main.util.builders.html.L2UI;
import main.util.builders.html.L2UI_CH3;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.skills.Stats;

/**
 * @author fissban
 */
public class StatsPlayer extends AbstractMods
{
	private enum BonusType
	{
		NORMAL,
		HERO,
		NOBLE,
		OLY
	}
	
	private class StatsHolder
	{
		private Map<BonusType, LinkedHashMap<Stats, Integer>> _stats = new LinkedHashMap<>();
		
		public StatsHolder()
		{
			initBonus();
		}
		
		private void initBonus()
		{
			// inicializamos todos los stats
			for (BonusType bt : BonusType.values())
			{
				for (Stats sts : Stats.values())
				{
					if (!_stats.containsKey(bt))
					{
						_stats.put(bt, new LinkedHashMap<>());
					}
					
					_stats.get(bt).put(sts, 1);
				}
			}
		}
		
		public void setBonus(BonusType type, Stats stat, int bonus)
		{
			_stats.get(type).put(stat, bonus);
		}
		
		public int getBonus(BonusType type, Stats stat)
		{
			return _stats.get(type).get(stat);
		}
		
		public LinkedHashMap<Stats, Integer> getAllBonus(BonusType type)
		{
			return _stats.get(type);
		}
		
		public void increaseBonus(BonusType type, Stats stat)
		{
			int oldBonus = _stats.get(type).get(stat);
			_stats.get(type).put(stat, oldBonus + 1);
		}
		
		public void decreaseBonus(BonusType type, Stats stat)
		{
			int oldBonus = _stats.get(type).get(stat);
			_stats.get(type).put(stat, oldBonus - 1);
		}
		
	}
	
	private static final Map<ClassId, StatsHolder> _classStats = new HashMap<>();
	
	public StatsPlayer()
	{
		registerMod(true);
		
		initStats();
	}
	
	@Override
	public void onModState()
	{
		//
	}
	
	private void initStats()
	{
		for (ClassId cs : ClassId.values())
		{
			// solo los de 3ra clase vamos a balancear
			if (cs.level() < 3)
			{
				continue;
			}
			
			_classStats.put(cs, new StatsHolder());
			
			for (BonusType bt : BonusType.values())
			{
				// en lugar de usar el objectId usaremos el id de la clase para almacenar en la DB.
				String values = getValueDB(cs.ordinal(), bt.name());
				
				if (values != null)
				{
					for (String split : values.split(";"))
					{
						String[] parse = split.split(",");
						
						Stats stat = Stats.valueOf(parse[0]);
						int bonus = Integer.parseInt(parse[1]);
						
						_classStats.get(cs).setBonus(bt, stat, bonus);
					}
				}
			}
			
		}
	}
	
	@Override
	public boolean onAdminCommand(Player player, String chat)
	{
		if (chat.equals("balance"))
		{
			htmlIndexClass(player);
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean onCommunityBoard(Player player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, ",");
		// _bbshome
		st.nextToken();
		if (!st.hasMoreTokens())
		{
			return false;
		}
		
		String event = st.nextToken();
		if (event.equals("balance"))
		{
			htmlIndexClass(player);
			return true;
		}
		if (event.equals("class"))
		{
			ClassId classId = ClassId.valueOf(st.nextToken());
			BonusType bonusType = st.hasMoreTokens() ? BonusType.valueOf(st.nextToken()) : BonusType.NORMAL;
			int page = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 1;
			
			htmlIndex(player, classId, bonusType, page);
			return true;
		}
		if (event.equals("modified"))
		{
			ClassId classId = ClassId.valueOf(st.nextToken());
			BonusType bonusType = BonusType.valueOf(st.nextToken());
			Stats stat = Stats.valueOf(st.nextToken());
			String type = st.nextToken(); // add - sub
			
			// page
			int page = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 1;
			
			switch (type)
			{
				case "add":
					_classStats.get(classId).increaseBonus(bonusType, stat);
					break;
				case "sub":
					_classStats.get(classId).decreaseBonus(bonusType, stat);
					break;
			}
			
			String parse = "";
			for (Entry<Stats, Integer> map : _classStats.get(classId).getAllBonus(bonusType).entrySet())
			{
				parse += map.getKey().name() + "," + map.getValue() + ";";
			}
			
			setValueDB(classId.ordinal(), bonusType.name(), parse);
			
			htmlIndex(player, classId, bonusType, page);
			return true;
		}
		return false;
	}
	
	private static void htmlIndexClass(Player player)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.COMUNITY_TYPE);
		hb.append(Html.HTML_START);
		hb.append("<br>");
		hb.append("<center>");
		
		hb.append("Selecciona la clase a la que quieres ajustar su balance<br>");
		hb.append("<br>");
		
		hb.append(Html.newFontColor("LEVEL", "HUMAN"));
		hb.append("<table bgcolor=000000>");
		hb.append("<tr>");
		hb.append(buttonClassId(ClassId.DUELIST));
		hb.append(buttonClassId(ClassId.DREADNOUGHT));
		hb.append(buttonClassId(ClassId.PHOENIX_KNIGHT));
		hb.append(buttonClassId(ClassId.HELL_KNIGHT));
		hb.append(buttonClassId(ClassId.SAGGITARIUS));
		hb.append(buttonClassId(ClassId.ADVENTURER));
		hb.append("</tr>");
		hb.append("<tr>");
		hb.append(buttonClassId(ClassId.ARCHMAGE));
		hb.append(buttonClassId(ClassId.SOULTAKER));
		hb.append(buttonClassId(ClassId.ARCANA_LORD));
		hb.append(buttonClassId(ClassId.CARDINAL));
		hb.append(buttonClassId(ClassId.HIEROPHANT));
		hb.append("<td></td>");
		hb.append("</tr>");
		hb.append("</table>");
		// --------------------------------------------------------------------------------
		hb.append(Html.newFontColor("LEVEL", "ELF"));
		hb.append("<table bgcolor=000000>");
		hb.append("<tr>");
		hb.append(buttonClassId(ClassId.EVAS_TEMPLAR));
		hb.append(buttonClassId(ClassId.SWORD_MUSE));
		hb.append(buttonClassId(ClassId.WIND_RIDER));
		hb.append(buttonClassId(ClassId.MOONLIGHT_SENTINEL));
		hb.append(buttonClassId(ClassId.MYSTIC_MUSE));
		hb.append(buttonClassId(ClassId.ELEMENTAL_MASTER));
		hb.append("</tr>");
		hb.append("<tr>");
		hb.append(buttonClassId(ClassId.EVAS_SAINT));
		hb.append("<td></td>");
		hb.append("<td></td>");
		hb.append("<td></td>");
		hb.append("<td></td>");
		hb.append("</tr>");
		hb.append("</table>");
		// --------------------------------------------------------------------------------
		hb.append(Html.newFontColor("LEVEL", "DARK ELF"));
		hb.append("<table bgcolor=000000>");
		hb.append("<tr>");
		hb.append(buttonClassId(ClassId.SHILLIEN_TEMPLAR));
		hb.append(buttonClassId(ClassId.SPECTRAL_DANCER));
		hb.append(buttonClassId(ClassId.GHOST_HUNTER));
		hb.append(buttonClassId(ClassId.GHOST_SENTINEL));
		hb.append(buttonClassId(ClassId.STORM_SCREAMER));
		hb.append(buttonClassId(ClassId.SPECTRAL_MASTER));
		hb.append("</tr>");
		hb.append("<tr>");
		hb.append(buttonClassId(ClassId.SHILLIEN_SAINT));
		hb.append("<td></td>");
		hb.append("<td></td>");
		hb.append("<td></td>");
		hb.append("<td></td>");
		hb.append("</tr>");
		hb.append("</table>");
		// --------------------------------------------------------------------------------
		hb.append(Html.newFontColor("LEVEL", "ORC"));
		hb.append("<table bgcolor=000000>");
		hb.append("<tr>");
		hb.append(buttonClassId(ClassId.TITAN));
		hb.append(buttonClassId(ClassId.GRAND_KHAVATARI));
		hb.append(buttonClassId(ClassId.DOMINATOR));
		hb.append(buttonClassId(ClassId.DOOMCRYER));
		hb.append("</tr>");
		hb.append("</table>");
		// --------------------------------------------------------------------------------
		hb.append(Html.newFontColor("LEVEL", "DWARF"));
		hb.append("<table bgcolor=000000>");
		hb.append("<tr>");
		hb.append(buttonClassId(ClassId.FORTUNE_SEEKER));
		hb.append(buttonClassId(ClassId.MAESTRO));
		hb.append("</tr>");
		hb.append("</table>");
		
		hb.append("<br>");
		hb.append("</center>");
		hb.append(Html.HTML_END);
		sendCommunity(player, hb.toString());
	}
	
	private static String buttonClassId(ClassId classId)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.HTML_TYPE);
		hb.append("<td><button value=", classId.toString().replace("_", " ").toLowerCase(), " action=\"bypass _bbshome,class,", classId.name(), "\" width=93 height=22 back=", L2UI_CH3.bigbutton_down, " fore=", L2UI_CH3.bigbutton, "></td>");
		return hb.toString();
	}
	
	private static void htmlIndex(Player player, ClassId classId, BonusType bonusType, int page)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.COMUNITY_TYPE);
		hb.append(Html.HTML_START);
		hb.append("<br>");
		hb.append("<center>");
		
		hb.append("<button value=INDEX action=\"bypass _bbshome,balance\" width=93 height=22 back=", L2UI_CH3.bigbutton_down, " fore=", L2UI_CH3.bigbutton, ">");
		hb.append("<br>");
		hb.append(Html.htmlHeadCommunity(classId.name()));
		hb.append("<br>");
		hb.append("<table width=460 height=22>");
		hb.append("<tr>");
		for (BonusType bt : BonusType.values())
		{
			hb.append("<td><button value=", bt.name(), " action=\"bypass _bbshome,class,", classId.name(), ",", bt.name(), "\" width=93 height=22 back=", L2UI_CH3.bigbutton_down, " fore=", L2UI_CH3.bigbutton, "></td>");
		}
		hb.append("</tr>");
		hb.append("</table>");
		
		hb.append("<br>");
		int MAX_PER_PAGE = 13;
		int searchPage = MAX_PER_PAGE * (page - 1);
		int count = 0;
		int color = 0;
		
		for (Stats stat : Stats.values())
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
			
			double value = _classStats.get(classId).getBonus(bonusType, stat);
			hb.append("<table width=460 height=22 ", color % 2 == 0 ? "bgcolor=000000 " : "", "cellspacing=0 cellpadding=0>");
			hb.append("<tr>");
			hb.append("<td fixwidth=16 height=22 align=center>", Html.newImage(L2UI_CH3.ps_sizecontrol2_over, 16, 16), "</td>");
			hb.append("<td width=100 height=22 align=center>", Html.newFontColor("LEVEL", stat.toString().replace("_", " ").toLowerCase()), " </td>");
			hb.append("<td width=62 align=center>", value, "%</td>");
			hb.append("<td width=32><button action=\"bypass _bbshome,modified,", classId.name(), ",", bonusType.name(), ",", stat, ",add\" width=16 height=16 back=sek.cbui343 fore=sek.cbui343></td>");
			hb.append("<td width=32><button action=\"bypass _bbshome,modified,", classId.name(), ",", bonusType.name(), ",", stat, ",sub\" width=16 height=16 back=sek.cbui347 fore=sek.cbui347></td>");
			hb.append("</tr>");
			hb.append("</table>");
			hb.append(Html.newImage(L2UI.SquareGray, 460, 1));
			
			color++;
			count++;
		}
		
		int currentPage = 1;
		int size = Stats.values().length;
		
		hb.append("<br>");
		hb.append("<table>");
		hb.append("<tr>");
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
					hb.append("<td width=20><a action=\"bypass _bbshome,class,", classId.name(), ",", bonusType.name(), ",", currentPage, "\">", currentPage, "</a></td>");
				}
				
				currentPage++;
			}
		}
		hb.append("</tr>");
		hb.append("</table>");
		
		hb.append("</center>");
		hb.append(Html.HTML_END);
		sendCommunity(player, hb.toString());
	}
	
	@Override
	public double onStats(Stats stat, Creature character, double value)
	{
		if (!Util.areObjectType(Playable.class, character))
		{
			return value;
		}
		
		Player player = character.getActingPlayer();
		
		if (!_classStats.containsKey(player.getClassId()))
		{
			return value;
		}
		
		BonusType bonusType = BonusType.NORMAL;
		
		if (player.isInOlympiadMode())
		{
			bonusType = BonusType.OLY;
		}
		if (player.isNoble())
		{
			bonusType = BonusType.NOBLE;
		}
		if (player.isHero())
		{
			bonusType = BonusType.HERO;
		}
		
		return value * (_classStats.get(player.getClassId()).getBonus(bonusType, stat) / 10.0 + 1.0);
	}
}
