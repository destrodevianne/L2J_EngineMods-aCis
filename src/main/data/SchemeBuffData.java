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
package main.data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import main.enums.BuffType;
import main.holders.BuffHolder;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;

/**
 * @author fissban
 */
public class SchemeBuffData
{
	private static final List<BuffHolder> _generalBuffs = new ArrayList<>();
	
	private static final List<BuffHolder> _warriorBuffs = new ArrayList<>();
	private static final List<BuffHolder> _mageBuffs = new ArrayList<>();
	
	public static void load()
	{
		// prevenimos datos duplicados en cado de recargar este metodo
		_generalBuffs.clear();
		_warriorBuffs.clear();
		_mageBuffs.clear();
		
		loadBuffs();
		loadMageBuffs();
		loadWarriorBuffs();
	}
	
	private static void loadBuffs()
	{
		try
		{
			File f = new File("./data/xml/engine/scheme_buffer/generalBuffs.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);
			
			Node n = doc.getFirstChild();
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (d.getNodeName().equalsIgnoreCase("buff"))
				{
					NamedNodeMap attrs = d.getAttributes();
					
					BuffType type = BuffType.valueOf(attrs.getNamedItem("type").getNodeValue());
					int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
					int lvl = Integer.parseInt(attrs.getNamedItem("lvl").getNodeValue());
					
					_generalBuffs.add(new BuffHolder(type, id, lvl));
				}
			}
			
			System.out.println("SchemeBuffData: Load " + _generalBuffs.size() + " buffs.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private static void loadMageBuffs()
	{
		try
		{
			File f = new File("./data/xml/engine/scheme_buffer/setMageBuffs.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);
			
			Node n = doc.getFirstChild();
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (d.getNodeName().equalsIgnoreCase("buff"))
				{
					NamedNodeMap attrs = d.getAttributes();
					
					int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
					int lvl = Integer.parseInt(attrs.getNamedItem("lvl").getNodeValue());
					
					_mageBuffs.add(new BuffHolder(id, lvl));
				}
			}
			
			System.out.println("SchemeBuffData: Load " + _mageBuffs.size() + " set mage buffs.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private static void loadWarriorBuffs()
	{
		try
		{
			File f = new File("./data/xml/engine/scheme_buffer/setWarriorBuffs.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);
			
			Node n = doc.getFirstChild();
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (d.getNodeName().equalsIgnoreCase("buff"))
				{
					NamedNodeMap attrs = d.getAttributes();
					
					int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
					int lvl = Integer.parseInt(attrs.getNamedItem("lvl").getNodeValue());
					
					_warriorBuffs.add(new BuffHolder(id, lvl));
				}
			}
			
			System.out.println("SchemeBuffData: Load " + _warriorBuffs.size() + " set warrior buffs.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static List<BuffHolder> getAllMageBuffs()
	{
		return _mageBuffs;
	}
	
	public static List<BuffHolder> getAllWarriorBuffs()
	{
		return _warriorBuffs;
	}
	
	public static List<BuffHolder> getAllGeneralBuffs()
	{
		return _generalBuffs;
	}
}
