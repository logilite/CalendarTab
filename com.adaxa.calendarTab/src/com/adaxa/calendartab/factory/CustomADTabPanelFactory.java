package com.adaxa.calendartab.factory;

import org.adempiere.webui.adwindow.IADTabpanel;
import org.adempiere.webui.factory.IADTabPanelFactory;

import com.adaxa.calendartab.webui.adwindow.CalendarTabPanel;

/**
 * Calendar tab panel factory
 * 
 * @author Sachin D Bhimani
 * @since Mar 07, 2017
 */
public class CustomADTabPanelFactory implements IADTabPanelFactory
{

	@Override
	public IADTabpanel getInstance(String type)
	{
		if ("CAL".equals(type))
			return new CalendarTabPanel();
		return null;
	}

}
