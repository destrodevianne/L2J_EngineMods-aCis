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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map.Entry;

import main.data.ConfigData;
import main.data.PlayerData;
import main.engine.AbstractMods;
import main.holders.PlayerHolder;
import main.holders.RewardHolder;
import main.util.Util;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.util.Broadcast;

/**
 * @author fissban
 */
public class VoteReward extends AbstractMods
{
	private enum TopType
	{
		HOPZONE,
		TOPZONE,
		NETWORK,
	}
	
	private static int _lastVoteCountHopzone = 0;
	private static int _lastVoteCountTopzone = 0;
	private static int _lastVoteCountNetwork = 0;
	
	public VoteReward()
	{
		registerMod(ConfigData.ENABLE_VoteReward && (ConfigData.ENABLE_HOPZONE || ConfigData.ENABLE_TOPZONE || ConfigData.ENABLE_NETWORK));
	}
	
	@Override
	public void onModState()
	{
		switch (getState())
		{
			case START:
				startTimer("getLastVote", 0, null, null, false);
				startTimer("getVotes", ConfigData.TIME_CHECK_VOTES * 60 * 1000, null, null, true);
				break;
			case END:
				cancelTimers("getLastVote");
				cancelTimers("getVotes");
				break;
		}
		// TODO Auto-generated method stub
	}
	
	@Override
	public void onTimer(String timerName, Npc npc, Player player)
	{
		
		switch (timerName)
		{
			case "getLastVote":
				if (ConfigData.ENABLE_HOPZONE)
				{
					_lastVoteCountHopzone = getVotesHopzone();
				}
				if (ConfigData.ENABLE_TOPZONE)
				{
					_lastVoteCountTopzone = getVotesTopzone();
				}
				if (ConfigData.ENABLE_NETWORK)
				{
					_lastVoteCountNetwork = getVotesNetwork();
				}
				break;
			
			case "getVotes":
				int voteHopzone = 0;
				int voteTopzone = 0;
				int voteNetwork = 0;
				// the number of votes obtained.
				if (ConfigData.ENABLE_HOPZONE)
				{
					voteHopzone = getVotesHopzone();
				}
				if (ConfigData.ENABLE_TOPZONE)
				{
					voteTopzone = getVotesTopzone();
				}
				if (ConfigData.ENABLE_NETWORK)
				{
					voteNetwork = getVotesNetwork();
				}
				// the number of votes of each page is displayed
				Broadcast.announceToOnlinePlayers("------------^^------------", true);
				if (ConfigData.ENABLE_HOPZONE)
				{
					checkVoteRewards(voteHopzone, TopType.HOPZONE);
				}
				if (ConfigData.ENABLE_TOPZONE)
				{
					checkVoteRewards(voteTopzone, TopType.TOPZONE);
				}
				if (ConfigData.ENABLE_NETWORK)
				{
					checkVoteRewards(voteNetwork, TopType.NETWORK);
				}
				Broadcast.announceToOnlinePlayers("------------^^------------", true);
				break;
		}
	}
	
	// MISC -----------------------------------------------------------------------------------------
	
	/**
	 * prizes are awarded.<br>
	 * <li>Check that the character is online.</li><br>
	 * <li>Check that the reward is there.</li><br>
	 * <li>Send message.</li><br>
	 * @param player
	 * @param reward
	 */
	private static void giveRewardAllPlayers(RewardHolder reward, TopType topName)
	{
		for (Player player : World.getInstance().getPlayers())
		{
			if (reward == null)
			{
				return;
			}
			
			PlayerHolder ph = PlayerData.get(player);
			if (ph.isOffline() || ph.isFake() || player.isInJail())
			{
				continue;
			}
			
			// XXX Si se desea agregar un control de ip aqui es el lugar
			
			player.addItem("voteReward", reward.getRewardId(), reward.getRewardCount(), null, true);
		}
	}
	
	/**
	 * @param rewardsList
	 * @param voteTop
	 * @return
	 */
	private static void checkVoteRewards(int voteTop, TopType topName)
	{
		// if votes are not obtained from the "top" no actions are performed
		if (voteTop == 0)
		{
			return;
		}
		
		RewardHolder reward = null;
		
		int nextVote = 0;
		int lastVote = 0;
		
		// the last vote of the page is obtained according to the top
		switch (topName)
		{
			case HOPZONE:
				lastVote = _lastVoteCountHopzone;
				break;
			case TOPZONE:
				lastVote = _lastVoteCountTopzone;
				break;
			case NETWORK:
				lastVote = _lastVoteCountNetwork;
				break;
		}
		
		for (Entry<Integer, RewardHolder> top : ConfigData.VOTE_REWARDS.entrySet())
		{
			if (top.getKey() < lastVote)
			{
				continue;
			}
			else
			{
				nextVote = top.getKey();
				reward = top.getValue();
				break;
			}
		}
		
		Broadcast.announceToOnlinePlayers(" @ " + topName.name().toLowerCase() + ":  " + voteTop + " votes.", true);
		
		if (nextVote == 0)
		{
			// no has more rewards!!!
			return;
		}
		
		if (nextVote < voteTop)
		{
			Broadcast.announceToOnlinePlayers(" @ " + topName.name().toLowerCase() + ": You won the Reward Vote!", true);
			
			giveRewardAllPlayers(reward, topName);
		}
		else
		{
			Broadcast.announceToOnlinePlayers(" @ " + topName.name().toLowerCase() + ": next reward in " + nextVote + " votes.", true);
		}
		
		// update last votes
		switch (topName)
		{
			case HOPZONE:
				_lastVoteCountHopzone = voteTop;
				break;
			case TOPZONE:
				_lastVoteCountTopzone = voteTop;
				break;
			case NETWORK:
				_lastVoteCountNetwork = voteTop;
				break;
		}
	}
	
	/**
	 * Get the votes of TOPZONE
	 * @return
	 */
	public static int getVotesTopzone()
	{
		int votes = 0;
		try
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(new URL(ConfigData.TOPZONE_URL).openConnection().getInputStream()));
			String cadena = in.readLine();
			in.close();
			return Integer.parseInt(Util.getJsonVariable(cadena, "totalVotes"));
		}
		catch (Exception e)
		{
			LOG.warning("Error while getting Topzone server vote count.");
			e.printStackTrace();
		}
		
		return votes;
	}
	
	/**
	 * Get the votes of HOPZONE
	 * @return
	 */
	public static int getVotesHopzone()
	{
		int votes = 0;
		try
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(new URL(ConfigData.HOPZONE_URL).openConnection().getInputStream()));
			String tokens = in.readLine();
			in.close();
			return Integer.parseInt(Util.getJsonVariable(tokens, "totalvotes"));
		}
		catch (Exception e)
		{
			LOG.warning("Error while getting Hopzone server vote count.");
			// e.printStackTrace();
		}
		
		return votes;
	}
	
	/**
	 * Get the votes of NETWORK
	 * @return
	 */
	public static int getVotesNetwork()
	{
		int votes = 0;
		try
		{
			URLConnection con = new URL(ConfigData.NETWORK_URL).openConnection();
			
			con.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36");
			con.setConnectTimeout(5000);
			
			try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream())))
			{
				String inputLine;
				while ((inputLine = in.readLine()) != null)
				{
					if (inputLine.contains("tls-in-sts"))
					{
						return Integer.valueOf(inputLine.split(">")[2].replace("</b", ""));
					}
				}
			}
		}
		catch (Exception e)
		{
			LOG.warning("Error while getting Network server vote count.");
			// e.printStackTrace();
		}
		
		return votes;
	}
}