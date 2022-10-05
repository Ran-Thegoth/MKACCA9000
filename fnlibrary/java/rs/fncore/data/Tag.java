package rs.fncore.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import rs.fncore.Const;
import rs.fncore.FZ54Tag;
import rs.utils.Utils;

/**
 * Основной элемент Tag, может быть как с сабтагами так и без
 *
 * @author amv
 */
public class Tag extends BaseTag {
    private static final String TAGS_TAG = "tags";
    private Object mAttachment;
    private final List<Tag> mChilds = new ArrayList<>();

    public Tag() {
        super();
    }

    public Tag(int tagId) {
        super();
        mTagId=tagId;
    }

    public Tag(Parcel p) {
        this();
        readFromParcel(p);
    }
    public Tag(ByteBuffer bb) {
    	this();
    	mTagId = bb.getShort();
    	byte [] buf = new byte[bb.getShort()];
    	bb.get(buf);
    	mData.put(buf);
    }
    public Tag(int tagId, byte value) {
        super(tagId, value);
    }
    public Tag(int tagId, boolean value) {
        super(tagId, value);
    }
    public Tag(int tagId, short value) {
        super(tagId, value);
    }
    public Tag(int tagId, int value) {
        super(tagId, value);
    }
    public Tag(int tagId, long value) {
        super(tagId, value);
    }
    public Tag(int tagId, String value) {
        super(tagId, value);
    }
    public Tag(int tagId, BigDecimal value, int digits) {
        super(tagId, value,digits);
    }
    public Tag(int tagId, Date value) {
        super(tagId, value);
    }
    public Tag(int tagId, byte[] value) {
        super(tagId, value);
    }
    public Tag(int tagId, ByteBuffer bb) {
        super(tagId, bb);
    }

    public void unpackSTLV() {
    	if(!getChilds().isEmpty()) return;
    	if(mData.position() < 4) return;
    	int p = mData.position();
    	mData.position(0);
    	try {
    		while(mData.position() < p) {
                int currTagId = Utils.readUint16LE(mData);
                int size = Utils.readUint16LE(mData);
                if (p - mData.position() < size)
                    break;
                byte[] raw = new byte[size];
                mData.get(raw);
                getChilds().add(new Tag(currTagId, raw));
    		}
    	} catch(Exception e) {
    	} finally {
    		mData.position(p);
    	}
    }
    
    public Tag(Tag [] tags) {
        super();
        mTagType = DataTypeE.TLV;
        for (Tag tag : tags) {
            addChildInternal(tag);
        }
    }

    public Tag(List<Tag> tags) {
        super();
        mTagType = DataTypeE.TLV;
        for (Tag tag : tags) {
            addChildInternal(tag);
        }
    }

    public byte[] pack(boolean excludeNonFnTags) {
        packChilds(excludeNonFnTags);
        if (excludeNonFnTags && (mTagId < 0 || mTagId>Short.MAX_VALUE)) {
            return new byte[0];
        }
        return super.pack(true, excludeNonFnTags);
    }

    public byte[] packToTlvList() {
        packChilds(true);
        return super.pack(false, true);
    }

    public byte[] packToTlvList(int [] requestedTags) {
        Tag res = new Tag();
        List<Tag> childs = getChilds(requestedTags);
        res.add(childs);
        return res.packToTlvList();
    }

    private void packChilds(boolean excludeNonFnTags){
        if (mTagType != DataTypeE.TLV) return;

        mData.clear();
        for (Tag tag : mChilds) {
            if (excludeNonFnTags && (tag.mTagId < 0 || tag.mTagId>Short.MAX_VALUE)) {
                continue;
            }
            mData.put(tag.pack(excludeNonFnTags));
        }
    }

    private void unPackChilds(ByteBuffer bb){
        if (mTagType != DataTypeE.TLV) return;
        addAll(bb);
    }

    public void readFromTLV(ByteBuffer bb) {
    	mTagType = DataTypeE.TLV;
        while (bb.position() < bb.limit()) {
            int tagId = Utils.readUint16LE(bb);
            getChilds().add(new Tag(tagId, bb));
        }
    	
    }
    public void unpackFromTlvList(ByteBuffer bb){
        mTagType = DataTypeE.TLV;
        unPackChilds(bb);
        if (bb.position()<=bb.limit()-2) {
            int tagId = Utils.readUint16LE(bb);
            mTagId = tagId;
        }
    }

    public Tag getTag(int tagId) {
        if (mTagType == DataTypeE.TLV){
            for (Tag tag : mChilds){
                if (tag.mTagId == tagId) return tag;
            }
            return null;
        }

        if (mTagType != DataTypeE.R) {
            return null;
        }
        if (mData.position() < 5) {
            return null;
        }
        int p = mData.position();
        try {
            mData.position(0);
            while (mData.position() < p) {
                int currTagId = Utils.readUint16LE(mData);
                int size = Utils.readUint16LE(mData);
                if (p - mData.position() < size)
                    return null;
                byte[] raw = new byte[size];
                mData.get(raw);
                if (currTagId == tagId) {
                    return new Tag(tagId, raw);
                }
            }
            return null;
        } finally {
            mData.position(p);
        }
    }

    public List<Tag> getChilds(int [] requestedTags){
        List<Tag> res = new ArrayList<>();
        for (Tag child: mChilds){
            if (Utils.contains(requestedTags, child.mTagId)){
                res.add(child);
            }
        }
        return res;
    }

    public String getTagString(int tagId){
        Tag data = getTag(tagId);
        if (data == null) return Const.EMPTY_STRING;
        return data.asString();
    }

    public Tag setRootTag(){
        mTagId=ROOT_TAG;
        return this;
    }

    private Tag setContainer(){
        mTagType = DataTypeE.TLV;
        mData.clear();
        return this;
    }

    public Boolean isContainer(){
        return  mTagType == DataTypeE.TLV;
    }

    private void addChildInternal(Tag tag){
        setContainer();
        mChilds.add(tag);
    }

    public int getId(){
        return mTagId;
    }

    public List<Tag> getChilds(){
        return mChilds;
    }

    public Tag getTagByIndex(int index) {
        return mChilds.get(index);
    }

    public int count(){
        return mChilds.size();
    }

    public void remove(int tagId){
        Iterator<Tag> i = mChilds.iterator();
        while (i.hasNext()) {
            Tag tag = i.next();
            if (tag.mTagId == tagId) {
                i.remove();
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel p, int flags) {
        super.writeToParcel(p, flags);

        p.writeInt(mChilds.size());
        for (Tag tag: mChilds){
            tag.writeToParcel(p, flags);
        }
    }

    @Override
    public void readFromParcel(Parcel p) {
        super.readFromParcel(p);

        mChilds.clear();
        int size = p.readInt();
        while (size-- > 0) {
            addChildInternal(new Tag(p));
        }
    }

    public boolean hasTag(int tagId) {
        for (Tag tag : mChilds){
            if (tag.mTagId == tagId) return true;
        }
        return false;
    }


    //region Добавление тегов
    public Tag add(int tagId, byte value) {
        remove(tagId);
        Tag res= new Tag(tagId, value);
        addChildInternal(res);
        return res;
    }

    public Tag add(int tagId, boolean value) {
        remove(tagId);
        Tag res= new Tag(tagId, value);
        addChildInternal(res);
        return res;
    }

    public Tag add(int tagId, short value) {
        remove(tagId);
        Tag res= new Tag(tagId, value);
        addChildInternal(res);
        return res;
    }

    public Tag add(int tagId, int value) {
        remove(tagId);
        Tag res= new Tag(tagId, value);
        addChildInternal(res);
        return res;
    }

    public Tag add(int tagId, long value) {
        remove(tagId);
        Tag res= new Tag(tagId, value);
        addChildInternal(res);
        return res;
    }

    public Tag add(int tagId, BigDecimal value) {
        remove(tagId);
        Tag res= new Tag(tagId, value,2);
        addChildInternal(res);
        return res;
    }

    public Tag add(int tagId, BigDecimal value, int digits) {
        remove(tagId);
        Tag res= new Tag(tagId, value,digits);
        addChildInternal(res);
        return res;
    }

    public Tag add(int tagId, String value) {
        remove(tagId);
        Tag res= new Tag(tagId, value);
        addChildInternal(res);
        return res;
    }

    public Tag add(int tagId, byte[] value) {
        remove(tagId);
        Tag res= new Tag(tagId, value);
        addChildInternal(res);
        return res;
    }

    public Tag add(Tag tag) {
        if (tag==null) return tag;
        remove(tag.mTagId);
        addChildInternal(tag);
        return tag;
    }

    public void add(List<Tag> tags) {
        if (tags==null || tags==mChilds) return;
        for (Tag tag:tags){
            add(tag);
        }
    }

    public void addAll(byte[] bb) {
        ByteBuffer buf = ByteBuffer.wrap(bb);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        addAll(buf);
    }

    public void addAll(ByteBuffer bb) {
        while (bb.position() < bb.limit()) {
            int tagId = Utils.readUint16LE(bb);
            Log.d("fncore2", "Adding tag "+tagId);
            add(new Tag(tagId, bb));
        }
    }
    //endregion

    public void dump() {
    	dump("");
    }
    public String asHex() {
    	return Utils.dump(mData.array(), 0, mData.position());
    }
    public void dump(String pad) {
    	String s = "";
    	if(mTagId > 0) {
    		s+="["+mTagId+"]";
    		if(mData.position() > 0) {
    			s+= " ("+mData.position()+")";
    			s+=" " +Utils.dump(mData.array(), 0, mData.position());
    					
    		}
    	}
    	if(!s.isEmpty())
    		Log.d("TAG", pad+s);
    	for(Tag t : mChilds) {
    		t.dump(pad+" ");
    	}
    }
    //region JSON
    public JSONObject toJSON() throws JSONException {
        JSONObject result = super.toJSON();
        JSONArray tags = new JSONArray();

        for (Tag tag : mChilds){
            JSONObject jTag = tag.toJSON();
            tags.put(jTag);
        }
        result.put(TAGS_TAG, tags);
        return result;
    }

    public Tag(JSONObject json) throws JSONException {
        super(json);
        if (json.has(TAGS_TAG)) {
            JSONArray a = json.getJSONArray(TAGS_TAG);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                addChildInternal(new Tag(o));
            }
        }
    }
    /**
     * Присоединить объект к тегу. Объект не передается через границы процесса
     * @param o объект
     * @return текущий объект
     */
    public Tag attach(Object o) {
    	mAttachment = o;
    	return this;
    }
    /**
     * Получить присоединенный объект
     * @return присоединенный объект
     */
    public Object attachment() { return mAttachment; }
    
    /**
     * Создать документ из абстрактного тега по полю 1000
     * @return
     */
    public Document createInstance() {
    	if(!hasTag(FZ54Tag.T1000_DOCUMENT_NAME))  {
    		return null;
    	}
    	Document result = null;
    	switch(getTag(FZ54Tag.T1000_DOCUMENT_NAME).asShort()) {
    	case 1:
    	case 11:
    		Log.d("fncore2", "Instancing new KKMInfo");
    		result =  new KKMInfo();
    		break;
    	case 2:
    		result =  new Shift(true);
    		break;
    	case 5:
    		result =  new Shift(false);
    		break;
    	case 3:
    	case 4:
    		result =  new SellOrder();
    		break;
    	case 31:
    	case 41:
    		result = new Correction();
    		break;
    	case 21:
    		result = new FiscalReport();
    		break;
    	case 6:
    		result = new ArchiveReport();
    		break;
    	case 100:
    		result = new FNCounters();
    		break;
    	}
    	if(result != null) result.fromTag(this);
    	return result;
    }

    public static final Parcelable.Creator<Tag> CREATOR = new Parcelable.Creator<Tag>() {

		@Override
		public Tag createFromParcel(Parcel p) {
			return new Tag(p);
		}

		@Override
		public Tag[] newArray(int size) {
			return new Tag[size];
		}
    	
    };

	public long UIntValue(int id) {
		Tag t = getTag(id);
		return t != null ? t.asUInt() : 0;
	}
	public double DoubleValue(int id) {
		Tag t = getTag(id);
		return t != null ? t.asDouble() : 0;
	}
}
