package rs.mkacca.ui.widgets;

import android.view.View;

public interface ItemCard<T> {
	public void setItem(T item);
	public boolean obtain();
	public View getView();
}
