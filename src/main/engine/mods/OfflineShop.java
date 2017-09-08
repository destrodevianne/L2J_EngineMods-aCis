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

import java.util.List;
import java.util.logging.Level;

import main.EngineModsManager;
import main.data.ConfigData;
import main.data.PlayerData;
import main.engine.AbstractMods;
import main.holders.PlayerHolder;
import main.util.Util;
import main.util.UtilPlayer;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.gameserver.model.L2ManufactureItem;
import net.sf.l2j.gameserver.model.L2ManufactureList;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.instance.Player.StoreType;
import net.sf.l2j.gameserver.model.group.Party.MessageType;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.model.tradelist.TradeItem;
import net.sf.l2j.gameserver.model.zone.ZoneId;

/**
 * @author fissban
 */
public class OfflineShop extends AbstractMods
{
	public OfflineShop()
	{
		if (ConfigData.OFFLINE_TRADE_ENABLE || ConfigData.OFFLINE_SELLBUFF_ENABLE)
		{
			registerMod(true);
		}
	}
	
	@Override
	public void onModState()
	{
		switch (getState())
		{
			case START:
			{
				restoreAllOfflineShops();
				clearValueDB();
				break;
			}
			case END:
			{
				break;
			}
		}
	}
	
	@Override
	public void onShutDown()
	{
		for (Player player : World.getInstance().getPlayers())
		{
			try
			{
				boolean saveValue = false;
				// saved state in memory
				String title = "";
				String storeItems = "";
				String storeType = "";
				
				if (player.isInStoreMode() && ConfigData.OFFLINE_TRADE_ENABLE)
				{
					storeType = player.getStoreType().name();
					
					switch (player.getStoreType())
					{
						case BUY:
							title = player.getBuyList().getTitle();
							for (TradeItem item : player.getBuyList().getItems())
							{
								// ItemId,count,price;
								storeItems += item.getItem().getItemId() + "," + item.getCount() + "," + item.getPrice() + ";";
							}
							break;
						case MANUFACTURE:
							title = player.getCreateList().getStoreName();
							for (L2ManufactureItem item : player.getCreateList().getList())
							{
								// recipeId,cost;
								storeItems += item.getRecipeId() + "," + item.getCost() + ";";
							}
							break;
						case PACKAGE_SELL:
						case SELL:
							title = player.getSellList().getTitle();
							for (TradeItem item : player.getSellList().getItems())
							{
								// objectId,count,price
								storeItems += item.getObjectId() + "," + item.getCount() + "," + item.getPrice() + ";";
							}
							break;
						default:
							System.out.println("NPE ->" + player.getStoreType().name());
							return;
					}
					
					saveValue = true;
				}
				else if (PlayerData.get(player).isSellBuff() && ConfigData.OFFLINE_SELLBUFF_ENABLE)
				{
					title = "SellBuff"; // TODO You could add to save this data as something custom.
					storeItems += PlayerData.get(player).getSellBuffPrice();
					storeType = "SELL_BUFF";
					saveValue = true;
				}
				
				if (saveValue)
				{
					// saved state and items in memory
					setValueDB(player.getObjectId(), "offlineShop", storeType + "#" + (title == null || title.length() == 0 ? "null" : title.replaceAll("#", " ")) + "#" + storeItems);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void onEnterWorld(Player player)
	{
		if (PlayerData.get(player).isOffline())
		{
			PlayerData.get(player).setOffline(false);
		}
	}
	
	@Override
	public boolean onExitWorld(Player player)
	{
		if (player.isInStoreMode() && ConfigData.OFFLINE_TRADE_ENABLE || PlayerData.get(player).isSellBuff() && ConfigData.OFFLINE_SELLBUFF_ENABLE)
		{
			if (!player.isInsideZone(ZoneId.PEACE))
			{
				player.sendMessage("Estas fuera de la zona de paz!");
				return true;
			}
			
			if (player.isInOlympiadMode() || player.isFestivalParticipant() || player.isInJail() || player.getVehicle() != null)
			{
				return true;
			}
			
			// If a party is in progress, leave it
			if (player.isInParty())
			{
				player.getParty().removePartyMember(player, MessageType.DISCONNECTED);
			}
			
			// If the Player has Pet, unsummon it
			if (player.getPet() != null)
			{
				player.getPet().unSummon(player);
			}
			
			// Handle removal from olympiad game
			if (OlympiadManager.getInstance().isRegistered(player) || player.getOlympiadGameId() != -1)
			{
				OlympiadManager.getInstance().removeDisconnectedCompetitor(player);
			}
			
			ThreadPool.schedule(() ->
			{
				if (ConfigData.OFFLINE_SET_NAME_COLOR)
				{
					player.getAppearance().setNameColor(ConfigData.OFFLINE_NAME_COLOR);
				}
			}, 5000);
			
			PlayerData.get(player).setOffline(true);
		}
		return false;
	}
	
	/**
	 * all players in "trade" mode is read from the db
	 */
	private void restoreAllOfflineShops()
	{
		for (PlayerHolder ph : PlayerData.getAllPlayers())
		{
			String shop = getValueDB(ph.getObjectId(), "offlineShop");
			// Don't has value in db
			if (shop == null)
			{
				continue;
			}
			
			Player player = null;
			
			try
			{
				// restore players
				String shopType = shop.split("#")[0];
				String shopTitle = shop.split("#")[1];
				String shopItems = shop.split("#")[2];
				
				player = UtilPlayer.spawnPlayer(ph.getObjectId());
				
				player.sitDown();
				player.setIsInvul(true);
				ph.setOffline(true);
				
				if (shopType.equals("SELL_BUFF"))
				{
					EngineModsManager.onEvent(player, "SellBuffs sell " + shopItems); // shopItems -> price
				}
				else
				{
					StoreType store = Enum.valueOf(StoreType.class, shopType);
					switch (store)
					{
						case BUY:
							for (String list : shopItems.split(";"))
							{
								List<Integer> items = Util.parseInt(list, ",");
								
								if (player.getBuyList().addItemByItemId(items.get(0), items.get(1), items.get(2)) == null)
								{
									throw new NullPointerException();
								}
							}
							player.getBuyList().setTitle(shopTitle.equals("null") ? "" : shopTitle);
							break;
						
						case MANUFACTURE:
							L2ManufactureList createList = new L2ManufactureList();
							for (String list : shopItems.split(";"))
							{
								List<Integer> items = Util.parseInt(list, ",");
								createList.add(new L2ManufactureItem(items.get(0), items.get(1)));
							}
							player.setCreateList(createList);
							player.getCreateList().setStoreName(shopTitle.equals("null") ? "" : shopTitle);
							break;
						
						case PACKAGE_SELL:
						case SELL:
							for (String list : shopItems.split(";"))
							{
								List<Integer> items = Util.parseInt(list, ",");
								if (player.getSellList().addItem(items.get(0), items.get(1), items.get(2)) == null)
								{
									throw new NullPointerException();
								}
							}
							player.getSellList().setTitle(shopTitle.equals("null") ? "" : shopTitle);
							player.getSellList().setPackaged(store == StoreType.PACKAGE_SELL);
							break;
						default:
							System.out.println("Wrong store type " + store);
							player.deleteMe();
							break;
					}
					
					player.setStoreType(store);
				}
				if (ConfigData.OFFLINE_SET_NAME_COLOR)
				{
					player.getAppearance().setNameColor(ConfigData.OFFLINE_NAME_COLOR);
				}
				
				// player.setInOfflineMode();
				// player.restoreEffects();
				player.broadcastUserInfo();
			}
			catch (Exception e)
			{
				LOG.log(Level.WARNING, getClass().getSimpleName() + ": Error loading trader: " + player, e);
				e.printStackTrace();
				if (player != null)
				{
					player.deleteMe();
				}
				ph.setOffline(false);
			}
		}
	}
}
