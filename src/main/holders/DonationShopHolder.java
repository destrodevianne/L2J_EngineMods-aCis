package main.holders;

public class DonationShopHolder
{
	String _name;
	boolean _allowMod;
	int _priceId;
	int _priceCount;
	
	public DonationShopHolder()
	{
		
	}
	
	public String getName()
	{
		return _name;
	}
	
	public void setName(String name)
	{
		_name = name;
	}
	
	public boolean isAllowMod()
	{
		return _allowMod;
	}
	
	public void setAllowMod(boolean allowMod)
	{
		_allowMod = allowMod;
	}
	
	public int getPriceId()
	{
		return _priceId;
	}
	
	public void setPriceId(int priceId)
	{
		_priceId = priceId;
	}
	
	public int getPriceCount()
	{
		return _priceCount;
	}
	
	public void setPriceCount(int priceCount)
	{
		_priceCount = priceCount;
	}
}