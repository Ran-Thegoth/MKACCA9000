package rs.fncore2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rs.fncore.Const;
import rs.fncore.data.Document;
import rs.fncore.data.Tag;
import rs.fncore.data.TemplateProcessor;
import rs.log.Logger;

public class PrintHelper {

	public static final String NO_DATE = "-";
	private static final String INCLUDE_TAG = "@include ";
	private static final String BASE_TAG = "base";

	private static Pattern P = Pattern.compile("\\$([^ \t\\$]+)\\$");

	private PrintHelper() {
	}

	public static String processTemplate(String s, TemplateProcessor p) {
		try {
			Matcher m = P.matcher(s);
			while (m.find()) {
				String key = m.group(1);
				if (key == null)
					continue;

				String value = p.onKey(key);
				if (value == null) {
					if (p instanceof Document)
						value = getCommonField((Document) p, key);
				}
				if (value == null)
					value = Const.EMPTY_STRING;

				key = "$" + key + "$";
				while (s.contains(key)) {
					s = s.replace(key, value);
				}
			}
			return s;
		} catch (Exception | Error e) {
			Logger.e(e, "Ошибка печати");
			return Const.EMPTY_STRING;
		}
	}

	private static final String TAG_NAME = "T_";

	public static String getCommonField(Document doc, String key) {
		if (key.startsWith(TAG_NAME)) {
			key = key.replace(TAG_NAME, "");
			try {
				String v[] = key.split("\\.");
				Tag tag = doc.getTag(Integer.parseInt(v[0]));
				for (int i = 1; i < v.length; i++) {
					if (tag == null)
						break;
					tag = tag.getTag(Integer.parseInt(v[i]));
				}

				if (tag != null)
					return tag.asString();
				return Const.EMPTY_STRING;

			} catch (Exception e) {
				return Const.EMPTY_STRING;
			}
		}
		return Const.EMPTY_STRING;

	}

	public static String loadTemplate(String template, String base) {
		if ((template == null || template.isEmpty()) && base != null)
			template = FNCore.getInstance().getTemplate(base);

		else if (template != null)
			try {
				ByteArrayInputStream is = new ByteArrayInputStream(template.getBytes());
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				LineNumberReader reader = new LineNumberReader(new InputStreamReader(is));
				String s;

				while ((s = reader.readLine()) != null) {
					if (s.startsWith(INCLUDE_TAG)) {
						s = s.substring(INCLUDE_TAG.length()).trim();
						if (BASE_TAG.equals(s))
							os.write(loadTemplate(null, base).getBytes());
						else
							try {
								File f = new File(s);
								if (f.exists() && f.isFile()) {
									FileInputStream fis = new FileInputStream(f);
									byte[] bb = new byte[fis.available()];
									fis.read(bb);
									fis.close();
									os.write(bb);
								}
							} catch (IOException ioe) {
								Logger.e(ioe, "loadTemplate include exc");
							}

					} else
						os.write((s + "\n").getBytes());
				}
				reader.close();
				return new String(os.toByteArray());

			} catch (IOException ioe) {
				Logger.e(ioe, "loadTemplate exc");
			}

		return template;
	}

}
