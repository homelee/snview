package com.starnet.snview.channelmanager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ImageButton;

import com.starnet.snview.R;
import com.starnet.snview.channelmanager.xml.ChannelListUtils;
import com.starnet.snview.channelmanager.xml.CloudAccountInfoOpt;
import com.starnet.snview.channelmanager.xml.MultiConnIdentifyTask;
import com.starnet.snview.component.BaseActivity;
import com.starnet.snview.devicemanager.DeviceItem;
import com.starnet.snview.global.GlobalApplication;
import com.starnet.snview.realplay.PreviewDeviceItem;
import com.starnet.snview.realplay.RealplayActivity;
import com.starnet.snview.syssetting.CloudAccount;
import com.starnet.snview.util.NetWorkUtils;
import com.starnet.snview.util.ReadWriteXmlUtils;

/** 主要用于星云账号、账号中的平台内的信息显示;1、显示本地通道列表；2、加载网络的设备列表 */
@SuppressLint({ "SdCardPath", "HandlerLeak" })
public class ChannelListActivity extends BaseActivity {

//	private static final String TAG = "ChannelListActivity";
	private final String CLOUD_ACCOUNT_PATH = "/data/data/com.starnet.snview/cloudAccount_list.xml";
	public static final String filePath = "/data/data/com.starnet.snview/deviceItem_list.xml";
	public static final String previewFilePath = "/data/data/com.starnet.snview/previewItem_list.xml";
	
	public static final int RESULT_CODE_BACK = 0;
	public static final int RESULT_CODE_PREVIEW = 8;

	private ChannelRequestTask[] tasks;
	private int titleNum = 0;
	private Context curContext;
	private TextView titleView;// 通道列表标题栏

	private EditText searchEdt;
	private ImageButton startScanBtn;
	private List<CloudAccount> searchList;
	private boolean isFirstSearch = false;
	private CloudAccount collectCloudAccount;
	private final int CONNIDENTIFYDIALOG = 5;

	private ExpandableListView mExpListView;
	private List<CloudAccount> enablAccounts;
	private List<PreviewDeviceItem> oriPreviewChnls;
	private List<PreviewDeviceItem> oriPreviewChnlsBackup;// 备份星云平台用户信息
	private ChannelExpandableListviewAdapter mAdapter;
	private List<CloudAccount> originAccounts = new ArrayList<CloudAccount>();// 用于网络访问时用户信息的显示(访问前与访问后)；
	private List<PreviewDeviceItem> preItemsInApplication = new ArrayList<PreviewDeviceItem>();
	
	private int connIdenSum = 0 ;
	private ProgressDialog multiPRG;
	private final int MULTICONNIDENTIFY = 0x001E;
	private MultiConnIdentifyTask[] connTasks;

	private boolean[] hasDatas;
	private boolean[] isLoading;
	
	public static final int STAR_LOADDATA_TIMEOUT = 0x0002;
	public static final int STAR_LOADDATA_SUCCESS = 0x0003;
	public static final int STAR_LOADDATA_LOADFAI = 0x0004;
	private final int CONNECTIFYIDENTIFY_WRONG = 0x0012;
	private final int CONNECTIFYIDENTIFY_SUCCESS = 0x0011;
	private final int CONNECTIFYIDENTIFY_TIMEOUT = 0x0013;
	public static final int CHANNELLISTVIEWACTIVITY = 0x001F;

	private Handler mHandler = new Handler() {// 处理线程的handler
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case CONNECTIFYIDENTIFY_SUCCESS:
				singleConnIdentifyBackWork(msg);
				break;
			case CONNECTIFYIDENTIFY_WRONG:
				singleConnIdentifyBackWork(msg);
				break;
			case CONNECTIFYIDENTIFY_TIMEOUT:
				singleConnIdentifyBackWork(msg);
				break;
			case STAR_LOADDATA_SUCCESS:
				Bundle msgD = msg.getData();
				int position = msgD.getInt("position");
				hasDatas[position] = true;
				isLoading[position] = false;
				final CloudAccount nCA = (CloudAccount) msgD.getSerializable("netCA");
				nCA.setRotate(true);
				originAccounts.set(position, nCA);
				mAdapter.notifyDataSetChanged();
				break;				
			case STAR_LOADDATA_LOADFAI:
				Bundle msgD1 = msg.getData();
				int posit = msgD1.getInt("position");
				hasDatas[posit] = false;
				isLoading[posit] = false;
				CloudAccount netCA1 = (CloudAccount) msgD1.getSerializable("netCA");
				netCA1.setRotate(true);
				if(posit!=0){
					netCA1.setDeviceList(null);
					showToast(netCA1.getUsername()+getString(R.string.channel_load_exception));
					
				}
				originAccounts.set(posit, netCA1);
				mAdapter.notifyDataSetChanged();
				break;
			case STAR_LOADDATA_TIMEOUT:
				msgD = msg.getData();
				int positi = msgD.getInt("position");
				hasDatas[positi] = false;
				isLoading[positi] = false;
				CloudAccount netCA = (CloudAccount) msgD.getSerializable("netCA");
				netCA.setRotate(true);
				if(positi!=0){
					netCA.setDeviceList(null);
					showToast(netCA.getUsername()+getString(R.string.channel_load_timeout));
				}
				originAccounts.set(positi, netCA);
				mAdapter.notifyDataSetChanged();
				break;
			case MultiConnIdentifyTask.MULTICONNIDENTIFYSUCCESS:
				multiConnIdentifyBackWork(msg);
				break;
			case MultiConnIdentifyTask.MULTICONNIDENTIFYFAIL:
				multiConnIdentifyBackWork(msg);
				break;
			case MultiConnIdentifyTask.MULTICONNIDENTIFYHOSTORPORT:
				multiConnIdentifyBackWork(msg);
				break;
			case MultiConnIdentifyTask.MULTICONNIDENTIFYLONGTIMEOUT:
				multiConnIdentifyBackWork(msg);
				break;
			case MultiConnIdentifyTask.MULTICONNIDENTIFYSENDERROR:
				multiConnIdentifyBackWork(msg);
				break;
			case MultiConnIdentifyTask.MULTICONNIDENTIFYTIMEOUT:
				multiConnIdentifyBackWork(msg);
				break;
			default:
				break;
			}
		}
	};
	
	private void singleConnIdentifyBackWork(Message msg){
		if (prg != null && prg.isShowing()) {
			prg.dismiss();
			gotoChannelListViewActivity(msg);
			mAdapter.notifyDataSetChanged();
		}
	}
	
	private void multiConnIdentifyBackWork(Message msg){
		connIdenSum--;
		Bundle data = msg.getData();
		DeviceItem deviceItem = (DeviceItem) data.getSerializable("deviceItem");
		ChannelListUtils.setChannelSelectedDeviceItem(deviceItem);
		int pos = data.getInt("position");
		originAccounts.get(0).getDeviceList().set(pos, deviceItem);
		String title = curContext.getString(R.string.navigation_title_channel_list);
		if (connIdenSum==0) {
			closeMultiSocket();
			dismissMultiConnPrg();
			boolean isAllLoaded = ChannelListUtils.checkCloudAccountsLoad(originAccounts);
			if (isAllLoaded) {// 加载完成，则从用户选择的情形进行数据刷新
				List<PreviewDeviceItem> preList = new ArrayList<PreviewDeviceItem>();
				preList = getPreviewChannelList(originAccounts);
				if (!isFirstSearch) {
					if (preList.size() > 0) {
						titleView.setText(title + "(" + preList.size() +")" );
						PreviewDeviceItem p = preList.get(0);
						PreviewDeviceItem[] l = new PreviewDeviceItem[preList.size()];
						preList.toArray(l);
						Intent intent = ChannelListActivity.this.getIntent();
						intent.putExtra("DEVICE_ITEM_LIST", l);
						ChannelListActivity.this.setResult(RESULT_CODE_PREVIEW,intent);
						ChannelListActivity.this.finish();
					} else {
						showToast(getString(R.string.channel_manager_channellistview_channelnotchoose));
					}
				} else {
					backAndLeftOperation();
				}
			}else {
				if (isAllUsersLoaded()) {
					obtainNewPreviewItemsAndPlay(originAccounts);
				} else {// 存在加载成功
					List<PreviewDeviceItem> pItems = getPreviewItems();
					if (pItems != null && pItems.size() > 0) {
						titleView.setText(title + "(" + pItems.size() +")" );
						PreviewDeviceItem p = pItems.get(0);
						PreviewDeviceItem[] l = new PreviewDeviceItem[pItems.size()];
						pItems.toArray(l);
						Intent intent = ChannelListActivity.this.getIntent();
						intent.putExtra("DEVICE_ITEM_LIST", l);
						ChannelListActivity.this.setResult(RESULT_CODE_PREVIEW,intent);
						ChannelListActivity.this.finish();
					} else {
						showToast(getString(R.string.channel_manager_channellistview_channelnotchoose));
					}
				}
			}
		}
	}

	private void closeMultiSocket() {
		if (connTasks!=null) {
			for (int i = 0; i < connTasks.length; i++) {
				if (connTasks[i] != null) {
					connTasks[i].closeSocket();
				}
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.channel_listview_activity_copy);
		initView();
		setListenersForWadgets();
	}

	protected void gotoChannelListViewActivity(Message msg) {
		Intent intent = new Intent();
		Bundle data = msg.getData();
		Bundle bundle = new Bundle();
		intent.setClass(curContext, ChannelListViewActivity.class);
		CloudAccount acount = (CloudAccount) data.getSerializable("clickCloudAccount");
		bundle.putBoolean("selectAll", data.getBoolean("selectAll"));
		bundle.putString("deviceName", data.getString("deviceName"));
		bundle.putString("childPosition",String.valueOf(data.getInt("childPos")));
		bundle.putString("groupPosition",String.valueOf(data.getInt("parentPos")));
		bundle.putSerializable("clickCloudAccount",acount);
		intent.putExtras(bundle);
		startActivityForResult(intent, CHANNELLISTVIEWACTIVITY);
	}

	private void setListenersForWadgets() {

		super.getRightButton().setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				titleNum++;
				if (titleNum % 2 == 0) {
					searchEdt.setVisibility(View.GONE);
				} else {
					searchEdt.setVisibility(View.VISIBLE);
				}
			}
		});

		searchEdt.addTextChangedListener(new TextWatcher() {// 搜索查询事件
					@Override
					public void beforeTextChanged(CharSequence s, int start,
							int count, int after) {
					}

					@Override
					public void onTextChanged(CharSequence s, int start,
							int before, int count) {
						Editable able = searchEdt.getText();
						if (able != null) {
							isFirstSearch = true;
							String content = searchEdt.getText().toString().trim();
							if (searchList != null && searchList.size() > 0) {
								searchList.clear();
							}
							if (searchEdt.getText().toString().trim().length() > 0) {
								searchList = getSearchListFromCloudAccounts(content);
								mAdapter = new ChannelExpandableListviewAdapter(curContext, searchList, titleView);
								mExpListView.setAdapter(mAdapter);
							} else {
								setOriginCloudAccountsEnable();
								mAdapter = new ChannelExpandableListviewAdapter(curContext, originAccounts,titleView);
								mExpListView.setAdapter(mAdapter);
							}
						} else {
							setOriginCloudAccountsEnable();
							mAdapter = new ChannelExpandableListviewAdapter(curContext, originAccounts, titleView);
							mExpListView.setAdapter(mAdapter);
						}
					}

					@Override
					public void afterTextChanged(Editable s) {
					}

				});

		mExpListView.setOnGroupClickListener(new OnGroupClickListener() {
			@Override
			public boolean onGroupClick(ExpandableListView parent, View v,
					int groupPosition, long id) {

				CloudAccount cloudAccount = (CloudAccount) parent
						.getExpandableListAdapter().getGroup(groupPosition);// 获取用户账号信息
				if (cloudAccount.isExpanded()) {// 判断列表是否已经展开
					cloudAccount.setExpanded(false);
				} else {
					cloudAccount.setExpanded(true);
				}
				return false;
			}
		});
		startScanBtn.setOnClickListener(new OnClickListener() {// 单击该按钮时，收集选择的通道列表，从cloudAccounts中就可以选择。。。

					@SuppressWarnings("deprecation")
					@Override
					public void onClick(View v) {
						if (NetWorkUtils.checkNetConnection(curContext)) {
							closeSockets();
							boolean isAllLoaded = ChannelListUtils.checkCloudAccountsLoad(originAccounts);
							if (isAllLoaded) {// 加载完成，则从用户选择的情形进行数据刷新
								CloudAccount ca = originAccounts.get(0);
								List <DeviceItem> items = ChannelListUtils.getDeviceItems(ca);
								if (items!= null && items.size() > 0) {
									//对通道列表进行验证
									showDialog(MULTICONNIDENTIFY);
									connIdenSum = items.size();
									connTasks = new MultiConnIdentifyTask[items.size()];
									for (int i = 0; i < items.size(); i++) {
										int index = ChannelListUtils.getIndex(items.get(i),ca);
										connTasks[i] = new MultiConnIdentifyTask(curContext,mHandler,ca,items.get(i),index);
										connTasks[i].start();
									}
								}else {
									startPlay();
								}
							} else {// 如果某个用户的数据尚未加载完成，则用户需要使用原来的数据，进行播放
								if (isAllUsersLoaded()) {
									startPlay();
								} else {// 存在加载成功
									CloudAccount ca = originAccounts.get(0);
									List <DeviceItem> items = ChannelListUtils.getDeviceItems(ca);
									if (items!= null && items.size() > 0) {
										//对通道列表进行验证
										showDialog(MULTICONNIDENTIFY);
										connIdenSum = items.size();
										connTasks = new MultiConnIdentifyTask[items.size()];
										for (int i = 0; i < items.size(); i++) {
											int index = ChannelListUtils.getIndex(items.get(i),ca);
											connTasks[i] = new MultiConnIdentifyTask(curContext,mHandler,ca,items.get(i),index);
											connTasks[i].start();
										}
									} else {
										startPlay();
									}
								}
							}
						}else {
							showToast(getString(R.string.channel_manager_channellistview_netnotopen));
						}
					}
				});

		super.getLeftButton().setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {// 添加返回事件
				backAndLeftOperation();
			}
		});
	}

	/** 检测所有的星云平台账户是否都未加载成功;如果都未加载返回true；否则，返回false **/
	private boolean isAllUsersLoaded() {
		boolean isAllLoad;
		int loadNum = 0;
		for (int i = 0; i < originAccounts.size(); i++) {
			CloudAccount ca = originAccounts.get(i);
			if (!ca.isRotate()) {// 未加载成功
				loadNum++;
			}
		}
		if (loadNum == originAccounts.size()) {
			isAllLoad = true;
		} else {
			isAllLoad = false;
		}
		return isAllLoad;
	}

	/** 获取预览通道列表项 **/
	private List<PreviewDeviceItem> getPreviewItems() {
		List<PreviewDeviceItem> pItems = new ArrayList<PreviewDeviceItem>();
		for (int i = 0; i < originAccounts.size(); i++) {
			CloudAccount ca = originAccounts.get(i);
			if (ca.isRotate()) {// 加载完成，则获取用户选择的新数据
				List<PreviewDeviceItem> items = getPItemsFromSpecify(ca);
				if (items != null) {
					for (int j = 0; j < items.size(); j++) {
						pItems.add(items.get(j));
					}
				}
			} else {// 该用户的数据未加载完成，则获取原理啊用户选择的旧数据
				List<PreviewDeviceItem> items = getPItemsFromOrignalPItems(ca);
				if (items != null) {
					for (int j = 0; j < items.size(); j++) {
						pItems.add(items.get(j));
					}
				}
			}
		}
		return pItems;
	}

	private List<PreviewDeviceItem> getPItemsFromOrignalPItems(CloudAccount ca) {
		if (oriPreviewChnls == null) {
			return null;
		}
		
		List<PreviewDeviceItem> items = new ArrayList<PreviewDeviceItem>();
		for (int i = 0; i < oriPreviewChnls.size(); i++) {
			PreviewDeviceItem item = oriPreviewChnls.get(i);
			if (item.getPlatformUsername().equals(ca.getUsername())) {
				items.add(item);
			}
		}
		return items;
	}

	/** 从指定的用户中获取收藏设备 **/
	private List<PreviewDeviceItem> getPItemsFromSpecify(CloudAccount ca) {
		List<PreviewDeviceItem> items = new ArrayList<PreviewDeviceItem>();
		List<DeviceItem> dItms = ca.getDeviceList();
		for (int i = 0; i < dItms.size(); i++) {
			DeviceItem item = dItms.get(i);
			if(item == null){
				return null;
			}
			List<Channel> chanels = item.getChannelList();
			for (int j = 0; j < chanels.size(); j++) {
				Channel chl = chanels.get(j);
				if (chl.isSelected()) {
					PreviewDeviceItem pItem = new PreviewDeviceItem();
					pItem.setChannel(chl.getChannelNo());
					pItem.setLoginPass(item.getLoginPass());
					pItem.setLoginUser(item.getLoginUser());
					pItem.setSvrIp(item.getSvrIp());
					pItem.setSvrPort(item.getSvrPort());
					String dName = item.getDeviceName();
					pItem.setPlatformUsername(item.getPlatformUsername());
					int len = dName.length();
					String wordLen = getString(R.string.device_manager_off_on_line_length);
					int wordLength = Integer.valueOf(wordLen);
					if (len >= wordLength) {
						String showName = dName.substring(0, wordLength);
						String word3 = getString(R.string.device_manager_online_en);
						String word4 = getString(R.string.device_manager_offline_en);
						if (showName.contains(word3) || showName.contains(word4)) {
							dName = dName.substring(wordLength);
						}
					}
					pItem.setDeviceRecordName(dName);
					items.add(pItem);
				}
			}
		}
		return items;
	}

	private void setOriginCloudAccountsEnable() {
		for (int i = 0; i < originAccounts.size(); i++) {
			originAccounts.get(i).setEnabled(enablAccounts.get(i).isEnabled());
		}
	}

	protected List<PreviewDeviceItem> mergeChannelAndSearchList(
			List<PreviewDeviceItem> previewChannelList2,
			List<PreviewDeviceItem> previewSearchList) {
		if (previewSearchList == null || previewSearchList.size() == 0) {
			return previewChannelList2;
		}
		int preview_size = previewSearchList.size();
		for (int i = 0; i < preview_size; i++) {
			previewChannelList2.add(previewSearchList.get(i));
		}
		return previewChannelList2;
	}
	
	private void closeSockets(){
		if (tasks != null) {
			for (int i = 0; i < tasks.length; i++) {
				if (tasks[i] != null) {
					tasks[i].setCanceled(true);
				}
			}
		}
		if (connTasks!=null) {
			for (int i = 0; i < connTasks.length; i++) {
				if (connTasks[i] != null) {
					connTasks[i].setCancel(true);
				}
			}
		}
	}
	
	private void startPlay(){
		obtainNewPreviewItemsAndPlay(originAccounts);
	}

	private void backAndLeftOperation() {
		if (NetWorkUtils.checkNetConnection(curContext)) {
			closeSockets();
		}
		
		if ((oriPreviewChnlsBackup!= null)&&(oriPreviewChnlsBackup.size() > 0)) {
			PreviewDeviceItem p = oriPreviewChnlsBackup.get(0);
			PreviewDeviceItem[] l = new PreviewDeviceItem[oriPreviewChnlsBackup.size()];
			oriPreviewChnlsBackup.toArray(l);
			Intent intent = ChannelListActivity.this.getIntent();
			intent.putExtra("DEVICE_ITEM_LIST", l);
			ChannelListActivity.this.setResult(RESULT_CODE_PREVIEW, intent);
			ChannelListActivity.this.finish();
		}
		ChannelListActivity.this.finish();
	}

	/**对于未加载的用户，使用上一次的通道数据，对于加载完毕的用户使用当前选择的通道数据 ***/
	private void obtainNewPreviewItemsAndPlay(List<CloudAccount> accounts) {
		if (accounts == null) {
			ChannelListActivity.this.finish();
			return;
		}
		List<PreviewDeviceItem> previewChanls = new ArrayList<PreviewDeviceItem>();
		for (int i = 0; i < accounts.size(); i++) {
			CloudAccount act = accounts.get(i);
			if (i == 0 && act != null) {//针对收藏设备，必须可以进行加载的
				if(act != null){
					List<DeviceItem> ds = act.getDeviceList();
					if (ds != null) {
						for (DeviceItem itm : ds) {
							List<Channel> chls = itm.getChannelList();
							for (Channel cl : chls) {
								if (cl.isSelected()) {
									PreviewDeviceItem pm = getPreviewItem(i, itm);
									pm.setChannel(cl.getChannelNo());
									previewChanls.add(pm);
								}
							}
						}
					}
					
					List<PreviewDeviceItem> lastSelectPs = ChannelListUtils.getLastSelectPreviewItems(act.getUsername(),preItemsInApplication);
					if (lastSelectPs!=null&&lastSelectPs.size()>0) {
						List<PreviewDeviceItem> delPs = ChannelListUtils.getDeletePreviewItems(previewChanls,lastSelectPs);
						if (delPs!=null&&delPs.size()>0) {
							for (PreviewDeviceItem pi : delPs) {
//								preItemsInApplication.remove(pi);
								int index = getIndexOfPreviewItem(pi);
								if (index != -1){
									preItemsInApplication.remove(index);
								}
							}
						}
					}
					List<PreviewDeviceItem> addPs = ChannelListUtils.getAddPreviewItems(previewChanls,lastSelectPs);
					if (addPs != null && addPs.size() > 0) {
						for(PreviewDeviceItem pi : addPs){
							if (!isExistInPreviewItems(pi)) {
								preItemsInApplication.add(pi);
							}
						}
					}
				}
			} else {//针对星云账户
				if (act != null) {
					if (hasDatas[i]) {//代表星云平台用户有数据,判断用户是否重新进行了选择；//如果是，删除以前选择的，而当下没有进行选择的
						List<DeviceItem> ds = act.getDeviceList();
						if (ds != null) {
							for (DeviceItem itm : ds) {
								List<Channel> chls = itm.getChannelList();
								for (Channel cl : chls) {
									if (cl.isSelected()) {
										PreviewDeviceItem pm = getPreviewItem(i,itm);
										pm.setChannel(cl.getChannelNo());
										previewChanls.add(pm);
									}
								}
							}
						}
						//获取上一次该账户的通道选择情况,先删除，后添加
						List<PreviewDeviceItem> lastSelectPs = ChannelListUtils.getLastSelectPreviewItems(act.getUsername(),preItemsInApplication);
						if (lastSelectPs!=null&&lastSelectPs.size()>0) {
							List<PreviewDeviceItem> delPs = ChannelListUtils.getDeletePreviewItems(previewChanls,lastSelectPs);
							if (delPs!=null&&delPs.size()>0) {
								for (PreviewDeviceItem pi : delPs) {
//									preItemsInApplication.remove(pi);//应该从后往前删除
									int index = getIndexOfPreviewItem(pi);
									if (index != -1){
										preItemsInApplication.remove(index);
									}
								}
							}
						}
						List<PreviewDeviceItem> addPs = ChannelListUtils.getAddPreviewItems(previewChanls, lastSelectPs);
						if (addPs != null && addPs.size() > 0) {
							for (PreviewDeviceItem pi : addPs) {
								if (!isExistInPreviewItems(pi)) {
									preItemsInApplication.add(pi);
								}
							}
						}						
					}else {//代表星云平台用户无数据，则需要判断是否正在加载
						String name = act.getUsername();
						List<PreviewDeviceItem> temp = getLastPreviewItems(name);//ps,
						if (isLoading[i]) {//表示正在加载
							if (temp!=null && temp.size() > 0) {
								for(PreviewDeviceItem p : temp){
									previewChanls.add(p);//用于预览
								}
								for(PreviewDeviceItem p : temp){
									if (!isExistInPreviewItems(p)) {
										preItemsInApplication.add(p);//用于显示；===针对未加载成功的星云平台，需要保存上一次的选择情况，用于下次加载成功时的正常显示
									}
								}
							}
						}else{//代表星云平台用户无数据，且不在加载数据，则使用以前的保存
							if (temp!=null && temp.size() > 0) {
								for(PreviewDeviceItem p : temp){
									if (!isExistInPreviewItems(p)) {
										preItemsInApplication.add(p);//针对未加载成功的星云平台，需要保存上一次的选择情况，用于下次加载成功时的正常显示
									}
								}
							}
						}
					//当星云平台加载成功时，获取该账户下用户选择的预览通道，并设置对应的GlobalApplication.java,当用户加载失败或者是加载超时的时候，则保存用户上一次的选择情形；
					}
				}
			}
		}
		GlobalApplication.getInstance().setLastPreviewItems(preItemsInApplication);//注入刚刚选择的情况（包括加载成功和未加载成功的情形）
		new Thread() {
			@Override
			public void run() {
				try {
					File file = new File(previewFilePath);
					if (file.exists()) {
						file.delete();
					}
					ReadWriteXmlUtils.writePreviewItemListInfoToXML(preItemsInApplication, previewFilePath);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
		//play
		if (previewChanls.size() > 0) {
			PreviewDeviceItem p = previewChanls.get(0);
			PreviewDeviceItem[] l = new PreviewDeviceItem[previewChanls.size()];
			previewChanls.toArray(l);
			Intent intent = ChannelListActivity.this.getIntent();
			intent.putExtra("DEVICE_ITEM_LIST", l);
			ChannelListActivity.this.setResult(RESULT_CODE_PREVIEW, intent);
			ChannelListActivity.this.finish();
		}else {
			List<PreviewDeviceItem> pItems = GlobalApplication.getInstance().getRealplayActivity().getPreviewDevices();
			if ((pItems!=null)&& (pItems.size() > 0)) {
				pItems.clear();
				GlobalApplication.getInstance().getRealplayActivity().notifyPreviewDevicesContentChanged();
			}
			ChannelListActivity.this.finish();
		}
	}
	
	private int getIndexOfPreviewItem(PreviewDeviceItem pi){
		int index = -1;
		for (int i = 0; i < preItemsInApplication.size(); i++) {
			 PreviewDeviceItem tp = preItemsInApplication.get(i);
			 if (tp.getPlatformUsername().equals(pi.getPlatformUsername())&&(tp.getDeviceRecordName().equals(pi.getDeviceRecordName()))&&(tp.getChannel()==pi.getChannel())) {
				 index = i;
				 break;
			} 
		}	
		return index;
	}
	
	private boolean isExistInPreviewItems(PreviewDeviceItem p){
		boolean isExist = false;
		if (preItemsInApplication == null) {
			return false;
		}
		if (preItemsInApplication != null && preItemsInApplication.size()==0) {
			return false;
		}
		for (PreviewDeviceItem tp : preItemsInApplication) {
			if (tp.getPlatformUsername().equals(p.getPlatformUsername())
					&& tp.getDeviceRecordName().equals(p.getDeviceRecordName())&&(tp.getChannel() == p.getChannel())) {
				return true;
			}
		}
		return isExist;
	}

	private List<PreviewDeviceItem> getLastPreviewItems(String name) {		//List<PreviewDeviceItem> ps,
		List<PreviewDeviceItem> temp = new ArrayList<PreviewDeviceItem>();
		if((oriPreviewChnls!=null)&&(oriPreviewChnls.size()>0)){
			int size = oriPreviewChnls.size();
			for (int i =0 ;i< size; i++) {
				PreviewDeviceItem pi = oriPreviewChnls.get(i);
				if(pi != null){
					if (pi.getPlatformUsername().equals(name)) {
						temp.add(pi);
					}
				}
			}
		}
		return temp;
	}

	private PreviewDeviceItem getPreviewItem(int i,DeviceItem itm){
		PreviewDeviceItem pm = new PreviewDeviceItem();
		pm.setLoginPass(itm.getLoginPass());
		pm.setLoginUser(itm.getLoginUser());
		pm.setSvrIp(itm.getSvrIp());
		pm.setSvrPort(itm.getSvrPort());
		String dName = itm.getDeviceName();
		pm.setPlatformUsername(itm.getPlatformUsername());
		if (i == 0) {
			pm.setDeviceRecordName(dName);
		}else{
			pm.setDeviceRecordName(dName.substring(4));
		}
		return pm;
	}

	protected List<PreviewDeviceItem> removeContain(
			List<PreviewDeviceItem> previewChannelList2,
			List<PreviewDeviceItem> sList) {

		if (sList == null || sList.size() == 0) {
			return previewChannelList2;
		}

		if (previewChannelList2 == null || previewChannelList2.size() == 0) {
			return previewChannelList2;
		}

		int sListSize = sList.size();

		int tempSize = previewChannelList2.size();
		for (int i = 0; i < sListSize; i++) {
			int j = 0;
			while (j < tempSize) {
				boolean isLike = isLikePreviewItem(previewChannelList2.get(j),
						sList.get(i));
				if (isLike) {
					int k = j;
					previewChannelList2.remove(j);
					j = k;
					tempSize = previewChannelList2.size();
				} else {
					j++;
				}
			}
		}
		return previewChannelList2;
	}

	private boolean isLikePreviewItem(PreviewDeviceItem preview1,PreviewDeviceItem preview2) {
		boolean isLike = false;
		if (preview1.getPlatformUsername().equals(preview2.getPlatformUsername()) && preview1.getDeviceRecordName().equals(preview2.getDeviceRecordName())) {
			isLike = true;
		}
		return isLike;
	}

	private void initView() {

		titleView = super.getTitleView();
		titleView.setSingleLine(true);
		List<PreviewDeviceItem> previews = GlobalApplication.getInstance().getRealplayActivity().getPreviewDevices();
		if ((previews == null) || (previews != null && previews.size() == 0)) {
			titleView.setText(getString(R.string.navigation_title_channel_list));// 设置列表标题名
		} else {
			titleView.setText(getString(R.string.navigation_title_channel_list) + "(" + previews.size() + ")");// 设置列表标题名
		}
		super.setToolbarVisiable(false);
		super.hideExtendButton();
		super.setLeftButtonBg(R.drawable.navigation_bar_back_btn_selector);
		super.setRightButtonBg(R.drawable.navigation_bar_search_btn_selector);
		RealplayActivity activity = GlobalApplication.getInstance().getRealplayActivity();
		if (activity!=null) {
			oriPreviewChnls = activity.getPreviewDevices();// 获取源预览通道列表
			if ((oriPreviewChnls != null) && (oriPreviewChnls.size() > 0)) {
				copyOriginPreviewItems();
			}
		}
		
		preItemsInApplication = ReadWriteXmlUtils.getPreviewItemListInfoFromXML(previewFilePath);
		GlobalApplication.getInstance().setLastPreviewItems(preItemsInApplication);
		
		enablAccounts = new ArrayList<CloudAccount>();
		curContext = ChannelListActivity.this;
		startScanBtn = (ImageButton) findViewById(R.id.startScan);// 开始预览按钮
		mExpListView = (ExpandableListView) findViewById(R.id.channel_listview);

		searchEdt = (EditText) findViewById(R.id.search_et);
		searchEdt.setVisibility(View.GONE);

		originAccounts = getCloudAccountInfoFromUI();// 获取收藏设备，以及用户信息
		
		hasDatas = new boolean[originAccounts.size()];
		isLoading = new boolean[originAccounts.size()];
		for (int i = 0; i < originAccounts.size(); i++) {
			isLoading[i] = true;
		}		
		copyCloudAccountEnable();
		
		mAdapter = new ChannelExpandableListviewAdapter(curContext, originAccounts, titleView);
		mAdapter.setHandler(mHandler);
		mExpListView.setAdapter(mAdapter);

		int netSize = originAccounts.size();
		tasks = new ChannelRequestTask[netSize];
		tasks[0] = new ChannelRequestTask(curContext, originAccounts.get(0),mHandler, 0);
		tasks[0].start();
		
		boolean isOpen = NetWorkUtils.checkNetConnection(curContext);// 查看网络是否开启
		if (isOpen) {
			int j = 1;
			for (int i = 1; i < netSize; i++) {
				CloudAccount cAccount = originAccounts.get(i);
				boolean isEnable = cAccount.isEnabled();
				if (isEnable) {
					cAccount.setRotate(false);
				} else {
					cAccount.setRotate(true);
				}
				if (isEnable) {// 如果启用该用户的话，则访问网络，否则，不访问；不访问网络时，其rotate=true;
					tasks[j] = new ChannelRequestTask(curContext, cAccount,mHandler, i);
					tasks[j].start();
					j++;
				}
			}
		} else {
			showToast(getString(R.string.channel_manager_channellistview_netnotopen));
		}
	}

	private void copyOriginPreviewItems() {
		oriPreviewChnlsBackup = new ArrayList<PreviewDeviceItem>();
		for (PreviewDeviceItem pi : oriPreviewChnls) {
			PreviewDeviceItem tp = new PreviewDeviceItem();
			tp.setChannel(pi.getChannel());
			tp.setDeviceRecordName(pi.getDeviceRecordName());
			tp.setLoginPass(pi.getLoginPass());
			tp.setLoginUser(pi.getLoginUser());
			tp.setPlatformUsername(pi.getPlatformUsername());
			tp.setSvrIp(pi.getSvrIp());
			tp.setSvrPort(pi.getSvrPort());
			oriPreviewChnlsBackup.add(tp);
		}
	}

	private void copyCloudAccountEnable() {
		for (int i = 0; i < originAccounts.size(); i++) {
			CloudAccount cloudAccount = new CloudAccount();
			cloudAccount.setUsername(originAccounts.get(i).getUsername());
			cloudAccount.setPassword(originAccounts.get(i).getPassword());
			cloudAccount.setEnabled(originAccounts.get(i).isEnabled());
			enablAccounts.add(cloudAccount);
		}
	}

	private void showToast(String content) {
		Toast.makeText(ChannelListActivity.this, content, Toast.LENGTH_SHORT).show();
	}

	private List<PreviewDeviceItem> getPreviewChannelList(List<CloudAccount> cloudAccounts) {
		List<PreviewDeviceItem> previewList = new ArrayList<PreviewDeviceItem>();
		if ((cloudAccounts == null) || (cloudAccounts.size() < 1)) {
			showToast(getString(R.string.channel_manager_channellistview_loadfail));
		} else {
			int size = cloudAccounts.size();
			for (int i = 0; i < size; i++) {
				CloudAccount cloudAccount = cloudAccounts.get(i);
				List<DeviceItem> deviceItems = cloudAccount.getDeviceList();
				if (deviceItems != null) {
					int deviceSize = deviceItems.size();
					for (int j = 0; j < deviceSize; j++) {
						DeviceItem dItem = deviceItems.get(j);
						List<Channel> channelList = dItem.getChannelList();
						if (channelList != null) {
							int channelSize = channelList.size();
							for (int k = 0; k < channelSize; k++) {
								Channel channel = channelList.get(k);
								if (channel.isSelected()) {// 判断通道列表是否选择
									PreviewDeviceItem pItem = new PreviewDeviceItem();
									pItem.setChannel(channel.getChannelNo());
									pItem.setLoginPass(dItem.getLoginPass());
									pItem.setLoginUser(dItem.getLoginUser());
									pItem.setSvrIp(dItem.getSvrIp());
									pItem.setSvrPort(dItem.getSvrPort());
									String deviceName = dItem.getDeviceName();
									pItem.setPlatformUsername(dItem.getPlatformUsername());
									int len = deviceName.length();
									String wordLen = getString(R.string.device_manager_off_on_line_length);
									int wLen = Integer.valueOf(wordLen);
									if (len >= wLen) {
										String showName = deviceName.substring(0, wLen);
										String word3 = getString(R.string.device_manager_online_en);
										String word4 = getString(R.string.device_manager_offline_en);
										if (showName.contains(word3)|| showName.contains(word4)) {
											deviceName = deviceName.substring(wLen);
										}
									}
									pItem.setDeviceRecordName(deviceName);
									previewList.add(pItem);
								}
							}
						}
					}
				}
			}

		}
		return previewList;
	}
	
	/** 从设置界面中获取用户信息 */
	private List<CloudAccount> getCloudAccountInfoFromUI() {
		List<CloudAccount> accoutInfo = new ArrayList<CloudAccount>();
		accoutInfo = new CloudAccountInfoOpt(this).getCloudAccountInfoFromUI(getString(R.string.device_manager_collect_device));
		return accoutInfo;
	}

	/** 从本地获取设备的通道列表 */
	public List<CloudAccount> getGroupListFromLocal() {// 注意，目前只有一个用户的情况下；从收藏设备中读取账户
		List<CloudAccount> groupList = ReadWriteXmlUtils.readCloudAccountFromXML(CLOUD_ACCOUNT_PATH);
		return groupList;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// 根据得到的值确定状态框的显示情形,全选、半选或者空选,通知ExpandableListView中状态框的改变
		if ((resultCode == CHANNELLISTVIEWACTIVITY)) {
			Bundle bundle = data.getExtras();
			collectCloudAccount = (CloudAccount) bundle.getSerializable("wca");
			// 更新ExpandableListView指定的按钮
			int pos = bundle.getInt("parentPos");
			if (!isFirstSearch) {
				originAccounts.set(pos, collectCloudAccount);
				mAdapter.notifyNum = 2;
				mAdapter.notifyDataSetChanged();
			} else {
				String userName = collectCloudAccount.getUsername();
				String password = collectCloudAccount.getPassword();

				for (int i = 0; i < searchList.size(); i++) {
					if (searchList.get(i).getUsername().equals(userName) && searchList.get(i).getPassword().equals(password)) {
						searchList.set(i, collectCloudAccount);
					}
				}
				mAdapter.notifyNum = 22;
				mAdapter.notifyDataSetChanged();
				List<DeviceItem> colDevices = collectCloudAccount.getDeviceList();
				if (colDevices != null && colDevices.size() > 0) {
					for (int i = 0; i < originAccounts.size(); i++) {
						CloudAccount origin_cloudAccount = originAccounts
								.get(i);
						if (origin_cloudAccount.getUsername().equals(userName)
								&& origin_cloudAccount.getPassword().equals(
										password)) {// 找到用户
							List<DeviceItem> originDevices = origin_cloudAccount
									.getDeviceList();
							for (int k = 0; k < colDevices.size(); k++) {
								DeviceItem colDeviceItem = colDevices.get(k);
								for (int j = 0; j < originDevices.size(); j++) {
									DeviceItem originDeviceItem = originDevices
											.get(j);
									if (originDeviceItem.getDeviceName()
											.equals(colDeviceItem
													.getDeviceName())) {
										List<Channel> channels = colDeviceItem
												.getChannelList();
										originDeviceItem
												.setChannelList(channels);
										break;
									}
								}
							}
						}
					}
				}
			}

			List<PreviewDeviceItem> newPreviewList = getPreviewChannelList(originAccounts);
			GlobalApplication.getInstance().getRealplayActivity().setPreviewDevices_copy(newPreviewList);

			// 判断获取的cloudAccount3是否是属于第一个用户(即“收藏设备”)，若是，则需要保存到收藏设备中，便于程序下一次启动时，读取结果
			if (collectCloudAccount.getUsername().equals(
					getString(R.string.device_manager_collect_device))
					&& (collectCloudAccount.getDomain().equals("com"))
					&& (collectCloudAccount.getPort().equals("808"))
					&& (collectCloudAccount.getPassword().equals("0208"))) {
				Thread thread = new Thread() {
					@Override
					public void run() {
						super.run();
						List<DeviceItem> deviceList = collectCloudAccount.getDeviceList();
						int size = deviceList.size();
						for (int i = 0; i < size; i++) {
							try {
								ReadWriteXmlUtils.replaceSpecifyDeviceItem(filePath, i, deviceList.get(i));
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				};
				thread.start();
			}
		}
	}

	private List<CloudAccount> getSearchListFromCloudAccounts(
			String input_content2) {
		List<CloudAccount> result = new ArrayList<CloudAccount>();

		for (int i = 0; i < originAccounts.size(); i++) {
			CloudAccount cloudAccount = originAccounts.get(i);
			CloudAccount resultCloudAccount = new CloudAccount();
			boolean add_flag = false;

			if (cloudAccount != null) {
				List<DeviceItem> result_deviceItem = new ArrayList<DeviceItem>();
				resultCloudAccount.setDomain(cloudAccount.getDomain());
				resultCloudAccount.setPassword(cloudAccount.getPassword());
				resultCloudAccount.setPort(cloudAccount.getPort());
				resultCloudAccount.setUsername(cloudAccount.getUsername());
				resultCloudAccount.setRotate(true);
				resultCloudAccount.setEnabled(true);
				resultCloudAccount.setExpanded(false);

				List<DeviceItem> deviceItems = cloudAccount.getDeviceList();
				if (deviceItems != null) {
					for (int j = 0; j < deviceItems.size(); j++) {
						DeviceItem deviceItem = deviceItems.get(j);
						if (deviceItem.getDeviceName().contains(input_content2)) {
							result_deviceItem.add(deviceItem);
							add_flag = true;
						}
					}
				}

				if (add_flag) {
					resultCloudAccount.setDeviceList(result_deviceItem);
					result.add(resultCloudAccount);
				}
			}
		}
		return result;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			backAndLeftOperation();
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	boolean isCanceled = false;
	boolean isClickCancel = false;

	ProgressDialog prg;

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case CONNIDENTIFYDIALOG:
			prg = ProgressDialog.show(this, "", getString(R.string.device_manager_conn_iden), true, true);
			prg.setOnCancelListener(new OnCancelListener() {
				@SuppressWarnings("deprecation")
				@Override
				public void onCancel(DialogInterface dialog) {
					dismissDialog(CONNIDENTIFYDIALOG);
					isCanceled = true;
					isClickCancel = true;
					if (mAdapter!=null) {
						mAdapter.setCancel(true);// 不进行验证
					}					
				}
			});
			return prg;
		case MULTICONNIDENTIFY:
			multiPRG = ProgressDialog.show(this, "", getString(R.string.device_manager_conn_iden), true, true);
			multiPRG.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					dismissMultiConnPrg();
					closeMultiSocket();
					for (int i = 0; i < connTasks.length; i++) {
						if (connTasks[i]!=null) {
							connTasks[i].setCancel(true);
						}
					}				
				}
			});
			return multiPRG;
		default:
			return null;
		}
	}
	
	private void dismissMultiConnPrg() {
		if (multiPRG != null && multiPRG.isShowing()) {
			multiPRG.dismiss();
		}
	}
}