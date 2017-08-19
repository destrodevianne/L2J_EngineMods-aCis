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
package main.util.builders.html;

/**
 * @author fissban
 */
public class HtmlBuilder
{
	public enum HtmlType
	{
		COMUNITY_TYPE(12270),
		HTML_TYPE(8191);
		
		int _lenght;
		
		HtmlType(int lenght)
		{
			_lenght = lenght;
		}
		
		public int getMaxValue()
		{
			return _lenght;
		}
	}
	
	private StringBuilder _html;
	private HtmlType _type;
	
	/**
	 * Constructor
	 * @param size
	 */
	public HtmlBuilder(HtmlType type)
	{
		_html = new StringBuilder(type.getMaxValue());
		_type = type;
	}
	
	/**
	 * Constructor
	 */
	public HtmlBuilder()
	{
		_html = new StringBuilder();
		_type = HtmlType.HTML_TYPE;
	}
	
	/**
	 * @param string
	 */
	public void append(String string)
	{
		_html.append(string);
	}
	
	/**
	 * @param obj
	 */
	public void append(Object... obj)
	{
		for (Object o : obj)
		{
			_html.append(o);
		}
	}
	
	@Override
	public String toString()
	{
		if (_html.length() >= _type.getMaxValue())
		{
			System.out.println("Warning html is too long! -> " + _html.length());
			return "<html><body><br>Html was too long.</body></html>";
		}
		return _html.toString();
	}
}
