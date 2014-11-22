package ru.subprogram.paranoidsmsblocker.fragments;

import java.util.ArrayList;
import java.util.List;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.util.SparseBooleanArray;
import android.view.*;
import ru.subprogram.paranoidsmsblocker.R;
import ru.subprogram.paranoidsmsblocker.activities.utils.MultiSelectionUtil;
import ru.subprogram.paranoidsmsblocker.adapters.CASmsListAdapter;
import ru.subprogram.paranoidsmsblocker.adapters.IASmsListAdapterObserver;
import ru.subprogram.paranoidsmsblocker.database.CADbEngine;
import ru.subprogram.paranoidsmsblocker.database.entities.CASms;
import ru.subprogram.paranoidsmsblocker.exceptions.CAException;
import ru.subprogram.paranoidsmsblocker.smsreceiver.CADefaultSmsReceiver;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class CASmsListFragment extends CAAbstractFragment implements OnItemClickListener, MultiSelectionUtil.MultiChoiceModeListener, IASmsListAdapterObserver {

	private static final int LOAD_PART_SIZE = 20;

	private CASmsListAdapter mAdapter;
	private MultiSelectionUtil.Controller mMultiSelectionController;
	private Bundle mViewDestroyedInstanceState;
	private ListView mListView;

	private final Runnable mLoadMoreRunnable = new Runnable() {
		@Override
		public void run() {
			List<CASms> list = getContent(mAdapter.getCount(), LOAD_PART_SIZE);
			mAdapter.addAll(list);
			mAdapter.notifyDataSetChanged();
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.fragment_sms_list, container, false);
		
		mListView = (ListView)v;
		mListView.setOnItemClickListener(this);
		
		mAdapter = new CASmsListAdapter(getActivity(), this);
		mListView.setAdapter(mAdapter);
		
		NotificationManager notificationManager = (NotificationManager)getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(CADefaultSmsReceiver.NOTIFICATION_ID);

		mMultiSelectionController = MultiSelectionUtil
			.attachMultiSelectionController(
				mListView,
				(ActionBarActivity) getActivity(), this);
		if (savedInstanceState == null && isMenuVisible()) {
			savedInstanceState = mViewDestroyedInstanceState;
		}
		mMultiSelectionController.tryRestoreInstanceState(savedInstanceState);

		return v ;
	}

	@Override
	public void onResume() {
		super.onResume();
		updateContent();
	}

	@Override
	public void onDestroyView() {
		if (mMultiSelectionController != null) {
			mMultiSelectionController.finish();
		}
		mMultiSelectionController = null;
		super.onDestroyView();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mMultiSelectionController != null) {
			mMultiSelectionController.saveInstanceState(outState);
		}
	}

	@Override
	public void setMenuVisibility(boolean menuVisible) {
		super.setMenuVisibility(menuVisible);

		if (mMultiSelectionController == null) {
			return;
		}

		// Hide the action mode when the fragment becomes invisible
		if (!menuVisible) {
			Bundle bundle = new Bundle();
			if (mMultiSelectionController.saveInstanceState(bundle)) {
				mViewDestroyedInstanceState = bundle;
				mMultiSelectionController.finish();
			}

		} else if (mViewDestroyedInstanceState != null) {
			mMultiSelectionController
				.tryRestoreInstanceState(mViewDestroyedInstanceState);
			mViewDestroyedInstanceState = null;
		}
	}

	private List<CASms> getContent(int offset, int count) {
		ArrayList<CASms> list = new ArrayList<CASms>();
		CADbEngine dbEngine = mObserver.getDbEngine();
		try {
			dbEngine.getSmsTable().getAll(list, offset, count);
		} catch (CAException e) {
			e.printStackTrace();
		}
		return list;
	}

	@Override
	public void updateContent() {
		if(mObserver==null) return;
		mAdapter.setList(getContent(0, LOAD_PART_SIZE));
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public void loadMore() {
		mListView.post(mLoadMoreRunnable);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
		CASms sms = mAdapter.getItem(pos);
		mObserver.showSmsDialog(sms);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.sms_list, menu);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		MenuItem deleteAllItem = menu.findItem(R.id.action_delete_all);
		deleteAllItem.setVisible(mAdapter.getCount() > 0);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_delete_all:
				mObserver.showDeleteAllSmsDialog();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
		List<Integer> selectedPositions = getSelectedItemsPositions();
		int numSelected = selectedPositions.size();
		mode.setTitle(getResources().getString(R.string.cab_selected_title, numSelected));

		mAdapter.updateSelection(selectedPositions);
	}

	@Override
	public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
		//mActionMode = mode;
		MenuInflater inflater = actionMode.getMenuInflater();
		inflater.inflate(R.menu.sms_list_item, menu);
		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
		return false;
	}

	private ArrayList<Integer> getSelectedItemsPositions() {
		ArrayList<Integer> checkedPositions = new ArrayList<Integer>();
		SparseBooleanArray checkedPositionsBool = mListView.getCheckedItemPositions();
		for (int i = 0; i < checkedPositionsBool.size(); i++) {
			if (checkedPositionsBool.valueAt(i)) {
				checkedPositions.add(checkedPositionsBool.keyAt(i));
			}
		}
		return checkedPositions;
	}

	private ArrayList<Integer> getSelectedItemsIds() {
		ArrayList<Integer> checkedPositions = new ArrayList<Integer>();
		SparseBooleanArray checkedPositionsBool = mListView.getCheckedItemPositions();
		for (int i = 0; i < checkedPositionsBool.size(); i++) {
			if (checkedPositionsBool.valueAt(i)) {
				checkedPositions.add((int) mAdapter.getItemId(checkedPositionsBool.keyAt(i)));
			}
		}
		return checkedPositions;
	}

	@Override
	public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
		ArrayList<Integer> selectedIds = getSelectedItemsIds();
		actionMode.finish();

		mObserver.showDeleteSelectedSmsDialog(selectedIds);
		return true;
	}

	@Override
	public void onDestroyActionMode(ActionMode actionMode) {
		mAdapter.clearSelection();
	}
}
