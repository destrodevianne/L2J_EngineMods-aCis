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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import main.data.ConfigData;
import main.data.IconData;
import main.data.PlayerData;
import main.engine.AbstractMods;
import main.holders.AuctionItemHolder;
import main.holders.PlayerHolder;
import main.util.builders.html.Html;
import main.util.builders.html.HtmlBuilder;
import main.util.builders.html.HtmlBuilder.HtmlType;
import main.util.builders.html.L2UI;
import main.util.builders.html.L2UI_CH3;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.EtcItem;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.model.item.type.CrystalType;
import net.sf.l2j.gameserver.model.item.type.EtcItemType;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage.SMPOS;

/**
 * @author fissban
 */
public class MemoCommunityBoard extends AbstractMods
{
	public MemoCommunityBoard()
	{
		registerMod(ConfigData.ENABLE_BBS_MEMO);
	}
	
	@Override
	public void onModState()
	{
		switch (getState())
		{
			case START:
				loadAllItems();
				break;
			
			case END:
				break;
		}
	}
	
	private void loadAllItems()
	{
		for (PlayerHolder ph : PlayerData.getAllPlayers())
		{
			try
			{
				// FIXME hasta 100 items se pueden maximo y ya es mucho aunq se puede definir lo q se quiera
				
				// se leen los items vendidos -------------------------------------------------------------------------------
				for (int i = 1; i <= 100; i++)
				{
					String auction = getValueDB(ph.getObjectId(), "auctionSold " + i);
					if (auction != null)
					{
						ph.addAuctionSold(i, new AuctionItemHolder(auction));
					}
				}
				// se leen los items a la venta ------------------------------------------------------------------------------
				for (int i = 1; i <= 100; i++)
				{
					String auction = getValueDB(ph.getObjectId(), "auctionSell " + i);
					if (auction != null)
					{
						ph.addAuctionSell(i, new AuctionItemHolder(auction));
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	private void removeSold(PlayerHolder ph, int key)
	{
		removeValueDB(ph.getObjectId(), "auctionSold " + key);
		ph.removeAuctionSold(key);
	}
	
	private void removeSell(PlayerHolder ph, int key)
	{
		removeValueDB(ph.getObjectId(), "auctionSell " + key);
		ph.removeAuctionSell(key);
	}
	
	@Override
	public void onEnterWorld(Player player)
	{
		PlayerHolder ph = PlayerData.get(player);
		
		for (AuctionItemHolder ih : ph.getAuctionsSold().values())
		{
			// remove from db and memory
			removeSold(ph, ih.getkey());
			
			giveAuctionSold(player, ih);
		}
	}
	
	@Override
	public boolean onCommunityBoard(Player player, String command)
	{
		if (command.startsWith("_bbsmemo"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			// _bbsmemo
			st.nextToken();
			// bypass
			String bypass = st.hasMoreTokens() ? st.nextToken() : "allAuctions";
			
			HtmlBuilder hb = new HtmlBuilder(HtmlType.COMUNITY_TYPE);
			hb.append(Html.HTML_START);
			hb.append("<br>");
			hb.append("<center>");
			hb.append(marcButton(bypass));
			hb.append("<table>");
			hb.append("<tr>");
			hb.append("<td><button value=\"ALL AUCTIONS\" action=\"bypass _bbsmemo allAuctions\" width=100 height=32 back=", L2UI_CH3.refinegrade3_21, " fore=", L2UI_CH3.refinegrade3_22, "></td>");
			hb.append("<td><button value=\"MY AUCTION\" action=\"bypass _bbsmemo myAuctions\" width=100 height=32 back=", L2UI_CH3.refinegrade3_21, " fore=", L2UI_CH3.refinegrade3_22, "></td>");
			hb.append("<td><button value=\"AUCTION ITEM\" action=\"bypass _bbsmemo setAuction\" width=100 height=32 back=", L2UI_CH3.refinegrade3_21, " fore=", L2UI_CH3.refinegrade3_22, "></td>");
			hb.append("</tr>");
			hb.append("</table>");
			hb.append(marcButton(bypass));
			hb.append("<br>");
			
			hb.append("<br>");
			
			switch (bypass)
			{
				case "buyItemConfirm":
				{
					try
					{
						PlayerHolder phOwner = PlayerData.get(Integer.parseInt(st.nextToken()));
						int key = Integer.parseInt(st.nextToken());
						
						hb.append(buyItemConfirm(player, phOwner, key));
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					break;
				}
				case "buyItem":
				{
					try
					{
						PlayerHolder phOwner = PlayerData.get(Integer.parseInt(st.nextToken()));
						int key = Integer.parseInt(st.nextToken());
						
						hb.append(buyItem(player, phOwner, key));
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					
					break;
				}
				case "sellItem":
				{
					int itemObjId = 0;
					int itemCount = 1;
					int itemPriceCount = 0;
					int itemPriceId = 57;
					
					try
					{
						itemObjId = Integer.parseInt(st.nextToken());
						itemCount = Integer.parseInt(st.nextToken());
						itemPriceCount = Integer.parseInt(st.nextToken());
						
						// Agregar cada case por cada item q pongamos nuevo
						switch (st.nextToken())
						{
							case "Adena":
								itemPriceId = 57;
								break;
						}
						
						hb.append(sellItem(player, itemObjId, itemCount, itemPriceCount, itemPriceId));
					}
					catch (Exception e)
					{
						// hb.append("bypass? olvidate!");
						// e.printStackTrace();
						// es solo por si dejan algun espacio vacio
						// De todas maneras falta generar la pag para confirnar la venta
						break;
					}
				}
				case "cancelSell":
				{
					if (st.hasMoreTokens())
					{
						PlayerHolder ph = PlayerData.get(player);
						
						int key = Integer.parseInt(st.nextToken());
						
						AuctionItemHolder ih = ph.getAuctionsSell().get(key);
						
						if (ih != null)
						{
							// check if item owner its player
							if (ih.getOwnerId() == player.getObjectId())
							{
								// remove from memory and db
								removeSell(ph, key);
								// create item
								ItemInstance itemBuy = ItemTable.getInstance().createItem("auction buy", ih.getItemId(), 1, player, player);// creamos el item
								itemBuy.setEnchantLevel(ih.getItemEnchantLevel());// se define el lvl del enchant
								player.addItem("auction buy", itemBuy, player, true);// se entrega al player
							}
							else
							{
								hb.append("bypass? xD");
							}
						}
						
					}
				}
				case "myAuctions":
				{
					hb.append(getMyAuctions(player));
					break;
				}
				// mostramos el listado de items q podra vender
				case "setAuction":
				{
					int page = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 1;
					hb.append(getSellList(player, page));
					break;
				}
				case "allAuctions":
				{
					// item type
					String itemType = st.hasMoreTokens() ? st.nextToken() : "all";
					// grade
					String grade = st.hasMoreTokens() ? st.nextToken() : "ALL";
					// page
					int page = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 1;
					
					hb.append(allAuctions(bypass, grade, page, itemType));
					break;
				}
				
			}
			hb.append("</center>");
			hb.append(Html.HTML_END);
			sendCommunity(player, hb.toString());
			return true;
		}
		
		return false;
	}
	
	// XXX HTML -----------------------------------------------------
	private static String getMyAuctions(Player player)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.COMUNITY_TYPE);
		
		int color = 0;
		
		PlayerHolder ph = PlayerData.get(player);
		hb.append("<br1>");
		hb.append("Si cancelas la venta de un item no se te devolvera<br1>");
		hb.append("la comision inicial que te cobramos<br>");
		
		hb.append(Html.newImage(L2UI.SquareGray, 600, 1));
		// recorremos todos los items del inventario q se pueden comerciar.
		for (Entry<Integer, AuctionItemHolder> entry : ph.getAuctionsSell().entrySet())
		{
			AuctionItemHolder holder = entry.getValue();
			int id = entry.getKey();
			
			if (player.getObjectId() != holder.getOwnerId())
			{
				continue;
			}
			
			ItemInstance item = ItemTable.getInstance().createDummyItem(holder.getItemId());
			
			hb.append("<table ", color % 2 == 0 ? "bgcolor=000000 " : "", "cellspacing=0 cellpadding=0 width=600 height=56>");
			hb.append("<tr>");
			hb.append("<td width=32>", Html.newImage(IconData.getIconByItemId(item.getItemId()), 32, 32), "</td>");
			hb.append("<td width=489>");
			
			hb.append("<table cellspacing=0 cellpadding=0 width=489 height=56>");
			hb.append("<tr><td>", Html.newFontColor("FF8000", "Name: "), item.getName(), "</td></tr>");
			hb.append("<tr><td>", Html.newFontColor("FF8000", "Grade: "), item.getItem().getCrystalType().toString(), "</td></tr>");
			hb.append("<tr><td>", Html.newFontColor("FF8000", "Enchant: "), "+", holder.getItemEnchantLevel(), "</td></tr>");
			hb.append("<tr><td>", Html.newFontColor("FF8000", "count: "), "+", holder.getItemCount(), "</td></tr>");
			hb.append("</table");
			
			hb.append("</td>");
			hb.append("<td width=75><button value=CANCEL action=\"bypass _bbsmemo cancelSell ", id, "\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, "></td>");
			hb.append("</tr>");
			hb.append("</table>");
			hb.append(Html.newImage(L2UI.SquareGray, 600, 1));
			color++;
		}
		
		return hb.toString();
	}
	
	private static String allAuctions(String bypass, String grade, int page, String itemType)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.COMUNITY_TYPE);
		
		// aca pondremos los grados de los items q se quieren filtrar
		hb.append("<table width=600 height=24 cellspacing=0 cellpadding=0>");
		hb.append("<tr>");
		// hb.append("<td width=126 height=24></td>");
		hb.append("<td width=18 align=right>", grade.equals("ALL") ? Html.newImage(L2UI_CH3.ps_sizecontrol2_over, 16, 16) : "", "</td><td width=64><button value=ALL action=\"bypass _bbsmemo ", bypass, " ", itemType, " ALL ", page, "\" width=64 height=22 back=", L2UI_CH3.herochat_tab2, " fore=", L2UI_CH3.herochat_tab2_over, "></td>");
		hb.append("<td width=18 align=right>", grade.equals("NONE") ? Html.newImage(L2UI_CH3.ps_sizecontrol2_over, 16, 16) : "", "</td><td width=64><button value=NO action=\"bypass _bbsmemo ", bypass, " ", itemType, " NONE ", page, "\" width=64 height=22 back=", L2UI_CH3.herochat_tab2, " fore=", L2UI_CH3.herochat_tab2_over, "></td>");
		hb.append("<td width=18 align=right>", grade.equals("D") ? Html.newImage(L2UI_CH3.ps_sizecontrol2_over, 16, 16) : "", "</td><td width=64><button value=D action=\"bypass _bbsmemo ", bypass, " ", itemType, " D ", page, "\" width=64 height=22 back=", L2UI_CH3.herochat_tab2, " fore=", L2UI_CH3.herochat_tab2_over, "></td>");
		hb.append("<td width=18 align=right>", grade.equals("C") ? Html.newImage(L2UI_CH3.ps_sizecontrol2_over, 16, 16) : "", "</td><td width=64><button value=C action=\"bypass _bbsmemo ", bypass, " ", itemType, " C ", page, "\" width=64 height=22 back=", L2UI_CH3.herochat_tab2, " fore=", L2UI_CH3.herochat_tab2_over, "></td>");
		hb.append("<td width=18 align=right>", grade.equals("B") ? Html.newImage(L2UI_CH3.ps_sizecontrol2_over, 16, 16) : "", "</td><td width=64><button value=B action=\"bypass _bbsmemo ", bypass, " ", itemType, " B ", page, "\" width=64 height=22 back=", L2UI_CH3.herochat_tab2, " fore=", L2UI_CH3.herochat_tab2_over, "></td>");
		hb.append("<td width=18 align=right>", grade.equals("A") ? Html.newImage(L2UI_CH3.ps_sizecontrol2_over, 16, 16) : "", "</td><td width=64><button value=A action=\"bypass _bbsmemo ", bypass, " ", itemType, " A ", page, "\" width=64 height=22 back=", L2UI_CH3.herochat_tab2, " fore=", L2UI_CH3.herochat_tab2_over, "></td>");
		hb.append("<td width=18 align=right>", grade.equals("S") ? Html.newImage(L2UI_CH3.ps_sizecontrol2_over, 16, 16) : "", "</td><td width=64><button value=S action=\"bypass _bbsmemo ", bypass, " ", itemType, " S ", page, "\" width=64 height=22 back=", L2UI_CH3.herochat_tab2, " fore=", L2UI_CH3.herochat_tab2_over, "></td>");
		hb.append("</tr>");
		hb.append("</table>");
		
		hb.append("<table cellspacing=0 cellpadding=0>");
		hb.append("<tr>");
		// menu izquierdo -------------------------------------------------------------------------------------------------------
		hb.append("<td align=center>");
		
		hb.append("<table width=126 height=22 cellspacing=0 cellpadding=0>");
		hb.append("<tr>");
		hb.append("<td width=16 height=22 valign=top align=center>", Html.newImage(L2UI_CH3.FrameBackLeft, 16, 22), "</td>");
		hb.append("<td width=94 height=22 valign=top align=center>", Html.newImage(L2UI_CH3.FrameBackMid, 94, 22), "</td>");
		hb.append("<td width=16 height=22 valign=top align=center>", Html.newImage(L2UI_CH3.FrameBackRight, 16, 22), "</td>");
		hb.append("</tr>");
		hb.append("</table>");
		
		hb.append("<table cellspacing=3 cellpadding=0>");
		hb.append(menuItemType(bypass + " all " + grade + " " + page, "all", itemType));
		hb.append(menuItemType(bypass + " weapon " + grade + " " + page, "weapon", itemType));
		hb.append(menuItemType(bypass + " chest " + grade + " " + page, "chest", itemType));
		hb.append(menuItemType(bypass + " legs " + grade + " " + page, "legs", itemType));
		hb.append(menuItemType(bypass + " head " + grade + " " + page, "head", itemType));
		hb.append(menuItemType(bypass + " glove " + grade + " " + page, "glove", itemType));
		hb.append(menuItemType(bypass + " boot " + grade + " " + page, "boot", itemType));
		hb.append(menuItemType(bypass + " shield " + grade + " " + page, "shield", itemType));
		hb.append(menuItemType(bypass + " jewel " + grade + " " + page, "jewel", itemType));
		hb.append(menuItemType(bypass + " scroll " + grade + " " + page, "scroll", itemType));
		hb.append(menuItemType(bypass + " misc " + grade + " " + page, "misc", itemType));
		hb.append("</table>");
		
		hb.append("</td>");
		// menu derecho -------------------------------------------------------------------------------------------------------
		hb.append("<td align=center>");
		
		hb.append("<table width=474 height=22 cellspacing=0 cellpadding=0>");
		hb.append("<tr>");
		hb.append("<td width=16 height=22 valign=top align=center>", Html.newImage(L2UI_CH3.FrameBackLeft, 16, 22), "</td>");
		hb.append("<td width=442 height=22 valign=top align=center>", Html.newImage(L2UI_CH3.FrameBackMid, 442, 22), "</td>");
		hb.append("<td width=16 height=22 valign=top align=center>", Html.newImage(L2UI_CH3.FrameBackRight, 16, 22), "</td>");
		hb.append("</tr>");
		hb.append("</table>");
		
		// aca tenemos q recorrer todos los items
		hb.append(getAuctionItems(bypass, itemType, grade, page));
		
		hb.append("</td>");
		// --------------------------------------------------------------------------------------------------------------------
		hb.append("</tr>");
		hb.append("</table>");
		
		return hb.toString();
	}
	
	private String sellItem(Player player, int itemObjId, int itemCount, int itemPriceCount, int itemPriceId)
	{
		// prevenimos numeros negativos
		if (itemPriceCount < 0)
		{
			itemPriceCount = 0;
		}
		
		PlayerHolder ph = PlayerData.get(player);
		
		if (ph.getAuctionsSell().size() > 100)
		{
			return "<br><br><br>has superado el maximo de items permitido de 100.";
		}
		
		if (!player.destroyItemByItemId("start auction", ConfigData.COMMISION_FOR_START_SELL_ID, ConfigData.COMMISION_FOR_START_SELL, player, false))
		{
			return "<br><br><br>No tienes suficiente para la comision inicial";
		}
		
		if (player.getInventory().getItemByObjectId(itemObjId).getCount() < itemCount)
		{
			return "<br><br><br>You do not have so many items to sell";
		}
		
		ItemInstance item = player.getInventory().destroyItem("auction", itemObjId, itemCount, player, player);
		
		// prevenimos posible bypass
		if (item != null)
		{
			// se busca un key no usado para los personajes
			int id = getNewKey(ph.getAuctionsSell());
			if (id == -1)
			{
				return "<br><br><br>ufff un error al obtener el nuevo id del item ";
			}
			
			AuctionItemHolder ash = new AuctionItemHolder(id, player.getObjectId(), itemObjId, item.getItemId(), itemCount, item.getEnchantLevel(), itemPriceCount, itemPriceId);
			// salvamos el item en la DB
			ph.addAuctionSell(id, ash);
			setValueDB(player.getObjectId(), "auctionSell " + id, ash.toString());
		}
		
		return "";
	}
	
	private String buyItem(Player player, PlayerHolder phOwner, int key)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.COMUNITY_TYPE);
		
		if (!phOwner.getAuctionsSell().containsKey(key))
		{
			// posible bypass
			hb.append("<br><br><br>");
			hb.append("Que demonios estas intentando hacer???");
		}
		else
		{
			AuctionItemHolder ih = phOwner.getAuctionsSell().get(key);
			hb.append("<br><br><br>");
			hb.append("<table cellspacing=0 cellpadding=0 height=80 width=600>");
			hb.append("<tr><td height=20 align=center>");
			hb.append(Html.newFontColor("LEVEL", "Estas seguro que deseas comprar???"));
			hb.append("</td></tr>");
			
			hb.append("<tr><td height=60 align=center>");
			ItemInstance item = ItemTable.getInstance().createDummyItem(ih.getItemId());
			
			hb.append("<table bgcolor=000000 cellspacing=0 cellpadding=0 width=474>");
			hb.append("<tr>");
			hb.append("<td fixwidth=32>", Html.newImage(IconData.getIconByItemId(item.getItemId()), 32, 32), "</td>");
			hb.append("<td align=center>");
			
			hb.append("<table cellspacing=0 cellpadding=0 width=337 height=60>");
			hb.append("<tr><td fixwidth=337>", Html.newFontColor("FF8000", "Name: "), item.getName(), " - ", item.getItem().getCrystalType().toString(), "</td></tr>");
			hb.append("<tr><td fixwidth=337>", Html.newFontColor("FF8000", "Enchant: "), "+", ih.getItemEnchantLevel(), "</td></tr>");
			hb.append("<tr><td fixwidth=337>", Html.newFontColor("FF8000", "Price: "), Html.formatAdena(ih.getItemPriceCount()), " ", ItemTable.getInstance().getTemplate(ih.getItemPriceId()).getName(), "</td></tr>");
			hb.append("<tr><td fixwidth=337>", Html.newFontColor("FF8000", "Owner: "), phOwner.getName(), "</td></tr>");
			hb.append("</table>");
			
			hb.append("</td></tr>");
			hb.append("</table>");
			
			hb.append("</td></tr>");
			hb.append("<tr><td align=center>");
			hb.append("<br>");
			hb.append("<table cellspacing=0 cellpadding=0>");
			hb.append("<tr>");
			hb.append("<td><button value=\"Yes\" action=\"bypass _bbsmemo buyItemConfirm ", phOwner.getObjectId(), " ", key, "\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, "></td>");
			hb.append("<td><button value=\"No\" action=\"bypass _bbsmemo allAuctions\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, "></td>");
			hb.append("</tr>");
			hb.append("</table>");
			
			hb.append("</td></tr>");
			hb.append("</table>");
		}
		
		return hb.toString();
	}
	
	private String buyItemConfirm(Player player, PlayerHolder phOwner, int key)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.COMUNITY_TYPE);
		if (!phOwner.getAuctionsSell().containsKey(key))
		{
			// posible bypass
			hb.append("<br><br><br>");
			hb.append("Que demonios estas intentando hacer???");
		}
		else
		{
			// obtenemos el valor de la memoria
			AuctionItemHolder ih = phOwner.getAuctionsSell().get(key);
			
			// chequeamos de q el comprador tenga lo suficiente para comprar el item
			int countBuy = player.getInventory().getInventoryItemCount(ih.getItemPriceId(), -1, false);
			
			if (countBuy < ih.getItemPriceCount())
			{
				hb.append("<br><br><br>");
				hb.append("Lo siento, te faltan recursos para comprar este item???");
				return hb.toString();
			}
			
			// descontamos el precio de la compra
			player.getInventory().destroyItemByItemId("auction buy", ih.getItemPriceId(), ih.getItemPriceCount(), player, player);
			
			// entregamos el item al comprador.
			ItemInstance itemBuy = ItemTable.getInstance().createItem("auction buy", ih.getItemId(), 1, player, player);// creamos el item
			itemBuy.setEnchantLevel(ih.getItemEnchantLevel());// definimos el lvl del enchant
			player.addItem("auction buy", itemBuy, player, true);// se lo entregamos al player
			
			Player owner = World.getInstance().getPlayer(phOwner.getObjectId());
			
			// se entrega la adena de la venta al duenio.
			if (owner == null)
			{
				// se busca un key no usado para el personaje
				int id = getNewKey(phOwner.getAuctionsSold());
				
				if (id == -1)
				{
					return "<br><br><br>ufff un error al obtener el nuevo id del item ";
				}
				
				ih.setKey(id);
				
				// se incrementa la cantidad de items vendidos a entregar cuando se conecte
				phOwner.addAuctionSold(ih.getkey(), ih);
				setValueDB(phOwner.getObjectId(), "auctionSold " + ih.getkey(), ih.toString());
			}
			else
			{
				giveAuctionSold(owner, ih);
			}
			
			// se remueve el valor de la memoria
			removeSell(phOwner, ih.getkey());
			
			// TODO podriamos crear la parte del historial no?
			
			hb.append("<br><br><br>");
			hb.append("Felicitaciones tu compra se realizo con exito");
		}
		
		return hb.toString();
	}
	
	private static String getSellList(Player player, int page)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.COMUNITY_TYPE);
		
		hb.append("<br1>");
		hb.append("Ten en cuenta que por poner un item a la venta<br1>");
		hb.append("se te va a cobrar ", Html.formatAdena(ConfigData.COMMISION_FOR_START_SELL), " de ", ItemTable.getInstance().getTemplate(ConfigData.COMMISION_FOR_START_SELL_ID).getName(), "<br1>");
		hb.append("y al momento de la venta nos quedamos con<br1>");
		hb.append("el ", ConfigData.COMMISION_FOR_END_SELL, "% del total.<br>");
		
		hb.append(Html.newImage(L2UI.SquareGray, 600, 1));
		// recorremos todos los items del inventario q se pueden comerciar.
		
		int MAX_PER_PAGE = 6;
		int searchPage = MAX_PER_PAGE * (page - 1);
		int count = 0;
		int color = 0;
		
		ItemInstance[] list = player.getInventory().getAvailableItems(false, true);
		
		for (ItemInstance item : list)
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
			
			// TODO si se quiere filtrar los items q se pueden vender aqui es el lugar!
			
			// De los etcItems solo dejaremos q vendan los enchants
			// if (item.getItemType() instanceof EtcItemType)
			// {
			// if (item.getItemType() != EtcItemType.SCRL_ENCHANT_WP && item.getItemType() != EtcItemType.SCRL_ENCHANT_AM && item.getItemType() != EtcItemType.BLESS_SCRL_ENCHANT_AM && item.getItemType() != EtcItemType.BLESS_SCRL_ENCHANT_WP)
			// {
			// continue;
			// }
			// }
			
			hb.append("<table ", color % 2 == 0 ? "bgcolor=000000 " : "", "cellspacing=0 cellpadding=0 width=600 height=42>");
			hb.append("<tr>");
			hb.append("<td width=32>", Html.newImage(IconData.getIconByItemId(item.getItemId()), 32, 32), "</td>");
			hb.append("<td width=300>");
			
			hb.append("<table cellspacing=0 cellpadding=0 width=300 height=42>");
			hb.append("<tr><td>", Html.newFontColor("FF8000", "Name: "), item.getName(), "</td></tr>");
			hb.append("<tr><td>", Html.newFontColor("FF8000", "Grade: "), item.getItem().getCrystalType().toString(), "</td></tr>");
			hb.append("<tr><td>", Html.newFontColor("FF8000", "Enchant: "), "+", item.getEnchantLevel(), "</td></tr>");
			hb.append("</table");
			
			hb.append("</td>");
			
			hb.append("<td width=50>Count:</td>");
			hb.append("<td width=50><edit var=\"count", color, "\" width=50></td>");
			
			hb.append("<td width=50>Price:</td>");
			hb.append("<td width=50><edit var=\"price", color, "\" width=50></td>");
			hb.append("<td width=50><combobox width=50 var=item", color, " list=Adena;coin1;coin2;coin3;></td>");
			hb.append("<td width=50><button value=SELL action=\"bypass _bbsmemo sellItem ", item.getObjectId(), " $count", color, " $price", color, " $item", color, "\" width=50 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, "></td>");
			hb.append("</tr>");
			hb.append("</table>");
			hb.append(Html.newImage(L2UI.SquareGray, 600, 1));
			color++;
			count++;
		}
		
		hb.append(Html.newImage(L2UI.SquareWhite, 474, 1));
		hb.append("<table>");
		hb.append("<tr>");
		int currentPage = 1;
		int size = list.length;
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
					hb.append("<td width=20><a action=\"bypass _bbsmemo setAuction ", currentPage, "\">", currentPage, "</a></td>");
				}
				currentPage++;
			}
		}
		
		hb.append("</tr>");
		hb.append("</table>");
		
		return hb.toString();
	}
	
	private static String getAuctionItems(String bypass, String itemType, String grade, int page)
	{
		List<AuctionItemHolder> itemList = new ArrayList<>();
		
		for (PlayerHolder ph : PlayerData.getAllPlayers())
		{
			for (AuctionItemHolder holder : ph.getAuctionsSell().values())
			{
				ItemInstance it = ItemTable.getInstance().createDummyItem(holder.getItemId());
				
				switch (grade)
				{
					case "ALL":
					{
						if (searchItemByType(itemType, it))
						{
							itemList.add(holder);
						}
						
						break;
					}
					default:
					{
						CrystalType cType = CrystalType.valueOf(grade);
						
						if (it.getItem().getCrystalType() == cType)
						{
							if (searchItemByType(itemType, it))
							{
								itemList.add(holder);
							}
						}
						
						break;
					}
				}
			}
		}
		
		HtmlBuilder hb = new HtmlBuilder(HtmlType.COMUNITY_TYPE);
		
		if (!itemList.isEmpty())
		{
			int MAX_PER_PAGE = 5;
			int searchPage = MAX_PER_PAGE * (page - 1);
			int count = 0;
			int color = 0;
			
			hb.append(Html.newImage(L2UI.SquareGray, 474, 1));
			hb.append("<table width=474 height=294 cellspacing=0 cellpadding=0>");
			hb.append("<tr>");
			hb.append("<td valign=top>");
			
			for (AuctionItemHolder holder : itemList)
			{
				ItemInstance item = ItemTable.getInstance().createDummyItem(holder.getItemId());
				
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
				
				hb.append("<table ", color % 2 == 0 ? "bgcolor=000000 " : "", "cellspacing=0 cellpadding=0 width=474 height=56>");
				hb.append("<tr>");
				hb.append("<td fixwidth=32>", Html.newImage(IconData.getIconByItemId(item.getItemId()), 32, 32), "</td>");
				hb.append("<td height=56>");
				
				hb.append("<table cellspacing=0 cellpadding=0 width=337 height=56>");
				hb.append("<tr><td fixwidth=337>", Html.newFontColor("FF8000", "Name: "), item.getName(), " - ", item.getItem().getCrystalType().toString(), " - (", holder.getItemCount(), ")</td></tr>");
				hb.append("<tr><td fixwidth=337>", Html.newFontColor("FF8000", "Enchant: "), "+", holder.getItemEnchantLevel(), "</td></tr>");
				hb.append("<tr><td fixwidth=337>", Html.newFontColor("FF8000", "Price: "), Html.formatAdena(holder.getItemPriceCount()), " ", ItemTable.getInstance().getTemplate(holder.getItemPriceId()).getName(), "</td></tr>");
				hb.append("<tr><td fixwidth=337>", Html.newFontColor("FF8000", "Owner: "), PlayerData.get(holder.getOwnerId()).getName(), "</td></tr>");
				hb.append("</table>");
				
				hb.append("</td>");
				hb.append("<td width=105><button value=BUY action=\"bypass _bbsmemo buyItem ", holder.getOwnerId(), " ", holder.getkey(), "\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, "></td>");
				hb.append("</tr>");
				hb.append("</table>");
				hb.append(Html.newImage(L2UI.SquareGray, 474, 1));
				
				color++;
				count++;
			}
			
			hb.append("</td>");
			hb.append("</tr>");
			hb.append("</table>");
			
			hb.append("<br>");
			hb.append(Html.newImage(L2UI.SquareWhite, 474, 1));
			hb.append("<table bgcolor=000000 cellspacing=0 cellpadding=0>");
			hb.append("<tr>");
			
			int currentPage = 1;
			int size = itemList.size();
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
						hb.append("<td width=20><a action=\"bypass _bbsmemo ", bypass, " ", itemType, " ", grade, " ", currentPage, "\">", currentPage, "</a></td>");
					}
					currentPage++;
				}
			}
			
			hb.append("</tr>");
			hb.append("</table>");
			hb.append(Html.newImage(L2UI.SquareWhite, 474, 1));
			hb.append("<br>");
		}
		else
		{
			hb.append("<center>No items for sale</center>");
		}
		
		hb.append("<button value=\"Refresh\" action=\"bypass _bbsmemo ", bypass, " ", itemType, " ", grade, " ", page, "\" width=75 height=21 back=", L2UI_CH3.Btn1_normalOn, " fore=", L2UI_CH3.Btn1_normal, ">");
		
		return hb.toString();
	}
	
	private static String menuItemType(String bypass, String itemType, String actualItemType)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.COMUNITY_TYPE);
		hb.append("<tr>");
		hb.append("<td width=16 height=22>", actualItemType.equals(itemType) ? Html.newImage(L2UI_CH3.ps_sizecontrol2_over, 16, 16) : "", "</td>");
		hb.append("<td><button value=\"", itemType, "\" action=\"bypass _bbsmemo ", bypass, "\" width=95 height=22 back=", L2UI_CH3.bigbutton_down, " fore=", L2UI_CH3.bigbutton, "></td>");
		hb.append("</tr>");
		return hb.toString();
	}
	
	private static String marcButton(String bypass)
	{
		HtmlBuilder hb = new HtmlBuilder(HtmlType.COMUNITY_TYPE);
		if (bypass != null)
		{
			hb.append("<table cellspacing=0 cellpadding=0>");
			hb.append("<tr>");
			hb.append("<td>", Html.newImage(bypass.equals("allAuctions") ? L2UI_CH3.fishing_bar1 : L2UI_CH3.ssq_cell1, 100, 1), "</td>");
			hb.append("<td>", Html.newImage(bypass.equals("myAuctions") ? L2UI_CH3.fishing_bar1 : L2UI_CH3.ssq_cell1, 100, 1), "</td>");
			hb.append("<td>", Html.newImage(bypass.equals("setAuction") ? L2UI_CH3.fishing_bar1 : L2UI_CH3.ssq_cell1, 100, 1), "</td>");
			hb.append("</tr>");
			hb.append("</table>");
		}
		else
		{
			hb.append(Html.newImage(L2UI.SquareGray, 506, 1));
		}
		return hb.toString();
	}
	
	// XXX MISC --------------------------------------------------------------------------------------------------------
	private static boolean searchItemByType(String itemType, ItemInstance it)
	{
		switch (itemType)
		{
			case "all":
			{
				return true;
			}
			case "weapon":
			{
				if (it.getItem() instanceof Weapon)
				{
					return true;
				}
				break;
			}
			case "chest":
			{
				if (it.getItem().getBodyPart() == Item.SLOT_CHEST || it.getItem().getBodyPart() == Item.SLOT_FULL_ARMOR)
				{
					return true;
				}
				break;
			}
			case "legs":
			{
				if (it.getItem().getBodyPart() == Item.SLOT_LEGS)
				{
					return true;
				}
				break;
			}
			case "head":
			{
				if (it.getItem().getBodyPart() == Item.SLOT_HEAD)
				{
					return true;
				}
				break;
			}
			case "glove":
			{
				if (it.getItem().getBodyPart() == Item.SLOT_GLOVES)
				{
					return true;
				}
				break;
			}
			case "boot":
			{
				if (it.getItem().getBodyPart() == Item.SLOT_FEET)
				{
					return true;
				}
				break;
			}
			case "shield":
			{
				if (it.getItem().getBodyPart() == Item.TYPE1_SHIELD_ARMOR)
				{
					return true;
				}
				break;
			}
			case "jewel":
			{
				switch (it.getItem().getBodyPart())
				{
					case Item.SLOT_R_EAR:
					case Item.SLOT_L_EAR:
					case Item.SLOT_LR_EAR:
					case Item.SLOT_NECK:
					case Item.SLOT_R_FINGER:
					case Item.SLOT_L_FINGER:
					case Item.SLOT_LR_FINGER:
						return true;
				}
				break;
			}
			case "scroll":
			{
				if (it.getItem().getItemType() == EtcItemType.BLESS_SCRL_ENCHANT_WP || it.getItem().getItemType() == EtcItemType.BLESS_SCRL_ENCHANT_AM || it.getItem().getItemType() == EtcItemType.SCRL_ENCHANT_WP || it.getItem().getItemType() == EtcItemType.SCRL_ENCHANT_AM)
				{
					return true;
				}
				break;
			}
			case "misc":
			{
				if (it.getItem() instanceof EtcItem)
				{
					if (it.getItem().getItemType() == EtcItemType.BLESS_SCRL_ENCHANT_WP || it.getItem().getItemType() == EtcItemType.BLESS_SCRL_ENCHANT_AM || it.getItem().getItemType() == EtcItemType.SCRL_ENCHANT_WP || it.getItem().getItemType() == EtcItemType.SCRL_ENCHANT_AM)
					{
						return false;
					}
					
					return true;
				}
				break;
			}
		}
		
		return false;
	}
	
	private static void giveAuctionSold(Player owner, AuctionItemHolder ih)
	{
		// template del item
		Item item = ItemTable.getInstance().getTemplate(ih.getItemId());
		// mensajes
		owner.sendPacket(new CreatureSay(0, Say2.TELL, "Auction", "Has vendido el item " + item.getName()));
		owner.sendPacket(new ExShowScreenMessage("Has vendido el item " + item.getName(), 10000, SMPOS.TOP_CENTER, false));
	}
	
	private static int getNewKey(Map<Integer, AuctionItemHolder> map)
	{
		// se busca un key no usado para el personaje
		int id = -1;
		for (int i = 1; i <= 100; i++)
		{
			if (!map.containsKey(i))
			{
				id = i;
				break;
			}
		}
		
		return id;
	}
}
