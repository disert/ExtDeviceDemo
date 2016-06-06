package com.coolshow.extdevicedemo.utils;

import android.content.Context;
import android.widget.Toast;

/**
 * 吐司工具类,使用它可以通过改变isshow的值来方便的控制是否要显示吐司
 * 
 * @author yangling
 *
 */
public class ToastUtils {
	public static boolean isShow = true;
	
	public static void showToast(Context context, String text){
		if(isShow){
			Toast.makeText(context, text, Toast.LENGTH_LONG).show();
		}
	}

}
