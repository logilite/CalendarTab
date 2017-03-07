package com.adaxa.calendartab.webui.adwindow;

import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.adwindow.ADTreePanel;
import org.adempiere.webui.adwindow.AbstractADWindowContent;
import org.adempiere.webui.adwindow.DetailPane;
import org.adempiere.webui.adwindow.GridView;
import org.adempiere.webui.adwindow.IADTabpanel;
import org.adempiere.webui.component.Panel;
import org.compiere.model.DataStatusEvent;
import org.compiere.model.DataStatusListener;
import org.compiere.model.GridTab;
import org.compiere.model.GridWindow;
import org.compiere.util.CLogger;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Button;

import com.adaxa.calendartab.webui.tabs.CalendarWindow;

/**
 * Calendar Tab Panel
 * 
 * @author Sachin D Bhimani
 * @since Mar 07, 2017
 */
public class CalendarTabPanel extends Panel implements IADTabpanel, DataStatusListener
{

	/**
	 * 
	 */
	private static final long		serialVersionUID	= 3090172444731938527L;

	private final CLogger			log					= CLogger.getCLogger(CalendarTabPanel.class);

	private AbstractADWindowContent	adWindowContent		= null;
	private GridTab					gridTab				= null;
	private int						windowNumber		= 0;
	private GridWindow				gridWindow			= null;
	private CalendarWindow			calendarboard		= null;
	private boolean					detailPaneMode		= false;
	private DetailPane				detailPane			= null;
	private GridView				listPanel			= new GridView();
	private boolean					activated			= false;
	private int						tabNo;

	@Override
	public void init(AbstractADWindowContent winPanel, int windowNo, GridTab gridTab, GridWindow gridWindow)
	{
		this.adWindowContent = winPanel;
		this.windowNumber = windowNo;
		this.gridTab = gridTab;
		this.gridWindow = gridWindow;

		if (gridTab.getParentTab() == null)
		{
			log.log(Level.SEVERE, "Parent Tab not found");
			throw new AdempiereException("Parent Tab not found");
		}

		calendarboard = new CalendarWindow(gridTab.getParentTab().getAD_Table_ID(), gridTab.getParentTab()
				.getRecord_ID());

		gridTab.addDataStatusListener(this);
		this.appendChild(calendarboard);
	}

	@Override
	public String get_ValueAsString(String variableName)
	{
		return gridTab.get_ValueAsString(variableName);
	}

	@Override
	public String getDisplayLogic()
	{
		return gridTab.getDisplayLogic();
	}

	@Override
	public int getTabLevel()
	{
		return gridTab.getTabLevel();
	}

	@Override
	public String getTableName()
	{
		return gridTab.getTableName();
	}

	@Override
	public int getRecord_ID()
	{
		return gridTab.getRecord_ID();
	}

	@Override
	public boolean isCurrent()
	{
		return gridTab.isCurrent();
	}

	@Override
	public String getTitle()
	{
		return gridTab.getName();
	}

	@Override
	public void createUI()
	{
		refresh();
	}

	@Override
	public GridTab getGridTab()
	{
		return this.gridTab;
	}

	@Override
	public void activate(boolean b)
	{
		activated = true;
		Event event = new Event(ON_ACTIVATE_EVENT, this, activated);
		Events.postEvent(event);
		if (b)
			calendarboard.isNeedToRender = true;
	}

	@Override
	public void query()
	{
	}

	@Override
	public void refresh()
	{
		calendarboard.setAD_Table_ID(gridTab.getParentTab().getAD_Table_ID());
		calendarboard.setC_BPartner_ID(gridTab.getParentTab().getRecord_ID());
	}

	@Override
	public void query(boolean currentRows, int currentDays, int maxRows)
	{
		if (!isVisible() || (isVisible() && isActivated()))
			calendarboard.renderCalenderEvent();
	}

	@Override
	public void switchRowPresentation()
	{
	}

	@Override
	public void dynamicDisplay(int i)
	{
		calendarboard.isNeedToRender = false;
	}

	@Override
	public void afterSave(boolean onSaveEvent)
	{
	}

	@Override
	public boolean onEnterKey()
	{
		if (listPanel.isVisible())
		{
			return listPanel.onEnterKey();
		}
		return false;
	}

	@Override
	public boolean isGridView()
	{
		return listPanel.isVisible();
	}

	@Override
	public boolean isActivated()
	{
		return activated;
	}

	@Override
	public void setDetailPaneMode(boolean detailMode)
	{
		this.detailPaneMode = detailMode;
		this.setVflex("true");
	}

	@Override
	public boolean isDetailPaneMode()
	{
		return this.detailPaneMode;
	}

	@Override
	public GridView getGridView()
	{
		return this.listPanel;
	}

	@Override
	public boolean needSave(boolean rowChange, boolean onlyRealChange)
	{
		return false;
	}

	@Override
	public boolean dataSave(boolean onSaveEvent)
	{
		return false;
	}

	@Override
	public void setTabNo(int tabNo)
	{
		this.tabNo = tabNo;
	}

	@Override
	public int getTabNo()
	{
		return this.tabNo; // gridTab.getTabNo();
	}

	@Override
	public void setDetailPane(DetailPane detailPane)
	{
		this.detailPane = detailPane;
	}

	@Override
	public DetailPane getDetailPane()
	{
		return detailPane;
	}

	@Override
	public void resetDetailForNewParentRecord()
	{
	}

	@Override
	public ADTreePanel getTreePanel()
	{
		return null;
	}

	@Override
	public boolean isEnableCustomizeButton()
	{
		return false;
	}

	@Override
	public boolean isEnableProcessButton()
	{
		return false;
	}

	@Override
	public List<Button> getToolbarButtons()
	{
		return null;
	}

	@Override
	public void dataStatusChanged(DataStatusEvent e)
	{
		if (e.getCurrentRow() == -1)
			refresh();
		calendarboard.renderCalenderEvent();
	}

	public AbstractADWindowContent getAdWindowContent()
	{
		return adWindowContent;
	}

	public int getWindowNumber()
	{
		return windowNumber;
	}

	public GridWindow getGridWindow()
	{
		return gridWindow;
	}

	@Override
	public void onPageDetached(Page page)
	{
		super.onPageDetached(page);
	}
}
