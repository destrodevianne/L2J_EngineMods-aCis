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
public class RewardHolder
{
	private int _id;
	private int _count;
	private int _chance;
	
	/**
	 * @param rewardId
	 * @param rewardCount
	 */
	public RewardHolder(int rewardId, int rewardCount)
	{
		_id = rewardId;
		_count = rewardCount;
		_chance = 100;
	}
	
	/**
	 * @param rewardId
	 * @param rewardCount
	 * @param rewardChance
	 */
	public RewardHolder(int rewardId, int rewardCount, int rewardChance)
	{
		_id = rewardId;
		_count = rewardCount;
		_chance = rewardChance;
	}
	
	public int getRewardId()
	{
		return _id;
	}
	
	public int getRewardCount()
	{
		return _count;
	}
	
	public int getRewardChance()
	{
		return _chance;
	}
	
	public void setId(int id)
	{
		_id = id;
	}
	
	public void setCount(int count)
	{
		_count = count;
	}
	
	public void setChance(int chance)
	{
		_chance = chance;
	}
}
