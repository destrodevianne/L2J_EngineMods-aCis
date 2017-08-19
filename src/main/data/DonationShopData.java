package main.data;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import main.holders.DonationShopHolder;

public class DonationShopData
{
	private static final Map<String, DonationShopHolder> _shop = new HashMap<>();
	
	public static void load()
	{
		_shop.clear();
		try
		{
			File file = new File("./config/engine/DonationShop.xml");
			Document document = XMLDocumentFactory.getInstance().loadDocument(file);
			
			DonationShopHolder shopHolder = new DonationShopHolder();
			for (Node n = document.getFirstChild().getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("instance".equalsIgnoreCase(n.getNodeName()))
				{
					String name = n.getAttributes().getNamedItem("name").getNodeValue();
					shopHolder.setName(name);
					
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("allowMod".equalsIgnoreCase(d.getNodeName()))
						{
							boolean val = Boolean.valueOf(d.getAttributes().getNamedItem("val").getNodeValue());
							shopHolder.setAllowMod(val);
						}
						else if ("priceId".equalsIgnoreCase(d.getNodeName()))
						{
							int val = Integer.parseInt(d.getAttributes().getNamedItem("val").getNodeValue());
							shopHolder.setPriceId(val);
						}
						else if ("priceCount".equalsIgnoreCase(d.getNodeName()))
						{
							int val = Integer.parseInt(d.getAttributes().getNamedItem("val").getNodeValue());
							shopHolder.setPriceCount(val);
						}
					}
					_shop.put(name, shopHolder);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static Map<String, DonationShopHolder> getAllConfigs()
	{
		return _shop;
	}
}