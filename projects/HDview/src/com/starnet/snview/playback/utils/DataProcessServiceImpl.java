/*
 * FileName:DataProcessServiceImpl.java
 * 
 * Package:com.starsecurity.service.impl
 * 
 * Date:2013-04-15
 * 
 * Copyright: Copyright (c) 2013 Minz.Chan
 */
package com.starnet.snview.playback.utils;

import java.util.ArrayList;
import java.util.List;

import android.os.Message;
import android.util.Log;

public class DataProcessServiceImpl implements DataProcessService {

	private String TAG = "DataProcessServiceImpl";
	private String conn_name;
	// private H264DecodeUtil h264 = new H264DecodeUtil();

	private boolean isIFrameFinished = false;

	public DataProcessServiceImpl(String conn_name) {
		super();
		this.conn_name = conn_name;
		// h264.init(352, 288);
	}

	@Override
	public int process(byte[] data, int length) {
		// VideoView v = ViewManager.getInstance().getVideoView();
		int returnValue = 0;
		int nLeft = length - 4; // 未处理的字节数
		int nLen_hdr = OWSP_LEN.TLV_HEADER;
		int flag = 0;

		// 循环处理所有的TLVl
		while (nLeft > nLen_hdr) {
			// 处理TLV头memcpy(&tlv_hdr,buf,nLen_hdr);
			TLV_HEADER tlv_Header = (TLV_HEADER) ByteArray2Object.convert2Object(TLV_HEADER.class, data, flag,OWSP_LEN.TLV_HEADER);
			Log.i(TAG, "TLV_HEADER, TYPE:"+tlv_Header.getTlv_type() + ", LEN:" + tlv_Header.getTlv_len());
			nLeft -= nLen_hdr;
			flag += nLen_hdr;
			// 处理TLV的V部分
			if (tlv_Header.getTlv_type() == TLV_T_Command.TLV_T_VIDEO_FRAME_INFO) {
				TLV_V_VideoFrameInfo tlv_V_VideoFrameInfo;
				tlv_V_VideoFrameInfo = (TLV_V_VideoFrameInfo) ByteArray2Object.convert2Object(TLV_V_VideoFrameInfo.class, data, flag,OWSP_LEN.TLV_V_VideoFrameInfo);
				Log.i(TAG, "######TLV TYPE: TLV_T_VIDEO_FRAME_INFO");
				// ConnectionManager.getConnection(conn_name).addResultItem(tlv_V_VideoFrameInfo);
			} else if (tlv_Header.getTlv_type() == TLV_T_Command.TLV_T_VIDEO_PFRAME_DATA) {
				// 若第1帧接到的不是I帧，则后续的P帧不处理
				if (!isIFrameFinished) {
					return 0;
				}

//				Log.i(TAG, "*********** P Frame process start  *******");
				Log.i(TAG, "######TLV TYPE: TLV_T_VIDEO_PFRAME_DATA");
				byte[] tmp = (byte[]) ByteArray2Object.convert2Object(
						TLV_V_VideoData.class, data, flag,
						tlv_Header.getTlv_len());
				Log.i(TAG, "vediodata length:" + tmp.length);
				int result = 0;
				try {
					// result = h264.decodePacket(tmp, tmp.length,
					// v.getmPixel());
				} catch (Exception e) {
					e.printStackTrace();
					// 解码过程发生异常
					// ViewManager.getInstance().setHelpMsg(R.string.IDS_Unknown);
				}
			} else if (tlv_Header.getTlv_type() == TLV_T_Command.TLV_T_AUDIO_INFO) {

				TLV_V_AudioInfo audioInfo = (TLV_V_AudioInfo) ByteArray2Object.convert2Object(TLV_V_AudioInfo.class, data, flag,OWSP_LEN.TLV_V_AudioInfo);
//				Log.i(TAG, "time:" + audioInfo.getTime());
				Log.i(TAG, "######TLV TYPE: TLV_T_AUDIO_INFO");
			} else if (tlv_Header.getTlv_type() == TLV_T_Command.TLV_T_AUDIO_DATA) {
				byte[] tmp = (byte[]) ByteArray2Object.convert2Object(
						TLV_V_AudioData.class, data, flag,
						tlv_Header.getTlv_len());
//				Log.i(TAG, "length:" + tmp.length);
				Log.i(TAG, "######TLV TYPE: TLV_T_AUDIO_DATA");
				try {
				} catch (Exception e) {
					e.printStackTrace();
				}

			} else if (tlv_Header.getTlv_type() == TLV_T_Command.TLV_T_VIDEO_IFRAME_DATA) {
				Log.i(TAG, "######TLV TYPE: TLV_T_VIDEO_IFRAME_DATA");
				byte[] tmp = (byte[]) ByteArray2Object.convert2Object(TLV_V_VideoData.class, data, flag,tlv_Header.getTlv_len());
				int result = 0;
				try {
					// result = h264.decodePacket(tmp, tmp.length,
					// v.getmPixel());
				} catch (Exception e) {
					// 解码过程发生异常
					// ViewManager.getInstance().setHelpMsg(R.string.IDS_Unknown);
				}

				isIFrameFinished = true;
				if (result == 1) {

				}
			} else if (tlv_Header.getTlv_type() == TLV_T_Command.TLV_T_STREAM_FORMAT_INFO) {
				Log.i(TAG, "######TLV TYPE: TLV_T_STREAM_FORMAT_INFO");
				TLV_V_StreamDataFormat tlv_V_StreamDataFormat;
				tlv_V_StreamDataFormat = (TLV_V_StreamDataFormat) ByteArray2Object.convert2Object(TLV_V_StreamDataFormat.class, data,flag, OWSP_LEN.TLV_V_StreamDataFormat);

				int framerate = tlv_V_StreamDataFormat.getVideoFormat().getFramerate();
				int width = tlv_V_StreamDataFormat.getVideoFormat().getWidth();
				int height = tlv_V_StreamDataFormat.getVideoFormat().getHeight();
				int bitrate = (int) (tlv_V_StreamDataFormat.getVideoFormat().getBitrate() / 1024);

			} else if (tlv_Header.getTlv_type() == TLV_T_Command.TLV_T_LOGIN_ANSWER) {
				Log.i(TAG, "######TLV TYPE: TLV_T_LOGIN_ANSWER");
				TLV_V_LoginResponse tlv_V_LoginResponse;
				tlv_V_LoginResponse = (TLV_V_LoginResponse) ByteArray2Object.convert2Object(TLV_V_LoginResponse.class, data, flag,OWSP_LEN.TLV_V_LoginResponse);
				int result = tlv_V_LoginResponse.getResult();
				return result;
			} else if (tlv_Header.getTlv_type() == TLV_T_Command.TLV_T_VIDEO_FRAME_INFO_EX) {
				Log.i(TAG, "######TLV TYPE: TLV_T_VIDEO_FRAME_INFO_EX");
				TLV_V_VideoFrameInfoEx infoEx;
				infoEx = (TLV_V_VideoFrameInfoEx) ByteArray2Object.convert2Object(TLV_V_VideoFrameInfoEx.class, data,flag, OWSP_LEN.TLV_V_VideoFrameInfoEX);
			} else if (tlv_Header.getTlv_type() == TLV_T_Command.TLV_T_PLAY_RECORD_RSP) {
				Log.i(TAG, "######TLV TYPE: TLV_T_PLAY_RECORD_RSP");
				TLV_V_PlayRecordResponse prr = (TLV_V_PlayRecordResponse) ByteArray2Object.convert2Object(TLV_V_PlayRecordResponse.class, data,flag, OWSP_LEN.TLV_V_PlayRecordResponse);
			} else if (tlv_Header.getTlv_type() == TLV_T_Command.TLV_T_RECORD_EOF) {
				Log.i(TAG, "######TLV TYPE: TLV_T_RECORD_EOF");
				returnValue = -1;
				break;
			}else if (tlv_Header.getTlv_type() == TLV_T_Command.TLV_T_LOGIN_ANSWER) {//登陆认证信息时
				
				TLV_V_LoginResponse loginRSP;
				loginRSP = (TLV_V_LoginResponse)ByteArray2Object.convert2Object(TLV_V_LoginResponse.class, data,flag, OWSP_LEN.TLV_V_LoginResponse);
				int reserve = loginRSP.getReserve();
				int result = loginRSP.getResult();
				break;

			}else if (tlv_Header.getTlv_type() == TLV_T_Command.TLV_V_SEARCHRECORD){
				TLV_V_SearchRecordResponse srr;
				srr = (TLV_V_SearchRecordResponse)ByteArray2Object.convert2Object(TLV_V_SearchRecordResponse.class, data,flag, OWSP_LEN.TLV_V_SearchFileResponse);
				int count = srr.getCount();
				int result = srr.getResult();
				if (count < 0 ) {
					count = count + 256;
				}
				if ((count > 0) && (result == 1)) {

				}
			} else if (tlv_Header.getTlv_type() == TLV_T_Command.TLV_V_RECORDINFO) {//RecordInfo
				TLV_V_RecordInfo info = (TLV_V_RecordInfo)ByteArray2Object.convert2Object(TLV_V_RecordInfo.class, data,flag, OWSP_LEN.TLV_V_RECORDINFO);
				recordInfoList.add(info);
				returnValue = 2;
				//通知远程回放的进行界面更刷新
			}
			nLeft -= tlv_Header.getTlv_len();
			flag += tlv_Header.getTlv_len();
		}
		return returnValue;
	}

	private List<TLV_V_RecordInfo> recordInfoList = new ArrayList<TLV_V_RecordInfo>();
	
	@Override
	public List<TLV_V_RecordInfo> getRecordInfos(){
		return recordInfoList;
	}
	/**
	 * 更新UI消息传递
	 * 
	 * @param msg
	 *            消息对象
	 */
	private void updateUIMessage(Message msg) {
		// Handler handler = ViewManager.getInstance().getHandler();
		// if (handler != null) {
		// handler.sendMessage(msg);
		// } else {
		// ViewManager.getInstance().setHelpMsg(R.string.IDS_Unknown);
		// }
	}
}