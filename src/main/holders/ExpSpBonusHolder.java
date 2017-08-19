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

import main.enums.ExpSpType;

/**
 * @author fissban
 */
public class ExpSpBonusHolder
{
	private ExpSpType _type;
	private double _bonus;
	
	public ExpSpBonusHolder(ExpSpType type, int bonus)
	{
		_type = type;
		_bonus = bonus / 100;
	}
	
	public ExpSpType getType()
	{
		return _type;
	}
	
	public double getBonus()
	{
		return _bonus;
	}
	
}
