package ru.subprogram.paranoidsmsblocker.adapters;

import java.util.List;

import ru.subprogram.paranoidsmsblocker.R;
import ru.subprogram.paranoidsmsblocker.database.entities.CAContact;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class CAContactListAdapter extends BaseAdapter {

    static class ViewHolder {
    	TextView address;
    	//TextView text;
    }

	private final Context mContext;
	private List<CAContact> mList;

	public CAContactListAdapter(Context context) {
		super();
		mContext = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
        if(position >= getCount()) return null;
    	View v = convertView;
    	if(v==null) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.contactlist_row_item, parent, false);
            ViewHolder holder = new ViewHolder();
            holder.address = (TextView) v.findViewById(R.id.address); 
            //holder.text = (TextView) v.findViewById(R.id.text); 
        	v.setTag(holder);
    	}
        
        
    	final CAContact contact = getItem(position);
    	if(contact!=null) {
    		ViewHolder holder = (ViewHolder) v.getTag();
    		
    		holder.address.setText(String.valueOf(position+1)+". "+contact.getAddress());
    		//holder.text.setText(String.valueOf(position+1)+". "+contact.getAddress());

    	}
        return v;
	}

	public void setList(List<CAContact> list) {
		mList = list;
	}

	@Override
	public int getCount() {
		return mList==null ? 0 : mList.size();
	}

	@Override
	public CAContact getItem(int position) {
		return mList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return mList.get(position).getId();
	}
}
