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
package main.util;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.gameserver.model.WorldObject;

/**
 * @author fissban
 */
public class Util
{
	public static final String SEPARATOR = "-----------------------------------------------------------";
	
	/**
	 * Check if the objects belong to a particular instance.
	 * @param type
	 * @param objects
	 * @return
	 */
	public static <A> boolean areObjectType(Class<A> type, WorldObject... objects)
	{
		if (objects == null || objects.length <= 0)
		{
			return false;
		}
		
		for (WorldObject o : objects)
		{
			if (!type.isAssignableFrom(o.getClass()))
			{
				return false;
			}
		}
		return true;
	}
	
	public static boolean isNumber(String text)
	{
		try
		{
			Integer.parseInt(text);
			return true;
		}
		catch (NumberFormatException nfe)
		{
			return false;
		}
	}
	
	public static List<Integer> parseInt(String line, String split)
	{
		List<Integer> list = new ArrayList<>();
		
		for (String s : line.split(split))
		{
			list.add(Integer.parseInt(s));
		}
		
		return list;
	}
}
