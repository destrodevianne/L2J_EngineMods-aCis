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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import main.data.ConfigData;
import main.engine.AbstractMods;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.Player;

/**
 * @author fissban
 */
public class SubClassAcumulatives extends AbstractMods
{
	// SQL
	private static final String RESTORE_SKILLS_FOR_CHAR = "SELECT skill_id,skill_level,class_index FROM character_skills WHERE char_obj_id=?";
	
	/**
	 * Constructor
	 */
	public SubClassAcumulatives()
	{
		registerMod(ConfigData.ENABLE_SubClassAcumulatives);
	}
	
	@Override
	public void onModState()
	{
		//
	}
	
	@Override
	public boolean onRestoreSkills(Player player)
	{
		Map<Integer, Integer> skills = new HashMap<>();
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(RESTORE_SKILLS_FOR_CHAR))
		{
			// Retrieve all skills of this Player from the database
			ps.setInt(1, player.getObjectId());
			
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final int id = rs.getInt("skill_id");
					final int level = rs.getInt("skill_level");
					final int classIndex = rs.getInt("class_index");
					
					// fake skills for base stats
					if (id > 9000)
					{
						continue;
					}
					
					if (player.getClassIndex() != classIndex)
					{
						final L2Skill skill = SkillTable.getInstance().getInfo(id, level);
						
						if (skill == null)
						{
							LOG.log(Level.SEVERE, "Skipped null skill Id: " + id + ", Level: " + level + " while restoring player skills for " + player.getName());
							continue;
						}
						
						if (!ConfigData.ACUMULATIVE_PASIVE_SKILLS)
						{
							if (skill.isPassive())
							{
								continue;
							}
						}
						
						if (ConfigData.DONT_ACUMULATIVE_SKILLS_ID.contains(id))
						{
							continue;
						}
					}
					
					// We save all the skills that we will teach our character.
					// This will avoid teaching a skill from lvl 1 to 15 for example
					// And directly we teach the lvl 15 =)
					skills.put(id, level);
				}
			}
		}
		catch (Exception e)
		{
			LOG.log(Level.SEVERE, "Could not restore " + player.getName() + " skills:", e);
		}
		
		for (Entry<Integer, Integer> skillLearn : skills.entrySet())
		{
			final int id = skillLearn.getKey();
			final int level = skillLearn.getValue();
			// Create a L2Skill object for each record
			final L2Skill skill = SkillTable.getInstance().getInfo(id, level);
			
			if (skill == null)
			{
				LOG.log(Level.SEVERE, "Skipped null skill Id: " + id + ", Level: " + level + " while restoring player skills for " + player.getName());
				continue;
			}
			
			// solo le enseniamos el skill si es que el mismo no lo tiene aun o si es el inferior al q le vamos a enseniar
			if (player.getSkillLevel(id) < level)
			{
				// Add the L2Skill object to the L2Character _skills and its Func objects to the calculator set of the L2Character
				player.addSkill(skill);
			}
		}
		
		return true;
	}
}