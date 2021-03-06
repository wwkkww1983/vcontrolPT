package com.vcontrol.vcontroliot.fragment;

import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.vcontrol.vcontroliot.R;
import com.vcontrol.vcontroliot.VcontrolApplication;
import com.vcontrol.vcontroliot.util.ConfigParams;
import com.vcontrol.vcontroliot.util.EventNotifyHelper;
import com.vcontrol.vcontroliot.util.ProgressBarUtil;
import com.vcontrol.vcontroliot.util.SocketUtil;
import com.vcontrol.vcontroliot.util.ToastUtil;
import com.vcontrol.vcontroliot.util.UiEventEntry;

/**
 * Created by Vcontrol on 2016/11/23.
 */

public class GroundADFragment extends BaseFragment implements View.OnClickListener, EventNotifyHelper.NotificationCenterDelegate
{


    private Button adUPButton;
    private Button adDownButton;
    private TextView currentLv;

    private boolean isHigh = true;


    private Runnable adRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            if (isHigh)
            {
                SocketUtil.getSocketUtil().sendContent(ConfigParams.ReadBatteryHighStatus);
            }
            else
            {
                SocketUtil.getSocketUtil().sendContent(ConfigParams.ReadBatteryLowStatus);
            }
            VcontrolApplication.applicationHandler.postDelayed(adRunnable, UiEventEntry.TIME);
        }
    };

    @Override
    public int getLayoutView()
    {
        return R.layout.fragment_setting_ground_ad;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        VcontrolApplication.applicationHandler.removeCallbacks(adRunnable);
        EventNotifyHelper.getInstance().removeObserver(this, UiEventEntry.READ_DATA);
        EventNotifyHelper.getInstance().removeObserver(this, UiEventEntry.READ_AD_LV);
    }

    @Override
    public void initComponentViews(View view)
    {
        EventNotifyHelper.getInstance().addObserver(this, UiEventEntry.READ_AD_LV);
        EventNotifyHelper.getInstance().addObserver(this, UiEventEntry.READ_DATA);

        adUPButton = (Button) view.findViewById(R.id.ad_up_button);
        adDownButton = (Button) view.findViewById(R.id.ad_down_button);
        currentLv = (TextView) view.findViewById(R.id.current_lv);
        currentLv.setText(R.string.nocollect);
    }

    @Override
    public void initData()
    {

    }

    @Override
    public void setListener()
    {
        adDownButton.setOnClickListener(this);
        adUPButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view)
    {

//        if (!SocketUtil.getSocketUtil().isConnected())
//        {
//            return;
//        }

        ProgressBarUtil.showProgressDialog(getActivity(), "", getString(R.string.In_configuration));
        switch (view.getId())
        {
            case R.id.ad_down_button:
                SocketUtil.getSocketUtil().sendContent(ConfigParams.SetBatteryLow);
                isHigh = false;
                break;
            case R.id.ad_up_button:
                SocketUtil.getSocketUtil().sendContent(ConfigParams.SetBatteryHigh);
                isHigh = true;
                break;

            default:
                break;
        }

        VcontrolApplication.applicationHandler.postDelayed(adRunnable, UiEventEntry.AD_TIME);

    }


    @Override
    public void didReceivedNotification(int id, Object... args)
    {
        if (id == UiEventEntry.READ_AD_LV)
        {
            String result = (String) args[0];
            if (TextUtils.isEmpty(result))
            {
                return;
            }
            if (result.contains("14V AD is measuring") || result.contains("11V AD is measuring"))
            {//正在配置

            }
            else if (result.contains("14V AD is OK") || result.contains("11V AD is OK"))
            {
                ProgressBarUtil.dismissProgressDialog();
                VcontrolApplication.applicationHandler.removeCallbacks(adRunnable);
                ToastUtil.showToastLong(getString(R.string.Acquisition_success));
            }
        }
        else if (id == UiEventEntry.READ_DATA)
        {
            String result = (String) args[0];
            if (TextUtils.isEmpty(result))
            {
                return;
            }
            if (result.contains(ConfigParams.BatteryVolts))
            {
                String lv = result.replaceAll(ConfigParams.BatteryVolts, "").trim();

                if (currentLv != null)
                {
                    currentLv.setText(lv + getString(R.string.v));
                }
            }
        }
    }
}
