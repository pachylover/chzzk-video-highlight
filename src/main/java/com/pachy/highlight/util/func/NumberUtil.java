package com.pachy.highlight.util.func;

import java.text.DecimalFormat;

public class NumberUtil {
	public static String numberCommaFormat(int number) {
		return new DecimalFormat("###,###,###").format(number);
	}
	public static String numberCommaFormat(String number) {
		if (!StringUtil.isInteger(number)) return number;
		return new DecimalFormat("###,###,###").format(Integer.parseInt(number));
	}
}