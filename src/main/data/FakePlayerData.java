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

import main.holders.PlayerHolder;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;

/**
 * @author fissban
 */
public class FakePlayerData
{
	private static final List<String> _fakesNames = new ArrayList<>();
	private static int _contNames = 0;
	private static final List<String> _fakesTitles = new ArrayList<>();
	private static final List<String> _fakesClanNames = new ArrayList<>();
	
	public static void load()
	{
		// prevenimos datos duplicados en cado de recargar este metodo
		_fakesNames.clear();
		_fakesTitles.clear();
		_fakesClanNames.clear();
		
		loadFakeNumbers();
		loadNames();
		loadTitles();
		loadClansNames();
	}
	
	/**
	 * Obtenemos la cantidad de fakes creados asi podemos asegurarnos de q no se repitiran nombres
	 */
	private static void loadFakeNumbers()
	{
		for (PlayerHolder ph : PlayerData.getAllPlayers())
		{
			if (ph.isFake())
			{
				_contNames++;
			}
		}
	}
	
	private static void loadClansNames()
	{
		try
		{
			File f = new File("./data/xml/engine/fakes/fakesClanNames.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);
			
			Node n = doc.getFirstChild();
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (d.getNodeName().equalsIgnoreCase("fake"))
				{
					NamedNodeMap attrs = d.getAttributes();
					
					String name = attrs.getNamedItem("clan").getNodeValue();
					_fakesClanNames.add(name);
				}
			}
			
			System.out.println("FakePlayerData: Load " + _fakesClanNames.size() + " clans names.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private static void loadNames()
	{
		try
		{
			File f = new File("./data/xml/engine/fakes/fakesNames.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);
			
			Node n = doc.getFirstChild();
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (d.getNodeName().equalsIgnoreCase("fake"))
				{
					NamedNodeMap attrs = d.getAttributes();
					
					String name = attrs.getNamedItem("name").getNodeValue();
					_fakesNames.add(name);
				}
			}
			
			System.out.println("FakePlayerData: Load " + _fakesNames.size() + " names.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private static void loadTitles()
	{
		try
		{
			File f = new File("./data/xml/engine/fakes/fakesTitles.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);
			
			Node n = doc.getFirstChild();
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (d.getNodeName().equalsIgnoreCase("fake"))
				{
					NamedNodeMap attrs = d.getAttributes();
					
					String titles = attrs.getNamedItem("title").getNodeValue();
					_fakesTitles.add(titles);
				}
			}
			
			System.out.println("FakePlayerData: Load " + _fakesTitles.size() + " titles.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Obtenemos un clan name de forma random
	 * @return
	 */
	public static String getClanName()
	{
		return _fakesClanNames.get(Rnd.get(0, _fakesClanNames.size() - 1));
	}
	
	/**
	 * Obtenemos un titlo de forma random
	 * @return
	 */
	public static String getTitle()
	{
		return _fakesTitles.get(Rnd.get(0, _fakesTitles.size() - 1));
	}
	
	/**
	 * Obtenemos un nombre que no este en uso
	 * @return
	 */
	public static String getName()
	{
		if (_contNames > _fakesNames.size())
		{
			System.out.println("FakePlayerData: Se supero el numero maximo de fakes");
			_contNames = 0;
		}
		
		String name = _fakesNames.get(_contNames);
		_contNames++;
		return name;
	}
}
