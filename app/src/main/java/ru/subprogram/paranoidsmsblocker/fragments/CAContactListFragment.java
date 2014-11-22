package ru.subprogram.paranoidsmsblocker.fragments;

import java.util.List;

import ru.subprogram.paranoidsmsblocker.R;
import ru.subprogram.paranoidsmsblocker.adapters.CAContactListAdapter;
import ru.subprogram.paranoidsmsblocker.database.entities.CAContact;
import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ProgressBar;

public abstract class CAContactListFragment extends CAAbstractFragment implements OnItemClickListener {

	protected CAContactListAdapter mAdapter;
	private ListView mListView;
	private ProgressBar mProgress;
	private boolean mShouldListVisible = true;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		
		View v = inflater.inflate(R.layout.fragment_contact_list, container, false);
		
		mProgress = (ProgressBar) v.findViewById(R.id.progress);
		mListView = (ListView)v.findViewById(R.id.list);
		mListView.setOnItemClickListener(this);
		
		mAdapter = new CAContactListAdapter(getActivity());
		mListView.setAdapter(mAdapter);
		
		setListVisible(mShouldListVisible);
		return v ;
	}


	public void setListVisible(boolean b) {
		mShouldListVisible = b;
		if(mListView==null || mProgress==null)
			return;
		mListView.setVisibility(b ? View.VISIBLE : View.INVISIBLE);
		mProgress.setVisibility(b ? View.INVISIBLE : View.VISIBLE);
	}

	@Override
	public void onResume() {
		super.onResume();
		updateContent();
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}
	
	protected abstract List<CAContact> getContent();

	@Override
	public void updateContent() {
		if(mObserver==null) return;
		mAdapter.setList(getContent());
		mAdapter.notifyDataSetChanged();
	}
	
}
