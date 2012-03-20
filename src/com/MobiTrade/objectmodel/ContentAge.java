package com.MobiTrade.objectmodel;


public class ContentAge {

	public int c_age_days = 0;
	public int c_age_hours = 0;
	public int c_age_minutes = 0;


	public ContentAge(int days, int hours, int min)
	{
		c_age_days = days;
		c_age_hours = hours;
		c_age_minutes = min;
	}
	
	public boolean isOlderThan(ContentAge ca)
	{
		if(c_age_days > ca.c_age_days)
		{
			return true;
		}else if(c_age_days == ca.c_age_days)
		{
			if(c_age_hours > ca.c_age_hours)
			{
				return true;
			}else if(c_age_hours == ca.c_age_hours)
			{
				if(c_age_minutes > ca.c_age_minutes)
				{
					return true;
				}else if(c_age_minutes == ca.c_age_minutes)
				{
					return true;
				}else if(c_age_minutes < ca.c_age_minutes)
				{
					return false;
				}
			}else if(c_age_hours < ca.c_age_hours)
			{
				return false;
			}
				
		}else if(c_age_days < ca.c_age_days)
		{
			return false;
		}
		// Not valid comparison
		return false;
	}
	
	public void addAge(ContentAge ca)
	{
		
		int newMin = c_age_minutes + c_age_hours * 60 + c_age_days*24*60 + 
		ca.c_age_minutes + ca.c_age_hours*60 + ca.c_age_days*24*60;
		
		c_age_minutes = newMin%60;
		
		c_age_hours = (newMin/60) % (24);
		
		c_age_days = (newMin/60)/24;
	}

}
