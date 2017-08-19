/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package main.holders;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.skills.Stats;

/**
 * @author fissban
 */
public class PlayerHolder
{
	private int _objectId;
	private String _name;
	private String _accountName;
	
	public PlayerHolder(int objectId, String name, String accountName)
	{
		_objectId = objectId;
		_name = name;
		_accountName = accountName;
	}
	
	public int getObjectId()
	{
		return _objectId;
	}
	
	public void setObjectId(int objectId)
	{
		_objectId = objectId;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public void setName(String name)
	{
		_name = name;
	}
	
	public String getAccountName()
	{
		return _accountName;
	}
	// XXX sell buff and storeType ----------------------------------------------------------------------------------------
	
	private boolean _isOffline = false;
	
	public boolean isOffline()
	{
		return _isOffline;
	}
	
	public void setOffline(boolean mode)
	{
		_isOffline = mode;
	}
	
	// XXX sell buff ------------------------------------------------------------------------------------------------------
	private boolean _isSellBuff = false;
	private int _sellBuffPrice;
	
	public boolean isSellBuff()
	{
		return _isSellBuff;
	}
	
	public void setSellBuff(boolean isSellBuff)
	{
		_isSellBuff = isSellBuff;
	}
	
	public int getSellBuffPrice()
	{
		return _sellBuffPrice;
	}
	
	public void setSellBuffPrice(int sellBuffPrice)
	{
		_sellBuffPrice = sellBuffPrice;
	}
	
	// XXX aio -----------------------------------------------------------------------------------------------------------
	private boolean _isAio = false;
	private long _aioExpireDate = 0;
	
	public boolean isAio()
	{
		return _isAio;
	}
	
	public void setAio(boolean isAio)
	{
		_isAio = isAio;
	}
	
	public String getAioExpireDateFormat()
	{
		return new SimpleDateFormat("dd-MMM-yyyy").format(new Date(_aioExpireDate));
	}
	
	public long getAioExpireDate()
	{
		return _aioExpireDate;
	}
	
	public void setAioExpireDate(long dayTime)
	{
		_aioExpireDate = dayTime;
	}
	
	// XXX vip ----------------------------------------------------------------------------------------------------------
	private boolean _isVip = false;
	private long _vipExpireDate = 0;
	
	public boolean isVip()
	{
		return _isVip;
	}
	
	public void setVip(boolean isVip)
	{
		_isVip = isVip;
	}
	
	public String getVipExpireDateFormat()
	{
		return new SimpleDateFormat("dd-MMM-yyyy").format(new Date(_vipExpireDate));
	}
	
	public long getVipExpireDate()
	{
		return _vipExpireDate;
	}
	
	public void setVipExpireDate(long dayTime)
	{
		_vipExpireDate = dayTime;
	}
	
	// XXX Fake -----------------------------------------------------------------------------------------------------------
	
	private boolean _isFake = false;
	
	public boolean isFake()
	{
		return _isFake;
	}
	
	public void setFake(boolean isFake)
	{
		_isFake = isFake;
	}
	
	private Location _posToFarm = null;
	
	public void setPosToFarm(String pos)
	{
		int x = Integer.parseInt(pos.split(",")[0]);
		int y = Integer.parseInt(pos.split(",")[1]);
		int z = Integer.parseInt(pos.split(",")[2]);
		
		_posToFarm = new Location(x, y, z);
	}
	
	public void setPosToFarm(int x, int y, int z)
	{
		_posToFarm = new Location(x, y, z);
	}
	
	public Location getPosToFarm()
	{
		return _posToFarm;
	}
	
	// XXX Rebirth -----------------------------------------------------------------------------------------------------------
	
	private int _rebirth = 0;
	// puntos de maestria
	private AtomicInteger _maestriasPoints = new AtomicInteger(0);
	
	public int getRebirth()
	{
		return _rebirth;
	}
	
	public void increaseRebirth()
	{
		_rebirth++;
	}
	
	public void setRebirth(int rebirth)
	{
		_rebirth = rebirth;
	}
	
	public AtomicInteger getMaestriasPoints()
	{
		return _maestriasPoints;
	}
	
	// puntos para stat
	private AtomicInteger _statsPoints = new AtomicInteger(0);
	
	public AtomicInteger getStatsPoints()
	{
		return _statsPoints;
	}
	
	private Map<Stats, Integer> _statsCustom = new HashMap<>();
	{
		_statsCustom.put(Stats.STAT_STR, 0);
		_statsCustom.put(Stats.STAT_CON, 0);
		_statsCustom.put(Stats.STAT_DEX, 0);
		_statsCustom.put(Stats.STAT_INT, 0);
		_statsCustom.put(Stats.STAT_WIT, 0);
		_statsCustom.put(Stats.STAT_MEN, 0);
	}
	
	public int getCustomStat(Stats stat)
	{
		return _statsCustom.get(stat);
	}
	
	public void addCustomStat(Stats stat, int value)
	{
		int oldValue = getCustomStat(stat);
		
		_statsCustom.put(stat, oldValue + value);
	}
	
	// XXX Auction ------------------------------------------------------------------------------------------
	
	// ids items que tiene el player a la venta.
	private Map<Integer, AuctionItemHolder> _auctionsSell = new LinkedHashMap<>(100);
	
	public Map<Integer, AuctionItemHolder> getAuctionsSell()
	{
		return _auctionsSell;
	}
	
	public void addAuctionSell(int id, AuctionItemHolder auction)
	{
		_auctionsSell.put(id, auction);
	}
	
	public void removeAuctionSell(int key)
	{
		_auctionsSell.remove(key);
	}
	
	// ids items que vendio el player
	private Map<Integer, AuctionItemHolder> _auctionsSold = new LinkedHashMap<>(100);
	
	public Map<Integer, AuctionItemHolder> getAuctionsSold()
	{
		return _auctionsSold;
	}
	
	public void addAuctionSold(int id, AuctionItemHolder auction)
	{
		_auctionsSold.put(id, auction);
	}
	
	public void removeAuctionSold(int id)
	{
		_auctionsSold.remove(id);
	}
	
	// XXX AntiBot ------------------------------------------------------------------------------------------
	
	// respuesta correcta al antibot
	public volatile String _answerRight;
	// cantidad de kills
	public volatile int _kills = 0;
	// cantidad de intentos
	public int _attempts = 3;
	
	public boolean isAnswerRight(String bypas)
	{
		return _answerRight.equals(bypas);
	}
	
	public void setAnswerRight(String anserRight)
	{
		_answerRight = anserRight;
	}
	
	public int getKills()
	{
		return _kills;
	}
	
	public void increaseKills()
	{
		_kills++;
	}
	
	public void resetKills()
	{
		_kills = 0;
	}
	
	public int getAttempts()
	{
		return _attempts;
	}
	
	public void decreaseAttempts()
	{
		_attempts--;
	}
	
	public void resetAttempts()
	{
		_attempts = 3;
	}
	
	// XXX Vote Reward -------------------------------------------------------------------------------------
	
	private boolean _hasVote;
	private long _lastVote;
	
	public boolean isHasVote()
	{
		return _hasVote;
	}
	
	public void setHasVote(boolean hasVote)
	{
		_hasVote = hasVote;
	}
	
	public long getLastVote()
	{
		return _lastVote;
	}
	
	public void setLastVote(long lastVote)
	{
		_lastVote = lastVote;
	}
}
