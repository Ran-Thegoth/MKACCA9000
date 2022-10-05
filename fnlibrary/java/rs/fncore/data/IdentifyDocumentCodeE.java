package rs.fncore.data;

/** 
 * Код документа, удостоверяющего личность
 * @author nick
 *
 */
public enum IdentifyDocumentCodeE {
	NONE(0,"Не указан"),
	RF_PASSPORT(21,"Паспорт гражданина РФ"),
	RF_FOREIGN(22,"Загранпаспорт РФ"),
	RF_TEMPORARY(26,"Временное удостоверение РФ"),
	RF_METRIC(27,"Свидетельство о рождении РФ"),
	RF_OTHER(28,"Иной документ РФ"),
	FR_PASSPORT(31,"Паспорт иностранного гражданина"),
	FR_NOCICITSENSFIP(32,"Документ лица без гражданства"),
	FR_RESIDENTCARD(33,"Вид на жительство"),
	FR_TEMPORARYRESIDENCE(34,"Вид на временное проживание"),
	FR_OTHER(35,"Иной иностранный документ"),
	FR_AWAIT(40,"Документ лица ожидающего гражданство");
	IdentifyDocumentCodeE(int code, String pName) {
		this.code = String.valueOf(code);
		this.pName = pName;
	}
	public final String pName;
	public final String code;
	@Override
	public String toString() {
		return pName;
	}
	public static IdentifyDocumentCodeE fromCode(String b) {
		for(IdentifyDocumentCodeE c : values())
			if(c.code.equals(b)) return c;
		return NONE;
	}
	 
	
}
