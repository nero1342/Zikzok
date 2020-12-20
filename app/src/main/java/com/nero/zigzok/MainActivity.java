package com.nero.zigzok;

import us.zoom.sdk.InviteOptions;
import us.zoom.sdk.JoinMeetingOptions;
import us.zoom.sdk.JoinMeetingParams;
import us.zoom.sdk.MeetingError;
import us.zoom.sdk.MeetingService;
import us.zoom.sdk.MeetingServiceListener;
import us.zoom.sdk.MeetingStatus;
import us.zoom.sdk.MeetingViewsOptions;
import us.zoom.sdk.StartMeetingOptions;
import us.zoom.sdk.StartMeetingParamsWithoutLogin;
import us.zoom.sdk.ZoomError;
import us.zoom.sdk.ZoomSDK;
import us.zoom.sdk.ZoomSDKInitParams;
import us.zoom.sdk.ZoomSDKInitializeListener;
import us.zoom.sdk.ZoomSDKRawDataMemoryMode;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.nero.zigzok.room.MeetingInfo;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import static com.nero.zigzok.room.Constants.MEETING_ID;
import static com.nero.zigzok.room.Constants.MEETING_PASSWORD;
import static com.nero.zigzok.room.Constants.SDK_KEY;
import static com.nero.zigzok.room.Constants.SDK_SECRET;
import static com.nero.zigzok.room.Constants.USER_ID;
import static com.nero.zigzok.room.Constants.WEB_DOMAIN;
import static com.nero.zigzok.room.Constants.ZOOM_ACCESS_TOKEN;

public class MainActivity extends AppCompatActivity implements MeetingServiceListener, ZoomSDKInitializeListener {

    private final static String TAG = "Zigzok";

    public final static String ACTION_RETURN_FROM_MEETING = "us.zoom.sdkexample2.action.ReturnFromMeeting";
    public final static String EXTRA_TAB_ID = "tabId";

    public final static int TAB_HOME = 1;
    public final static int TAB_CREATEROOM = 2;
    public final static int TAB_JOINROOM  = 3;
    public final static int TAB_SETTING  = 3;

    private final static int STYPE = MeetingService.USER_TYPE_API_USER;
    private final static String DISPLAY_NAME = "Zigzok";

    private View _viewHome;
    private View _viewCreateRoom;
    private View _viewJoinRoom;
    private View _viewSettings;

    private ImageButton _btnCreateRoom;
    private ImageButton _btnJoinRoom;
    private ImageButton _btnSettings;

    private Button _btnJoinRoomFinal;
    private Button _btnCreateRoomFinal;
    private ProgressDialog _progressDialog;

    MutableLiveData<String> _responseCreateRoom = new MutableLiveData<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

//        setupTabs();

        ZoomSDK zoomSDK = ZoomSDK.getInstance();

        if(savedInstanceState == null) {
            ZoomSDKInitParams initParams = new ZoomSDKInitParams();
            initParams.appKey = SDK_KEY;
            initParams.appSecret = SDK_SECRET;
            initParams.domain= WEB_DOMAIN;
            //initParams.videoRawDataMemoryMode = ZoomSDKRawDataMemoryMode.ZoomSDKRawDataMemoryModeStack;
            zoomSDK.initialize(getApplicationContext(), this, initParams);
        }

        if(zoomSDK.isInitialized()) {
            registerMeetingServiceListener();
        }

        initComponents();
        _progressDialog = new ProgressDialog(this);
    }

    private void initComponents() {
        // Home's buttons
        _btnCreateRoom = (ImageButton) findViewById(R.id.btnCreateRoom);
        _btnJoinRoom = (ImageButton) findViewById(R.id.btnJoinRoom);
        _btnSettings = (ImageButton) findViewById(R.id.btnSettings);

//        _btnCreateRoom.setOnClickListener(helper(R.layout.create_room));
//
//        _btnSettings.setOnClickListener(helper(R.layout.settings));

        initCreateRoom();
        initJoinRoom();
        initSettings();
    }


    private void initSettings() {
        _viewSettings = LayoutInflater.from(this).inflate(R.layout.settings, null);
        _btnSettings.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                // create the popup window
                int width = (int) pxToDp(321);
                int height = (int) pxToDp(360);
                boolean focusable = true; // lets taps outside the popup also dismiss it
                final PopupWindow popupWindow = new PopupWindow(_viewSettings, width, height, focusable);
                // show the popup window
                // which view you pass in doesn't matter, it is only used for the window tolken
                popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
            }
        });
    }

    private void initJoinRoom() {
        _viewJoinRoom = LayoutInflater.from(this).inflate(R.layout.join_room, null);
        _btnJoinRoom.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                // create the popup window
                int width = (int) pxToDp(321);
                int height = (int) pxToDp(360);
                boolean focusable = true; // lets taps outside the popup also dismiss it
                final PopupWindow popupWindow = new PopupWindow(_viewJoinRoom, width, height, focusable);
                // show the popup window
                // which view you pass in doesn't matter, it is only used for the window tolken
                popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
            }
        });

        _btnJoinRoomFinal = (Button) _viewJoinRoom.findViewById(R.id.btnJoinRoomFinal);
        _btnJoinRoomFinal.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String _username = String.valueOf(((EditText)_viewJoinRoom.findViewById(R.id.txtUsername)).getText());
                String _roomID = String.valueOf(((EditText)_viewJoinRoom.findViewById(R.id.txtRoomID)).getText());
                joinRoom(_roomID, _username);
            }
        });
    }

    private double pxToDp(int px) {
        return px * getResources().getDisplayMetrics().density;
    }

    private void initCreateRoom() {
        _viewCreateRoom = LayoutInflater.from(this).inflate(R.layout.create_room, null);
        _btnCreateRoom.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                // create the popup window
                int width = (int) pxToDp(321);
                int height = (int) pxToDp(360);
                boolean focusable = true; // lets taps outside the popup also dismiss it
                final PopupWindow popupWindow = new PopupWindow(_viewCreateRoom, width, height, focusable);
                // show the popup window
                // which view you pass in doesn't matter, it is only used for the window tolken
                popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
            }
        });
        _btnCreateRoomFinal = (Button) _viewCreateRoom.findViewById(R.id.btnCreateRoomFinal);
        _btnCreateRoomFinal.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String _username = String.valueOf(((EditText)_viewCreateRoom.findViewById(R.id.txtUsername)).getText());
                createRoom(_username);
            }
        });
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // disable animation
        overridePendingTransition(0,0);

        String action = intent.getAction();

        if(ACTION_RETURN_FROM_MEETING.equals(action)) {
//            int tabId = intent.getIntExtra(EXTRA_TAB_ID, TAB_HOME);
//            selectTab(tabId);
        }
    }

    @Override
    public void onZoomSDKInitializeResult(int errorCode, int internalErrorCode) {
        Log.i(TAG, "onZoomSDKInitializeResult, errorCode=" + errorCode + ", internalErrorCode=" + internalErrorCode);

        if(errorCode != ZoomError.ZOOM_ERROR_SUCCESS) {
            Toast.makeText(this, "Failed to initialize Zoom SDK. Error: " + errorCode + ", internalErrorCode=" + internalErrorCode, Toast.LENGTH_LONG);
        } else {
            Toast.makeText(this, "Initialize Zoom SDK successfully.", Toast.LENGTH_LONG).show();

            registerMeetingServiceListener();
        }
    }

    private void registerMeetingServiceListener() {
        ZoomSDK zoomSDK = ZoomSDK.getInstance();
        MeetingService meetingService = zoomSDK.getMeetingService();
        if(meetingService != null) {
            meetingService.addListener(this);
        }
    }

    @Override
    protected void onDestroy() {
        ZoomSDK zoomSDK = ZoomSDK.getInstance();

        if(zoomSDK.isInitialized()) {
            MeetingService meetingService = zoomSDK.getMeetingService();
            meetingService.removeListener(this);
        }

        super.onDestroy();
    }
    private void joinRoom(String roomId, String username) {
        MeetingInfo meetingInfo = MeetingInfo.getInstance();
        meetingInfo.setMeetingId(roomId);
        meetingInfo.setPassword("??????");
        ZoomSDK zoomSDK = ZoomSDK.getInstance();

        if(!zoomSDK.isInitialized()) {
            Toast.makeText(this, "ZoomSDK has not been initialized successfully", Toast.LENGTH_LONG).show();
            return;
        }

        if(roomId == null) {
            Toast.makeText(this, "roomID can not be NULL", Toast.LENGTH_LONG).show();
            return;
        }

        MeetingService meetingService = zoomSDK.getMeetingService();
//
        JoinMeetingOptions opts = new JoinMeetingOptions();
        opts.no_driving_mode = true;
        opts.no_titlebar = true;
        opts.no_bottom_toolbar = true;
        opts.no_invite = true;
        opts.no_video = true;
        JoinMeetingParams params = new JoinMeetingParams();
        params.meetingNo = roomId;
        params.displayName = username;

        int ret = meetingService.joinMeetingWithParams(getApplicationContext(), params, opts);

        Log.i(TAG, "onClickBtnStartMeeting, ret=" + ret);
    }

    public void createRoom(final String username) {
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "{}");
        Request request = new Request.Builder()
                .url("https://api.zoom.us/v2/users/nero18@apcs.vn/meetings")
                .post(body)
                .addHeader("content-type", "application/json")
                .addHeader("authorization", "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOm51bGwsImlzcyI6Ilk1WTdUZlJvUVZPaElhYnQ0aG9zS3ciLCJleHAiOjE3NjYyMjk5MDAsImlhdCI6MTYwODQ1ODEyNX0.zSvQLIN0w-9PMjufhPRSLfmAO7r2fBBsWkZZUCKcrEA")
                .build();

        _progressDialog.setTitle("Creating room...");
        _progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        _progressDialog.setMessage("Wait a few seconds...");
        _progressDialog.show();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                Log.i("Error","Failed to connect: "+e.getMessage());
            }

            @Override
            public void onResponse(Response response) throws IOException {
                // Log.d(TAG, response.body().string());
                String x = response.body().string();
                try {
                    JSONObject json = new JSONObject(x);
                    String roomID = json.getString("id");
                    String password = json.getString("password");
                    createRoom(username, roomID, password);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                _progressDialog.dismiss();
            }
        });
    }
    public void createRoom(String username, String roomId, String password) {
        MeetingInfo meetingInfo = MeetingInfo.getInstance();
        meetingInfo.setMeetingId(roomId);
        meetingInfo.setPassword(password);

        ZoomSDK zoomSDK = ZoomSDK.getInstance();

        if(!zoomSDK.isInitialized()) {
            Toast.makeText(this, "ZoomSDK has not been initialized successfully", Toast.LENGTH_LONG).show();
            return;
        }

        if(MEETING_ID == null) {
            Toast.makeText(this, "MEETING_ID in Constants can not be NULL", Toast.LENGTH_LONG).show();
            return;
        }

        MeetingService meetingService = zoomSDK.getMeetingService();

        JoinMeetingOptions opts = new JoinMeetingOptions();
        opts.no_driving_mode = true;
        opts.no_titlebar = true;
        opts.no_bottom_toolbar = true;
        opts.no_invite = true;
        opts.no_video = true;

        StartMeetingParamsWithoutLogin  params = new StartMeetingParamsWithoutLogin ();
        params.userId = USER_ID;
        params.userType = 1;
        params.meetingNo = roomId;
        params.displayName = username;
        params.zoomAccessToken = ZOOM_ACCESS_TOKEN;
        int ret = meetingService.startMeetingWithParams(getApplicationContext(), params, opts);
        Log.i(TAG, "create room, ret=" + ret);
    }

    @Override
    public void onMeetingStatusChanged(MeetingStatus meetingStatus, int errorCode,
                                       int internalErrorCode) {

        if(meetingStatus == meetingStatus.MEETING_STATUS_FAILED && errorCode == MeetingError.MEETING_ERROR_CLIENT_INCOMPATIBLE) {
            Toast.makeText(this, "Version of ZoomSDK is too low!", Toast.LENGTH_LONG).show();
        }

        if(meetingStatus == MeetingStatus.MEETING_STATUS_IDLE || meetingStatus == MeetingStatus.MEETING_STATUS_FAILED) {
//            selectTab(TAB_WELCOME);
        }
    }

    @Override
    public void onZoomAuthIdentityExpired() {

    }

}