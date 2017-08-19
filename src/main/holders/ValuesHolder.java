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
public class ValuesHolder
{
	private final String _mod;
	private final String _event;
	private String _value;
	
	public ValuesHolder(String mod, String event, String value)
	{
		_mod = mod;
		_event = event;
		_value = value;
	}
	
	public String getEvent()
	{
		return _event;
	}
	
	public String getMod()
	{
		return _mod;
	}
	
	public String getValue()
	{
		return _value;
	}
	
	public void setValue(String value)
	{
		_value = value;
	}
}
