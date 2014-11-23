package ru.subprogram.paranoidsmsblocker.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import ru.subprogram.paranoidsmsblocker.R;
import ru.subprogram.paranoidsmsblocker.activities.filemanager.CAFileManagerActivity;
import ru.subprogram.paranoidsmsblocker.activities.filemanager.CAFileManagerFragment;
import ru.subprogram.paranoidsmsblocker.activities.utils.CAErrorDisplay;
import ru.subprogram.paranoidsmsblocker.adapters.CATabCollectionPagerAdapter;
import ru.subprogram.paranoidsmsblocker.database.CADbEngine;
import ru.subprogram.paranoidsmsblocker.database.entities.CAContact;
import ru.subprogram.paranoidsmsblocker.database.entities.CASms;
import ru.subprogram.paranoidsmsblocker.database.entities.TAContactStatus;
import ru.subprogram.paranoidsmsblocker.dialogs.CAAlertDialogFragment;
import ru.subprogram.paranoidsmsblocker.dialogs.CASmsDialogFragment;
import ru.subprogram.paranoidsmsblocker.dialogs.IAAlertDialogObserver;
import ru.subprogram.paranoidsmsblocker.dialogs.IASmsDialogObserver;
import ru.subprogram.paranoidsmsblocker.exceptions.CAError;
import ru.subprogram.paranoidsmsblocker.exceptions.CAException;
import ru.subprogram.paranoidsmsblocker.fragments.CABlackListFragment;
import ru.subprogram.paranoidsmsblocker.utils.CAUtils;
import ru.subprogram.paranoidsmsblocker.utils.CAUtils.GetMissingBlackListItemsObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CAMainActivity extends ActionBarActivity 
								implements IAMainActivityFragmentObserver,
									IASmsDialogObserver, IAAlertDialogObserver {
	
	public static final String KEY_TAB_POSITION = "KEY_TAB_POSITION";

	private static final int REQUEST_PICK_FOLDER = 10001;
	private static final int REQUEST_PICK_FILE 	 = 10002;

	private static final String SMS_DIALOG = "SMS_DIALOG";
	private static final String DELETE_ALL_DIALOG = "DELETE_ALL_DIALOG";
	private static final String DELETE_SELECTED_ITEMS_DIALOG = "DELETE_SELECTED_ITEMS_DIALOG";

	private static final String TAB_POSITION = "TAB_POSITION";

	private static final String KEY_SELECTED_IDS = "KEY_SELECTED_IDS";

	private CADbEngine mDbEngine;

    private CATabCollectionPagerAdapter mCollectionPagerAdapter;
    private ViewPager mViewPager;

	private CAScanInboxSmsTask mTask;
	private boolean mIsViewCreated = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);

		mDbEngine = new CADbEngine(getApplicationContext());
		int err=openDataBase();
		if(err!=CAError.NO_ERROR)
			CAErrorDisplay.showError(this, err);

		mCollectionPagerAdapter =
                new CATabCollectionPagerAdapter(
                        getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mCollectionPagerAdapter);
        
        
        
        final ActionBar actionBar = getSupportActionBar();
        
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayShowHomeEnabled(false);
		actionBar.setDisplayShowTitleEnabled(false);

        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
			
			@Override
			public void onTabReselected(Tab tab, FragmentTransaction ft) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTabSelected(Tab tab, FragmentTransaction ft) {
				 mViewPager.setCurrentItem(tab.getPosition());	
				 if(mIsViewCreated)
					 setPreference(TAB_POSITION, tab.getPosition());
			}

			@Override
			public void onTabUnselected(Tab tab, FragmentTransaction ft) {
				// TODO Auto-generated method stub
				
			}
        };
        mViewPager.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        getSupportActionBar().setSelectedNavigationItem(position);
                    }
                });
	

        Tab[] tabs = new Tab[3];
        tabs[0] = actionBar.newTab().setText(R.string.black_list_tab).setTabListener(tabListener);
        tabs[1] = actionBar.newTab().setText(R.string.white_list_tab).setTabListener(tabListener);
        tabs[2] = actionBar.newTab().setText(R.string.blocked_sms_list_tab).setTabListener(tabListener);
        
        actionBar.addTab(tabs[0]);
        actionBar.addTab(tabs[1]);
        actionBar.addTab(tabs[2]);
        
    	int tabPos = getPreference(TAB_POSITION, 0);
        Bundle bundle = getIntent().getExtras();
        if(bundle!=null) {
			tabPos = bundle.getInt(KEY_TAB_POSITION, tabPos);
        }
        actionBar.selectTab(tabs[tabPos]);
        
        mIsViewCreated = true;

		try {
			scanInboxSms();
		} catch (CAException e) {
			CAErrorDisplay.showError(this, e);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(mTask!=null)
			mTask.cancel(false);
		mDbEngine.close();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_backup:
			if(mTask==null) {
				Intent intent = new Intent(this, CAFileManagerActivity.class);
				intent.setAction(CAFileManagerFragment.ACTION_PICK_DIRECTORY);
				startActivityForResult(intent, REQUEST_PICK_FOLDER); 
			}
			return true;
		case R.id.action_restore:
			if(mTask==null) {
				Intent intent = new Intent(this, CAFileManagerActivity.class);
				intent.setAction(CAFileManagerFragment.ACTION_PICK_FILE);
				intent.setType("file/database");
				startActivityForResult(intent, REQUEST_PICK_FILE); 
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_PICK_FOLDER && resultCode == Activity.RESULT_OK) {
			ArrayList<String> list = data.getExtras().getStringArrayList(CAFileManagerFragment.EXTRA_RESULT_PATH);
			if(list!=null && list.size()>0) {
				String path = list.get(0)+"/psb.db";
	
				mDbEngine.close();
				if(mDbEngine.backup(path))
					CAErrorDisplay.showText(this, getString(R.string.backup_complete, path));
				else
					CAErrorDisplay.showText(this, getString(R.string.backup_failed, path));

				int err = openDataBase();
				if(err!=CAError.NO_ERROR)
					CAErrorDisplay.showError(this, err);

				updateView();
			}
		}
		else if (requestCode == REQUEST_PICK_FILE && resultCode == Activity.RESULT_OK) {
			ArrayList<String> list = data.getExtras().getStringArrayList(CAFileManagerFragment.EXTRA_RESULT_PATH);
			if(list!=null && list.size()>0) {
				String path = list.get(0);
				mDbEngine.close();
				if(mDbEngine.restore(path)) {
					int err = openDataBase();
					if(err==CAError.NO_ERROR)
						CAErrorDisplay.showText(this, getString(R.string.restore_complete, path));
					else
						CAErrorDisplay.showError(this, err);
				}
				else
					CAErrorDisplay.showText(this, getString(R.string.restore_failed, path));

				updateView();
			}
		}
	}

	private int openDataBase() {
		try {
			mDbEngine.open();
			return CAError.NO_ERROR;
		} catch (CAException e) {
			mDbEngine.clear();
			try {
				mDbEngine.open();
			} catch (CAException e2) {
				e2.printStackTrace();
			}
			return e.getErrorCode();
		}
	}

	@Override
	public CADbEngine getDbEngine() {
		return mDbEngine;
	}

	@Override
	public void showSmsDialog(CAContact contact) {
		try {
			CASms lastsms = mDbEngine.getSmsTable().getLastSms(contact.getAddress());
			DialogFragment dialog = CASmsDialogFragment.newInstance(contact, lastsms);
			dialog.show(getSupportFragmentManager(), SMS_DIALOG);
		}
		catch (CAException e) {
			CAErrorDisplay.showError(this, e);
		}
	}
	
	@Override
	public void showSmsDialog(CASms sms) {
		DialogFragment dialog = CASmsDialogFragment.newInstance(sms);
		dialog.show(getSupportFragmentManager(), SMS_DIALOG);
	}

	@Override
	public void smsDialogMoveToWhiteListButtonClick(CAContact contact) {
		if(contact.getStatus()== TAContactStatus.EBlackList)
			getDbEngine().getContactsTable().moveToWhiteList(contact);
		else
			getDbEngine().getContactsTable().moveToBlackList(contact);
			
		updateView();
	}

	@Override
	public void showDeleteAllSmsDialog() {
		CAAlertDialogFragment dialog = CAAlertDialogFragment.newInstance(getString(R.string.delete_all_question));
		dialog.show(getSupportFragmentManager(), DELETE_ALL_DIALOG);
	}

	@Override
	public void showDeleteSelectedSmsDialog(ArrayList<Integer> selectedIds) {
		Bundle args = new Bundle();
		args.putIntegerArrayList(KEY_SELECTED_IDS, selectedIds);
		CAAlertDialogFragment dialog = CAAlertDialogFragment.newInstance(
			getString(R.string.delete_selected_items_question),
			args);
		dialog.show(getSupportFragmentManager(), DELETE_SELECTED_ITEMS_DIALOG);
	}

	@Override
	public void alertDialogOkButtonClick(String tag, Bundle arguments) {
		if(DELETE_ALL_DIALOG.equals(tag)) {
			getDbEngine().getSmsTable().deleteAll();
			updateView();
		}
		else if(DELETE_SELECTED_ITEMS_DIALOG.equals(tag)) {
			ArrayList<Integer> selectedIds = arguments.getIntegerArrayList(KEY_SELECTED_IDS);
			getDbEngine().getSmsTable().deleteByIds(selectedIds);
			updateView();
		}
	}

	private void updateView() {
		mCollectionPagerAdapter.getItem(1).updateContent();
		mCollectionPagerAdapter.getItem(0).updateContent();
		mCollectionPagerAdapter.getItem(2).updateContent();
	}

	@SuppressWarnings("unchecked")
	private void scanInboxSms() throws CAException {
		ArrayList<CAContact> oldBlackList = new ArrayList<CAContact>();
		mDbEngine.getContactsTable().getBlackList(oldBlackList);

		ArrayList<CAContact> oldWhiteList = new ArrayList<CAContact>();
		mDbEngine.getContactsTable().getWhiteList(oldWhiteList);
		
		CABlackListFragment blackListFragment = (CABlackListFragment) mCollectionPagerAdapter.getItem(0);
		blackListFragment.setListVisible(false);
		
		mTask = new CAScanInboxSmsTask();
		mTask.execute(oldBlackList, oldWhiteList);
	}

	private class CAScanInboxSmsTask extends AsyncTask<ArrayList<CAContact>, Integer, List<CAContact>>
										implements GetMissingBlackListItemsObserver {

		protected List<CAContact> doInBackground(ArrayList<CAContact>... arg) {
			Context context = getApplicationContext();
			ArrayList<CAContact> oldBlackList = arg[0];
			ArrayList<CAContact> oldWhiteList = arg[1];
			Set<String> addresses = CAUtils.getContactsByExistentSms(context, this);
			ArrayList<CASms> blockedSmsList = new ArrayList<CASms>();
			try {
				mDbEngine.getSmsTable().getAll(blockedSmsList);
			} catch (CAException e) {
				e.printStackTrace();
			}
			for(CASms sms: blockedSmsList)
				addresses.add(sms.getAddress());
			List<CAContact> newBlackList = CAUtils.getMissingBlackListItems(context, addresses, oldBlackList, oldWhiteList, this);
			return newBlackList;
		}

		protected void onProgressUpdate(Integer... progress) {
		}

		protected void onPostExecute(List<CAContact> newBlackList) {
			if(newBlackList!=null) {
				for(CAContact contact: newBlackList) {
					mDbEngine.getContactsTable().insert(contact);
				}
	
				CABlackListFragment blackListFragment = (CABlackListFragment) mCollectionPagerAdapter.getItem(0);
				blackListFragment.updateContent();
				blackListFragment.setListVisible(true);
			}
			mTask = null;
		}

	}

	private int getPreference(String name, int defValue) {
		 SharedPreferences pm = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		 int val = pm.getInt(name, defValue);
		 return val;
	}

	private void setPreference(String name, int value) {
		 SharedPreferences pm = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		 Editor editor = pm.edit();
		 editor.putInt(name, value);
		 editor.commit();
	}
}