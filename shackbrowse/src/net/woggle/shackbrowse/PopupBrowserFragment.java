package net.woggle.shackbrowse;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings.ZoomDensity;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PopupBrowserFragment extends Fragment {
	
	private SharedPreferences mPrefs;
	private boolean mViewAvailable;
	WebView mWebview;
	private int mAttemptZoom = 0;
	private int dWidth;
	private int dHeight;
	private String _href;
	private String[] _hrefs;
	private int coarseZoom = 0;
	private int fineZoom = 0;
	public int mState;
	final static public String TEST_IMAGE = "arrows.png";
	public static final int BROWSER = 100;
	public static final int SHOW_ZOOM_CONTROLS = 200;
	public static final int SHOW_PHOTO_VIEW = 300;
	private boolean mIsCustomView = false;

	@Override
    public void onCreate(Bundle savedInstanceState)
    {
    	super.onCreate(savedInstanceState);
    	setRetainInstance(true);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
    }
    
    public View getParentView() { return getView(); }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {        
        mViewAvailable = true;
        return inflater.inflate(R.layout.popupbrowser, null);
    }

    
    @Override
    public void onDestroyView()
    {
    	mViewAvailable = false;
    	super.onDestroyView();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        mWebview = (WebView) getView().findViewById(R.id.popup_webView);
        mAttemptZoom = Integer.parseInt(mPrefs.getString("browserImageZoom5", "2500"));
        
        mWebview.getSettings().setBuiltInZoomControls(true);
		mWebview.getSettings().setDisplayZoomControls(true);
        mWebview.getSettings().setJavaScriptEnabled(true);
		mWebview.getSettings().setDomStorageEnabled(true);
		mWebview.getSettings().setDatabaseEnabled(true);
	   // mWebview.getSettings().setUserAgentString("Mozilla/5.0 (Linux; U; Android 2.0; en-us; Droid Build/ESD20) AppleWebKit/530.17 (KHTML, like Gecko) Version/4.0 Mobile Safari/530.17");
		mWebview.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);

		mWebview.setBackgroundColor(0x00000000);
        if (getActivity() != null)
    		((MainActivity)getActivity()).showOnlyProgressBarFromPTRLibrary(true);
        
        mWebview.setWebChromeClient(new WebChromeClient() {
			public void onProgressChanged(WebView view, int progress) {
				if ((getActivity() != null) && (progress > 9) && (progress < 100)) {
					((MainActivity) getActivity()).showOnlyProgressBarFromPTRLibraryDeterminate(true, progress);
				}
            	/*
            	if (pb != null && progress < 100)
            	{
            		pb.setVisibility(View.VISIBLE);
            		parent.setVisibility(View.VISIBLE);
            		inactive.setVisibility(View.GONE);
            		pb.bringToFront();
            		pb.setProgress(progress);
            	}
            	*/
				System.out.println("prog:" + progress);
				if (progress >= 100) {
					if (getActivity() != null)
						((MainActivity) getActivity()).showOnlyProgressBarFromPTRLibrary(false);
				}
				if (progress >= 50) {
					// way to tell if showing image or not
					if (!view.getSettings().getUserAgentString().contentEquals("nothing"))
						view.setBackgroundColor(Color.WHITE);
				}
			}

			@Override
			public void onReceivedTitle(WebView view, String title) {
				super.onReceivedTitle(view, title);
				if (!TextUtils.isEmpty(title)) {
					if (getActivity() != null) {
						((MainActivity) getActivity()).setBrowserTitle(title);
						if (!mIsCustomView)
						{
							_href = view.getUrl();
						}
					}
				}
			}
        });
        
        mWebview.setWebViewClient(new WebViewClient() { @Override public boolean shouldOverrideUrlLoading(WebView view,String _href) { return false; } });

        mWebview.getSettings().setUseWideViewPort(true);
        mWebview.getSettings().setLoadWithOverviewMode(true);
        
        // calculate sizes
        Display display = ((WindowManager)getActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics displaymetrics = new DisplayMetrics();
        display.getMetrics(displaymetrics);
        dWidth = (int) Math.floor(displaymetrics.widthPixels);// - (28 * (displaymetrics.xdpi / 160)));
        // px = dp * (dpi / 160) according to android docs. so to use dips, use formula
        dHeight = (int) Math.floor(displaymetrics.heightPixels);// - ( 110 * (displaymetrics.ydpi / 160) ));
        
        Bundle args = getArguments();
        String[] hrefs = args.getStringArray("hrefs");
        
        mState = BROWSER;
        
        if (args.containsKey("showZoomSetup"))
        {
        	showZoomSetup();
        }
		else if (args.containsKey("showPhotoView"))
		{
			showPhotoView(hrefs);
		}
        else
        	open(hrefs);
    }
    
    // reset the progress bars when we are detached from the activity
    @Override
    public void onDetach()
    {
	    try
	    {
		    if (getActivity() != null)
			    ((MainActivity) getActivity()).showOnlyProgressBarFromPTRLibrary(false);
	    }
	    catch (Exception e)
	    {

	    }


    	super.onDetach();


	    if (mWebview != null)
	    {
		    mWebview.removeAllViews();
		    mWebview.destroy();
	    }
    }
    
    public void open(String... hrefs)
    {
    	open(false, hrefs);
    }
	public void open(boolean isTest, String... hrefs)
	{
		_href = hrefs[0];
		_hrefs = hrefs;
        _href = _href.trim();
        final String url = _href;
        
        // multi image
        if (hrefs.length > 1)
        {
        	String html = "<html><head><title>Images</title><style type='text/css'> body{ margin: 0; padding: 0 } </style></head><body>";
        	for (int i = hrefs.length -1; i >= 0; i--)
        	{
        		_href = imageUrlFixer(hrefs[i].trim());
        		if (isImage(_href))
        		{
        			if (mAttemptZoom > 0)
        				html = html +"<img width=\""+(dWidth * (.07 + (.0001 * mAttemptZoom)))+"\" src=\""+_href+"\" /><br />";
        			else
        				html = html +"<img src=\""+_href+"\" /><br />";
        		}
        	}
        	html = html +"</body></html>";
        	if (mAttemptZoom > 0)
        		mWebview.getSettings().setUseWideViewPort(false);
        	mWebview.getSettings().setUserAgentString("nothing");
        	//wb.setBackground(con.getResources().getDrawable(R.drawable.bg_app_ics));
            // wb.setBackgroundColor(con.getResources().getColor(R.color.webview_bg));
	        mIsCustomView = true;
        	mWebview.loadData(html, "text/html", null);
        }
        // check for images, we scale for them
        else if (isImage(_href))
        {
        	_href = imageUrlFixer(_href);
        	if (mAttemptZoom > 0)
        		mWebview.getSettings().setUseWideViewPort(false);
        	mWebview.getSettings().setUserAgentString("nothing");
        	String data="<html><head><title>Image</title><style type='text/css'> body{ margin: 0; padding: 0; " + (isTest ? "background-color: #fff;" : "") + " } </style></head>";
        	
        	if (mAttemptZoom > 0)
        	{
	        	if (dWidth < dHeight)
	        		data=data+"<body><center><img width=\""+(dWidth * (.07 + (.0001 * mAttemptZoom)))+"\" src=\""+_href+"\" /></center></body></html>";
	        	else
	        		data=data+"<body><center><img height=\""+(dHeight * (.07 + (.0001 * mAttemptZoom)))+"\" src=\""+_href+"\" /></center></body></html>";
        	}
        	else
        		data=data+"<body><center><img src=\""+_href+"\" /></center></body></html>";

        	//wb.setBackground(con.getResources().getDrawable(R.drawable.bg_app_ics));
            // wb.setBackgroundColor(con.getResources().getColor(R.color.webview_bg));
        	if (isTest)
	        {
		        mIsCustomView = true;
		        mWebview.loadDataWithBaseURL("file:///android_asset/", data, "text/html", null, null);
	        }
        	else
	        {
		        mIsCustomView = true;
		        mWebview.loadData(data, "text/html", null);
	        }
        }
        else
        {
	        mIsCustomView = false;
            mWebview.loadUrl(_href);
			if (getActivity() != null)
			{
				((MainActivity)getActivity()).setBrowserSubTitle(_href);
			}
        }
        mWebview.setTag("webview_tag");
	}

	public static boolean isImage (String _href)
	{
		return isImage(_href, true);
	}
	public static boolean isImage (String _href, boolean allowGif)
	{
		if (
				(
                 ((_href.length() >= 4) && (_href.trim().substring(_href.length() - 4).toLowerCase().contentEquals(".jpg")))
        	|| ((_href.length() >= 4) && allowGif && (_href.trim().substring(_href.length() - 4).toLowerCase().contentEquals(".gif")))
        	|| ((_href.length() >= 4) && (_href.trim().substring(_href.length() - 4).toLowerCase().contentEquals(".png")))
        	|| ((_href.length() >= 5) && (_href.trim().substring(_href.length() - 5).toLowerCase().contentEquals(".jpeg")))
				)
        	
        	)
        {
			return true;
        }
		else
		{
			return false;
		}
	}
	public static boolean isTweet (String _href)
	{
		if (getTweetId(_href) != null)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	public static Long getTweetId (String _href)
	{
		if (
				_href.contains("twitter.com/")
				)
		{
			String href[] = _href.split("/");
			String idplus[] = href[href.length -1].split("\\?");
			Long tid = null;
			try {
				tid = Long.parseLong(idplus[0]);
			}
			catch (Exception e) { e.printStackTrace(); }
			return tid;
		}
		else
		{
			return null;
		}
	}

	public static boolean isYoutube (String _href)
	{
		if ((
				_href.contains("/youtu.be/")
				) || (
			_href.contains("/www.youtube.com/")
	) || (
				_href.contains("/youtube.com/")
		))
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	public static String getYoutubeId (String _href)
	{
		if ((_href.contains("/youtu.be/")) || (_href.contains("youtube.com/")))
		{
			String pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*";

			Pattern compiledPattern = Pattern.compile(pattern);
			Matcher matcher = compiledPattern.matcher(_href); //url is youtube url for which you want to extract the id.
			if (matcher.find()) {
				return matcher.group();
			}
			else return null;
		}
		else
		{
			return null;
		}
	}

	// DEPRECATED
	private void showPhotoView(String... hrefs)
	{
		mState = SHOW_PHOTO_VIEW;

		_href = hrefs[0];
		_hrefs = hrefs;
		_href = _href.trim();
		final String url = _href;

		mWebview.setVisibility(View.GONE);
		ImageView image = (ImageView)getActivity().findViewById(R.id.popup_photoView);
		image.setVisibility(View.VISIBLE);

		getActivity().findViewById(R.id.popup_seekbar).setVisibility(View.GONE);
		getActivity().findViewById(R.id.popup_seekbarfine).setVisibility(View.GONE);
		getActivity().findViewById(R.id.popup_zoomcont).setVisibility(View.GONE);

		Glide.with(getActivity())
				.load(PopupBrowserFragment.imageUrlFixer(url))
				.apply(new RequestOptions()
				.fitCenter()
				.diskCacheStrategy(DiskCacheStrategy.ALL)
				.placeholder(R.drawable.ic_action_image_photo)
				.error(R.drawable.ic_action_content_flag))
				.into(image);


		((MainActivity)getActivity()).setTitleContextually();
		if (getActivity() != null)
			((MainActivity)getActivity()).showOnlyProgressBarFromPTRLibrary(false);
	}

	private void showZoomSetup()
	{
		mState = SHOW_ZOOM_CONTROLS;
		mWebview.getSettings().setBuiltInZoomControls(false);
		mWebview.getSettings().setDisplayZoomControls(false);
		getActivity().findViewById(R.id.popup_seekbar).setVisibility(View.VISIBLE);
		getActivity().findViewById(R.id.popup_seekbarfine).setVisibility(View.VISIBLE);
		getActivity().findViewById(R.id.popup_zoomcont).setVisibility(View.VISIBLE);
		
		coarseZoom = Math.round(Math.round(Math.floor(mAttemptZoom / 150f)));
		fineZoom = Math.round(Math.round(Math.floor((mAttemptZoom % 150) / 3)));
		mAttemptZoom = ((coarseZoom * 150) + (fineZoom * 3)) + 1;
		Editor edit = mPrefs.edit();
		edit.putString("browserImageZoom5", Integer.toString(mAttemptZoom));
		edit.commit();
		
		// move seekbars to their correct positions
		((SeekBar)getActivity().findViewById(R.id.popup_seekbar)).setProgress(coarseZoom);
		((SeekBar)getActivity().findViewById(R.id.popup_seekbarfine)).setProgress(fineZoom);
		
		((SeekBar)getActivity().findViewById(R.id.popup_seekbar)).setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				coarseZoom = (progress);
				mAttemptZoom = ((coarseZoom * 150) + (fineZoom * 3)) + 1;
				Editor edit = mPrefs.edit();
				edit.putString("browserImageZoom5", Integer.toString(mAttemptZoom));
				edit.commit();
				open(true, TEST_IMAGE);

			}
		});
		((SeekBar)getActivity().findViewById(R.id.popup_seekbarfine)).setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				fineZoom = (progress);
				mAttemptZoom = ((coarseZoom * 150) + (fineZoom * 3)) + 1;
				Editor edit = mPrefs.edit();
				edit.putString("browserImageZoom5", Integer.toString(mAttemptZoom));
				edit.commit();
				open(true, TEST_IMAGE);

			}
		});
		
		open(true, TEST_IMAGE);
		((MainActivity)getActivity()).setTitleContextually();
	}
	
	public static String imageUrlFixer (String _href)
	{
		if (_href.contains("shackpics.com"))
        {
        	String[] splt = _href.split("shackpics.com");
        	if (splt[1] != null)
        	{
        		_href = "http://chattypics.com/" + splt[1];
        	}
        }
		if (_href.contains("chattypics.com/viewer.php?"))
        {
        	String[] splt = _href.split("=");
        	if (splt[1] != null)
        	{
        		_href = "http://chattypics.com/files/" + splt[1];
        	}
        }
        if (_href.contains("chatty.pics/viewer.php?"))
        {
            String[] splt = _href.split("=");
            if (splt[1] != null)
            {
                _href = "http://chatty.pics/files/" + splt[1];
            }
        }
        if (_href.contains("fukung.net/v/"))
        {
        	String[] splt = _href.split(".net/v");
        	if (splt[1] != null)
        	{
        		_href = "http://media.fukung.net/images/" + splt[1];
        	}
        }
        if (_href.contains("www.dropbox"))
        {
        	_href = _href + "?dl=1";
        }
        return _href;
	}

	public void openExternal() { openExternal(_href); }
	public void openExternal(String href) {
		if (getActivity() != null)
		{
			if (!href.contains("http://") && !href.contains("https://"))
			{
				href = "http://" + href;
			}
			Intent i = new Intent(Intent.ACTION_VIEW, 
	  		       Uri.parse(href));
	  		getActivity().startActivity(i);
		}
	}
	
	public void copyURL()
	{
		if (getActivity() != null)
		{
			((MainActivity)getActivity()).copyText(getHREFText());
		}
	}
	public void shareURL()
	{
		if (getActivity() != null)
		{
	    	Toast.makeText(getActivity(), getHREFText(), Toast.LENGTH_SHORT).show();
	    	Intent sendIntent = new Intent();
    	    sendIntent.setAction(Intent.ACTION_SEND);
    	    sendIntent.putExtra(Intent.EXTRA_TEXT, getHREFText());
    	    sendIntent.setType("text/plain");
    	    getActivity().startActivity(Intent.createChooser(sendIntent, "Share Link"));
		}
	}
	public String getHREFText()
	{
		String copyText = null;
		if (_hrefs.length > 1)
    	{
    		
    		for (int i = 0; i < _hrefs.length; i++)
    		{
    			if (i == 0)
    			{
    				copyText = _hrefs[i];
    			}
    			else
    			{
    				copyText = copyText + " " + _hrefs[i];
    			}
    		}
    	}
		else
			copyText = _href;
		return copyText;
	}
}
