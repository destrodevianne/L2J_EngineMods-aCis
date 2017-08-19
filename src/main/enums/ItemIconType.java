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
package main.enums;

/**
 * @author fissban
 */
public enum ItemIconType
{
	WEAPON("Icon.weapon_"),
	ARMOR("Icon.armor_"),
	ETCITEM("Icon.etc_"),
	SHIELD("Icon.shield_"),
	RECIPE("Icon.etc_recipe"),
	POTION("Icon.etc_potion_");
	
	String _searchItem;
	
	private ItemIconType(String searchItem)
	{
		_searchItem = searchItem;
	}
	
	public String getSearchItem()
	{
		return _searchItem;
	}
}
