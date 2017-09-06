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

import java.util.StringTokenizer;

import main.data.PlayerData;
import main.data.SkillData;
import main.engine.AbstractMods;
import main.enums.TeamType;
import main.holders.PlayerHolder;
import main.packets.PrivateCustomTitle;
import main.packets.PrivateCustomTitle.TitleType;
import main.util.builders.html.Html;
import main.util.builders.html.HtmlBuilder;
import main.util.builders.html.HtmlBuilder.HtmlType;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

/**
 * @author fissban
 */
public class SellBuffs extends AbstractMods
{
	public SellBuffs()
	{
		registerMod(true);// TODO missing config
	}
	
	@Override
	public void onModState()
	{
		//
	}
	
	@Override
	public boolean onVoicedCommand(Player player, String chat)
	{
		if (chat.startsWith("cancelsellbuff")) // cancel sellbuff state
		{
			if (!PlayerData.get(player).isSellBuff())
			{
				return true;
			}
			
			PlayerData.get(player).setSellBuff(false);
			PlayerData.get(player).setSellBuffPrice(0);
			
			player.standUp();
			player.setIsImmobilized(false);
			player.setTeam(TeamType.NONE.ordinal());
			player.broadcastUserInfo();
			return true;
		}
		if (chat.startsWith("sellbuff")) // init sellbuff html
		{
			if (!player.isInsideZone(ZoneId.PEACE))
			{
				return true;
			}
			
			HtmlBuilder hb = new HtmlBuilder(HtmlType.HTML_TYPE);
			
			hb.append("<html><body><center>");
			hb.append(Html.headHtml("SELL BUFF"));
			hb.append("<font color=\"LEVEL\">Welcome </font><font color=\"00C3FF\">", player.getName(), "</font> system selling buffs.<br1>");
			hb.append("You can only sell buffs if your class<br1>");
			hb.append("is the type of support.<br1>");
			hb.append("All your buffs will be sold at a single price.<br1>");
			hb.append("To cancel this state should use<br1>");
			hb.append("the command. <font color=\"LEVEL\">CancelSellBuffs</font>");
			hb.append("<br><br>");
			hb.append("<font color=\"00C3FF\">Choose the price of your services</font>");
			hb.append("<br>");
			hb.append("<edit var=\"price\" width=\"200\" height=\"15\">");
			hb.append("<br>");
			hb.append("<button value=\"Next\" action=\"bypass -h Engine SellBuffs sell $price\" width=\"80\" height=\"25\" back=\"L2UI_CH3.Btn1_normalOn\" fore=\"L2UI_CH3.btn1_normal\">");
			hb.append("</center></body></html>");
			sendHtml(player, null, hb);
			return true;
		}
		
		return false;
	}
	
	@Override
	public void onEvent(Player player, Creature character, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		switch (st.nextToken())
		{
			case "sendPacket":
			{
				if (!PlayerData.get(player).isSellBuff())
				{
					break;
				}
				
				character.sendPacket(new PrivateCustomTitle(player, TitleType.SELL, "SellBuffs"));
				
				break;
			}
			case "view":// see list of buffs for sale
			{
				int page = 1;
				if (st.hasMoreTokens())
				{
					page = Integer.parseInt(st.nextToken());
				}
				getBuffList(player, player.getTarget(), page);
				break;
			}
			case "sell": // sit to sell buffs
			{
				int price = 0;
				try
				{
					price = Integer.parseInt(st.nextToken());
				}
				catch (Exception e)
				{
					//
				}
				
				PlayerData.get(player).setSellBuff(true);
				PlayerData.get(player).setSellBuffPrice(price);
				
				player.sitDown();
				player.setIsImmobilized(true);
				player.setTeam(TeamType.BLUE.ordinal());
				player.broadcastUserInfo();
				// TODO Hay que variar la posicion del doInteract en core de aCis y aqui poner modo de venta en Sell para que aparezca
				player.broadcastPacket(new PrivateCustomTitle(player, TitleType.SELL, "SellBuffs"));
				break;
			}
			case "buy":// buy buff
			{
				int id = Integer.parseInt(st.nextToken());
				int lvl = Integer.parseInt(st.nextToken());
				String sellerName = st.nextToken();
				
				Player sellerBuff = World.getInstance().getPlayer(sellerName);
				
				if (sellerBuff == null)
				{
					return;
				}
				
				if (!player.isInsideRadius(sellerBuff, 500, false, false))
				{
					return;
				}
				
				PlayerHolder ph = PlayerData.get(sellerBuff.getObjectId());
				
				if (ph == null || !ph.isSellBuff())
				{
					return;
				}
				
				// Check if target get skill
				if (sellerBuff.getSkillLevel(id) != lvl)
				{
					return;
				}
				
				int price = ph.getSellBuffPrice();
				
				if (!player.reduceAdena("sell buff", price, sellerBuff, true))
				{
					return;
				}
				
				sellerBuff.addAdena("sell buff", price, player, true);
				
				sellerBuff.standUp();
				sellerBuff.setTarget(sellerBuff);
				L2Skill skill = SkillTable.getInstance().getInfo(id, lvl);
				sellerBuff.doCast(skill);
				skill.getEffects(player, player);
				sellerBuff.setCurrentMp(sellerBuff.getMaxMp());
				
				ThreadPool.schedule(() ->
				{
					sellerBuff.setIsImmobilized(false);
					sellerBuff.sitDown(false);
					sellerBuff.setIsImmobilized(true);
				}, Formulas.calcAtkSpd(sellerBuff, skill, skill.getHitTime()) + 100);
				
				int page = 1;
				if (st.hasMoreTokens())
				{
					page = Integer.parseInt(st.nextToken());
				}
				
				getBuffList(player, sellerBuff, page);
				break;
			}
		}
	}
	
	@Override
	public boolean onInteract(Player player, Creature sellerBuff)
	{
		PlayerHolder ph = PlayerData.get(sellerBuff.getObjectId());
		
		if (ph == null || !ph.isSellBuff())
		{
			return false;
		}
		
		if (!player.isInsideRadius(sellerBuff, 500, false, false))
		{
			return false;
		}
		
		HtmlBuilder tb = new HtmlBuilder();
		
		tb.append("<html><body>");
		tb.append(Html.headHtml("SELL BUFF"));
		tb.append("<center>");
		tb.append("<br><br>");
		tb.append("Hello <font color=\"00C3FF\">", player.getName(), "</font>");
		tb.append("<br><center>My Buff Cost: <font color=\"LEVEL\">", ph.getSellBuffPrice(), "</font> adena each!</center>");
		tb.append("<br>");
		tb.append("<center><button value=\"View my Buffs\" action=\"bypass -h Engine SellBuffs view\" width=\"80\" height=\"25\" back=\"L2UI_CH3.Btn1_normalOn\" fore=\"L2UI_CH3.btn1_normal\">");
		tb.append("</center>");
		tb.append("</body></html>");
		
		sendHtml(player, null, tb);
		// Enviamos este packet para evitar que el personaje quede bugueado sin poder moverse
		player.sendPacket(ActionFailed.STATIC_PACKET);
		return true;
	}
	
	// HTML's ----------------------------------------------------------------------------------------------------------- //
	
	private void getBuffList(Player buyer, WorldObject sellerBuff, int page)
	{
		PlayerHolder ph = PlayerData.get(sellerBuff.getObjectId());
		
		if (ph == null || !ph.isSellBuff())
		{
			return;
		}
		
		HtmlBuilder tb = new HtmlBuilder();
		
		tb.append("<html><body>");
		tb.append("<br><br>");
		tb.append("<center>");
		tb.append("<font color=\"LEVEL\">Hello </font><font color=\"00C3FF\">", buyer.getName(), "</font><font color=\"LEVEL\"> want my Buff!</font>");
		tb.append("<br>My Buff Cost: <font color=\"00C3FF\">", ph.getSellBuffPrice(), "</font><font color=\"LEVEL\"> adena each!</font><br>");
		
		int MAX_SKILL_PER_PAGE = 12;
		int searchPage = MAX_SKILL_PER_PAGE * (page - 1);
		int skillCount = 0;
		
		int skillBuffs = 0;
		for (L2Skill sk : ((Creature) sellerBuff).getSkills().values())
		{
			if (sk.isPassive() || sk.getTargetType() == SkillTargetType.TARGET_SELF || sk.isOffensive() || sk.getSkillType() != L2SkillType.BUFF)
			{
				continue;
			}
			
			skillBuffs++;
			
			// min
			if (skillCount < searchPage)
			{
				skillCount++;
				continue;
			}
			// max
			if (skillCount >= searchPage + MAX_SKILL_PER_PAGE)
			{
				continue;
			}
			
			tb.append("<table>");
			tb.append("<tr>");
			tb.append("<td width=\"32\"><center><img src=\"", SkillData.getSkillIcon(sk.getId()), "\" width=\"32\" height=\"16\"></center></td>");
			tb.append("<td width=\"180\"><center><a action=\"bypass -h Engine SellBuffs buy ", sk.getId(), " ", sk.getLevel(), " ", sellerBuff.getName(), " ", page, "\">", sk.getName(), "</center></td>");
			tb.append("<td width=\"32\"><center><img src=\"", SkillData.getSkillIcon(sk.getId()), "\" width=\"32\" height=\"16\"></center></td>");
			tb.append("</tr>");
			tb.append("</table>");
			skillCount++;
		}
		
		tb.append("<center>");
		tb.append("<img src=\"L2UI.SquareGray\" width=\"264\" height=\"1\">");
		tb.append("<table bgcolor=CC99FF>");
		tb.append("<tr>");
		
		int currentPage = 1;
		
		for (int i = 0; i < skillBuffs; i++)
		{
			if (i % MAX_SKILL_PER_PAGE == 0)
			{
				tb.append("<td width=\"18\"><center><a action=\"bypass -h Engine SellBuffs view " + currentPage + "\">" + currentPage + "</center></a></td>");
				currentPage++;
			}
		}
		
		tb.append("</tr>");
		tb.append("</table>");
		tb.append("<img src=\"L2UI.SquareGray\" width=\"264\" height=\"1\">");
		tb.append("</center>");
		
		tb.append("</body></html>");
		
		sendHtml(buyer, null, tb);
	}
}