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

import main.enums.BuffType;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill;

/**
 * Clase usada en NpcBufferScheme
 * @author fissban
 */
public class BuffHolder
{
	private int _id;
	private int _level;
	private BuffType _type = BuffType.NONE;
	
	public BuffHolder(int id, int level)
	{
		_id = id;
		_level = level;
	}
	
	public BuffHolder(BuffType type, int id, int level)
	{
		_type = type;
		_id = id;
		_level = level;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getLevel()
	{
		return _level;
	}
	
	public BuffType getType()
	{
		return _type;
	}
	
	/**
	 * @return the L2Skill associated to the id/value.
	 */
	public final L2Skill getSkill()
	{
		return SkillTable.getInstance().getInfo(_id, _level);
	}
}
