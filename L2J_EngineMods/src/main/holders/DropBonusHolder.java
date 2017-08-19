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
public class DropBonusHolder
{
	private double _amountBonus = 1.0;
	private double _chanceBonus = 1.0;
	
	public DropBonusHolder()
	{
		//
	}
	
	/**
	 * 100 -> normal drop<br>
	 * 110 -> 10% bonus
	 * @param amount
	 */
	public void increaseAmountBonus(double amount)
	{
		_amountBonus += amount - 1;
	}
	
	/**
	 * 1.0 -> normal drop<br>
	 * 1.1 -> 10% bonus
	 * @param amount
	 */
	public void increaseChanceBonus(double chance)
	{
		_chanceBonus += chance - 1;
	}
	
	public double getAmountBonus()
	{
		return _amountBonus;
	}
	
	public double getChanceBonus()
	{
		return _chanceBonus;
	}
}
