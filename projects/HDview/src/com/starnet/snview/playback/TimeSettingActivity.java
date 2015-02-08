package com.starnet.snview.playback;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.starnet.snview.R;
import com.starnet.snview.channelmanager.Channel;
import com.starnet.snview.channelmanager.xml.PinyinComparator;
import com.starnet.snview.component.BaseActivity;
import com.starnet.snview.component.wheelview.widget.NumericWheelAdapter;
import com.starnet.snview.component.wheelview.widget.OnWheelScrollListener;
import com.starnet.snview.component.wheelview.widget.WheelView;
import com.starnet.snview.devicemanager.DeviceItem;
import com.starnet.snview.playback.utils.LoginDeviceItem;
import com.starnet.snview.playback.utils.TLV_V_RecordInfo;
import com.starnet.snview.playback.utils.TLV_V_SearchRecordRequest;
import com.starnet.snview.protocol.message.OWSPDateTime;
import com.starnet.snview.syssetting.CloudAccount;
import com.starnet.snview.util.NetWorkUtils;

@SuppressLint({ "SimpleDateFormat", "HandlerLeak" })
public class TimeSettingActivity extends BaseActivity {

	private int dayNum;
	private int curyear;
	private int curMonth;
	private int curDate;
	private int curHour;
	private int curMint;

	private Context ctx;
	private WheelView day;
	private WheelView hour;
	private WheelView year;
	private WheelView month;
	private WheelView minute;
	private View endtimeView;
	private View starttimeView;
	private View remotePreView;
	private TextView endtimeTxt;
	private TextView startTimeTxt;
	private PopupWindow typePopupWindow;
	private PopupWindow timePopupWindow;
	private NumericWheelAdapter dayAdapter;
	private NumericWheelAdapter yearAdapter;
	private NumericWheelAdapter hourAdapter;
	private NumericWheelAdapter monthAdapter;
	private NumericWheelAdapter minuteAdapter;

	private TextView videoType;
	private Button staBtn0;
	private Button staBtn1;
	private Button staBtn2;
	private Button staBtn3;
	private Button staBtn4;
	private String typeSD;
	private String typeDsh;
	private String typeAll;
	private String typeYDZC;
	private String typeKGLJG;

	private TLV_V_SearchRecordRequest srr;
	private LoginDeviceItem loginItem;
	private DeviceItem visitDevItem;
	private Button startScanBtn;
	private ProgressDialog prg;
	
	private boolean endFlag = false;
	private boolean startFlag = false;
	private final int TIMEOUT = 0x0002;
	private final int LOADSUC = 0x0003;
	private final int LOADFAI = 0x0004;
	private DeviceItemRequestTask[] tasks;
	private List<CloudAccount> originCAs;
	private final int REQUESTCODE = 0x0005;
	private final int TIMESETTING = 0x0007;
	private final int REQUESTCODE_DOG = 0x0005;
	private final int NOTIFYREMOTEUIFRESH_SUC = 0x0008;
	private final int NOTIFYREMOTEUIFRESH_FAIL = 0x0009;
	private final int NOTIFYREMOTEUIFRESH_TMOUT = 0x0006;

	private ExpandableListView cloudAccountView;
	private AccountsPlayBackExpanableAdapter actsAdapter;

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case TIMEOUT:
				Bundle msgD = msg.getData();
				CloudAccount netCA1 = (CloudAccount) msgD.getSerializable("netCA");
				String reqExt = getString(R.string.playback_req_extime);
				showToast(netCA1.getUsername() + reqExt);
				int positi = msgD.getInt("position");
				netCA1.setRotate(true);
				originCAs.set(positi, netCA1);
				actsAdapter.notifyDataSetChanged();
				break;
			case LOADSUC:
				msgD = msg.getData();
				final int posi = msgD.getInt("position");
				String suc = msgD.getString("success");
				if (suc.equals("Yes")) {
					final int pos = Integer.valueOf(posi);
					final CloudAccount netCA = (CloudAccount) msgD.getSerializable("netCA");
					netCA.setRotate(true);
					if (netCA != null) {
						List<DeviceItem> dList = netCA.getDeviceList();
						if ((dList != null) && (dList.size() > 0)) {
							Collections.sort(dList, new PinyinComparator());// 排序...
						}
					}
					originCAs.set(pos, netCA);
					actsAdapter.notifyDataSetChanged();
				}
				break;
			case LOADFAI:
				
				msgD = msg.getData();
				int posit = msgD.getInt("position");
				CloudAccount netCA2 = (CloudAccount) msgD.getSerializable("netCA");
				String fail = getString(R.string.playback_req_fail);
				showToast(netCA2.getUsername() + fail);
				netCA2.setRotate(true);
				originCAs.set(posit, netCA2);
				actsAdapter.notifyDataSetChanged();
				break;
			case NOTIFYREMOTEUIFRESH_SUC:
				dismissPRGDialog();
				Bundle bundle = msg.getData();
				ArrayList<TLV_V_RecordInfo> srres = bundle.getParcelableArrayList("srres");
				Intent data = new Intent();
				Bundle bu = new Bundle();
				bu.putParcelableArrayList("srres", srres);
				data.putExtras(bu);
				setResult(TIMESETTING, data);
				TimeSettingActivity.this.finish();
				break;
			case NOTIFYREMOTEUIFRESH_FAIL:
				dismissPRGDialog();
				showContentToast(getString(R.string.playback_netvist_datanull));
				break;
			case NOTIFYREMOTEUIFRESH_TMOUT:
				dismissPRGDialog();
				showContentToast(getString(R.string.playback_netvist_timeout));
				break;
			default:
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.playback_time_setting_activity);
		initViews();
		setExtPandableListview();
		setListenersForWadgets();
	}

	private void dismissPRGDialog() {
		if (prg != null && prg.isShowing()) {
			prg.dismiss();
		}
	}

	/** 为用户添加设备数据 **/
	private void setExtPandableListview() {
		originCAs = downloadDatas();
		actsAdapter = new AccountsPlayBackExpanableAdapter(ctx, originCAs);
		cloudAccountView.setAdapter(actsAdapter);
	}

	/** 加载新的数据 **/
	private List<CloudAccount> downloadDatas() {
		List<CloudAccount> accounts = PlaybackUtils.getCloudAccounts();
		if (accounts != null) {
			boolean isOpen = NetWorkUtils.checkNetConnection(ctx);
			int enableSize = PlaybackUtils.getEnableCACount(accounts);
			tasks = new DeviceItemRequestTask[enableSize];
			if (isOpen) {
				int j = 0;
				for (int i = 0; i < accounts.size(); i++) {
					CloudAccount c = accounts.get(i);
					if (c.isEnabled()) {
						tasks[j] = new DeviceItemRequestTask(ctx, c, mHandler,
								i);
						tasks[j].start();
						j++;
					}
				}
			}
		}
		return accounts;
	}

	private void backAndLeftOperation() {
		dismissTimeDialog();
		if (tasks != null) {
			for (int i = 0; i < tasks.length; i++) {
				if (tasks[i] != null) {
					tasks[i].setCanceled(true);
				}
			}
		}
		TimeSettingActivity.this.finish();
	}

	private void setListenersForWadgets() {

		setVideoTypeOnClick();

		super.getLeftButton().setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				backAndLeftOperation();
			}
		});

		cloudAccountView.setOnGroupClickListener(new OnGroupClickListener() {
			@Override
			public boolean onGroupClick(ExpandableListView parent, View v,int groupPosition, long id) {
				CloudAccount cA = (CloudAccount) parent.getExpandableListAdapter().getGroup(groupPosition);// 获取用户账号信息
				if (cA.isExpanded()) {// 判断列表是否已经展开
					cA.setExpanded(false);
				} else {
					cA.setExpanded(true);
				}
				return false;
			}
		});

		starttimeView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (typePopupWindow != null && typePopupWindow.isShowing()) {
					typePopupWindow.dismiss();
					cloudAccountView.setVisibility(View.VISIBLE);
				}
				startFlag = true;
				endFlag = false;
				if (timePopupWindow.isShowing()) {
					timePopupWindow.dismiss();
				} else {
					timePopupWindow.showAsDropDown(v);
					timePopupWindow.setFocusable(false);
					timePopupWindow.setOutsideTouchable(true);
					timePopupWindow.update();
				}
			}
		});

		endtimeView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (typePopupWindow != null && typePopupWindow.isShowing()) {
					typePopupWindow.dismiss();
					cloudAccountView.setVisibility(View.VISIBLE);
				}
				endFlag = true;
				startFlag = false;
				if (timePopupWindow.isShowing()) {
					timePopupWindow.dismiss();
				} else {
					timePopupWindow.showAsDropDown(v);
					timePopupWindow.setFocusable(false);
					timePopupWindow.setOutsideTouchable(true);
					timePopupWindow.update();
				}
			}
		});

		remotePreView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				if (timePopupWindow != null && timePopupWindow.isShowing()) {
					timePopupWindow.dismiss();
				}

				if (typePopupWindow.isShowing()) {
					typePopupWindow.dismiss();
					cloudAccountView.setVisibility(View.VISIBLE);
				} else {
					cloudAccountView.setVisibility(View.GONE);
					typePopupWindow.showAsDropDown(v);
					typePopupWindow.setFocusable(false);
					typePopupWindow.setOutsideTouchable(true);
					typePopupWindow.update();

				}
			}
		});

		startScanBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean isOpen = NetWorkUtils.checkNetConnection(ctx);
				if (isOpen) {
					startPlayBack();
				} else {
					showContentToast(getString(R.string.playback_network_not_open));
				}
			}
		});
	}

	private void setVideoTypeOnClick() {

		staBtn0.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				videoType.setText(typeAll);
				staBtn0.setBackgroundResource(R.drawable.channellist_select_alled);
				staBtn1.setBackgroundResource(R.drawable.channellist_select_empty);
				staBtn2.setBackgroundResource(R.drawable.channellist_select_empty);
				staBtn3.setBackgroundResource(R.drawable.channellist_select_empty);
				staBtn4.setBackgroundResource(R.drawable.channellist_select_empty);

			}
		});

		staBtn1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				videoType.setText(typeSD);
				staBtn0.setBackgroundResource(R.drawable.channellist_select_empty);
				staBtn1.setBackgroundResource(R.drawable.channellist_select_alled);
				staBtn2.setBackgroundResource(R.drawable.channellist_select_empty);
				staBtn3.setBackgroundResource(R.drawable.channellist_select_empty);
				staBtn4.setBackgroundResource(R.drawable.channellist_select_empty);

			}
		});
		staBtn2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				videoType.setText(typeDsh);
				staBtn0.setBackgroundResource(R.drawable.channellist_select_empty);
				staBtn2.setBackgroundResource(R.drawable.channellist_select_alled);
				staBtn1.setBackgroundResource(R.drawable.channellist_select_empty);
				staBtn3.setBackgroundResource(R.drawable.channellist_select_empty);
				staBtn4.setBackgroundResource(R.drawable.channellist_select_empty);
			}
		});
		staBtn3.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				videoType.setText(typeYDZC);
				staBtn3.setBackgroundResource(R.drawable.channellist_select_alled);
				staBtn2.setBackgroundResource(R.drawable.channellist_select_empty);
				staBtn1.setBackgroundResource(R.drawable.channellist_select_empty);
				staBtn4.setBackgroundResource(R.drawable.channellist_select_empty);
				staBtn0.setBackgroundResource(R.drawable.channellist_select_empty);
			}
		});
		staBtn4.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				videoType.setText(typeKGLJG);
				staBtn0.setBackgroundResource(R.drawable.channellist_select_empty);
				staBtn4.setBackgroundResource(R.drawable.channellist_select_alled);
				staBtn2.setBackgroundResource(R.drawable.channellist_select_empty);
				staBtn3.setBackgroundResource(R.drawable.channellist_select_empty);
				staBtn1.setBackgroundResource(R.drawable.channellist_select_empty);
			}
		});
	}

	private void showContentToast(String content) {
		Toast.makeText(ctx, content, Toast.LENGTH_SHORT).show();
	}

	/** 开始进行回放操作 **/
	private void startPlayBack() {
		if (!okFlag) {// if (!okFlag) {
			showContentToast(ctx.getString(R.string.playback_content_null));
		} else {
			String vType = videoType.getText().toString();
			int rTyPe = setRecordTypeAcc(vType);
			if (rTyPe == -1) {
				showContentToast(ctx.getString(R.string.playback_videotype_null));
			} else {
				setDataToPlayActivity();
			}
		}
	}

	private void setDataToPlayActivity() {
		dismissPRGDialog();
		Bundle bundle = new Bundle();
		Intent data = new Intent();
		srr = getRequestInfo();
		loginItem = new LoginDeviceItem();
		if (visitDevItem!=null) {
			String svrIp = visitDevItem.getSvrIp();
			String svrPort = visitDevItem.getSvrPort();
			String svrPass = visitDevItem.getLoginPass();
			String svrUser = visitDevItem.getLoginUser();

			String svrIps[] = svrIp.split("\\.");
			bundle.putString("svrPort", svrPort);
			bundle.putString("svrPass", svrPass);
			bundle.putString("svrUser", svrUser);
			bundle.putStringArray("svrIps", svrIps);
			
			loginItem.setLoginUser(svrUser);
			loginItem.setLoginPass(svrPass);
			loginItem.setSvrIP(svrIps);
			loginItem.setSvrPort(svrPort);
			bundle.putParcelable("loginItem", loginItem);
		}
		bundle.putParcelable("srr", srr);
		data.putExtras(bundle);
		setResult(TIMESETTING, data);
		TimeSettingActivity.this.finish();
	}

	private TLV_V_SearchRecordRequest getRequestInfo() {
		TLV_V_SearchRecordRequest srr = new TLV_V_SearchRecordRequest();
		String startTime = (String) startTimeTxt.getText();
		String endTime = (String) endtimeTxt.getText();
		visitDevItem = (DeviceItem) actsAdapter.getChild(clickGroup, clickChild);

		OWSPDateTime sTime = PlaybackUtils.getOWSPDateTime(startTime);
		OWSPDateTime eTime = PlaybackUtils.getOWSPDateTime(endTime);

		String vType = videoType.getText().toString();
		setRecordTypeAcc(vType);
		int channel = 0;
		if (visitDevItem != null) {
			channel = PlaybackUtils.getScanChannel(visitDevItem.getChannelList());
		}
		srr.setStartTime(sTime);
		srr.setEndTime(eTime);
		srr.setCount(255);
		srr.setChannel(channel);
		srr.setDeviceId(0);
		srr.setRecordType(recordType);
		srr.setReserve(new int[] { 0, 0, 0 });
		return srr;
	}

	private void showToast(String content) {
		Toast.makeText(ctx, content, Toast.LENGTH_SHORT).show();
	}

	private void initViews() {
		super.setToolbarVisiable(false);
		super.getRightButton().setVisibility(View.GONE);
		super.getExtendButton().setVisibility(View.GONE);
		super.setLeftButtonBg(R.drawable.navigation_bar_back_btn_selector);
		super.setTitleViewText(getString(R.string.navigation_title_remote_playback));
		ctx = TimeSettingActivity.this;
		endtimeTxt = (TextView) findViewById(R.id.endtime);
		endtimeView = findViewById(R.id.input_endtime_view);
		videoType = (TextView) findViewById(R.id.video_type);
		startScanBtn = (Button) findViewById(R.id.startScan);
		remotePreView = findViewById(R.id.input_remote_type);
		startTimeTxt = (TextView) findViewById(R.id.starttime);
		starttimeView = findViewById(R.id.input_starttime_view);
		cloudAccountView = (ExpandableListView) findViewById(R.id.cloudaccountExtListview);
		cloudAccountView.setGroupIndicator(null);

		typeSD = getString(R.string.playback_alarm_type1);
		typeAll = getString(R.string.playback_alarm_type);
		typeDsh = getString(R.string.playback_alarm_type2);
		typeYDZC = getString(R.string.playback_alarm_type3);
		typeKGLJG = getString(R.string.playback_alarm_type4);

		setCurrentTimeForTxt();
		initTimePopupWindow();
		initTypePopWindow();
	}

	/** 对开始、结束时间设置为当前时间 **/
	@SuppressWarnings("deprecation")
	private void setCurrentTimeForTxt() {
		Date d = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		String dateNowStr = sdf.format(d);
		endtimeTxt.setText(dateNowStr);
		Date startDate = new Date();
		Calendar c = Calendar.getInstance();
		int day = c.get(Calendar.DAY_OF_MONTH);
		if (day <= 4) {
			startDate.setDate(1);
			String dateStr = sdf.format(startDate);
			startTimeTxt.setText(dateStr);
		} else {
			int tempDay = day - 3;
			startDate.setDate(tempDay);
			String dateStr = sdf.format(startDate);
			startTimeTxt.setText(dateStr);
		}
	}

	private void initTypePopWindow() {
		LayoutInflater inflater = LayoutInflater.from(ctx);
		View view = inflater.inflate(R.layout.type_popupwindow, null);
		typePopupWindow = new PopupWindow(view, LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
		typePopupWindow.setAnimationStyle(R.style.PopupAnimation);
		View view2 = typePopupWindow.getContentView();

		staBtn0 = (Button) view2.findViewById(R.id.stateBtn0);
		staBtn1 = (Button) view2.findViewById(R.id.stateBtn1);
		staBtn2 = (Button) view2.findViewById(R.id.stateBtn2);
		staBtn3 = (Button) view2.findViewById(R.id.stateBtn3);
		staBtn4 = (Button) view2.findViewById(R.id.stateBtn4);

		view2.getRootView().setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (typePopupWindow != null && typePopupWindow.isShowing()) {
					typePopupWindow.dismiss();
					cloudAccountView.setVisibility(View.VISIBLE);
				}
			}
		});
	}

	private void initTimePopupWindow() {
		LayoutInflater inflater = LayoutInflater.from(ctx);
		View view = inflater.inflate(R.layout.time_dialog, null);
		timePopupWindow = new PopupWindow(view, LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT);
		View view2 = timePopupWindow.getContentView();

		year = (WheelView) view2.findViewById(R.id.year);
		month = (WheelView) view2.findViewById(R.id.month);
		day = (WheelView) view2.findViewById(R.id.day);
		hour = (WheelView) view2.findViewById(R.id.hour);
		minute = (WheelView) view2.findViewById(R.id.minute);
		timePopupWindow.setAnimationStyle(R.style.PopupAnimation);
		setWheelView();
	}

	private void setWheelView() {
		Calendar c1 = Calendar.getInstance();
		Calendar c = Calendar.getInstance();
		curyear = c.get(Calendar.YEAR);
		curMonth = c.get(Calendar.MONTH);
		curDate = c.get(Calendar.DAY_OF_MONTH);
		curHour = c.get(Calendar.HOUR_OF_DAY);
		curMint = c.get(Calendar.MINUTE);

		yearAdapter = new NumericWheelAdapter(2009, c1.get(Calendar.YEAR));
		year.setAdapter(yearAdapter);
		year.setLabel(null);
		year.setCyclic(true);

		monthAdapter = new NumericWheelAdapter(1, 12, "%02d");
		month.setAdapter(monthAdapter);
		month.setLabel(null);
		month.setCyclic(true);
		curMonth += 1;

		dayNum = setwheelDay(curyear, curMonth);
		dayAdapter = new NumericWheelAdapter(1, dayNum, "%02d");
		day.setAdapter(dayAdapter);
		day.setLabel(null);
		day.setCyclic(true);

		hourAdapter = new NumericWheelAdapter(0, 23, "%02d");
		hour.setAdapter(hourAdapter);
		hour.setLabel(null);
		hour.setCyclic(true);

		minuteAdapter = new NumericWheelAdapter(0, 59, "%02d");
		minute.setAdapter(minuteAdapter);
		minute.setLabel(null);
		minute.setCyclic(true);

		year.setCurrentItem(curyear + 2);
		month.setCurrentItem(curMonth + 1);
		day.setCurrentItem(curDate + 1);
		hour.setCurrentItem(curHour + 2);
		minute.setCurrentItem(curMint + 2);

		year.addScrollingListener(scrollListener);
		month.addScrollingListener(scrollListener);
		day.addScrollingListener(scrollListener);
		hour.addScrollingListener(scrollListener);
		minute.addScrollingListener(scrollListener);

	}

	private int setwheelDay(int year, int month) {
		int day = 31;
		if (month == 2) {// 闰年
			if ((year % 4 == 0) && ((year % 100 != 0) || (year % 400 == 0))) {
				day = 29;
			} else {
				day = 28;
			}
		}
		if (month == 4 || month == 6 || month == 9 || month == 11) {
			day = 30;
		}
		return day;
	}

	private void dismissTimeDialog() {
		if (timePopupWindow != null && timePopupWindow.isShowing()) {
			timePopupWindow.dismiss();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK
				&& event.getAction() == KeyEvent.ACTION_DOWN) {
			backAndLeftOperation();
		}
		return super.onKeyDown(keyCode, event);
	}

	private boolean okFlag = false;
	private int clickGroup;
	private int clickChild;
	private int recordType;
	private DeviceItem visitDItem;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUESTCODE) {
			if (data != null) {
				okFlag = data.getBooleanExtra("okBtn", false);
				if (okFlag) {
					actsAdapter.setOkFlag(true);
					clickGroup = data.getIntExtra("group", 0);
					clickChild = data.getIntExtra("child", 0);
					int tempCh = data.getIntExtra("chnl", 0);
					visitDItem = (DeviceItem) actsAdapter.getChild(clickGroup,clickChild);
					List<Channel> channels = visitDItem.getChannelList();
					for (int i = 0; i < channels.size(); i++) {
						if (i == tempCh) {
							channels.get(i).setSelected(true);
						} else {
							channels.get(i).setSelected(false);
						}
					}
					actsAdapter.setChild(clickChild);
					actsAdapter.setGroup(clickGroup);
					actsAdapter.notifyDataSetChanged();
				}
			}
		}
	}

	private int setRecordTypeAcc(String type2) {
		String typeShAll = getString(R.string.playback_alarm_type);
		String typeShD = getString(R.string.playback_alarm_type1);
		String typeDsh = getString(R.string.playback_alarm_type2);
		String typeYDZC = getString(R.string.playback_alarm_type3);
		String typeKGLJG = getString(R.string.playback_alarm_type4);
		if (type2.equals(typeShD)) {
			recordType = 8;
		} else if (type2.equals(typeDsh)) {
			recordType = 4;
		} else if (type2.equals(typeYDZC)) {
			recordType = 2;
		} else if (type2.equals(typeKGLJG)) {
			recordType = 1;
		} else if (type2.equals(typeShAll)) {
			recordType = 0;
		} else {
			recordType = -1;
		}
		return recordType;
	}

	private OnWheelScrollListener scrollListener = new OnWheelScrollListener() {
		@Override
		public void onScrollingStarted(WheelView wheel) {
		}

		@Override
		public void onScrollingFinished(WheelView wheel) {
			String yearNum = getTime(year);
			int yNum = Integer.valueOf(yearNum);
			String monNums = getTime(month);
			int moNum = Integer.valueOf(monNums);
			dayNum = setwheelDay(yNum, moNum);
			day.setAdapter(new NumericWheelAdapter(1, dayNum, "%02d"));
			String dayTime = getTime(day);
			String hourTime = getTime(hour);
			String minTime = getTime(minute);

			String contentDate = yearNum + "-" + monNums + "-" + dayTime;
			String contentHm = hourTime + ":" + minTime;
			String content = contentDate + " " + contentHm;

			if (startFlag) {
				String endTime = endtimeTxt.getText().toString();
				String startTime = startTimeTxt.getText().toString();
				String eTime[] = endTime.split(" ");
				String sTime[] = startTime.split(" ");
				startTimeTxt.setText(content);
				int dayTimeNum = Integer.valueOf(dayTime);
				boolean isLeaapYear = PlaybackUtils.isLeapYear(yNum);
				String newContDate = setDate(isLeaapYear, dayTimeNum, yearNum,moNum, monNums);
				content = newContDate + " " + contentHm;
				endtimeTxt.setText(content);
				 if (sTime[0].equals(eTime[0])) {// 日期相同
				 //允许修改小时和分钟
				 } else {// 日期不同
				 }
			} else if (endFlag) {
				String newContDate = "";
				endtimeTxt.setText(content);
				int dayTimeNum = Integer.valueOf(dayTime);
				boolean isLeaapYear = PlaybackUtils.isLeapYear(yNum);
				if (isLeaapYear) {
					if (moNum == 2) {
						if (dayTimeNum > 3) {
							String data = "";
							if ((dayTimeNum - 3) < 10) {
								data = "0" + (dayTimeNum - 3);
							} else {
								data = "" + (dayTimeNum - 3);
							}
							newContDate = yearNum + "-" + monNums + "-" + data;
						} else {
							newContDate = yearNum + "-" + monNums + "-" + "01";
						}
					}
				} else {
					if (moNum == 2) {
						if (dayTimeNum > 3) {
							String data = "";
							if ((dayTimeNum - 3) < 10) {
								data = "0" + (dayTimeNum - 3);
							} else {
								data = "" + (dayTimeNum - 3);
							}
							newContDate = yearNum + "-" + monNums + "-" + data;
						} else {
							newContDate = yearNum + "-" + monNums + "-" + "01";
						}
					}
				}
				if ((moNum == 4) || (moNum == 6) || (moNum == 9)
						|| (moNum == 11)) {
					if (dayTimeNum > 3) {
						String data = "";
						if ((dayTimeNum - 3) < 10) {
							data = "0" + (dayTimeNum - 3);
						} else {
							data = "" + (dayTimeNum - 3);
						}
						newContDate = yearNum + "-" + monNums + "-" + data;
					} else {
						newContDate = yearNum + "-" + monNums + "-01";
					}
				} else if ((moNum != 2)) {
					if (dayTimeNum > 3) {
						String data = "";
						if ((dayTimeNum - 3) < 10) {
							data = "0" + (dayTimeNum - 3);
						} else {
							data = "" + (dayTimeNum - 3);
						}
						newContDate = yearNum + "-" + monNums + "-" + data;
					} else {
						newContDate = yearNum + "-" + monNums + "-01";
					}
				}
				content = newContDate + " " + contentHm;
				startTimeTxt.setText(content);
			}
		}
	};

	private String setDate(boolean isLeaapYear, int dayTimeNum, String yearNum,
			int moNum, String monNums) {
		String newContDate = "";
		if (isLeaapYear) {
			if (moNum == 2) {
				if (dayTimeNum < 27) {
					String data = "";
					if ((dayTimeNum + 3) < 10) {
						data = "0" + (dayTimeNum + 3);
					} else {
						data = "" + (dayTimeNum + 3);
					}
					newContDate = yearNum + "-" + monNums + "-" + data;
				} else {
					newContDate = yearNum + "-" + monNums + "-" + 29;
				}
			}
		} else {
			if (moNum == 2) {
				if (dayTimeNum < 26) {
					String data = "";
					if ((dayTimeNum + 3) < 10) {
						data = "0" + (dayTimeNum + 3);
					} else {
						data = "" + (dayTimeNum + 3);
					}
					newContDate = yearNum + "-" + monNums + "-" + data;
				} else {
					newContDate = yearNum + "-" + monNums + "-" + 28;
				}
			}
		}
		if ((moNum == 4) || (moNum == 6) || (moNum == 9) || (moNum == 11)) {
			if (dayTimeNum < 28) {
				String data = "";
				if ((dayTimeNum + 3) < 10) {
					data = "0" + (dayTimeNum + 3);
				} else {
					data = "" + (dayTimeNum + 3);
				}
				newContDate = yearNum + "-" + monNums + "-" + data;
			} else {
				newContDate = yearNum + "-" + monNums + "-" + 30;
			}
		} else if ((moNum != 2)) {
			if (dayTimeNum < 29) {
				String data = "";
				if ((dayTimeNum + 3) < 10) {
					data = "0" + (dayTimeNum + 3);
				} else {
					data = "" + (dayTimeNum + 3);
				}
				newContDate = yearNum + "-" + monNums + "-" + data;
			} else {
				newContDate = yearNum + "-" + monNums + "-" + 31;
			}
		}

		return newContDate;
	}

	private String getTime(WheelView wv) {
		int hourPos = wv.getCurrentItem();
		int hourCount = wv.getAdapter().getItemsCount();
		if (hourPos >= 2) {
			hourPos = hourPos - 2;
		} else {
			hourPos = hourPos + hourCount - 2;
		}
		String time = wv.getAdapter().getItem(hourPos);
		return time;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case REQUESTCODE_DOG:
			prg = new ProgressDialog(this);
			prg.setMessage(getString(R.string.playback_timesetting_reqinfo));
			prg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			prg.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					if (prg != null && prg.isShowing()) {
						prg.dismiss();
					}
				}
			});
			return prg;
		default:
			return null;
		}
	}
}