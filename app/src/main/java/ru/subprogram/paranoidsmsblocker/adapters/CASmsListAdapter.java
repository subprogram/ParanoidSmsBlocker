package ru.subprogram.paranoidsmsblocker.adapters;

import java.text.DateFormat;
import java.util.LinkedList;
import java.util.List;

import android.widget.ProgressBar;
import ru.subprogram.paranoidsmsblocker.R;
import ru.subprogram.paranoidsmsblocker.database.entities.CASms;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class CASmsListAdapter extends BaseAdapter {

	static class ViewHolder {
    	TextView address;
    	TextView text;
    	TextView date;
    }

	private final Context mContext;
	private final IASmsListAdapterObserver mObserver;
	private boolean mIsMoreDataExist = true;

	private List<CASms> mList;
	private final List<Integer> mSelectedItems = new LinkedList<Integer>();

	public CASmsListAdapter(Context context, IASmsListAdapterObserver observer) {
		super();
		mContext = context;
		mObserver = observer;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
        if(position >= getCount()) return null;
		ViewHolder holder;
		if(position == mList.size()-1 && mIsMoreDataExist) {
			mObserver.loadMore();
			return new ProgressBar(mContext);
		}

    	if(convertView==null || convertView.getTag()==null) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.smslist_row_item, parent, false);
            holder = new ViewHolder();
            holder.address = (TextView) convertView.findViewById(R.id.address);
            holder.text = (TextView) convertView.findViewById(R.id.text);
            holder.date = (TextView) convertView.findViewById(R.id.date);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

    	final CASms sms = getItem(position);
    	if(sms!=null) {
    		holder.address.setText(sms.getAddress());
    		holder.text.setText(sms.getText());

    		DateFormat df = DateFormat.getDateInstance();
    		holder.date.setText(df.format(sms.getDate()));
    		//SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", mContext.get);
    		
    	}

		if (isSelected(position)) {
			selectView(holder, convertView);
		} else {
			restoreView(holder, convertView);
		}

		return convertView;
	}

	private void restoreView(ViewHolder holder, View convertView) {
		convertView.setEnabled(true);
		convertView.setBackgroundResource(0);

		//holder.address.setTextColor(mBlueColor);
	}

	private void selectView(ViewHolder holder, View convertView) {
		convertView.setEnabled(true);
		convertView
			.setBackgroundResource(R.color.item_background_selected);

	}

	public void setList(List<CASms> list) {
		mList = list;
		mSelectedItems.clear();
		mIsMoreDataExist = true;
	}

	public void selectItem(int position) {
		int item = mSelectedItems.indexOf(position);
		if(item>=0)
			mSelectedItems.remove(item);
		else
			mSelectedItems.add(position);
		notifyDataSetInvalidated();
	}

	public List<Integer> getSelectedItems() {
		return mSelectedItems;
	}

	public void clearSelection() {
		mSelectedItems.clear();
		notifyDataSetChanged();
	}

	private boolean isSelected(int position) {
		return mSelectedItems.contains(position);
	}

	public void updateSelection(List<Integer> selectedPositions) {
		mSelectedItems.clear();
		mSelectedItems.addAll(selectedPositions);
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return mList==null ? 0 : mList.size();
	}

	@Override
	public CASms getItem(int position) {
		return mList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return mList.get(position).getId();
	}

	public void addAll(List<CASms> list) {
		if(list.size()>0)
			mList.addAll(list);
		else
			mIsMoreDataExist = false;
	}

}
