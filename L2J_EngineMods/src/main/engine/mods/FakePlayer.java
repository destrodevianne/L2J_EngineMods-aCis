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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import main.data.ConfigData;
import main.data.FakePlayerData;
import main.data.PlayerData;
import main.engine.AbstractMods;
import main.engine.ai.FakePlayerAI;
import main.holders.PlayerHolder;
import main.util.UtilPlayer;
import main.util.builders.html.Html;
import main.util.builders.html.HtmlBuilder;
import main.util.builders.html.HtmlBuilder.HtmlType;
import main.util.builders.html.L2UI;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.data.CharTemplateTable;
import net.sf.l2j.gameserver.data.MapRegionTable;
import net.sf.l2j.gameserver.data.PlayerNameTable;
import net.sf.l2j.gameserver.data.sql.ClanTable;
import net.sf.l2j.gameserver.data.xml.ArmorSetData;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.template.PlayerTemplate;
import net.sf.l2j.gameserver.model.base.ClassType;
import net.sf.l2j.gameserver.model.base.Sex;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.model.pledge.ClanMember;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.serverpackets.TitleUpdate;

/**
 * @author fissban
 */
public class FakePlayer extends AbstractMods
{
	// Maximo level
	private static final int FAKE_MAX_LEVEL = ConfigData.FAKE_LEVEL;
	private static final int FAKE_MAX_PVP = ConfigData.FAKE_MAX_PVP;
	private static final int FAKE_MAX_PK = ConfigData.FAKE_MAX_PK;
	
	private static final int FAKE_CHANCE_SIT = ConfigData.FAKE_CHANCE_SIT;
	private static final int FAKE_CHANCE_HAS_CLAN = ConfigData.FAKE_CHANCE_HAS_CLAN;
	
	private static final int SPAWN_RANGE = ConfigData.SPAWN_RANGE;
	
	private static final int CHANCE_SELL_BUFF = 10;
	private static final int[] FAKE_SET_WARRIOR =
	{
		6379, // Draconic leather set
		6373, // Imperial crusader set
		2392, // Doom leather set
		356, // Full Plate Armor set
	};
	private static final int[] FAKE_WEAPON_WARRIOR =
	{
		6368, // shining bow
		6369, // dragon hunter axe
		6370, // saint spear
		6372, // heavens divider
	};
	private static final int[] FAKE_SET_MAGE =
	{
		6383, // Major arcana robe set
		2409, // Majestic robe set
		2407, // Dark Crystal robe set
		439, // Karmian robe set
	};
	private static final int[] FAKE_WEAPON_MAGE =
	{
		6366, // imperial staff
		7722, // Sword of Valhalla
		6313, // Homunkulus's Sword
		5641, // Sword of Miracles
	};
	
	// crestId
	private static int _crestId = 0;
	
	public FakePlayer()
	{
		// Desabilitado hasta q estemos seguro de q funcione correctamente
		// registerMod(ConfigData.ENABLE_fakePlayers);
	}
	
	@Override
	public void onModState()
	{
		switch (getState())
		{
			case START:
				restoreAllFakePlayers();
				break;
			
			case END:
				break;
		}
	}
	
	private void restoreAllFakePlayers()
	{
		for (PlayerHolder ph : PlayerData.getAllPlayers())
		{
			String fake = getValueDB(ph.getObjectId(), "fakePlayer");
			// Don't has value in db
			if (fake == null)
			{
				continue;
			}
			
			// ->>> SPAWNEAMOS AL FAKE
			Player fakePlayer = UtilPlayer.spawnPlayer(ph.getObjectId());
			
			boolean insideCity = MapRegionTable.getTown(fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ()) != null;
			
			if (fake.equals("sitDown") && insideCity)
			{
				fakePlayer.sitDown();
			}
			else
			{
				// obtenemos la zona de farm del fake si es q se definio
				String posToFarm = getValueDB(ph.getObjectId(), "posToFarm");
				if (posToFarm != null)
				{
					// definimos la zona de farm del fake
					ph.setPosToFarm(posToFarm);
					// enviamos al fake a su zona de farm
					fakePlayer.teleToLocation(ph.getPosToFarm(), 0);
				}
				else if (!insideCity)
				{
					// XXX para los q ya estaban usando el sistema de fake usaremos la posicion actual
					ph.setPosToFarm(fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ());
				}
				
				// dentemos el viejo Ai
				fakePlayer.detachAI();
				// inicializamos el Ai de los fakes
				fakePlayer.setAI(new FakePlayerAI(fakePlayer));
			}
			fakePlayer.broadcastUserInfo();
			
			// ;)
			ph.setFake(true);
		}
	}
	
	@Override
	public void onEvent(Player player, Creature npc, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		
		String event = st.nextToken();
		switch (event)
		{
			case "allFakes":
			{
				getAllFakePlayer(player, Integer.parseInt(st.nextToken()));
				break;
			}
			case "deleteFake":
			{
				// TODO falta remover de la db el resto de la info del personaje.
				
				int objId = Integer.parseInt(st.nextToken());
				int page = Integer.parseInt(st.nextToken());
				// desconectamos el personaje
				Player fake = World.getInstance().getPlayer(objId);
				if (fake != null)
				{
					fake.deleteMe();
				}
				// removemos el valor de la memoria y de la tabla "engine"
				removeValueDB(objId, "fakePlayer");
				// removemos de la db todo lo referente a este personaje
				L2GameClient.deleteCharByObjId(objId);
				
				// ya no es mas fake este personaje
				PlayerData.get(objId).setFake(false);
				// volvemos a enviar el html para continuar
				getAllFakePlayer(player, page);
				break;
			}
		}
	}
	
	@Override
	public boolean onAdminCommand(Player player, String chat)
	{
		StringTokenizer st = new StringTokenizer(chat, " ");
		
		switch (st.nextToken())
		{
			case "allFakes":
			{
				getAllFakePlayer(player, 1);
				return true;
			}
			case "formatFake":
			{
				player.sendMessage("//createRndFake count Lvl pvp pk clan(1=true 0=false) template(0 118)");
				return true;
			}
			// Formato:
			// createRndFake "count"
			case "createRndFake":
			{
				int count = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 1;
				int lvl = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : FAKE_MAX_LEVEL;
				int pvp = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : FAKE_MAX_PVP;
				int pk = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : FAKE_MAX_PK;
				int clan = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : Rnd.get(100) < FAKE_CHANCE_HAS_CLAN ? 1 : 0;
				int templateId = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : -1;
				
				try
				{
					for (int i = 0; i < count; i++)
					{
						int rndTemplateId = templateId;
						
						if (rndTemplateId < 0)
						{
							if (Rnd.nextBoolean())
							{
								rndTemplateId = Rnd.get(0, 57);
							}
							else
							{
								rndTemplateId = Rnd.get(88, 118);
							}
						}
						// ------------------------------------------------------------->>> CREAMOS AL FAKE
						Player fakePlayer = createPlayer(player, SPAWN_RANGE, rndTemplateId, lvl);
						// ------------------------------------------------------------->>> SPAWNEAMOS AL FAKE
						fakePlayer = UtilPlayer.spawnPlayer(fakePlayer.getObjectId());
						fakePlayer.giveAvailableSkills();
						// random pvp/pk
						fakePlayer.setPkKills(pvp > 0 ? Rnd.get(pvp) : 0); // TODO incrementar a medida q transcurre el tiempo
						fakePlayer.setPvpKills(pk > 0 ? Rnd.get(pk) : 0); // TODO incrementar a medida q transcurre el tiempo
						
						String action = "stand";
						// algunos sentados y otros parados
						if (Rnd.get(100) < FAKE_CHANCE_SIT && player.isInsideZone(ZoneId.PEACE))
						{
							fakePlayer.sitDown();
							action = "sitDown";
						}
						else
						{
							PlayerData.get(fakePlayer).setPosToFarm(fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ());
							fakePlayer.detachAI();
							fakePlayer.setAI(new FakePlayerAI(fakePlayer));
						}
						
						// clan
						if (clan > 0)
						{
							if (defineClan(fakePlayer))
							{
								//
							}
						}
						
						if (fakePlayer.getClanId() != 0)
						{
							fakePlayer.setTitle(FakePlayerData.getTitle());
						}
						
						//
						UtilPlayer.storeCharBase(fakePlayer);
						// fakePlayer.store(false);
						PlayerData.get(fakePlayer).setFake(true);
						// Salvamos al fake en la db
						setValueDB(fakePlayer, "fakePlayer", action);
						// Salvamos la localizacion de farm
						setValueDB(fakePlayer, "posToFarm", fakePlayer.getX() + "," + fakePlayer.getY() + "," + fakePlayer.getZ());
						
						fakePlayer.broadcastUserInfo();
						fakePlayer.broadcastPacket(new TitleUpdate(fakePlayer));
						
						Thread.sleep(100);
					}
				}
				catch (Exception e)
				{
					player.sendMessage("format: //createRndFake count Lvl pvp pk clan(1=true 0=false) template(0 118)");
					e.printStackTrace();
				}
				return true;
			}
		}
		return false;
	}
	
	public void getAllFakePlayer(Player player, int page)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.HTML_TYPE);
		
		hb.append("<html><body>");
		hb.append("<br>");
		hb.append(Html.headHtml("All FAKE Players"));
		hb.append("<br>");
		
		hb.append("<table>");
		hb.append("<tr>");
		hb.append("<td width=200><font color=LEVEL>Player:</font></td>");
		hb.append("<td width=64><font color=LEVEL>Action:</font></td>");
		hb.append("</tr>");
		hb.append("</table>");
		
		int MAX_PER_PAGE = 12;
		int searchPage = MAX_PER_PAGE * (page - 1);
		int count = 0;
		int countFakes = 0;
		
		for (PlayerHolder ph : PlayerData.getAllPlayers())
		{
			if (ph.isFake())
			{
				countFakes++;
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
				hb.append("<td width=200>", ph.getName(), "</td><td width=64><a action=\"bypass -h Engine FakePlayer deleteFake ", ph.getObjectId(), " ", page, "\">DELETE</a></td>");
				hb.append("</tr>");
				hb.append("</table>");
				count++;
			}
		}
		
		hb.append("<center>");
		hb.append("<img src=", L2UI.SquareGray, " width=264 height=1>");
		hb.append("<table bgcolor=CC99FF>");
		hb.append("<tr>");
		
		int currentPage = 1;
		
		for (int i = 0; i < countFakes; i++)
		{
			if (i % MAX_PER_PAGE == 0)
			{
				hb.append("<td width=18 align=center><a action=\"bypass -h Engine FakePlayer allFakes ", currentPage, "\">", currentPage, "</a></td>");
				currentPage++;
			}
		}
		
		hb.append("</tr>");
		hb.append("</table>");
		hb.append("<img src=", L2UI.SquareGray, " width=264 height=1>");
		hb.append("</center>");
		
		hb.append("</body></html>");
		sendHtml(player, null, hb);
	}
	
	private boolean defineClan(Player player)
	{
		try
		{
			String clanName = FakePlayerData.getClanName();
			
			Clan clan = ClanTable.getInstance().getClanByName(clanName);
			// si el clan no existe lo creamos
			if (clan == null)
			{
				clan = new Clan(IdFactory.getInstance().getNextId(), clanName);
				clan.setLevel(Rnd.get(3, 5));
				ClanMember leader = new ClanMember(clan, player);
				clan.setLeader(leader);
				leader.setPlayerInstance(player);
				storeClan(clan);
				player.setClan(clan);
				player.setPledgeClass(ClanMember.calculatePledgeClass(player));
				player.setClanPrivileges(Clan.CP_ALL);
				
				ClanTable.getInstance().addNewClan(clan);
				
				// player.sendPacket(new PledgeShowMemberListAll(clan, 0));
				// player.sendPacket(new UserInfo(player));
				// player.sendPacket(SystemMessageId.CLAN_CREATED);
				
				// definimos la crest del clan
				_crestId++;
				clan.changeClanCrest(_crestId);
			}
			else
			{
				// si existe el clan
				clan.addClanMember(player);
				player.setClan(clan);
				player.setPledgeClass(ClanMember.calculatePledgeClass(player));
				player.setClanPrivileges(clan.getRankPrivs(player.getPowerGrade()));
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return true;
	}
	
	private static boolean createShops()
	{
		boolean offlineShop = false;
		String type = "NONE";
		int chance = Rnd.get(100);
		
		if (chance < 10) // sellbuff -> 10%
		{
			type = "SELL_BUFF";
			offlineShop = true;
		}
		else if (chance < 30)// buy -> 20%
		{
			type = "BUY";
			offlineShop = true;
		}
		else if (chance < 50)// sell -> 20%
		{
			type = "SELL";
			offlineShop = true;
		}
		else if (chance < 80) // Ai -> 30%
		{
			type = "AI";
		}
		
		if (offlineShop)
		{
			
		}
		else
		{
			// EngineManager.onEvent(player, null, "OfflineShop " + shopItems); // shopItems -> price
		}
		
		return true;
	}
	
	private static Player createPlayer(Player admin, int offSet, int templateId, int lvl)
	{
		// playerName....buscamos hasta encontrar un nombre q no exista
		String playerName = FakePlayerData.getName();
		while (PlayerNameTable.getInstance().getPlayerObjectId(playerName) > 0)
		{
			playerName = FakePlayerData.getName();
		}
		
		// Account....buscamos hasta encontrar una cuenta q no exista
		String accountName = "fake" + Rnd.get(Integer.MAX_VALUE);
		
		for (PlayerHolder ph : PlayerData.getAllPlayers())
		{
			if (ph.getAccountName().equals(accountName))
			{
				accountName = "fake" + Rnd.get(Integer.MAX_VALUE);
			}
		}
		
		// Sexo -> random
		Sex sex = Sex.values()[Rnd.get(2)];
		// hairStyle -> random
		byte hairStyle = (byte) (sex == Sex.MALE ? Rnd.get(5) : Rnd.get(7));
		// Face -> random
		byte face = (byte) Rnd.get(3);
		
		// String shopType = "NONE";
		// Obtenemos el template del player segun la classId
		// ClassId -> random 0 - 118 (solo hasta segundo cambio de clase)
		// PcTemplate template = null;
		// va a vender sus buff asiq devemos sentarlo xD
		// if (shopType.equals("SELL_BUFF"))
		// {
		// int chance = Rnd.get(3);
		// switch (chance)
		// {
		// case 0:
		// template = CharTemplateTable.getInstance().getTemplate(98);// Hierophant
		// break;
		// case 1:
		// template = CharTemplateTable.getInstance().getTemplate(105);// EvaSaint
		// break;
		// case 2:
		// template = CharTemplateTable.getInstance().getTemplate(112);// ShillienSaint
		// break;
		// }
		// }
		
		PlayerTemplate template = CharTemplateTable.getInstance().getTemplate(templateId);
		
		List<Integer> items = new ArrayList<>();
		
		int chestId = template.getClassId().getType() != ClassType.FIGHTER ? FAKE_SET_MAGE[Rnd.get(FAKE_SET_MAGE.length - 1)] : FAKE_SET_WARRIOR[Rnd.get(FAKE_SET_WARRIOR.length - 1)];
		if (ArmorSetData.getInstance().getSet(chestId) != null)
		{
			// lo vestimos "SET"
			for (int it : ArmorSetData.getInstance().getSet(chestId).getSetItemsId())
			{
				items.add(it);
			}
		}
		else
		{
			System.out.println("chestId-> " + chestId + " no tiene ningun set asignado");
		}
		
		// lo vestimos "WEAPON"
		items.add(template.getClassId().getType() != ClassType.FIGHTER ? FAKE_WEAPON_MAGE[Rnd.get(FAKE_WEAPON_MAGE.length)] : FAKE_WEAPON_WARRIOR[Rnd.get(FAKE_WEAPON_WARRIOR.length)]);
		
		return UtilPlayer.createPlayer(admin, playerName, accountName, template, lvl, sex, hairStyle, face, items, offSet);
	}
	
	private static void storeClan(Clan clan)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO clan_data (clan_id,clan_name,clan_level,hasCastle,ally_id,ally_name,leader_id,crest_id,crest_large_id,ally_crest_id) values (?,?,?,?,?,?,?,?,?,?)"))
		{
			statement.setInt(1, clan.getClanId());
			statement.setString(2, clan.getName());
			statement.setInt(3, clan.getLevel());
			statement.setInt(4, clan.getCastleId());
			statement.setInt(5, clan.getAllyId());
			statement.setString(6, clan.getAllyName());
			statement.setInt(7, clan.getLeaderId());
			statement.setInt(8, clan.getCrestId());
			statement.setInt(9, clan.getCrestLargeId());
			statement.setInt(10, clan.getAllyCrestId());
			statement.execute();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
