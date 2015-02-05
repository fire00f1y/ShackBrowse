package net.woggle.shackbrowse;

import java.util.LinkedList;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBarActivity;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import org.json.JSONArray;
import org.json.JSONObject;


/**
 */
public class DonateActivity extends ActionBarActivity {
    // Debug tag, for logging
    static final String TAG = "ShackBrowseDonate";

    // Does the user have the premium upgrade?
    boolean mIsUnlocked = false;
    boolean mHasChecked = false;



    // (arbitrary) request code for the purchase flow
    static final int RC_REQUEST = 10001;

	public SharedPreferences _prefs;

	protected String _unlockData;

	protected String _unlockSign;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.donate);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        _prefs = PreferenceManager.getDefaultSharedPreferences(this);


        new DonatorTask().execute();

        _donatorStatus = _prefs.getBoolean("enableDonatorFeatures", false);
        mIsUnlocked = _prefs.getBoolean("enableDonatorFeatures", false);
        mHasChecked = false;
        // Some sanity checks to see if the developer (that's you!) really followed the
        // instructions to run this sample (don't put these checks on your app!)

        
        this.findViewById(R.id.donateUnlockButton).setOnClickListener(new OnClickListener (){
			@Override
			public void onClick(View v) {
				if (mIsUnlocked)
					setScreen(2);
			}
        });
        
        this.findViewById(R.id.donateDisableLimeDisplay).setOnClickListener(new OnClickListener (){
			@Override
			public void onClick(View v) {
				boolean displayLimes = true;
				if (((CheckBox)v).isChecked())
				{
					displayLimes = false;
				}
				SharedPreferences.Editor editor = _prefs.edit();
            	editor.putBoolean("displayLimes", displayLimes);
            	editor.commit();
			}
        });
        
        
        this.findViewById(R.id.donateToggleLime).setOnClickListener(new OnClickListener (){
			@Override
			public void onClick(View v) {
				boolean verified = _prefs.getBoolean("usernameVerified", false);
		        if (!verified)
		        {
		        	LoginForm login = new LoginForm(DonateActivity.this);
		        	login.setOnVerifiedListener(new LoginForm.OnVerifiedListener() {
						@Override
						public void onSuccess() {
			                if (_limeRegistered)
			                	new LimeTask().execute("remove");
			                else
			                	new LimeTask().execute("add");
						}

						@Override
						public void onFailure() {
						}
					});
		        	return;
		        }
		        else
		        {
	                if (_limeRegistered)
	                	new LimeTask().execute("remove");
	                else
	                	new LimeTask().execute("add");
		        }
			}
        });
        
        String versionName = "Unknown";
        try {
			versionName = this.getApplication().getPackageManager().getPackageInfo(getApplication().getPackageName(), 0 ).versionName;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        ((TextView)findViewById(R.id.donateVersion)).setText("Version " + versionName);
        
        this.findViewById(R.id.changeLog).setOnClickListener(new OnClickListener (){
			@Override
			public void onClick(View v) {
				ChangeLog cl = new ChangeLog(DonateActivity.this);
		        cl.getFullLogDialog().show();
			}
        });
        
        
        updateUi();
    }


	public MaterialDialog _progressDialog;

	private boolean _limeRegistered;

	private boolean _donatorStatus;

    
    // We're being destroyed. It's important to dispose of the helper here!
    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    // updates UI to reflect model
    public void updateUi() {
        // update the car color to reflect premium status or lack thereof
    	if (mHasChecked)
    	{
	        ((Button)findViewById(R.id.donateUnlockButton)).setEnabled(true);
	        if (mIsUnlocked)
	        {
	        	((Button)findViewById(R.id.donateUnlockButton)).setText("Access Lime Settings");
	        	Editor edit = _prefs.edit();
	        	edit.putBoolean("enableDonatorFeatures", true);
	        	edit.commit();
	        }
	        else
	        {
	        	((Button)findViewById(R.id.donateUnlockButton)).setText("Lime Settings Locked");
                ((Button)findViewById(R.id.donateUnlockButton)).setEnabled(false);
	        }
    	}
        
        _limeRegistered = _prefs.getString("limeUsers", "").contains(_prefs.getString("userName", "")) && !_prefs.getString("userName", "").equals("");
        
        SpannableString reg = new SpannableString("Serverside Lime Status: Registered for display next to username \"" + _prefs.getString("userName", "") + "\"");
        reg.setSpan(new ForegroundColorSpan(Color.rgb(100,255,100)), 23, reg.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString notreg = new SpannableString("Serverside Lime Status: Not registered for display");
        notreg.setSpan(new ForegroundColorSpan(Color.rgb(255,100,100)), 23, notreg.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        ((TextView)findViewById(R.id.donateLimeStatus)).setText(((_limeRegistered) ? reg : notreg));

        _donatorStatus = _prefs.getBoolean("enableDonatorFeatures", false);
        SpannableString locked = new SpannableString("Donator Status: locked");
        locked.setSpan(new ForegroundColorSpan(Color.rgb(255,100,100)), 15, locked.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString unlocked = new SpannableString("Donator Status: unlocked");
        unlocked.setSpan(new ForegroundColorSpan(Color.rgb(100,255,100)), 15, unlocked.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        ((TextView)findViewById(R.id.donatorStatus)).setText(((_donatorStatus) ? unlocked : locked));
        
        ((CheckBox)findViewById(R.id.donateDisableLimeDisplay)).setChecked(!_prefs.getBoolean("displayLimes", true));
    }

    class DonatorTask extends AsyncTask<String, Void, JSONArray>
    {
        Exception _exception;
        private String userName;

        @Override
        protected JSONArray doInBackground(String... params)
        {
            try
            {
                userName = _prefs.getString("userName", "");
                if (!userName.equals(""))
                    return ShackApi.getDonators();
                else
                    return null;
            }
            catch (Exception e)
            {
                Log.e("shackbrowse", "Error getting donators", e);
                _exception = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONArray result)
        {
            try {
                _progressDialog.dismiss();
            }
            catch (Exception e)
            {

            }

            if (_exception != null)
            {
                System.out.println("limechange: err");
                ErrorDialog.display(DonateActivity.this, "Error", "Error getting list of donators:\n" + _exception.getMessage());
            }
            else if (result == null)
            {
                mHasChecked = true;
                mIsUnlocked = false;
                updateUi();
            }
            else {
                System.out.println("DONATEACTIVITY: downloaded donator list: " + result);

                for (int i = 0; i < result.length(); i++) {
                    try {
                        if (result.getJSONObject(i).getString("user").equals(userName))
                        {
                            ErrorDialog.display(DonateActivity.this, "Congrats", "Found your name in the online donator list.");
                            Editor edit = _prefs.edit();
                            edit.putBoolean("enableDonatorFeatures", true);
                            edit.commit();
                            break;
                        }
                    }
                    catch (Exception e) {}
                }

                runOnUiThread(new Runnable(){
                    @Override public void run()
                    {
                        mHasChecked = true;
                        mIsUnlocked = _prefs.getBoolean("enableDonatorFeatures", false);
                        updateUi();
                    }
                });
            }
        }
    }
    
	class LimeTask extends AsyncTask<String, Void, String>
	{
	    Exception _exception;
		private String _taskMode;
	    
        @Override
        protected String doInBackground(String... params)
        {
            try
            {
                String userName = _prefs.getString("userName", "");
                
                _taskMode = params[0];
                
                // actually upload the thing
                if (params[0].equals("add"))
                {
                	runOnUiThread(new Runnable(){
                		@Override public void run()
                		{
                			_progressDialog = MaterialProgressDialog.show(DonateActivity.this, "Adding Lime", "Communicating with server...");
                		}
                	});
                	return ShackApi.putDonator(true, userName);
                }
                
                if (params[0].equals("remove"))
                {
                	runOnUiThread(new Runnable(){
                		@Override public void run()
                		{
                			_progressDialog = MaterialProgressDialog.show(DonateActivity.this, "Removing Lime", "Communicating with server...");
                		}
                	});
                	return ShackApi.putDonator(false, userName);
                }
                
                if (params[0].equals("get"))
                	return ShackApi.getLimeList();
                
                return null;
            }
            catch (Exception e)
            {
                Log.e("shackbrowse", "Error changing lime status", e);
                _exception = e;
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(String result)
        {
        	try {
        		_progressDialog.dismiss();
        	}
        	catch (Exception e)
        	{
        		
        	}
            
            if (_exception != null)
            {
            	System.out.println("limechange: err");
                ErrorDialog.display(DonateActivity.this, "Error", "Error changing lime:\n" + _exception.getMessage());
            }
            else if (result == null)
            {
            	System.out.println("limechange: err");
                ErrorDialog.display(DonateActivity.this, "Error", "Unknown lime error.");
            }
            else if (!_taskMode.equals("get"))
            {
            	runOnUiThread(new Runnable(){
            		@Override public void run()
            		{
            			new LimeTask().execute("get");
            		}
            	});
            }
            else if (_taskMode.equals("get"))
            {
            	System.out.println("DONATEACTIVITY: downloaded donator list: " + result);
            	SharedPreferences.Editor editor = _prefs.edit();
            	editor.putString("limeUsers", result);
            	editor.commit();
            	
            	runOnUiThread(new Runnable(){
            		@Override public void run()
            		{
            			updateUi();
            		}
            	});
            }
        }
	}
    
    public String getUsername()
    {
        AccountManager manager = AccountManager.get(this); 
        Account[] accounts = manager.getAccountsByType("com.google"); 
        List<String> possibleEmails = new LinkedList<String>();

        for (Account account : accounts)
        {
          // TODO: Check possibleEmail against an email regex or treat
          // account.name as an email address only for certain account.type values.
        	possibleEmails.add(account.name);
        }

        if(!possibleEmails.isEmpty() && possibleEmails.get(0) != null)
        {
            String email = possibleEmails.get(0);
            String[] parts = email.split("@");
            if(parts.length > 0 && parts[0] != null)
                return parts[0];
            else
                return null;
        }
        else
            return null;
    }

    // Enables or disables the "please wait" screen.
    void setScreen(int screen) {
        findViewById(R.id.screen_main).setVisibility(screen == 0 ? View.VISIBLE : View.GONE);
        findViewById(R.id.screen_wait).setVisibility(screen == 1 ? View.VISIBLE : View.GONE);
        findViewById(R.id.screen_features).setVisibility(screen == 2 ? View.VISIBLE : View.GONE);
    }

    void complain(String message) {
        Log.e(TAG, "**** TrivialDrive Error: " + message);
        alert("Error: " + message);
    }

    void alert(String message) {
        AlertDialog.Builder bld = new AlertDialog.Builder(this);
        bld.setMessage(message);
        bld.setNeutralButton("OK", null);
        Log.d(TAG, "Showing alert dialog: " + message);
        bld.create().show();
    }
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent upIntent = new Intent(this, MainActivity.class);
                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                    // This activity is not part of the application's task, so create a new task
                    // with a synthesized back stack.
                    TaskStackBuilder.from(this)
                            .addNextIntent(upIntent)
                            .startActivities();
                    finish();
                } else {
                    // This activity is part of the application's task, so simply
                    // navigate up to the hierarchical parent activity.
                    finish();
                }
                return true;
        }
		return false;
    }
}