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
	
	/**
	 * Metodo para extraer cualquier tipo variable de un documento en JSON sin necesidad de librerias externas
	 * @param cadena Texto que devuelva la API de TopZone o Hopzone
	 * @param variable Valor que quieres obtener
	 * @return Valor de la variable que se define
	 */
	public static String getJsonVariable(String cadena, String variable)
	{
		cadena.replaceAll("\\}", "");
		String[] cadena_separada = cadena.split("\\{");
		for (String a : cadena_separada)
		{
			if (a.contains(variable))
			{
				String[] a_separada = a.split(",");
				for (String b : a_separada)
				{
					if (b.contains(variable))
					{
						String[] b_separada = b.split(":");
						return b_separada[1].replaceAll("\"", "");
					}
				}
			}
		}
		return "";
	}
}
