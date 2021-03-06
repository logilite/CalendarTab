package com.adaxa.calendartab.webui.tabs;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.component.MultiSelectionBox;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.dashboard.ADCalendarContactActivity;
import org.adempiere.webui.dashboard.ADCalendarEvent;
import org.adempiere.webui.dashboard.ADCalendarResourceAssignment;
import org.adempiere.webui.dashboard.ActivityWindow;
import org.adempiere.webui.dashboard.AssignmentWindow;
import org.adempiere.webui.dashboard.CalendarColor;
import org.adempiere.webui.dashboard.DPCalendar;
import org.adempiere.webui.dashboard.EventWindow;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.event.ValueChangeListener;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.theme.ThemeManager;
import org.adempiere.webui.util.UserPreference;
import org.compiere.model.MRefList;
import org.compiere.model.MSysConfig;
import org.compiere.model.MUser;
import org.compiere.model.X_AD_User;
import org.compiere.model.X_C_ContactActivity;
import org.compiere.model.X_R_RequestType;
import org.compiere.model.X_S_Resource;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.compiere.util.ValueNamePair;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.encoders.EncoderUtil;
import org.jfree.chart.encoders.ImageFormat;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.data.general.DefaultPieDataset;
import org.zkoss.calendar.Calendars;
import org.zkoss.calendar.api.CalendarEvent;
import org.zkoss.calendar.event.CalendarsEvent;
import org.zkoss.calendar.impl.SimpleCalendarEvent;
import org.zkoss.calendar.impl.SimpleCalendarModel;
import org.zkoss.image.AImage;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.North;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Span;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbarbutton;

/**
 * Calendar window for Sub Tab of BP [C_BPartner_ID] column
 * 
 * @author Sachin D Bhimani
 * @since Mar 07, 2017
 */
public class CalendarWindow extends Window implements EventListener<Event>, ValueChangeListener
{
	/**
	 * 
	 */
	private static final long						serialVersionUID		= 1576992746053720647L;
	private static CLogger							log						= CLogger.getCLogger(CalendarWindow.class);

	private UserPreference							userPreference;
	private Calendars								calendars;
	private SimpleCalendarModel						scm;
	private Toolbarbutton							btnRefresh;

	private Listbox									lbxRequestTypes;
	private Listbox									lbxContactActivityTypes;
	private Listbox									lbxResources;
	private Image									myChart;
	private Button									btnCurrentDate, btnSwitchTimeZone;
	private Label									lblDate;
	private Label									lblRes;
	private Component								divArrowLeft, divArrowRight;
	private Span									FDOW;
	private Listbox									lbxFDOW;
	private Component								divTabDay, divTabWeek, divTabWeekdays, divTabMonth;
	private Popup									updateMsg;
	private Label									popupLabel;
	private Timer									timer;
	private Checkbox								showRes;

	int												R_RequestType_ID		= 0;
	int												S_Resource_ID			= 0;
	int												old_Request_ID			= 0;
	int												old_Resource_ID			= 0;
	String											ContactActivityType		= "";
	String											old_ContactActivityType	= "";

	private MultiSelectionBox						lbxUserHeirarchy;
	private ActivityWindow							activityWin;
	private AssignmentWindow						assignmentWin;
	private EventWindow								eventWin;

	private Properties								ctx;
	private ArrayList<ADCalendarEvent>				events;
	private ArrayList<ADCalendarContactActivity>	activities;
	private ArrayList<ADCalendarResourceAssignment>	assignments;

	private int										AD_Table_ID				= 0;
	private int										C_BPartner_ID			= 0;
	public boolean									isNeedToRender			= true;

	public CalendarWindow(int AD_Table_ID, int C_BPartner_ID)
	{
		super();
		this.C_BPartner_ID = C_BPartner_ID;

		ctx = new Properties();
		ctx.putAll(Env.getCtx());
		setAttribute(Window.MODE_KEY, Window.MODE_EMBEDDED);

		Component component = Executions.createComponents(ThemeManager.getThemeResource("zul/calendar/calendar.zul"),
				this, null);
		calendars = (Calendars) component.getFellow("cal");

		Borderlayout borderlayout = (Borderlayout) component.getFellow("main");
		borderlayout.setStyle("position: absolute");
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");

		Grid grid = new Grid();
		Rows rows = new Rows();
		grid.appendChild(rows);

		ArrayList<X_AD_User> usersList = DPCalendar.getUserList(Env.getCtx(), Env.getAD_User_ID(Env.getCtx()));
		ArrayList<ValueNamePair> supervisors = new ArrayList<ValueNamePair>();
		supervisors.add(DPCalendar.SALES_REPRESENTATIVE_ALL);
		for (X_AD_User uType : usersList)
			supervisors.add(new ValueNamePair(uType.getAD_User_ID() + "", uType.getName()));

		lbxUserHeirarchy = new MultiSelectionBox(supervisors, true);

		Row row = new Row();
		Cell cell = new Cell();
		cell.appendChild(new Label(Msg.getElement(ctx, MUser.COLUMNNAME_SalesRep_ID) + ":"));
		cell.setWidth("10%");
		row.appendChild(cell);

		cell = new Cell();
		cell.appendChild(lbxUserHeirarchy);
		lbxUserHeirarchy.addValueChangeListener(this);
		cell.setWidth("75%");
		row.appendChild(cell);

		rows.appendChild(row);

		North north = new North();
		north.appendChild(grid);
		borderlayout.appendChild(north);

		calendars = (Calendars) component.getFellow("cal");
		if (calendars.getCurrentDate() != null)
			calendars.setCurrentDate(calendars.getCurrentDate());
		setTimeZone();

		btnRefresh = (Toolbarbutton) component.getFellow("btnRefresh");
		btnRefresh.addEventListener(Events.ON_CLICK, this);
		// btnRefresh.setVisible(false);

		// Request Type
		lbxRequestTypes = (Listbox) component.getFellow("lbxRequestTypes");
		lbxRequestTypes.addEventListener(Events.ON_SELECT, this);

		lbxRequestTypes.appendItem(Msg.getMsg(ctx, "ShowAll"), "0");
		ArrayList<X_R_RequestType> types = DPCalendar.getRequestTypes(Env.getCtx());
		for (X_R_RequestType type : types)
			lbxRequestTypes.appendItem(type.getName(), type.getR_RequestType_ID() + "");
		lbxRequestTypes.setSelectedIndex(0);

		// Contact Activity type
		lbxContactActivityTypes = (Listbox) component.getFellow("lbxContactActivityTypes");
		lbxContactActivityTypes.addEventListener(Events.ON_SELECT, this);

		lbxContactActivityTypes.appendItem(Msg.getMsg(ctx, "ShowAll"), "");
		ArrayList<MRefList> ActivityTypes = DPCalendar.getContactActivityTypes(Env.getCtx());
		for (MRefList type : ActivityTypes)
		{
			String name = MRefList.getListName(Env.getCtx(), type.getAD_Reference_ID(), type.getValue());
			lbxContactActivityTypes.appendItem(name, type.getValue() + "");
		}

		// Resources
		lblRes = (Label) component.getFellow("Resourcelbl");
		lblRes.setValue("Resource:");
		lbxContactActivityTypes.setSelectedIndex(0);

		lbxResources = (Listbox) component.getFellow("lbxResources");
		lbxResources.addEventListener(Events.ON_SELECT, this);

		lbxResources.appendItem(Msg.getMsg(ctx, "ShowAll"), "0");
		ArrayList<X_S_Resource> resources = DPCalendar.getResources(Env.getCtx());
		for (X_S_Resource r : resources)
			lbxResources.appendItem(r.getName(), r.getS_Resource_ID() + "");
		lbxResources.setSelectedIndex(0);

		showRes = (Checkbox) component.getFellow("showRes");
		showRes.addEventListener(Events.ON_CHECK, this);

		userPreference = new UserPreference();
		userPreference.loadPreference(Env.getAD_User_ID(Env.getCtx()));

		if (userPreference.getProperty(UserPreference.P_SHOWRESOURCES).equalsIgnoreCase("Y"))
		{
			lblRes.setVisible(true);
			lbxResources.setVisible(true);
			showRes.setChecked(true);
		}
		else
		{
			lblRes.setVisible(false);
			lbxResources.setVisible(false);
			showRes.setChecked(false);
		}

		myChart = (Image) component.getFellow("mychart");
		myChart.addEventListener(Events.ON_CREATE, this);

		btnCurrentDate = (Button) component.getFellow("btnCurrentDate");
		btnCurrentDate.addEventListener(Events.ON_CLICK, this);

		btnSwitchTimeZone = (Button) component.getFellow("btnSwitchTimeZone");
		btnSwitchTimeZone.addEventListener(Events.ON_CLICK, this);

		lblDate = (Label) component.getFellow("lblDate");
		lblDate.addEventListener(Events.ON_CREATE, this);

		divArrowLeft = component.getFellow("divArrowLeft");
		divArrowLeft.addEventListener("onMoveDate", this);

		divArrowRight = component.getFellow("divArrowRight");
		divArrowRight.addEventListener("onMoveDate", this);

		FDOW = (Span) component.getFellow("FDOW");
		FDOW.addEventListener(Events.ON_CREATE, this);

		lbxFDOW = (Listbox) component.getFellow("lbxFDOW");
		lbxFDOW.addEventListener(Events.ON_SELECT, this);
		lbxFDOW.addEventListener(Events.ON_CREATE, this);

		String[] days = { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday" };
		for (String day : days)
		{
			lbxFDOW.appendItem(Msg.getMsg(Env.getCtx(), day), day);
		}

		divTabDay = component.getFellow("divTabDay");
		divTabDay.addEventListener("onUpdateView", this);

		divTabWeek = component.getFellow("divTabWeek");
		divTabWeek.addEventListener("onUpdateView", this);

		divTabWeekdays = component.getFellow("divTabWeekdays");
		divTabWeekdays.addEventListener("onUpdateView", this);

		divTabMonth = component.getFellow("divTabMonth");
		divTabMonth.addEventListener("onUpdateView", this);

		updateMsg = (Popup) component.getFellow("updateMsg");

		popupLabel = (Label) component.getFellow("popupLabel");

		timer = (Timer) component.getFellow("timer");

		this.appendChild(component);

		calendars.addEventListener("onEventCreate", this);
		calendars.addEventListener("onEventEdit", this);
		calendars.addEventListener("onEventUpdate", this);
		calendars.addEventListener("onMouseOver", this);
		this.addEventListener("onRefresh", this);

		onRefreshModel();
		divTabClicked(0);
	}

	public void onEvent(Event e) throws Exception
	{
		String type = e.getName();

		if (type.equals("onRefresh"))
		{
			renderCalenderEvent();
		}

		if (type.equals(Events.ON_CLICK))
		{
			if (e.getTarget() == btnRefresh)
				renderCalenderEvent();
			else if (e.getTarget() == btnCurrentDate)
				btnCurrentDateClicked();
			else if (e.getTarget() == btnSwitchTimeZone)
				btnSwitchTimeZoneClicked();
		}
		else if (type.equals(Events.ON_CHECK))
		{
			if (e.getTarget() == showRes)
			{
				if (showRes.isChecked())
				{
					lblRes.setVisible(true);
					lbxResources.setVisible(true);
					userPreference.setProperty(UserPreference.P_SHOWRESOURCES, "Y");
				}
				else
				{
					lblRes.setVisible(false);
					lbxResources.setVisible(false);
					userPreference.setProperty(UserPreference.P_SHOWRESOURCES, "N");
				}

				userPreference.savePreference();
				renderCalenderEvent();
			}
		}
		else if (type.equals(Events.ON_CREATE))
		{
			if (e.getTarget() == lblDate)
				updateDateLabel();
			else if (e.getTarget() == FDOW)
				FDOW.setVisible("month".equals(calendars.getMold()) || calendars.getDays() == 7);
			else if (e.getTarget() == myChart)
				syncModel();
			else if (e.getTarget() == lbxFDOW)
				lbxFDOW.setSelectedIndex(0);
		}
		else if (type.equals("onMoveDate"))
		{
			if (e.getTarget() == divArrowLeft)
				divArrowClicked(false);
			else if (e.getTarget() == divArrowRight)
				divArrowClicked(true);
		}
		else if (type.equals("onUpdateView"))
		{
			String text = String.valueOf(e.getData());
			int days = Msg.getMsg(Env.getCtx(), "Day").equals(text) ? 1 : Msg.getMsg(Env.getCtx(), "5Days")
					.equals(text) ? 5 : Msg.getMsg(Env.getCtx(), "Week").equals(text) ? 7 : 0;
			divTabClicked(days);
		}
		else if (type.equals(Events.ON_SELECT))
		{
			if (e.getTarget() == lbxRequestTypes)
			{
				renderCalenderEvent();
			}
			else if (e.getTarget() == lbxFDOW)
			{
				calendars.setFirstDayOfWeek(lbxFDOW.getSelectedItem().getValue().toString());
				syncModel();
			}
			else if (e.getTarget() == lbxContactActivityTypes)
			{
				renderCalenderEvent();
			}
			else if (e.getTarget() == lbxResources)
			{
				renderCalenderEvent();
			}
		}
		else if (type.equals("onEventCreate"))
		{
			CalendarsEvent calendarsEvent = (CalendarsEvent) e;
			DecisionWindow decisionWin = new DecisionWindow(calendarsEvent, this);
			SessionManager.getAppDesktop().showWindow(decisionWin);
		}
		else if (type.equals("onEventEdit"))
		{
			if (e instanceof CalendarsEvent)
			{
				CalendarsEvent calendarsEvent = (CalendarsEvent) e;
				CalendarEvent calendarEvent = calendarsEvent.getCalendarEvent();

				if (calendarEvent instanceof ADCalendarEvent)
				{
					ADCalendarEvent ce = (ADCalendarEvent) calendarEvent;

					if (eventWin == null)
						eventWin = new EventWindow();
					eventWin.setData(ce);
					SessionManager.getAppDesktop().showWindow(eventWin);
				}
				else if (calendarEvent instanceof ADCalendarContactActivity)
				{
					ADCalendarContactActivity ce = (ADCalendarContactActivity) calendarEvent;

					if (activityWin == null)
						activityWin = new ActivityWindow(this);
					activityWin.setData(ce);
					SessionManager.getAppDesktop().showWindow(activityWin);
				}
				else if (calendarEvent instanceof ADCalendarResourceAssignment)
				{
					ADCalendarResourceAssignment ce = (ADCalendarResourceAssignment) calendarEvent;

					if (assignmentWin == null)
						assignmentWin = new AssignmentWindow();
					assignmentWin.setData(ce);
					SessionManager.getAppDesktop().showWindow(assignmentWin);
				}
			}
		}
		else if (type.equals("onEventUpdate"))
		{
			if (e instanceof CalendarsEvent)
			{
				CalendarsEvent evt = (CalendarsEvent) e;
				SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy/MM/d");
				sdf1.setTimeZone(calendars.getDefaultTimeZone());
				StringBuilder sb = new StringBuilder("Update... from ");
				sb.append(sdf1.format(evt.getCalendarEvent().getBeginDate()));
				sb.append(" to ");
				sb.append(sdf1.format(evt.getBeginDate()));
				popupLabel.setValue(sb.toString());
				int left = evt.getX();
				int top = evt.getY();
				if (top + 100 > evt.getDesktopHeight())
					top = evt.getDesktopHeight() - 100;
				if (left + 330 > evt.getDesktopWidth())
					left = evt.getDesktopWidth() - 330;
				updateMsg.open(left, top);
				timer.start();
				org.zkoss.calendar.Calendars cal = (org.zkoss.calendar.Calendars) evt.getTarget();
				SimpleCalendarModel m = (SimpleCalendarModel) cal.getModel();
				SimpleCalendarEvent sce = (SimpleCalendarEvent) evt.getCalendarEvent();
				sce.setBeginDate(evt.getBeginDate());
				sce.setEndDate(evt.getEndDate());
				m.update(sce);
			}
		}
	}

	private void syncModel()
	{
		Hashtable<String, BigDecimal> ht = new Hashtable<String, BigDecimal>();

		if (calendars.getModel() == null)
			return;

		List<?> list = calendars.getModel().get(calendars.getBeginDate(), calendars.getEndDate(), null);
		int size = list.size();
		for (int i = 0; i < size; i++)
		{
			String key;

			if (list.get(i).getClass().equals(ADCalendarEvent.class))
				key = ((ADCalendarEvent) list.get(i)).getR_RequestType_ID() + "";
			else if (list.get(i).getClass().equals(ADCalendarContactActivity.class))
				key = ((ADCalendarContactActivity) list.get(i)).getContactActivityType() + "";
			else if (list.get(i).getClass().equals(ADCalendarResourceAssignment.class))
				key = ((ADCalendarResourceAssignment) list.get(i)).getS_Resource_ID() + "";

			else
				continue;

			if (!ht.containsKey(key))
				ht.put(key, BigDecimal.ONE);
			else
			{
				BigDecimal value = ht.get(key);
				ht.put(key, value.add(BigDecimal.ONE));
			}
		}

		Hashtable<Object, String> htTypes = new Hashtable<Object, String>();
		for (int i = 0; i < lbxRequestTypes.getItemCount(); i++)
		{
			Listitem li = lbxRequestTypes.getItemAtIndex(i);
			if (li != null && li.getValue() != null)
				htTypes.put(li.getValue(), li.getLabel());
		}

		DefaultPieDataset pieDataset = new DefaultPieDataset();
		Enumeration<?> keys = ht.keys();
		while (keys.hasMoreElements())
		{
			String key = (String) keys.nextElement();
			BigDecimal value = ht.get(key);
			String name = (String) htTypes.get(key);
			pieDataset.setValue(name == null ? "" : name, new Double(size > 0 ? value.doubleValue() / size * 100 : 0));
		}

		JFreeChart chart = ChartFactory.createPieChart3D(Msg.getMsg(Env.getCtx(), "EventsAnalysis"), pieDataset, true,
				true, true);
		PiePlot3D plot = (PiePlot3D) chart.getPlot();
		plot.setForegroundAlpha(0.5f);
		BufferedImage bi = chart.createBufferedImage(600, 250);
		try
		{
			byte[] bytes = EncoderUtil.encode(bi, ImageFormat.PNG, true);
			AImage image = new AImage("Pie Chart", bytes);
			myChart.setContent(image);
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "Chart is not rendered-" + e.getLocalizedMessage());
		}
		htTypes = null;
		ht = null;
	}

	private void setTimeZone()
	{
		String alternateTimeZone = MSysConfig.getValue(MSysConfig.CALENDAR_ALTERNATE_TIMEZONE, "Pacific Time=PST",
				Env.getAD_Client_ID(Env.getCtx()));
		TimeZone defaultTimeZone = TimeZone.getDefault();
		calendars.addTimeZone(defaultTimeZone.getDisplayName(), defaultTimeZone);
		if (!Util.isEmpty(alternateTimeZone, true))
		{
			if (!alternateTimeZone.equalsIgnoreCase(defaultTimeZone.getDisplayName()))
			{
				String[] pair = alternateTimeZone.split("=");
				calendars.addTimeZone(pair[0].trim(), pair[1].trim());
			}
		}
	}

	private void updateDateLabel()
	{
		Date b = calendars.getBeginDate();
		Date e = calendars.getEndDate();
		SimpleDateFormat sdfV = DisplayType.getDateFormat();
		sdfV.setTimeZone(calendars.getDefaultTimeZone());
		lblDate.setValue(sdfV.format(b) + " - " + sdfV.format(e));
	}

	private void btnCurrentDateClicked()
	{
		calendars.setCurrentDate(Calendar.getInstance(calendars.getDefaultTimeZone()).getTime());
		updateDateLabel();
		syncModel();
	}

	private void btnSwitchTimeZoneClicked()
	{
		Map<?, ?> zone = calendars.getTimeZones();
		if (!zone.isEmpty())
		{
			@SuppressWarnings("unchecked")
			Map.Entry<TimeZone, String> me = (Map.Entry<TimeZone, String>) zone.entrySet().iterator().next();
			calendars.removeTimeZone((TimeZone) me.getKey());
			calendars.addTimeZone((String) me.getValue(), (TimeZone) me.getKey());
		}
		syncModel();
	}

	private void divArrowClicked(boolean isNext)
	{
		if (isNext)
			calendars.nextPage();
		else
			calendars.previousPage();
		updateDateLabel();
		syncModel();
	}

	private void divTabClicked(int days)
	{
		if (days > 0)
		{
			calendars.setMold("default");
			calendars.setDays(days);
		}
		else
			calendars.setMold("month");
		updateDateLabel();
		FDOW.setVisible("month".equals(calendars.getMold()) || calendars.getDays() == 7);
	}

	@Override
	public void valueChange(ValueChangeEvent e)
	{
		if (e.getSource() == lbxUserHeirarchy)
		{
			Listitem li = lbxRequestTypes.getSelectedItem();
			if (li == null)
				return;
			if (li.getValue() == null)
				return;
			int R_RequestType_ID = Integer.parseInt(li.getValue().toString());

			scm.clear();
			ArrayList<ADCalendarEvent> events = getEvents(R_RequestType_ID, Env.getCtx(), lbxUserHeirarchy.getValues(),
					C_BPartner_ID);
			for (ADCalendarEvent event : events)
			{
				scm.add(event);
			}
			ArrayList<ADCalendarContactActivity> activities = getContactActivities(ContactActivityType, Env.getCtx(),
					lbxUserHeirarchy.getValues(), C_BPartner_ID);
			for (ADCalendarContactActivity activity : activities)
				scm.add(activity);

			userPreference.loadPreference(Env.getAD_User_ID(Env.getCtx()));
			String showres = userPreference.getProperty(UserPreference.P_SHOWRESOURCES);
			if (showres.equalsIgnoreCase("Y"))
			{
				ArrayList<ADCalendarResourceAssignment> resourceAssignments = DPCalendar.getResourceAssignments(
						S_Resource_ID, Env.getCtx());
				for (ADCalendarResourceAssignment resource : resourceAssignments)
					scm.add(resource);
			}
			calendars.invalidate();
			syncModel();
		}
	}

	public void renderCalenderEvent()
	{
		if (!isNeedToRender)
			return;

		if (scm == null)
		{
			scm = new SimpleCalendarModel();
			calendars.setModel(scm);
		}
		scm.clear();

		// For Requests
		Listitem li = lbxRequestTypes.getSelectedItem();
		if (li == null)
			return;

		if (li.getValue() == null)
			return;
		int R_RequestType_ID = Integer.parseInt(li.getValue().toString());

		ArrayList<ADCalendarEvent> events = getEvents(R_RequestType_ID, Env.getCtx(), lbxUserHeirarchy.getValues(),
				C_BPartner_ID);
		for (ADCalendarEvent event : events)
			scm.add(event);

		// For Contact Activities
		li = lbxContactActivityTypes.getSelectedItem();
		if (li == null)
			return;

		if (li.getValue() == null)
			return;
		ContactActivityType = li.getValue();

		ArrayList<ADCalendarContactActivity> activities = getContactActivities(ContactActivityType, Env.getCtx(),
				lbxUserHeirarchy.getValues(), C_BPartner_ID);
		for (ADCalendarContactActivity activity : activities)
			scm.add(activity);

		// For Resources
		userPreference.loadPreference(Env.getAD_User_ID(Env.getCtx()));
		String showres = userPreference.getProperty(UserPreference.P_SHOWRESOURCES);

		if (showres.equals("Y"))
		{
			lblRes.setVisible(true);
			lbxResources.setVisible(true);
			li = lbxResources.getSelectedItem();
			if (li == null)
				return;

			if (li.getValue() == null)
				return;
			S_Resource_ID = Integer.parseInt(li.getValue().toString());

			ArrayList<ADCalendarResourceAssignment> resourceAssignments = DPCalendar.getResourceAssignments(
					S_Resource_ID, Env.getCtx());
			for (ADCalendarResourceAssignment resource : resourceAssignments)
				scm.add(resource);
		}

		calendars.invalidate();
		// syncModel();
	}

	public static ArrayList<ADCalendarEvent> getEvents(int RequestTypeID, Properties ctx,
			ArrayList<ValueNamePair> users, int C_BPartner_ID)
	{
		ArrayList<ADCalendarEvent> events = new ArrayList<ADCalendarEvent>();
		if (C_BPartner_ID == 0)
			return events;

		String userStr = "";
		if (users != null && users.size() > 0 && !users.contains(DPCalendar.SALES_REPRESENTATIVE_ALL))
		{
			for (ValueNamePair i : users)
				userStr += Integer.parseInt(i.getID()) + ",";
			userStr = userStr.substring(0, userStr.length() - 1);
		}

		String sql = "SELECT DISTINCT r.R_Request_ID, r.DateNextAction, " + "r.DateStartPlan, r.DateCompletePlan, "
				+ "u.Name || '-' || r.Summary AS Summary, rt.HeaderColor, rt.ContentColor, rt.R_RequestType_ID "
				+ "FROM R_Request r " + "INNER JOIN R_RequestType rt ON rt.R_RequestType_ID=r.R_RequestType_ID "
				+ "INNER JOIN AD_User u ON u.AD_User_ID=r.SalesRep_ID "
				+ "WHERE r.R_RequestType_ID = rt.R_RequestType_ID ";
		if (users != null && users.size() > 0 && !users.contains(DPCalendar.SALES_REPRESENTATIVE_ALL))
			sql += "AND (r.SalesRep_ID IN (" + userStr + ")) ";
		sql += "AND r.AD_Client_ID = ? AND r.IsActive = 'Y' "
				+ "AND (r.R_Status_ID IS NULL OR r.R_Status_ID IN (SELECT R_Status_ID FROM R_Status WHERE IsClosed='N')) ";

		if (RequestTypeID > 0)
			sql += "AND rt.R_RequestType_ID = ? ";

		if (C_BPartner_ID > 0)
			sql += "AND r.C_BPartner_ID = ? ";

		PreparedStatement ps = null;
		ResultSet rs = null;
		int count = 1;

		try
		{
			ps = DB.prepareStatement(sql, null);
			ps.setInt(count++, Env.getAD_Client_ID(ctx));
			if (RequestTypeID > 0)
				ps.setInt(count++, RequestTypeID);
			if (C_BPartner_ID > 0)
				ps.setInt(count++, C_BPartner_ID);

			rs = ps.executeQuery();

			while (rs.next())
			{
				int R_Request_ID = rs.getInt("R_Request_ID");
				Date dateNextAction = rs.getDate("DateNextAction");
				Timestamp dateStartPlan = rs.getTimestamp("DateStartPlan");
				Timestamp dateCompletePlan = rs.getTimestamp("DateCompletePlan");
				String summary = rs.getString("Summary");
				String headerColor = rs.getString("HeaderColor");
				String contentColor = rs.getString("ContentColor");
				int R_RequestType_ID = rs.getInt("R_RequestType_ID");

				if (dateNextAction != null)
				{
					Calendar cal = Calendar.getInstance();
					cal.setTime(dateNextAction);

					ADCalendarEvent event = new ADCalendarEvent();
					event.setR_Request_ID(R_Request_ID);

					cal.set(Calendar.HOUR_OF_DAY, 0);
					cal.set(Calendar.MINUTE, 0);
					cal.set(Calendar.SECOND, 0);
					cal.set(Calendar.MILLISECOND, 0);
					event.setBeginDate(cal.getTime());

					cal.add(Calendar.HOUR_OF_DAY, 24);
					event.setEndDate(cal.getTime());

					event.setContent(summary);
					event.setHeaderColor(headerColor);
					event.setContentColor(contentColor);
					event.setR_RequestType_ID(R_RequestType_ID);
					event.setLocked(true);
					events.add(event);
				}

				if (dateStartPlan != null && dateCompletePlan != null)
				{

					ADCalendarEvent event = new ADCalendarEvent();
					event.setR_Request_ID(R_Request_ID);

					event.setBeginDate(dateStartPlan);
					event.setEndDate(dateCompletePlan);

					if (event.getBeginDate().compareTo(event.getEndDate()) >= 0)
						continue;

					event.setContent(summary);
					event.setHeaderColor(headerColor);
					event.setContentColor(contentColor);
					event.setR_RequestType_ID(R_RequestType_ID);
					event.setLocked(true);
					events.add(event);
				}
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Request not saved-" + e.getLocalizedMessage());
			e.printStackTrace();
		}
		finally
		{
			DB.close(rs, ps);
		}

		return events;
	} // getEvents

	public static ArrayList<ADCalendarContactActivity> getContactActivities(String ContactActivityType, Properties ctx,
			ArrayList<ValueNamePair> users, int C_BPartner_ID)
	{
		ArrayList<ADCalendarContactActivity> events = new ArrayList<ADCalendarContactActivity>();
		if (C_BPartner_ID == 0)
			return events;

		String userIDs = "";
		StringBuilder where = new StringBuilder(
				" WHERE ca.IsActive = 'Y' AND ca.AD_Client_ID = ? AND ca.C_BPartner_ID = ? ");
		if (users != null && users.size() > 0 && !users.contains(DPCalendar.SALES_REPRESENTATIVE_ALL))
		{
			for (ValueNamePair i : users)
			{
				userIDs += Integer.parseInt(i.getID()) + ",";
			}
			userIDs = userIDs.substring(0, userIDs.length() - 1);
			where.append(" AND au.AD_USER_ID IN (").append(userIDs).append(") ");
		}

		if (ContactActivityType.length() > 0)
			where.append(" AND ca.ContactActivityType = '").append(ContactActivityType).append("' ");

		String sql = "WITH ContactActivityAttendess AS "
				+ "( "
				+ " SELECT ca.C_ContactActivity_ID, STRING_AGG(DISTINCT au.name,',') AS UserNameList "
				+ " FROM C_ContactActivity_V ca "
				+ " LEFT JOIN C_ContactActivity_Attendees cca ON (cca.C_ContactActivity_ID = ca.C_ContactActivity_ID) "
				+ " LEFT JOIN AD_USER au ON (au.AD_USER_ID = cca.salesRep_ID OR au.AD_User_ID = ca.SalesRep_ID) "
				+ where.toString()
				+ " GROUP BY ca.C_ContactActivity_ID "
				+ ") "
				+ "SELECT c.C_ContactActivity_ID, c.StartDate, c.EndDate, c.Comments, c.ContactActivityType, c.Description, rl.Name AS Type, "
				+ "		COALESCE(c.C_BPartner_ID, 0) AS C_BPartner_ID, c.C_Opportunity_ID, "
				+ "		UserNameList ||'-'|| rl.Name AS Name, c.SalesRep_ID AS SalesRep_ID, COALESCE(c.AD_User_ID, 0) AS AD_User_ID, "
				+ "		COALESCE(rlt.Name, rl.Name) AS ActivityTypeName " + "FROM C_ContactActivity c "
				+ "INNER JOIN ContactActivityAttendess caa ON (caa.C_ContactActivity_ID = c.C_ContactActivity_ID) "
				+ "INNER JOIN AD_Ref_List rl ON (rl.Value = c.ContactActivityType AND rl.AD_Reference_ID = ? ) "
				+ "LEFT JOIN AD_Ref_List_Trl rlt ON (rlt.AD_Ref_List_ID = rl.AD_Ref_List_ID AND AD_Language = ? ) ";

		PreparedStatement ps = null;
		ResultSet rs = null;

		try
		{
			ps = DB.prepareStatement(sql, null);
			ps.setInt(1, Env.getAD_Client_ID(ctx));
			ps.setInt(2, C_BPartner_ID);
			ps.setInt(3, X_C_ContactActivity.CONTACTACTIVITYTYPE_AD_Reference_ID);
			ps.setString(4, Env.getAD_Language(Env.getCtx()));

			rs = ps.executeQuery();

			while (rs.next())
			{
				int C_ContactActivity_ID = rs.getInt("C_ContactActivity_ID");

				String activityTypeName = rs.getString("ActivityTypeName");
				Timestamp startTime = rs.getTimestamp("StartDate");
				Timestamp endTime = rs.getTimestamp("EndDate");
				String description = rs.getString("Description");
				String comments = rs.getString("Comments");
				String activityType = rs.getString("ContactActivityType");
				int salesRep_ID = rs.getInt("SalesRep_ID");
				int AD_User_ID = rs.getInt("AD_User_ID");
				int C_Opportunity_ID = rs.getInt("C_Opportunity_ID");

				ADCalendarContactActivity event = new ADCalendarContactActivity();
				event.setC_ContactActivity_ID(C_ContactActivity_ID);
				event.setSalesRep_ID(salesRep_ID);
				event.setAD_User_ID(AD_User_ID);
				event.setC_Opportunity_ID(C_Opportunity_ID);

				MUser salesRep = MUser.get(ctx, salesRep_ID);
				if (salesRep != null)
					event.setSalesRepName(salesRep.getName());

				if (endTime == null)
				{
					Calendar calEnd = Calendar.getInstance();
					calEnd.setTimeInMillis(startTime.getTime());
					calEnd.set(Calendar.HOUR_OF_DAY, 23);
					calEnd.set(Calendar.MINUTE, 59);
					calEnd.set(Calendar.SECOND, 0);
					calEnd.set(Calendar.MILLISECOND, 0);
					endTime = new Timestamp(calEnd.getTimeInMillis());
				}

				event.setBeginDate(startTime);
				event.setEndDate(endTime);

				if (event.getBeginDate().compareTo(event.getEndDate()) >= 0)
					continue;

				if (activityType.equals(X_C_ContactActivity.CONTACTACTIVITYTYPE_Email))
				{
					event.setHeaderColor(CalendarColor.DPCALENDAR_HDR_EMAIL);
					event.setContentColor(CalendarColor.DPCALENDAR_CNT_EMAIL);
				}
				else if (activityType.equals(X_C_ContactActivity.CONTACTACTIVITYTYPE_PhoneCall))
				{
					event.setHeaderColor(CalendarColor.DPCALENDAR_HDR_PHONECALL);
					event.setContentColor(CalendarColor.DPCALENDAR_CNT_PHONECALL);
				}
				else if (activityType.equals(X_C_ContactActivity.CONTACTACTIVITYTYPE_Meeting))
				{
					event.setHeaderColor(CalendarColor.DPCALENDAR_HDR_MEETING);
					event.setContentColor(CalendarColor.DPCALENDAR_CNT_MEETING);
				}
				else if (activityType.equals(X_C_ContactActivity.CONTACTACTIVITYTYPE_Task))
				{
					event.setHeaderColor(CalendarColor.DPCALENDAR_HDR_TASK);
					event.setContentColor(CalendarColor.DPCALENDAR_CNT_TASK);
				}
				if (!(Util.isEmpty(rs.getString("name"))))
					event.setContent(rs.getString("name"));
				else
					event.setContent(rs.getString("type"));

				event.setContent(activityTypeName + ": " + description);
				event.setContactActivityType(activityType);
				event.setDescription(description);
				event.setComments(comments);
				event.setContactActivityTypeName(activityTypeName);
				event.setLocked(true);
				events.add(event);
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Activity not sync with calendar - " + e);
			throw new AdempiereException(e);
		}
		finally
		{
			DB.close(rs, ps);
			rs = null;
			ps = null;
		}

		return events;
	} // getContactActivities

	public void onRefreshModel()
	{
		refreshModel();
		updateUI();
	}

	public void updateUI()
	{
		if (scm == null)
		{
			scm = new SimpleCalendarModel();
			calendars.setModel(scm);
		}

		scm.clear();
		for (ADCalendarEvent event : events)
			scm.add(event);

		for (ADCalendarContactActivity activity : activities)
			scm.add(activity);

		if (assignments != null)
			for (ADCalendarResourceAssignment resource : assignments)
				scm.add(resource);

		calendars.invalidate();

	} // updateUI

	private void refreshModel()
	{
		events = getEvents(0, ctx, null, getC_BPartner_ID());
		activities = getContactActivities("", ctx, null, getC_BPartner_ID());
		assignments = DPCalendar.getResourceAssignments(0, ctx);
	}

	@Override
	public void onPageAttached(Page newpage, Page oldpage)
	{
		super.onPageAttached(newpage, oldpage);
		boolean isRender = isNeedToRender;
		isNeedToRender = true;
		if (!isRender)
		{
			calendars.setModel(null);
			scm = null;
			renderCalenderEvent();
		}
	}

	@Override
	public void onPageDetached(Page page)
	{
		super.onPageDetached(page);
		calendars.setModel(null);
		scm = null;
		isNeedToRender = false;
	}

	public int getAD_Table_ID()
	{
		return AD_Table_ID;
	}

	public void setAD_Table_ID(int AD_Table_ID)
	{
		this.AD_Table_ID = AD_Table_ID;
	}

	public int getC_BPartner_ID()
	{
		return C_BPartner_ID;
	}

	public void setC_BPartner_ID(int C_BPartner_ID)
	{
		this.C_BPartner_ID = C_BPartner_ID;
	}
}