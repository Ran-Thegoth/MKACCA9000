package rs.fncore.data;

import android.os.Parcel;
import android.os.Parcelable;

public enum PrintFormTypeE  implements Parcelable {
	Registration,
	Shift,
	BillHeader,
	BillItem,
	BillFooter,
	Report,
	CorrectionHeader,
	CorrectionFooter,
	Cash,
	Counters,
	Archive;
	

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel p, int arg1) {
		p.writeInt(ordinal());
		
	}
	
	public static final Parcelable.Creator<PrintFormTypeE> CREATOR = new Parcelable.Creator<PrintFormTypeE>() {

		@Override
		public PrintFormTypeE createFromParcel(Parcel p) {
			return PrintFormTypeE.values()[p.readInt()];
		}

		@Override
		public PrintFormTypeE[] newArray(int size) {
			// TODO Auto-generated method stub
			return new PrintFormTypeE[size];
		}
		
	};


}
