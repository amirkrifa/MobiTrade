package com.MobiTrade.objectmodel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Channel extends HashMap<String, String>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String keyWords;
	private Float utility;
	private Integer nbr_contents;

	// 1 for a local channel and 0 for a foreign one
	private Integer is_local_channel;
	private String image;
	private String creationDate;
	private Integer ch_size; 
	private Integer ch_max_allowed_size;

	public static final String CHANNEL_KEYWORDS = "ch_keywords";
	public static final String CHANNEL_UTILITY = "ch_utility";
	public static final String CHANNEL_TEXT_UTILITY = "ch_utility_text";

	public static final String CHANNEL_NBR_CONTENTS = "ch_nbr_contents";
	public static final String CHANNEL_STATUS = "is_local";
	public static final String CHANNEL_IMAGE = "ch_image";
	public static final String CHANNEL_CREATION_DATE = "ch_creation_date";
	public static final String CHANNEL_SIZE_BYTE = "ch_size";
	public static final String CHANNEL_MAX_ALLOWED_SIZE_BYTE = "ch_max_allowed_size";

	private List<Content> listAvailableContents = new ArrayList<Content>();

	// CHANNEL_KEYWORDS, CHANNEL_NBR_CONTENTS, CHANNEL_UTILITY
	public Channel(String keywords, int nbrContents, Float utility, int local, String image, String date, Integer ch_size, Integer ch_max_allowed_size)
	{
		keyWords = keywords;
		this.utility = utility;
		nbr_contents = nbrContents;
		is_local_channel = local;
		this.image = image;
		creationDate = date;
		this.ch_size = ch_size;
		this.ch_max_allowed_size = ch_max_allowed_size;
	}

	public void addAvailableContent(List<Content> l)
	{
		listAvailableContents.addAll(l);
	}

	public List<Content> getAvailableContents()
	{
		return listAvailableContents;
	}

	public float getUtility()
	{
		return utility;
	}
	
	public void updateUtility(float u)
	{
		utility = u;
	}
	

	public void updateNumberOfContents(int nbr)
	{
		nbr_contents = nbr;
	}
	
	public int getNumberOfContents()
	{
		return nbr_contents;
	}
	
	public void updateChannelSize(long size)
	{
		ch_size = (int)size;
	}
	
	public int getChannelSize()
	{
		return ch_size;
	}
	
	public boolean isLocal()
	{
		return (is_local_channel == 1);
	}
	
	@Override
	public String get(Object k) {

		String key = (String) k;

		if (CHANNEL_IMAGE.equals(key))
			return image;
		else if (CHANNEL_KEYWORDS.equals(key))
			return keyWords;
		else if(CHANNEL_NBR_CONTENTS.equals(key))
			return nbr_contents.toString()+ " contents";
		else if(CHANNEL_STATUS.equals(key))
			return is_local_channel.toString();
		else if(CHANNEL_UTILITY.equals(key))
		{
			return Float.toString(utility);
		}else if(CHANNEL_TEXT_UTILITY.equals(key))
		{
			return "Max allowed space: "+Float.toString((int)Math.ceil(utility/1024))+" Kb";
		}else if(CHANNEL_CREATION_DATE.equals(key))
			return creationDate;
		else if(CHANNEL_SIZE_BYTE.equals(key))
			return ch_size.toString();
		else if(CHANNEL_MAX_ALLOWED_SIZE_BYTE.equals(key))
			return ch_max_allowed_size.toString();
		return null;
	}

}
