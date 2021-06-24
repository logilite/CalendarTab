package com.adaxa.calendartab.webui.adwindow;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.adwindow.ADTreePanel;
import org.adempiere.webui.adwindow.ADWindow;
import org.adempiere.webui.adwindow.ADWindowToolbar;
import org.adempiere.webui.adwindow.AbstractADWindowContent;
import org.adempiere.webui.adwindow.DetailPane;
import org.adempiere.webui.adwindow.GridView;
import org.adempiere.webui.adwindow.IADTabpanel;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.ToolBarButton;
import org.compiere.model.DataStatusEvent;
import org.compiere.model.DataStatusListener;
import org.compiere.model.GridTab;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Button;
import org.zkoss.zul.Toolbar;

import com.adaxa.calendartab.webui.tabs.CalendarWindow;

/**
 * Calendar Tab Panel
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 * @since  Mar 07, 2017
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
	private CalendarWindow			calendarboard		= null;
	private DetailPane				detailPane			= null;
	private GridView				listPanel			= new GridView();

	private boolean					detailPaneMode		= false;
	private boolean					activated			= false;

	private int						windowNumber		= 0;
	private int						tabNo;

	@Override
	public void init(AbstractADWindowContent winPanel, GridTab gridTab)
	{
		this.adWindowContent = winPanel;
		this.windowNumber = winPanel.getWindowNo();
		this.gridTab = gridTab;

		if (gridTab.getParentTab() == null)
		{
			log.log(Level.SEVERE, "Parent Tab not found");
			throw new AdempiereException("Parent Tab not found");
		}

		calendarboard = new CalendarWindow(gridTab.getParentTab().getAD_Table_ID(), getC_BPartner_ID());
		this.appendChild(calendarboard);

		gridTab.addDataStatusListener(this);

	}

	public int getC_BPartner_ID()
	{
		if (Util.isEmpty(get_ValueAsString("C_BPartner_ID"), true))
			return 0;
		else
			return Integer.parseInt(get_ValueAsString("C_BPartner_ID"));
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
		return gridTab != null ? gridTab.isCurrent() : false;
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
	public void activate(boolean isActivate)
	{
		activated = true;
		Event event = new Event(ON_ACTIVATE_EVENT, this, activated);
		Events.postEvent(event);

		if (isActivate)
		{
			calendarboard.isNeedToRender = true;

			//
			if (adWindowContent.getADTab().getSelectedTabpanel().getDetailPane() != null)
			{
				setDetailPane(adWindowContent.getADTab().getSelectedTabpanel().getDetailPane());
			}
		}
		else
		{
			updateToolbar(adWindowContent.getToolbar());
		}
	} // activate

	@Override
	public void query()
	{
	}

	@Override
	public void refresh()
	{
		calendarboard.setAD_Table_ID(gridTab.getParentTab().getAD_Table_ID());
		calendarboard.setC_BPartner_ID(getC_BPartner_ID());
	}

	@Override
	public void query(boolean currentRows, int currentDays, int maxRows)
	{
		if (!isVisible() || (isVisible() && isActivated()))
		{
			// calendarboard.renderCalenderEvent();
			gridTab.query(currentRows, currentDays, maxRows);

			// Empty status show
			if (adWindowContent.getADTab().getSelectedTabpanel().getDetailPane() != null)
			{
				adWindowContent.getADTab().getSelectedTabpanel().getDetailPane().setStatusMessage("1 " + Msg.getMsg(Env.getCtx(), "Records"), false);
			}
		}
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
		return false;
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

		// Show empty record count info for Calendar
		if (!detailMode)
		{
			// Disabling Navigation buttons
			adWindowContent.getBreadCrumb().enableFirstNavigation(false);
			adWindowContent.getBreadCrumb().enableLastNavigation(false);
			adWindowContent.getBreadCrumb().setStatusDB(null);
		}
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
		return this.tabNo;
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
		return new ArrayList<Button>();
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

	@Override
	public void onPageDetached(Page page)
	{
		super.onPageDetached(page);
	}

	@Override
	public boolean isEnableQuickFormButton()
	{
		return false;
	}

	@Override
	public void updateToolbar(ADWindowToolbar toolbar)
	{
		if (activated)
		{
			toolbar.enableActiveWorkflows(false);
			toolbar.enableArchive(false);
			toolbar.enableAttachment(false);
			toolbar.enableChat(false);
			toolbar.enableCopy(false);
			toolbar.enableCSVImport(false);
			toolbar.enableCustomize(false);
			toolbar.enableDelete(false);
			toolbar.enableExport(false);
			toolbar.enableFileImport(false);
			toolbar.enableIgnore(false);
			toolbar.enableNew(false);
			toolbar.enablePostIt(false);
			toolbar.enablePrint(false);
			toolbar.enableProcessButton(false);
			toolbar.enableQuickForm(false);
			toolbar.enableReport(false);
			toolbar.enableRefresh(false);
			toolbar.enableRequests(false);
			toolbar.enableSave(false);
			toolbar.enableZoomAcross(false);
		}

		toolbar.enableFind(!activated);
		toolbar.enableGridToggle(!activated);
	} // updateToolbar

	@Override
	public void updateDetailToolbar(Toolbar toolbar)
	{
		ADWindow adwindow = ADWindow.findADWindow(this);
		List<String> tabRestrictList = adwindow.getTabToolbarRestrictList(getGridTab().getAD_Tab_ID());
		List<String> windowRestrictList = adwindow.getWindowToolbarRestrictList();

		for (Component c : toolbar.getChildren())
		{
			if (c instanceof ToolBarButton)
			{
				ToolBarButton btn = (ToolBarButton) c;
				if (DetailPane.BTN_NEW_ID.equals(btn.getId()))
				{
					btn.setDisabled(true);
				}
				else if (DetailPane.BTN_DELETE_ID.equals(btn.getId()))
				{
					btn.setDisabled(true);
				}
				else if (DetailPane.BTN_EDIT_ID.equals(btn.getId()))
				{
					btn.setDisabled(false);
				}
				else if (DetailPane.BTN_SAVE_ID.equals(btn.getId()))
				{
					btn.setDisabled(true);
				}
				else if (DetailPane.BTN_CUSTOMIZE_ID.equals(btn.getId()))
				{
					btn.setDisabled(true);
				}
				else if (DetailPane.BTN_QUICK_FORM_ID.equals(btn.getId()))
				{
					btn.setDisabled(true);
				}
				else if (DetailPane.BTN_PROCESS_ID.equals(btn.getId()))
				{
					btn.setDisabled(true);
				}
				else if ("BtnToggle".equals(btn.getId()))
				{
					btn.setDisabled(true);
				}
				if (windowRestrictList.contains(btn.getId()))
				{
					btn.setVisible(false);
				}
				else if (tabRestrictList.contains(btn.getId()))
				{
					btn.setVisible(false);
				}
				else
				{
					btn.setVisible(true);
				}
			}
		}
	} // updateDetailToolbar
}
