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

import java.util.Arrays;
import java.util.List;

import main.data.ConfigData;
import main.data.IconData;
import main.data.PlayerData;
import main.engine.AbstractMods;
import main.enums.ItemIconType;
import main.util.Util;
import main.util.builders.html.Html;
import main.util.builders.html.HtmlBuilder;
import main.util.builders.html.HtmlBuilder.HtmlType;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.instance.Player.PunishLevel;
import net.sf.l2j.gameserver.skills.AbnormalEffect;

/**
 * AntiBot style system Google
 * @author fissban
 */
public class AntiBot extends AbstractMods
{
	public AntiBot()
	{
		registerMod(ConfigData.ENABLE_AntiBot);
	}
	
	@Override
	public void onModState()
	{
		switch (getState())
		{
			case START:
			{
				break;
			}
			case END:
			{
				cancelTimers("sendJail");
				break;
			}
		}
	}
	
	@Override
	public void onKill(Creature killer, Creature victim, boolean isPet)
	{
		if (!Util.areObjectType(Monster.class, victim) || killer.getActingPlayer() == null)
		{
			return;
		}
		
		Player activeChar = killer.getActingPlayer();
		
		if (PlayerData.get(activeChar).isFake())
		{
			return;
		}
		
		// increasing the amount of dead mobs
		PlayerData.get(activeChar).increaseKills();
		
		int count = ConfigData.KILLER_MONSTERS_ANTIBOT_INCREASE_LEVEL ? ConfigData.KILLER_MONSTERS_ANTIBOT + activeChar.getLevel() * 3 : ConfigData.KILLER_MONSTERS_ANTIBOT;
		
		if (PlayerData.get(activeChar).getKills() >= count)
		{
			PlayerData.get(activeChar).resetKills();
			PlayerData.get(activeChar).setAnswerRight("");
			PlayerData.get(activeChar).resetAttempts();
			
			// stop any action
			activeChar.abortAttack();
			activeChar.abortCast();
			activeChar.stopMove(null);
			// start abrnomal effect
			activeChar.startAbnormalEffect(AbnormalEffect.REDCIRCLE);
			// player is paralized
			activeChar.setIsParalyzed(true);
			// player is invulnerable
			activeChar.setIsInvul(true);
			// send html index
			generateHtmlIndex(activeChar);
			// the timer starts to send to jail the player does not respond.
			startTimer("sendJail", ConfigData.TIME_CHECK_ANTIBOT * 1000, null, activeChar, false);
		}
	}
	
	@Override
	public void onTimer(String timerName, Npc npc, Player player)
	{
		switch (timerName)
		{
			case "sendJail":
				if (PlayerData.get(player).getAttempts() <= 0)
				{
					sendPlayerJail(player);
				}
				else
				{
					// the timer starts to send to jail the player does not respond.
					startTimer("sendJail", ConfigData.TIME_CHECK_ANTIBOT * 1000, null, player, false);
					// decrease attempts
					PlayerData.get(player).decreaseAttempts();
					// send player html
					generateHtmlIndex(player);
				}
				break;
		}
	}
	
	@Override
	public void onEvent(Player player, Creature npc, String command)
	{
		// if the answer is correct
		if (PlayerData.get(player).isAnswerRight(command))
		{
			player.sendMessage("Correct Verification!");
			
			player.stopAbnormalEffect(AbnormalEffect.REDCIRCLE);
			// paralysis is removed when the player
			player.setIsParalyzed(false);
			// now player is mortal
			player.setIsInvul(false);
			// the timer is canceled to send him to jail
			cancelTimer("sendJail", null, player);
		}
		else
		{
			player.sendMessage("Incorrect verification!");
			
			// the timer is canceled to send him to jail
			cancelTimer("sendJail", null, player);
			// the number of failed attempts is checked
			if (PlayerData.get(player).getAttempts() <= 0)
			{
				sendPlayerJail(player);
			}
			else
			{
				// the timer is canceled to send him to jail
				cancelTimer("sendJail", null, player);
				// the timer starts to send to jail the player does not respond.
				startTimer("sendJail", ConfigData.TIME_CHECK_ANTIBOT * 1000, null, player, false);
				// decrease attemps
				PlayerData.get(player).decreaseAttempts();
				// send player html
				generateHtmlIndex(player);
			}
		}
	}
	
	@Override
	public boolean onExitWorld(Player player)
	{
		// si ya esta activo el antibot no dejamos que el player salga del juego evitando el control.
		if (getTimer("sendJail", player) != null)
		{
			return true;
		}
		return false;
	}
	
	private static synchronized void generateHtmlIndex(Player activeChar)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.HTML_TYPE);
		hb.append("<html><body><center>");
		hb.append("<br>");
		hb.append(Html.headHtml("ANTI BOT"));
		hb.append("<br>");
		hb.append("has ", PlayerData.get(activeChar).getAttempts(), " attemps!<br>");
		// For clarity of the images could only come out a rnd of these values
		List<Integer> aux = Arrays.asList(0, 1, 3, 4, 5);
		
		ItemIconType itemIconType1 = ItemIconType.values()[aux.get(Rnd.get(aux.size()))];
		// Get a different type
		ItemIconType itemIconType2 = ItemIconType.values()[aux.get(Rnd.get(aux.size()))];
		
		// Ensure that both random are different
		while (itemIconType1 == itemIconType2)
		{
			itemIconType2 = ItemIconType.values()[aux.get(Rnd.get(aux.size()))];
		}
		
		// Inform the type of item you have to look
		hb.append("It indicates which of these items is: <font color=\"LEVEL\">", itemIconType1.name().toLowerCase(), "</font><br>");
		
		hb.append("<table>");
		hb.append("<tr>");
		// Generate a random column where will the correct answer.
		int rnd = Rnd.get(0, 3);
		PlayerData.get(activeChar).setAnswerRight(rnd + "");
		
		for (int i = 0; i <= 3; i++)
		{
			// Generate random icons.
			String icon = "";
			if (i == rnd)
			{
				icon = IconData.getRandomItemType(itemIconType1, 40);
			}
			else
			{
				icon = IconData.getRandomItemType(itemIconType2, 40);
			}
			
			hb.append("<td align=\"center\" fixwidth=\"32\">");
			hb.append("<button value=\"\" action=\"bypass -h Engine AntiBot ", i, "\" width=\"32\" height=\"32\" back=\"", icon, "\" fore=\"", icon, "\">");
			hb.append("</td>");
		}
		hb.append("</tr>");
		hb.append("</table>");
		
		hb.append("</center></body></html>");
		
		// send player html
		sendHtml(activeChar, null, hb);
	}
	
	private static void sendPlayerJail(Player player)
	{
		if (player != null)
		{
			player.stopAbnormalEffect(AbnormalEffect.REDCIRCLE);
			// paralysis is removed when the player
			player.setIsParalyzed(false);
			// now player is mortal
			player.setIsInvul(false);
			// the character is sent to jail if he did not answer
			player.setPunishLevel(PunishLevel.JAIL, 10); // TODO missing config
		}
	}
}
