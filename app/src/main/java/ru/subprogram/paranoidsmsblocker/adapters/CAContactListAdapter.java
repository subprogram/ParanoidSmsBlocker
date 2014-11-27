package ru.subprogram.paranoidsmsblocker.adapters;

import java.util.List;

import android.support.v7.widget.RecyclerView;
import ru.subprogram.paranoidsmsblocker.R;
import ru.subprogram.paranoidsmsblocker.database.entities.CAContact;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class CAContactListAdapter extends RecyclerView.Adapter<CAContactListAdapter.ViewHolder> implements IAOnClickListener {

	public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

		private final IAOnClickListener listener;

		TextView address;

		public ViewHolder(View v, IAOnClickListener listener) {
			super(v);

			this.listener = listener;
			address = (TextView) v.findViewById(R.id.address);

			v.setOnClickListener(this);
		}

		@Override
		public void onClick(View v) {
			listener.onItemClick(v, getPosition());
		}
	}

	private final Context mContext;
	private List<CAContact> mList;
	private IAOnClickListener mListener;

	public CAContactListAdapter(Context context) {
		super();
		mContext = context;
	}

	public void setOnItemClickListener(IAOnClickListener listener) {
		mListener = listener;
	}

	@Override
	public void onItemClick(View view, int pos) {
		if(mListener!=null)
			mListener.onItemClick(view, pos);
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View v = LayoutInflater.from(parent.getContext())
			.inflate(R.layout.contactlist_row_item, parent, false);
		return new ViewHolder(v, this);
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		final CAContact contact = getItem(position);
		holder.address.setText(String.valueOf(position+1)+". "+contact.getAddress());
	}

	public void setList(List<CAContact> list) {
		mList = list;
	}

	@Override
	public int getItemCount() {
		return mList==null ? 0 : mList.size();
	}

	public CAContact getItem(int position) {
		return mList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return mList.get(position).getId();
	}
}
