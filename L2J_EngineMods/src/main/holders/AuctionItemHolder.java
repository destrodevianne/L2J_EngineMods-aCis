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
package main.holders;

/**
 * @author fissban
 */
public class AuctionItemHolder
{
	private int _key;
	private int _ownerId;
	private int _itemObjId;
	private int _itemId;
	private int _itemCount;
	private int _itemEnchantLevel;
	private int _itemPriceCount;
	private int _itemPriceId;
	
	/**
	 * @param key
	 * @param ownerId
	 * @param itemObjId
	 * @param itemId
	 * @param itemCount
	 * @param itemEnchantLevel
	 * @param itemPriceCount
	 * @param itemPriceId
	 */
	public AuctionItemHolder(String auction)
	{
		String[] auctions = auction.split(" ");
		_key = Integer.parseInt(auctions[0]);
		_ownerId = Integer.parseInt(auctions[1]);
		_itemObjId = Integer.parseInt(auctions[2]);
		_itemId = Integer.parseInt(auctions[3]);
		_itemCount = Integer.parseInt(auctions[4]);
		_itemEnchantLevel = Integer.parseInt(auctions[5]);
		_itemPriceCount = Integer.parseInt(auctions[6]);
		_itemPriceId = Integer.parseInt(auctions[7]);
	}
	
	/**
	 * @param key
	 * @param ownerId
	 * @param itemObjId
	 * @param itemId
	 * @param itemCount
	 * @param itemEnchantLevel
	 * @param itemPriceCount
	 * @param itemPriceId
	 */
	public AuctionItemHolder(int key, int ownerId, int itemObjId, int itemId, int itemCount, int itemEnchantLevel, int itemPriceCount, int itemPriceId)
	{
		_key = key;
		_ownerId = ownerId;
		_itemObjId = itemObjId;
		_itemId = itemId;
		_itemCount = itemCount;
		_itemEnchantLevel = itemEnchantLevel;
		_itemPriceCount = itemPriceCount;
		_itemPriceId = itemPriceId;
	}
	
	public int getkey()
	{
		return _key;
	}
	
	public void setKey(int key)
	{
		_key = key;
	}
	
	public int getOwnerId()
	{
		return _ownerId;
	}
	
	public int getItemObjId()
	{
		return _itemObjId;
	}
	
	public int getItemId()
	{
		return _itemId;
	}
	
	public int getItemEnchantLevel()
	{
		return _itemEnchantLevel;
	}
	
	public int getItemPriceCount()
	{
		return _itemPriceCount;
	}
	
	public int getItemPriceId()
	{
		return _itemPriceId;
	}
	
	public int getItemCount()
	{
		return _itemCount;
	}
	
	@Override
	public String toString()
	{
		return _key + " " + _ownerId + " " + _itemObjId + " " + _itemId + " " + _itemCount + " " + _itemEnchantLevel + " " + _itemPriceCount + " " + _itemPriceId;
	}
}
