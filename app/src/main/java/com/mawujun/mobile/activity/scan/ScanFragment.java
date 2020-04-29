package com.mawujun.mobile.activity.scan;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;


import com.mawujun.mobile.R;

import org.json.JSONException;
import org.json.JSONObject;

import me.devilsen.czxing.code.BarcodeFormat;
import me.devilsen.czxing.view.ScanListener;
import me.devilsen.czxing.view.ScanView;

public class ScanFragment extends Fragment {

    private ScanView scanView;

    private static Handler handler = new Handler();

    private Beep beep;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.scan_frag_layout, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        beep = new Beep(getActivity());
        beep.initBeepPlayer();
        scanView = (ScanView) getView().findViewById(R.id.scan_view);
        scanView.setScanMode(2);
        scanView.setScanListener(new ScanListener() {
            @Override
            public void onScanSuccess(final String result, BarcodeFormat format) {
                System.out.println("-------------" + result);
                // Toast.makeText(getActivity(),result,Toast.LENGTH_SHORT).show();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(),result, Toast.LENGTH_SHORT).show();
                    }
                });

                /*扫码成功  播放音频*/
                beep.play();

                /*延迟1.5秒 重新初始化扫码*/
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scanView.resetZoom();
                        scanView.startScan();
                    }
                }, 1500);
            }

            @Override
            public void onOpenCameraError() {

            }
        });
    }

    /**
     * 停止扫码
     */
    public void stopScan() {
        scanView.closeCamera();
        scanView.stopScan();
    }

    /**
     * 开始扫码
     */
    public void resumeScan() {
        scanView.openCamera();
        scanView.startScan();
    }

    @Override
    public void onStart() {
        super.onStart();
        scanView.openCamera();
        scanView.startScan();
    }

    @Override
    public void onResume() {
        super.onResume();
        scanView.openCamera();
        scanView.startScan();
    }

    @Override
    public void onPause() {
        super.onPause();
        scanView.stopScan();
        scanView.closeCamera();
    }

    @Override
    public void onDestroy() {
        scanView.onDestroy();
        super.onDestroy();
    }
}
