package rs.mkacca.ui.widgets;

import rs.mkacca.ui.widgets.GoodList.OnItemLongClickListener;
import rs.utils.GoodCardAdapterHelper;
import rs.utils.GoodCardAdapterHelper.OnObjectClickListener;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;

public class Favorites extends RecyclerView implements OnObjectClickListener {

	private OnItemLongClickListener _l;
	public Favorites(Context context,OnItemLongClickListener l) {
		super(context);
		_l = l;
		setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
		setAdapter(GoodCardAdapterHelper.createFavoriteAdapter(getContext(), this));
	}

	public Favorites(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public Favorites(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public void onObjectClick(Object g) {
		if(_l!=null) _l.onLongClick(g); 
	}

}
