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
import java.util.StringTokenizer;

import main.data.SchemeBuffData;
import main.data.SkillData;
import main.engine.AbstractMods;
import main.enums.BuffType;
import main.holders.BuffHolder;
import main.util.Util;
import main.util.UtilInventory;
import main.util.builders.html.Html;
import main.util.builders.html.HtmlBuilder;
import main.util.builders.html.HtmlBuilder.HtmlType;
import main.util.builders.html.L2UI;
import main.util.builders.html.L2UI_CH3;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Cubic;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.stat.PlayerStat;
import net.sf.l2j.gameserver.model.actor.stat.SummonStat;
import net.sf.l2j.gameserver.model.actor.status.PlayerStatus;
import net.sf.l2j.gameserver.model.actor.status.SummonStatus;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SetSummonRemainTime;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge.GaugeColor;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

/**
 * Adaptacion del buffer de RIn4
 * @author fissban
 */
public class NpcBufferScheme extends AbstractMods
{
	private static final int NPC_ID = 60012;
	
	private static final String TITLE_NAME = "Buffer";
	
	private static final boolean FREE_BUFFS = true;
	
	// precio de usar cada buff, sea del tipo q sea.
	private static final int BUFF_PRICE = 1000;
	
	// precio de los buffs pre-establecidos
	private static final int BUFF_SET_PRICE = 1;
	// item a cobrar
	private static final int CONSUMABLE_ID = 57;
	
	private static final boolean TIME_OUT = true;
	// tiempo entre cada accion...crear scheme,bufearse,etc en segundos
	private static final int TIME_OUT_TIME = 1;
	// minimo nievel para usar el buffer
	private static final int MIN_LEVEL = 0;
	private static final int BUFF_REMOVE_PRICE = 1000;
	private static final int SCHEME_BUFF_PRICE = 1000;
	private static final int SCHEMES_PER_PLAYER = 3;
	
	// maximo de buffs q se pueden agregar de cada tipo por scheme
	private static final int MAX_SCHEME_BUFFS = 24;
	private static final int MAX_SCHEME_DANCES = 12;
	
	public NpcBufferScheme()
	{
		registerMod(true);
	}
	
	@Override
	public void onModState()
	{
		// TODO Auto-generated method stub
	}
	
	@Override
	public void onEvent(Player player, Creature npc, String command)
	{
		if (((Npc) npc).getNpcId() != NPC_ID)
		{
			return;
		}
		
		StringTokenizer st = new StringTokenizer(command, " ");
		String bypass = st.hasMoreTokens() ? st.nextToken() : "redirect_main";
		String eventParam1 = st.hasMoreTokens() ? st.nextToken() : "";
		String eventParam2 = st.hasMoreTokens() ? st.nextToken() : "";
		String eventParam3 = st.hasMoreTokens() ? st.nextToken() : "";
		String eventParam4 = st.hasMoreTokens() ? st.nextToken() : "";
		
		switch (bypass)
		{
			case "reloadscript":
			{
				if (eventParam1.equals("0"))
				{
					rebuildMainHtml(player);
					return;
				}
			}
			case "redirect_main":
				rebuildMainHtml(player);
				return;
			case "redirect_view_buff":
				buildHtml(player, BuffType.BUFF, eventParam1.equals("") ? 1 : Integer.parseInt(eventParam1));
				return;
			case "redirect_view_resist":
				buildHtml(player, BuffType.RESIST, eventParam1.equals("") ? 1 : Integer.parseInt(eventParam1));
				return;
			case "redirect_view_song":
				buildHtml(player, BuffType.SONG, eventParam1.equals("") ? 1 : Integer.parseInt(eventParam1));
				return;
			case "redirect_view_dances":
				buildHtml(player, BuffType.DANCE, eventParam1.equals("") ? 1 : Integer.parseInt(eventParam1));
				return;
			case "redirect_view_chants":
				buildHtml(player, BuffType.CHANT, eventParam1.equals("") ? 1 : Integer.parseInt(eventParam1));
				return;
			case "redirect_view_other":
				buildHtml(player, BuffType.OTHER, eventParam1.equals("") ? 1 : Integer.parseInt(eventParam1));
				return;
			case "redirect_view_special":
				buildHtml(player, BuffType.SPECIAL, eventParam1.equals("") ? 1 : Integer.parseInt(eventParam1));
				return;
			case "redirect_view_cubic":
				buildHtml(player, BuffType.CUBIC, eventParam1.equals("") ? 1 : Integer.parseInt(eventParam1));
				return;
			case "buffpet":
			{
				if (checkTimeOut(player))
				{
					setValueDB(player.getObjectId(), "Pet-On-Off", eventParam1);
					if (TIME_OUT)
					{
						addTimeout(player, GaugeColor.GREEN, TIME_OUT_TIME / 2, 600);
					}
				}
				rebuildMainHtml(player);
				return;
			}
			case "create":
			{
				// anti sql inject
				String name = eventParam1.replaceAll("[ !" + "\"" + "#$%&'()*+,/:;<=>?@" + "\\[" + "\\\\" + "\\]" + "\\^" + "`{|}~]", ""); // JOJO
				
				if (name.length() == 0 || name.equals("no_name"))
				{
					player.sendPacket(SystemMessageId.INCORRECT_NAME_TRY_AGAIN);
					showText(player, "Info", "Please, enter the scheme name!", true, "Return", "main");
					return;
				}
				
				// XXX INSERT INTO npcbuffer_scheme_list (player_id,scheme_name) VALUES (?,?)
				// obtenemos el listado de schemes
				String allSchemes = getValueDB(player.getObjectId(), "schemeName");
				
				// if first scheme create...init var
				if (allSchemes == null)
				{
					allSchemes = "";
				}
				else
				{
					// check if scheme name exist
					for (String s : allSchemes.split(","))
					{
						if (s != null && s.equals(name))
						{
							player.sendPacket(SystemMessageId.INCORRECT_NAME_TRY_AGAIN);
							showText(player, "Info", "The name you are trying to use is already in use!", true, "Return", "main");
							return;
						}
					}
				}
				
				allSchemes += name + ",";
				
				// salvamos el nuevo listado
				setValueDB(player.getObjectId(), "schemeName", allSchemes);
				
				rebuildMainHtml(player);
				return;
			}
			case "delete":
			{
				String schemeName = eventParam1;
				// removemos la lista de buffs
				removeValueDB(player.getObjectId(), schemeName);
				// removemos el nombre del scheme del listado
				String schemes = getValueDB(player.getObjectId(), "schemeName");
				
				// prevent bypass
				if (schemes != null)
				{
					if (schemes.contains(schemeName + ","))
					{
						schemes = schemes.replace(schemeName + ",", "");
					}
					else
					{
						// TODO prevenimos para los q ya tienen mas de un scheme con el viejo sistema
						schemes = schemes.replace(schemeName, "");
					}
					
					// salvamos el nuevo listado de los nombres de los schemes
					setValueDB(player.getObjectId(), "schemeName", schemes);
				}
				
				rebuildMainHtml(player);
				return;
			}
			case "delete_c":
			{
				// TODO podriamos crear un metodo q devuelva el html para evitar
				// tenerlo todo en una misma linea no?
				sendHtml(player, null, "<html><title>" + TITLE_NAME + "</title><body><center>" + Html.headHtml("BUFFER") + "<br>Do you really want to delete '" + eventParam1 + "' scheme?<br><br>" + "<button value=\"Yes\" action=\"bypass -h Engine NpcBufferScheme delete " + eventParam1 + "\" width=75 height=21 back=" + L2UI_CH3.Btn1_normalOn + " fore=" + L2UI_CH3.Btn1_normal + ">" + "<button value=\"No\" action=\"bypass -h Engine NpcBufferScheme delete_1\" width=75 height=21 back=" + L2UI_CH3.Btn1_normalOn + " fore=" + L2UI_CH3.Btn1_normal + ">" + "<br><font color=303030>" + TITLE_NAME + "</font></center></body></html>");
				return;
			}
			case "create_1":
			{
				createScheme(player);
				return;
			}
			case "edit_1":
			{
				editScheme(player);
				return;
			}
			case "delete_1":
			{
				deleteScheme(player);
				return;
			}
			case "manage_scheme_add":
			{
				viewAllSchemeBuffs(player, eventParam1, eventParam2, "add");
				return;
			}
			case "manage_scheme_remove":
			{
				viewAllSchemeBuffs(player, eventParam1, eventParam2, "remove");
				return;
			}
			case "manage_scheme_select":
			{
				getOptionList(player, eventParam1);
				return;
			}
			case "remove_buff":
			{
				String[] split = eventParam1.split("_");
				String schemeNameRemove = split[0];
				String id = split[1];
				String level = split[2];
				// "DELETE FROM npcbuffer_scheme_contents WHERE scheme_id=? AND skill_id=? AND skill_level=? LIMIT 1"
				
				// obtenemos el scheme actual
				String listBuff = getValueDB(player.getObjectId(), schemeNameRemove);
				// agregamos el nuevo buff
				listBuff = listBuff.replaceFirst(id + "," + level + ";", "");
				// lo salvamos en la memoria y la db
				setValueDB(player.getObjectId(), schemeNameRemove, listBuff);
				
				int temp = Integer.parseInt(eventParam3) - 1;
				
				if (temp <= 0)
				{
					getOptionList(player, schemeNameRemove);
				}
				else
				{
					viewAllSchemeBuffs(player, schemeNameRemove, eventParam2, "remove");
				}
				
				return;
			}
			case "add_buff":
			{
				String[] split = eventParam1.split("_");
				String schemeNameAdd = split[0];
				String id = split[1];
				String level = split[2];
				
				// "INSERT INTO npcbuffer_scheme_contents (scheme_id,skill_id,skill_level,buff_class) VALUES (?,?,?,?)"
				
				// obtenemos el scheme actual
				String listBuff = getValueDB(player.getObjectId(), schemeNameAdd);
				// agregamos el nuevo buff
				if (listBuff == null)
				{
					listBuff = id + "," + level + ";";
				}
				else
				{
					listBuff = listBuff.concat(id + "," + level + ";");
				}
				
				// lo salvamos en la memoria y la db
				setValueDB(player.getObjectId(), schemeNameAdd, listBuff);
				
				int temp = Integer.parseInt(eventParam3) + 1;
				
				if (temp >= MAX_SCHEME_BUFFS + MAX_SCHEME_DANCES)
				{
					getOptionList(player, schemeNameAdd);
				}
				else
				{
					viewAllSchemeBuffs(player, schemeNameAdd, eventParam2, "add");
				}
				return;
			}
			case "heal":
			{
				if (checkTimeOut(player))
				{
					if (UtilInventory.getItemsCount(player, CONSUMABLE_ID) < BUFF_PRICE)
					{
						showText(player, "Sorry", "You don't have the enough items:<br>You need: <font color=LEVEL>" + BUFF_PRICE + " " + getItemNameHtml(CONSUMABLE_ID) + "!", false, "0", "0");
						return;
					}
					final boolean getPetbuff = isPetBuff(player);
					if (getPetbuff)
					{
						if (player.getPet() != null)
						{
							heal(player, getPetbuff);
						}
						else
						{
							showText(player, "Info", "You can't use the Pet's options.<br>Summon your pet first!", false, "Return", "main");
							return;
						}
					}
					else
					{
						heal(player, getPetbuff);
					}
					UtilInventory.takeItems(player, CONSUMABLE_ID, BUFF_PRICE);
					if (TIME_OUT)
					{
						addTimeout(player, GaugeColor.BLUE, TIME_OUT_TIME / 2, 600);
					}
				}
				rebuildMainHtml(player);
				return;
			}
			case "removeBuffs":
			{
				if (checkTimeOut(player))
				{
					if (UtilInventory.getItemsCount(player, CONSUMABLE_ID) < BUFF_REMOVE_PRICE)
					{
						showText(player, "Sorry", "You don't have the enough items:<br>You need: <font color=LEVEL>" + BUFF_REMOVE_PRICE + " " + getItemNameHtml(CONSUMABLE_ID) + "!", false, "0", "0");
						return;
					}
					final boolean getPetbuff = isPetBuff(player);
					if (getPetbuff)
					{
						if (player.getPet() != null)
						{
							player.getPet().stopAllEffects();
						}
						else
						{
							showText(player, "Info", "You can't use the Pet's options.<br>Summon your pet first!", false, "Return", "main");
							return;
						}
					}
					else
					{
						player.stopAllEffects();
						if (player.getCubics() != null)
						{
							for (Cubic cubic : player.getCubics().values())
							{
								cubic.stopAction();
								player.delCubic(cubic.getId());
							}
						}
					}
					UtilInventory.takeItems(player, CONSUMABLE_ID, BUFF_REMOVE_PRICE);
					if (TIME_OUT)
					{
						addTimeout(player, GaugeColor.RED, TIME_OUT_TIME / 2, 600);
					}
				}
				
				rebuildMainHtml(player);
				return;
			}
			case "cast":
			{
				if (checkTimeOut(player))
				{
					List<BuffHolder> buffs = new ArrayList<>();
					
					String shemeName = eventParam1;
					String buffList = getValueDB(player.getObjectId(), shemeName);
					
					if (buffList != null)
					{
						for (String buff : buffList.split(";"))
						{
							int id = Integer.parseInt(buff.split(",")[0]);
							int level = Integer.parseInt(buff.split(",")[1]);
							
							if (isEnabled(id, level))
							{
								buffs.add(new BuffHolder(id, level));
							}
						}
					}
					
					if (buffs.isEmpty())
					{
						viewAllSchemeBuffs(player, eventParam1, "1", "add");
						return;
					}
					if (!FREE_BUFFS)
					{
						if (UtilInventory.getItemsCount(player, CONSUMABLE_ID) < SCHEME_BUFF_PRICE)
						{
							showText(player, "Sorry", "You don't have the enough items:<br>You need: <font color=LEVEL>" + SCHEME_BUFF_PRICE + " " + getItemNameHtml(CONSUMABLE_ID) + "!", false, "0", "0");
							return;
						}
					}
					
					final boolean getPetbuff = isPetBuff(player);
					
					for (BuffHolder bh : buffs)
					{
						if (!getPetbuff)
						{
							SkillTable.getInstance().getInfo(bh.getId(), bh.getLevel()).getEffects(player, player);
						}
						else
						{
							if (player.getPet() != null)
							{
								SkillTable.getInstance().getInfo(bh.getId(), bh.getLevel()).getEffects(player.getPet(), player.getPet());
							}
							else
							{
								showText(player, "Info", "You can't use the Pet's options.<br>Summon your pet first!", false, "Return", "main");
								return;
							}
						}
					}
					UtilInventory.takeItems(player, CONSUMABLE_ID, SCHEME_BUFF_PRICE);
					if (TIME_OUT)
					{
						addTimeout(player, GaugeColor.CYAN, TIME_OUT_TIME, 600);
					}
				}
				rebuildMainHtml(player);
				return;
			}
			case "giveBuffs":
			{
				final int cost = BUFF_PRICE;
				
				int id = Integer.parseInt(eventParam1);
				int level = Integer.parseInt(eventParam2);
				if (!isEnabled(id, level))
				{
					// posible bypass
					System.out.println("posible bypass en scheme buff -> " + player.getName());
					return;
				}
				
				if (checkTimeOut(player))
				{
					if (!FREE_BUFFS)
					{
						if (UtilInventory.getItemsCount(player, CONSUMABLE_ID) < cost)
						{
							showText(player, "Sorry", "You don't have the enough items:<br>You need: <font color=LEVEL>" + cost + " " + getItemNameHtml(CONSUMABLE_ID) + "!", false, "0", "0");
							return;
						}
					}
					L2Skill skill = SkillTable.getInstance().getInfo(id, level);
					if (skill.getSkillType() == L2SkillType.SUMMON)
					{
						if (UtilInventory.getItemsCount(player, skill.getItemConsumeId()) < skill.getItemConsume())
						{
							showText(player, "Sorry", "You don't have the enough items:<br>You need: <font color=LEVEL>" + skill.getItemConsume() + " " + getItemNameHtml(skill.getItemConsumeId()) + "!", false, "0", "0");
							return;
						}
					}
					final boolean getPetbuff = isPetBuff(player);
					if (!getPetbuff)
					{
						if (eventParam3.equals("CUBIC"))
						{
							if (!player.getCubics().isEmpty())
							{
								for (Cubic cubic : player.getCubics().values())
								{
									cubic.stopAction();
									player.delCubic(cubic.getId());
								}
							}
						}
					}
					else
					{
						if (eventParam3.equals("CUBIC"))
						{
							if (!player.getCubics().isEmpty())
							{
								for (Cubic cubic : player.getCubics().values())
								{
									cubic.stopAction();
									player.delCubic(cubic.getId());
								}
							}
						}
						else
						{
							if (player.getPet() == null)
							{
								showText(player, "Info", "You can't use the Pet's options.<br>Summon your pet first!", false, "Return", "main");
								return;
							}
						}
					}
					
					skill.getEffects(player, player);
					UtilInventory.takeItems(player, CONSUMABLE_ID, cost);
					if (TIME_OUT)
					{
						addTimeout(player, GaugeColor.CYAN, TIME_OUT_TIME / 10, 600);
					}
				}
				buildHtml(player, BuffType.valueOf(eventParam3), eventParam4.equals("") ? 1 : Integer.parseInt(eventParam4));
				return;
			}
			case "castBuffSet":
			{
				if (checkTimeOut(player))
				{
					if (!FREE_BUFFS)
					{
						if (UtilInventory.getItemsCount(player, CONSUMABLE_ID) < BUFF_SET_PRICE)
						{
							showText(player, "Sorry", "You don't have the enough items:<br>You need: <font color=LEVEL>" + BUFF_SET_PRICE + " " + getItemNameHtml(CONSUMABLE_ID) + "!", false, "0", "0");
							return;
						}
					}
					
					final boolean getPetbuff = isPetBuff(player);
					if (!getPetbuff)
					{
						for (BuffHolder bh : player.isMageClass() ? SchemeBuffData.getAllMageBuffs() : SchemeBuffData.getAllWarriorBuffs())
						{
							SkillTable.getInstance().getInfo(bh.getId(), bh.getLevel()).getEffects(player, player);
						}
					}
					else
					{
						if (player.getPet() != null)
						{
							// a los pets le daremos los mismos buff que a los guerreros
							for (BuffHolder bh : SchemeBuffData.getAllWarriorBuffs())
							{
								SkillTable.getInstance().getInfo(bh.getId(), bh.getLevel()).getEffects(player.getPet(), player.getPet());
							}
						}
						else
						{
							showText(player, "Info", "You can't use the Pet's options.<br>Summon your pet first!", false, "Return", "main");
							return;
						}
					}
					UtilInventory.takeItems(player, CONSUMABLE_ID, BUFF_SET_PRICE);
					if (TIME_OUT)
					{
						addTimeout(player, GaugeColor.CYAN, TIME_OUT_TIME, 600);
					}
				}
				rebuildMainHtml(player);
				return;
			}
			
		}
		rebuildMainHtml(player);
	}
	
	@Override
	public boolean onInteract(Player player, Creature npc)
	{
		if (!Util.areObjectType(Npc.class, npc))
		{
			return false;
		}
		
		if (((Npc) npc).getNpcId() != NPC_ID)
		{
			return false;
		}
		
		if (player.isGM())
		{
			rebuildMainHtml(player);
			return true;
		}
		
		if (checkTimeOut(player))
		{
			if (player.getLevel() < MIN_LEVEL)
			{
				showText(player, "Info", "Your level is too low!<br>You have to be at least level <font color=LEVEL>" + MIN_LEVEL + "</font>,<br>to use my services!", false, "Return", "main");
				return true;
			}
			else if (player.isInCombat())
			{
				showText(player, "Info", "You can't buff while you are attacking!<br>Stop your fight and try again!", false, "Return", "main");
				return true;
			}
			// return showText(st, "Sorry", "You have to wait a while!<br>if you wish to use my services!", false, "Return", "main");
		}
		rebuildMainHtml(player);
		return true;
	}
	
	private static String getSkillIconHtml(int id, int level)
	{
		String iconNumber = SkillData.getSkillIcon(id);
		return "<button action=\"bypass -h Engine NpcBufferScheme description " + id + " " + level + " x\" width=32 height=32 back=\"Icon.skill" + iconNumber + "\" fore=\"Icon.skill" + iconNumber + "\">";
	}
	
	private boolean checkTimeOut(Player player)
	{
		String blockUntilTime = getValueDB(player.getObjectId(), "blockUntilTime");
		if (blockUntilTime == null || (int) (System.currentTimeMillis() / 1000) > Integer.parseInt(blockUntilTime))
		{
			return true;
		}
		
		return false;
	}
	
	private void addTimeout(Player player, GaugeColor gaugeColor, int amount, int offset)
	{
		int endtime = (int) ((System.currentTimeMillis() + amount * 1000) / 1000);
		setValueDB(player.getObjectId(), "blockUntilTime", String.valueOf(endtime));
		player.sendPacket(new SetupGauge(gaugeColor, amount * 1000 + offset));
	}
	
	private static String getItemNameHtml(int itemId)
	{
		return "&#" + itemId + ";";
	}
	
	private static void heal(Player player, boolean isPet)
	{
		Summon target = player.getPet();
		if (!isPet)
		{
			PlayerStatus pcStatus = player.getStatus();
			PlayerStat pcStat = player.getStat();
			pcStatus.setCurrentHp(pcStat.getMaxHp());
			pcStatus.setCurrentMp(pcStat.getMaxMp());
			pcStatus.setCurrentCp(pcStat.getMaxCp());
		}
		else if (target != null)
		{
			SummonStatus petStatus = target.getStatus();
			SummonStat petStat = target.getStat();
			petStatus.setCurrentHp(petStat.getMaxHp());
			petStatus.setCurrentMp(petStat.getMaxMp());
			if (target instanceof Pet)
			{
				Pet pet = (Pet) target;
				pet.setCurrentFed(pet.getPetData().getMaxMeal());
				player.sendPacket(new SetSummonRemainTime(pet.getPetData().getMaxMeal(), pet.getCurrentFed()));
			}
			else
			{
				throw new RuntimeException();
			}
		}
	}
	
	private void rebuildMainHtml(Player player)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.HTML_TYPE);
		hb.append("<html><body>");
		hb.append(Html.headHtml("BUFFER"));
		hb.append("<br>");
		
		hb.append("<center>");
		
		hb.append(Html.newImage(L2UI.SquareWhite, 264, 1));
		hb.append("<table width=275 border=0 cellspacing=0 cellpadding=1 bgcolor=000000>");
		hb.append("<tr>");
		hb.append("<td align=center><font color=FFFF00>Buffs:</font></td>");
		hb.append("</tr>");
		hb.append("</table>");
		hb.append(Html.newImage(L2UI.SquareWhite, 264, 1));
		
		hb.append("<br>");
		
		final String bottonA, bottonB, bottonC;
		String pet = getValueDB(player.getObjectId(), "Pet-On-Off");
		if (pet == null || pet.equals("1"))
		{
			bottonA = "Auto Buff Pet";
			bottonB = "Heal My Pet";
			bottonC = "Remove Buffs";
			hb.append("<button value=\"Pet Options\" action=\"bypass -h Engine NpcBufferScheme buffpet 0\" width=75 height=21  back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, ">");
		}
		else
		{
			bottonA = "Auto Buff";
			bottonB = "Heal";
			bottonC = "Remove Buffs";
			hb.append("<button value=\"Char Options\" action=\"bypass -h Engine NpcBufferScheme buffpet 1\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, ">");
		}
		
		hb.append("<table width=80% cellspacing=0 cellpadding=1>");
		hb.append("<tr>");
		hb.append("<td height=32 align=center><button value=Buffs action=\"bypass -h Engine NpcBufferScheme redirect_view_buff\" width=75 height=21back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, "></td>");
		hb.append("<td height=32 align=center><button value=Resist action=\"bypass -h Engine NpcBufferScheme redirect_view_resist\" width=75 height=21back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, "></td>");
		hb.append("</tr>");
		
		hb.append("<tr>");
		hb.append("<td height=32 align=center><button value=Songs action=\"bypass -h Engine NpcBufferScheme redirect_view_song\" width=75 height=21back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, "></td>");
		hb.append("<td height=32 align=center><button value=Dances action=\"bypass -h Engine NpcBufferScheme redirect_view_dances\" width=75 height=21back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, "></td>");
		hb.append("</tr>");
		
		hb.append("<tr>");
		hb.append("<td height=32 align=center><button value=Chants action=\"bypass -h Engine NpcBufferScheme redirect_view_chants\" width=75 height=21back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, "></td>");
		hb.append("<td height=32 align=center><button value=Special action=\"bypass -h Engine NpcBufferScheme redirect_view_special\" width=75 height=21back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, "></td>");
		hb.append("</tr>");
		
		hb.append("<tr>");
		hb.append("<td height=32 align=center><button value=Others action=\"bypass -h Engine NpcBufferScheme redirect_view_other\" width=75 height=21back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, "></td>");
		hb.append("<td height=32 align=center><button value=Cubics action=\"bypass -h Engine NpcBufferScheme redirect_view_cubic\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, "></td>");
		hb.append("</tr>");
		hb.append("</table>");
		// ---------------------------------------------------------------------------------------------
		hb.append(Html.newImage(L2UI.SquareWhite, 264, 1));
		hb.append("<table width=275 border=0 cellspacing=0 cellpadding=1 bgcolor=000000>");
		hb.append("<tr>");
		hb.append("<td align=center><font color=FFFF00>Preset:</font></td>");
		hb.append("</tr>");
		hb.append("</table>");
		hb.append(Html.newImage(L2UI.SquareWhite, 264, 1));
		hb.append("<table width=100% height=37 border=0 cellspacing=0 cellpadding=5>");
		hb.append("<tr>");
		hb.append("<td><button value=\"", bottonA, "\" action=\"bypass -h Engine NpcBufferScheme castBuffSet 0 0 0\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, "></td>");
		hb.append("<td><button value=\"", bottonB, "\" action=\"bypass -h Engine NpcBufferScheme heal\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, "></td>");
		hb.append("<td><button value=\"", bottonC, "\" action=\"bypass -h Engine NpcBufferScheme removeBuffs 0 0 0\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, "></td>");
		hb.append("</tr>");
		hb.append("</table>");
		
		hb.append("");
		// generate html scheme
		hb.append(generateScheme(player));
		
		hb.append("<br>");
		hb.append("<font color=303030>" + TITLE_NAME + "</font>");
		hb.append("</center>");
		hb.append("</body></html>");
		
		sendHtml(player, null, hb);
	}
	
	private String generateScheme(Player player)
	{
		String schemeNames = getValueDB(player.getObjectId(), "schemeName");
		
		HtmlBuilder hb = new HtmlBuilder(HtmlType.HTML_TYPE);
		hb.append("<br1>");
		hb.append(Html.newImage(L2UI.SquareWhite, 264, 1));
		hb.append("<table width=271 border=0 cellspacing=0 cellpadding=1 bgcolor=000000>");
		hb.append("<tr>");
		hb.append("<td align=center><font color=FFFF00>Scheme:</font></td>");
		hb.append("<td align=right><font color=LEVEL></font></td>");
		hb.append("</tr>");
		hb.append("</table>");
		hb.append(Html.newImage(L2UI.SquareWhite, 264, 1));
		
		hb.append("<br1>");
		
		hb.append("<table cellspacing=0 cellpadding=5 height=28>");
		
		if (schemeNames != null)
		{
			String[] TRS =
			{
				"<tr><td>",
				"</td>",
				"<td>",
				"</td></tr>"
			};
			
			hb.append("<table>");
			int td = 0;
			for (int i = 0; i < schemeNames.split(",").length; ++i)
			{
				if (td > 2)
				{
					td = 0;
				}
				hb.append(TRS[td] + "<button value=\"", schemeNames.split(",")[i], "\" action=\"bypass -h Engine NpcBufferScheme cast ", schemeNames.split(",")[i], "\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, ">", TRS[td + 1]);
				td += 2;
			}
			
			hb.append("</table>");
		}
		
		if (schemeNames == null || schemeNames.split(",").length < SCHEMES_PER_PLAYER)
		{
			hb.append("<br1><table><tr><td><button value=\"Create\" action=\"bypass -h Engine NpcBufferScheme create_1\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, "></td>");
		}
		else
		{
			hb.append("<br1><table width=100><tr>");
		}
		
		if (schemeNames != null)
		{
			hb.append("<td><button value=\"Edit\" action=\"bypass -h Engine NpcBufferScheme edit_1\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, "></td><td><button value=\"Delete\" action=\"bypass -h Engine NpcBufferScheme delete_1\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, "></td></tr></table>");
		}
		else
		{
			hb.append("</tr></table>");
		}
		return hb.toString();
	}
	
	/**
	 * Chequeamos la cantidad de buffs q tiene un player en su scheme.
	 * @param player
	 * @param schemeName
	 * @return
	 */
	private int getBuffCount(Player player, String schemeName)
	{
		String buffList = getValueDB(player.getObjectId(), schemeName);
		if (buffList != null)
		{
			return buffList.split(";").length;
		}
		
		return 0;
	}
	
	/**
	 * Chequeamos si el buff aun esta en nuestro listado de buff habilitados.
	 * @param id
	 * @param level
	 * @return
	 */
	private static boolean isEnabled(int id, int level)
	{
		for (BuffHolder bh : SchemeBuffData.getAllGeneralBuffs())
		{
			if (bh.getId() == id && bh.getLevel() == level)
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Chequeamos si el player tiene un determinado buff en su scheme o no.
	 * @param player
	 * @param scheme
	 * @param id
	 * @param level
	 * @return
	 */
	private boolean isUsed(Player player, String scheme, int id, int level)
	{
		String buffList = getValueDB(player.getObjectId(), scheme);
		
		if (buffList == null)
		{
			return false;
		}
		
		for (String buff : buffList.split(";"))
		{
			if (Integer.parseInt(buff.split(",")[0]) == id && Integer.parseInt(buff.split(",")[1]) == level)
			{
				return true;
			}
		}
		
		return false;
	}
	
	private static void showText(Player player, String type, String text, boolean buttonEnabled, String buttonName, String location)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.HTML_TYPE);
		hb.append("<html><head><title>", TITLE_NAME, "</title></head><body>");
		hb.append(Html.headHtml("BUFFER"));
		hb.append("<center>");
		hb.append("<br>");
		hb.append("<font color=LEVEL>", type, "</font>");
		hb.append("<br>", text, "<br>");
		if (buttonEnabled)
		{
			hb.append("<button value=\"" + buttonName + "\" action=\"bypass -h Engine NpcBufferScheme redirect_", location, " 0 0\" width=75 height=21  back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, ">");
		}
		hb.append("<font color=303030>", TITLE_NAME, "</font></center></body></html>");
		
		sendHtml(player, null, hb);
	}
	
	/**
	 * Chequeamos si el player selecciono la opcion de pet o char para bufear
	 * @param player
	 * @return
	 */
	private boolean isPetBuff(Player player)
	{
		String pettBuff = getValueDB(player.getObjectId(), "Pet-On-Off");
		return pettBuff == null || pettBuff.equals("1");
	}
	
	/**
	 * Mini menu para los scheme
	 * @return
	 */
	private static void createScheme(Player player)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.HTML_TYPE);
		hb.append("<html><head><title>", TITLE_NAME, "</title></head><body>");
		hb.append(Html.headHtml("BUFFER"));
		hb.append("<center>");
		hb.append("<br><br>");
		hb.append("You MUST seprerate new words with a dot (.)");
		hb.append("<br><br>");
		hb.append("Scheme name: <edit var=\"name\" width=100>");
		hb.append("<br><br>");
		hb.append("<button value=\"Create Scheme\" action=\"bypass -h Engine NpcBufferScheme create $name no_name\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, ">");
		hb.append("<br>");
		hb.append("<button value=\"Back\" action=\"bypass -h Engine NpcBufferScheme redirect_main\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, ">");
		hb.append("<br>");
		hb.append("<font color=303030>", TITLE_NAME, "</font>");
		hb.append("</center>");
		hb.append("</body></html>");
		sendHtml(player, null, hb);
	}
	
	private void deleteScheme(Player player)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.HTML_TYPE);
		hb.append("<html><head><title>", TITLE_NAME, "</title></head><body>");
		hb.append(Html.headHtml("BUFFER"));
		hb.append("<center>");
		hb.append("<br>Available schemes:<br><br>");
		
		// XXX "SELECT * FROM npcbuffer_scheme_list WHERE player_id=?"
		String schemeNames = getValueDB(player.getObjectId(), "schemeName");
		for (String scheme : schemeNames.split(","))
		{
			hb.append("<button value=\"", scheme, "\" action=\"bypass -h Engine NpcBufferScheme delete_c ", scheme, " x\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, ">");
		}
		
		hb.append("<br>");
		hb.append("<button value=\"Back\" action=\"bypass -h Engine NpcBufferScheme redirect_main\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, ">");
		hb.append("<br>");
		hb.append("<font color=303030>" + TITLE_NAME + "</font>");
		hb.append("</center>");
		hb.append("</body></html>");
		
		sendHtml(player, null, hb);
	}
	
	private void editScheme(Player player)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.HTML_TYPE);
		hb.append("<html><head><title>", TITLE_NAME, "</title></head><body>");
		hb.append(Html.headHtml("BUFFER"));
		hb.append("<center>");
		hb.append("<br>Select a scheme that you would like to manage:<br><br>");
		
		// XXX"SELECT * FROM npcbuffer_scheme_list WHERE player_id=?"
		
		String schemeNames = getValueDB(player.getObjectId(), "schemeName");
		for (String scheme : schemeNames.split(","))
		{
			hb.append("<button value=\"" + scheme + "\" action=\"bypass -h Engine NpcBufferScheme manage_scheme_select " + scheme + "\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, ">");
		}
		
		hb.append("<br>");
		hb.append("<button value=\"Back\" action=\"bypass -h Engine NpcBufferScheme redirect_main\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, ">");
		hb.append("<br>");
		hb.append("<font color=303030>" + TITLE_NAME + "</font>");
		hb.append("</center>");
		hb.append("</body></html>");
		
		sendHtml(player, null, hb);
	}
	
	private void getOptionList(Player player, String scheme)
	{
		int bcount = getBuffCount(player, scheme);
		
		HtmlBuilder hb = new HtmlBuilder(HtmlType.HTML_TYPE);
		hb.append("<html><head><title>", TITLE_NAME, "</title></head><body>");
		hb.append(Html.headHtml("BUFFER"));
		hb.append("<center>");
		hb.append("<br>There are ", Html.newFontColor("LEVEL", bcount), " buffs in current scheme!<br><br>");
		
		if (bcount < MAX_SCHEME_BUFFS + MAX_SCHEME_DANCES)
		{
			hb.append("<button value=\"Add buffs\" action=\"bypass -h Engine NpcBufferScheme manage_scheme_add ", scheme, " 1\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, ">");
		}
		if (bcount > 0)
		{
			hb.append("<button value=\"Remove buffs\" action=\"bypass -h Engine NpcBufferScheme manage_scheme_remove ", scheme, " 1\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, ">");
		}
		hb.append("<br>");
		hb.append("<button value=\"Back\" action=\"bypass -h Engine NpcBufferScheme edit_1\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, ">");
		hb.append("<button value=\"Home\" action=\"bypass -h Engine NpcBufferScheme redirect_main\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, ">");
		hb.append("<br>");
		hb.append(Html.newFontColor("303030", TITLE_NAME));
		hb.append("</center>");
		hb.append("</body></html>");
		
		sendHtml(player, null, hb);
	}
	
	private static void buildHtml(Player player, BuffType buffType, int page)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.HTML_TYPE);
		hb.append("<html><head><title>", TITLE_NAME, "</title></head><body>");
		hb.append("<center><br>");
		
		List<BuffHolder> buffs = new ArrayList<>();
		
		for (BuffHolder bh : SchemeBuffData.getAllGeneralBuffs())
		{
			if (bh.getSkill() == null)
			{
				System.out.println("buffId: " + bh.getId());
			}
			else if (bh.getType() == buffType)
			{
				buffs.add(bh);
			}
		}
		
		if (buffs.size() == 0)
		{
			hb.append("No buffs are available at this moment!");
		}
		else
		{
			if (FREE_BUFFS)
			{
				hb.append("All buffs are for <font color=LEVEL>free</font>!");
			}
			else
			{
				int price = BUFF_PRICE;
				
				hb.append("All special buffs cost <font color=LEVEL>" + Html.formatAdena(price) + "</font> adena!");
			}
			hb.append("<br1>");
			
			int MAX_PER_PAGE = 12;
			int searchPage = MAX_PER_PAGE * (page - 1);
			int count = 0;
			
			hb.append(Html.newImage(L2UI.SquareWhite, 264, 1));
			for (BuffHolder bh : buffs)
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
				
				hb.append("<table width=264", count % 2 == 0 ? " bgcolor=000000>" : ">");
				String name = bh.getSkill().getName().replace("+", " ");
				hb.append("<tr>");
				hb.append("<td height=32 fixwidth=32>", getSkillIconHtml(bh.getId(), bh.getLevel()), "</td>");
				hb.append("<td height=32 fixwidth=232 align=center><a action=\"bypass -h Engine NpcBufferScheme giveBuffs ", bh.getId(), " ", bh.getLevel(), " ", buffType.name(), " ", page, "\">", name, "</a></td>");
				hb.append("<td height=32 fixwidth=32>", getSkillIconHtml(bh.getId(), bh.getLevel()), "</td>");
				hb.append("</tr>");
				
				hb.append("</table>");
				hb.append(Html.newImage(L2UI.SquareWhite, 264, 1));
				count++;
			}
			
			hb.append("<center>");
			hb.append(Html.newImage(L2UI.SquareWhite, 264, 1));
			hb.append("<table bgcolor=000000>");
			hb.append("<tr>");
			
			int currentPage = 1;
			for (int i = 0; i < buffs.size(); i++)
			{
				if (i % MAX_PER_PAGE == 0)
				{
					hb.append("<td width=20 align=center><a action=\"bypass -h Engine NpcBufferScheme redirect_view_", buffType.name().toLowerCase(), " ", currentPage, "\">", currentPage, "</a></td>");
					currentPage++;
				}
			}
			
			hb.append("</tr>");
			hb.append("</table>");
			hb.append(Html.newImage(L2UI.SquareWhite, 264, 1));
			hb.append("</center>");
		}
		hb.append("<br>");
		hb.append("<button value=\"Back\" action=\"bypass -h Engine NpcBufferScheme redirect_main\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, ">");
		hb.append("<br>");
		hb.append("<font color=303030>", TITLE_NAME, "</font>");
		hb.append("</center>");
		hb.append("</body></html>");
		
		sendHtml(player, null, hb);
	}
	
	private String viewAllSchemeBuffsGetBuffCount(Player player, String scheme)
	{
		int count = 0;
		int D_S_Count = 0;
		int B_Count = 0;
		
		// obtenemos el listado de skills del scheme de un player
		String buffList = getValueDB(player.getObjectId(), scheme);
		
		if (buffList != null)
		{
			// parseamos cada buff
			for (String buff : buffList.split(";"))
			{
				// parseamos el id y lvl de cada buff
				int id = Integer.parseInt(buff.split(",")[0]);
				int level = Integer.parseInt(buff.split(",")[1]);
				
				count++;
				
				for (BuffHolder bh : SchemeBuffData.getAllGeneralBuffs())
				{
					if (!isEnabled(id, level))
					{
						continue;
					}
					
					if (bh.getId() == id && bh.getLevel() == level)
					{
						if (bh.getType() == BuffType.SONG || bh.getType() == BuffType.DANCE)
						{
							D_S_Count++;
						}
						else
						{
							B_Count++;
						}
						break;
					}
				}
			}
		}
		
		return count + " " + B_Count + " " + D_S_Count;
	}
	
	/**
	 * @param player
	 * @param schemeName
	 * @param page
	 * @param action
	 */
	private void viewAllSchemeBuffs(Player player, String schemeName, String page, String action)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.HTML_TYPE);
		hb.append("<html><head><title>", TITLE_NAME, "</title></head><body>");
		hb.append(Html.headHtml("BUFFER"));
		hb.append("<center>");
		
		hb.append("<br>");
		
		String[] eventSplit = viewAllSchemeBuffsGetBuffCount(player, schemeName).split(" ");
		
		int buffsTotal = Integer.parseInt(eventSplit[0]);
		int buffsCount = Integer.parseInt(eventSplit[1]);
		int daceSong = Integer.parseInt(eventSplit[2]);
		
		List<BuffHolder> buffs = new ArrayList<>();
		
		if (action.equals("add"))
		{
			hb.append("You can add <font color=LEVEL>", MAX_SCHEME_BUFFS - buffsCount, "</font> Buffs and <font color=LEVEL>", MAX_SCHEME_DANCES - daceSong, "</font> Dances more!");
			
			for (BuffHolder bh : SchemeBuffData.getAllGeneralBuffs())
			{
				if (daceSong > MAX_SCHEME_DANCES)
				{
					if (bh.getType() == BuffType.DANCE || bh.getType() == BuffType.SONG)
					{
						continue;
					}
				}
				
				if (buffsCount > MAX_SCHEME_BUFFS)
				{
					if (bh.getType() != BuffType.DANCE && bh.getType() != BuffType.SONG)
					{
						continue;
					}
				}
				
				buffs.add(bh);
			}
		}
		else if (action.equals("remove"))
		{
			hb.append("You have <font color=LEVEL>", buffsCount, "</font> Buffs and <font color=LEVEL>", daceSong, "</font> Dances");
			
			String buffList = getValueDB(player.getObjectId(), schemeName);
			if (buffList == null)
			{
				System.out.println("error en remove buff");
			}
			else
			{
				for (String buff : buffList.split(";"))
				{
					int id = Integer.parseInt(buff.split(",")[0]);
					int level = Integer.parseInt(buff.split(",")[1]);
					
					buffs.add(new BuffHolder(id, level));
				}
			}
		}
		else
		{
			throw new RuntimeException();
		}
		
		hb.append("<br1>", Html.newImage(L2UI.SquareWhite, 264, 1), "<table border=0 bgcolor=000000><tr>");
		final int buffsPerPage = 10;
		final String width;
		int pc = (buffs.size() - 1) / buffsPerPage + 1;
		
		// definimos el largo de las celdas con las pagina
		if (pc > 5)
		{
			width = "25";
		}
		else
		{
			width = "50";
		}
		
		for (int ii = 1; ii <= pc; ++ii)
		{
			// creamos la botonera con las paginas
			if (ii == Integer.parseInt(page))
			{
				hb.append("<td width=", width, " align=center><font color=LEVEL>", ii, "</font></td>");
			}
			else if (action.equals("add"))
			{
				hb.append("<td width=", width, ">", "<a action=\"bypass -h Engine NpcBufferScheme manage_scheme_add ", schemeName, " ", ii, " x\">", ii, "</a></td>");
			}
			else if (action.equals("remove"))
			{
				hb.append("<td width=", width, ">", "<a action=\"bypass -h Engine NpcBufferScheme manage_scheme_remove ", schemeName, " ", ii, " x\">", ii, "</a></td>");
			}
			else
			{
				throw new RuntimeException();
			}
		}
		hb.append("</tr></table>", Html.newImage(L2UI.SquareWhite, 264, 1));
		
		int limit = buffsPerPage * Integer.parseInt(page);
		int start = limit - buffsPerPage;
		int end = Math.min(limit, buffs.size());
		int k = 0;
		for (int i = start; i < end; ++i)
		{
			BuffHolder bh = buffs.get(i);
			
			String name = bh.getSkill().getName();
			int id = bh.getId();
			int level = bh.getLevel();
			
			if (action.equals("add"))
			{
				if (!isUsed(player, schemeName, id, level))
				{
					if (k % 2 != 0)
					{
						
						hb.append("<br1>", Html.newImage(L2UI.SquareGray, 264, 1), "<table border=0>");
					}
					else
					{
						hb.append("<br1>", Html.newImage(L2UI.SquareGray, 264, 1), "<table border=0 bgcolor=000000>");
					}
					
					hb.append("<tr>");
					hb.append("<td width=35>", getSkillIconHtml(id, level), "</td>");
					hb.append("<td fixwidth=170>", name, "</td>");
					hb.append("<td><button value=\"Add\" action=\"bypass -h Engine NpcBufferScheme add_buff ", schemeName, "_", id, "_", level, " ", page, " ", buffsTotal, "\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, "></td>");
					hb.append("</tr>");
					hb.append("</table>");
					k += 1;
				}
			}
			else if (action.equals("remove"))
			{
				if (k % 2 != 0)
				{
					hb.append("<br1>", Html.newImage(L2UI.SquareGray, 264, 1), "<table border=0>");
				}
				else
				{
					hb.append("<br1>", Html.newImage(L2UI.SquareGray, 264, 1), "<table border=0 bgcolor=000000>");
				}
				hb.append("<tr>");
				hb.append("<td width=35>", getSkillIconHtml(id, level), "</td>");
				hb.append("<td fixwidth=170>", name, "</td>");
				hb.append("<td><button value=\"Remove\" action=\"bypass -h Engine NpcBufferScheme remove_buff ", schemeName, "_", id, "_", level, " ", page, " ", buffsTotal, "\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, "></td>");
				hb.append("</tr>");
				hb.append("</table>");
				k += 1;
			}
		}
		hb.append("<br><br>");
		hb.append("<button value=Back action=\"bypass -h Engine NpcBufferScheme manage_scheme_select ", schemeName, "\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, ">");
		hb.append("<button value=Home action=\"bypass -h Engine NpcBufferScheme redirect_main\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, ">");
		hb.append("<br>");
		hb.append("<font color=303030>", TITLE_NAME, "</font>");
		hb.append("</center>");
		hb.append("</body></html>");
		
		sendHtml(player, null, hb);
	}
}
