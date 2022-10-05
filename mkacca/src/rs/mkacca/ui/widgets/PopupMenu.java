package rs.mkacca.ui.widgets;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.BlendMode;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.view.ActionProvider;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.View;
import android.widget.ArrayAdapter;
import rs.mkacca.Core;

public class PopupMenu implements Menu {

	public class PopupMenuItem implements MenuItem {
		private String _title;
		private int _id;
		private PopupMenuItem(int id, String title) {
			_title = title;
			_id = id;
		}
		private PopupMenuItem(int id, int titleId) {
			_title = Core.getInstance().getString(titleId);
			_id = id;
		}
		@Override
		public boolean collapseActionView() {
			return false;
		}

		@Override
		public boolean expandActionView() {
			return false;
		}

		@Override
		public ActionProvider getActionProvider() {
			return null;
		}

		@Override
		public View getActionView() {
			return null;
		}

		@Override
		public int getAlphabeticModifiers() {
			return 0;
		}

		@Override
		public char getAlphabeticShortcut() {
			return 0;
		}

		@Override
		public CharSequence getContentDescription() {
			return null;
		}

		@Override
		public int getGroupId() {
			return 0;
		}

		@Override
		public Drawable getIcon() {
			return null;
		}

		@Override
		public BlendMode getIconTintBlendMode() {
			return null;
		}

		@Override
		public ColorStateList getIconTintList() {
			return null;
		}

		@Override
		public Mode getIconTintMode() {
			return null;
		}

		@Override
		public Intent getIntent() {
			return null;
		}

		@Override
		public int getItemId() {
			return _id;
		}

		@Override
		public ContextMenuInfo getMenuInfo() {
			return null;
		}

		@Override
		public int getNumericModifiers() {
			return 0;
		}

		@Override
		public char getNumericShortcut() {
			return 0;
		}

		@Override
		public int getOrder() {
			return 0;
		}

		@Override
		public SubMenu getSubMenu() {
			return null;
		}

		@Override
		public CharSequence getTitle() {
			return _title;
		}

		@Override
		public CharSequence getTitleCondensed() {
			return _title;
		}

		@Override
		public CharSequence getTooltipText() {
			return null;
		}

		@Override
		public boolean hasSubMenu() {
			return false;
		}

		@Override
		public boolean isActionViewExpanded() {
			return false;
		}

		@Override
		public boolean isCheckable() {
			return false;
		}

		@Override
		public boolean isChecked() {
			return false;
		}

		@Override
		public boolean isEnabled() {
			return true;
		}

		@Override
		public boolean isVisible() {
			return true;
		}

		@Override
		public MenuItem setActionProvider(ActionProvider arg0) {
			return this;
		}

		@Override
		public MenuItem setActionView(View arg0) {
			return this;
		}

		@Override
		public MenuItem setActionView(int arg0) {
			return this;
		}

		@Override
		public MenuItem setAlphabeticShortcut(char arg0) {
			return this;
		}

		@Override
		public MenuItem setAlphabeticShortcut(char alphaChar, int alphaModifiers) {
			return this;
		}

		@Override
		public MenuItem setCheckable(boolean arg0) {
			return this;
		}

		@Override
		public MenuItem setChecked(boolean arg0) {
			return this;
		}

		@Override
		public MenuItem setContentDescription(CharSequence contentDescription) {
			return this;
		}

		@Override
		public MenuItem setEnabled(boolean arg0) {
			return this;
		}

		@Override
		public MenuItem setIcon(Drawable arg0) {
			return this;
		}

		@Override
		public MenuItem setIcon(int arg0) {
			return this;
		}

		@Override
		public MenuItem setIconTintBlendMode(BlendMode blendMode) {
			return this;
		}

		@Override
		public MenuItem setIconTintList(ColorStateList tint) {
			return this;
		}

		@Override
		public MenuItem setIconTintMode(Mode tintMode) {
			return this;
		}

		@Override
		public MenuItem setIntent(Intent arg0) {
			return this;
		}

		@Override
		public MenuItem setNumericShortcut(char arg0) {
			return this;
		}

		@Override
		public MenuItem setNumericShortcut(char numericChar,
				int numericModifiers) {
			return this;
		}

		@Override
		public MenuItem setOnActionExpandListener(OnActionExpandListener arg0) {
			return this;
		}

		@Override
		public MenuItem setOnMenuItemClickListener(OnMenuItemClickListener arg0) {
			return this;
		}

		@Override
		public MenuItem setShortcut(char arg0, char arg1) {
			return this;
		}

		@Override
		public MenuItem setShortcut(char numericChar, char alphaChar,
				int numericModifiers, int alphaModifiers) {
			return this;
		}

		@Override
		public void setShowAsAction(int arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public MenuItem setShowAsActionFlags(int arg0) {
			return this;
		}

		@Override
		public MenuItem setTitle(CharSequence title) {
			_title = title.toString();
			return this;
		}

		@Override
		public MenuItem setTitle(int id) {
			_title = Core.getInstance().getString(id);
			return this;
		}

		@Override
		public MenuItem setTitleCondensed(CharSequence arg0) {
			return this;
		}

		@Override
		public MenuItem setTooltipText(CharSequence tooltipText) {
			return this;
		}

		@Override
		public MenuItem setVisible(boolean arg0) {
			return this;
		}
		
	}
	
	private List<PopupMenuItem> _items = new ArrayList<>();
	public PopupMenu() {
	}

	@Override
	public MenuItem add(CharSequence title) {
		PopupMenuItem item = new PopupMenuItem(Core.getInstance().nextId(), title.toString());
		_items.add(item);
		return item;
	}

	@Override
	public MenuItem add(int title) {
		PopupMenuItem item = new PopupMenuItem(Core.getInstance().nextId(), title);
		_items.add(item);
		return item;
	}

	@Override
	public MenuItem add(int group, int id, int order, CharSequence title) {
		PopupMenuItem item = new PopupMenuItem(id, title.toString());
		if(order > -1 && order < _items.size())
			_items.add(order,item);
		else 
			_items.add(item);
		return item;
	}

	@Override
	public MenuItem add(int group, int id, int order, int title) {
		PopupMenuItem item = new PopupMenuItem(id, title);
		if(order > -1 && order < _items.size() )
			_items.add(order,item);
		else 
			_items.add(item);
		return item;
	}

	@Override
	public int addIntentOptions(int arg0, int arg1, int arg2,
			ComponentName arg3, Intent[] arg4, Intent arg5, int arg6,
			MenuItem[] arg7) {
		return 0;
	}

	@Override
	public SubMenu addSubMenu(CharSequence arg0) {
		return null;
	}

	@Override
	public SubMenu addSubMenu(int arg0) {
		return null;
	}

	@Override
	public SubMenu addSubMenu(int arg0, int arg1, int arg2, CharSequence arg3) {
		return null;
	}

	@Override
	public SubMenu addSubMenu(int arg0, int arg1, int arg2, int arg3) {
		return null;
	}

	@Override
	public void clear() {
		_items.clear();
		
	}

	@Override
	public void close() {
		
	}

	@Override
	public MenuItem findItem(int id) {
		for(PopupMenuItem i : _items)
			if(i._id == id)
				return i;
		return null;
	}

	@Override
	public MenuItem getItem(int p) {
		return _items.get(p);
	}

	@Override
	public boolean hasVisibleItems() {
		return true;
	}

	@Override
	public boolean isShortcutKey(int arg0, KeyEvent arg1) {
		return false;
	}

	@Override
	public boolean performIdentifierAction(int arg0, int arg1) {
		return false;
	}

	@Override
	public boolean performShortcut(int arg0, KeyEvent arg1, int arg2) {
		return false;
	}

	@Override
	public void removeGroup(int arg0) {
	}

	@Override
	public void removeItem(int id) {
		_items.remove(findItem(id));
	}

	@Override
	public void setGroupCheckable(int arg0, boolean arg1, boolean arg2) {
	}

	@Override
	public void setGroupDividerEnabled(boolean groupDividerEnabled) {
	}

	@Override
	public void setGroupEnabled(int arg0, boolean arg1) {
	}

	@Override
	public void setGroupVisible(int arg0, boolean arg1) {
	}

	@Override
	public void setQwertyMode(boolean arg0) {
	}

	@Override
	public int size() {
		return _items.size();
	}
	public void show(Context ctx, final OnMenuItemClickListener l, String title) {
		if(size() == 0) return;
		AlertDialog.Builder b = new AlertDialog.Builder(ctx);
		if(title != null)
			b.setTitle(title);
		ArrayAdapter<String> a =new ArrayAdapter<String>(ctx, android.R.layout.simple_list_item_1);
		for(int i=0;i<size();i++)
			a.add(getItem(i).getTitle().toString());
		b.setAdapter(a,new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int what) {
				if(l != null)
					l.onMenuItemClick(getItem(what));
			}
			
		});
		b.show();
	}

}
