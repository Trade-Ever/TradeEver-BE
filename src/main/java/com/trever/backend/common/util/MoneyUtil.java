package com.trever.backend.common.util;

import com.ibm.icu.text.RuleBasedNumberFormat;

import java.util.Locale;

public class MoneyUtil {

    public static String toKorean(long amount) {
        RuleBasedNumberFormat rbnf = new RuleBasedNumberFormat(Locale.KOREAN, RuleBasedNumberFormat.SPELLOUT);
        return "일금 " + rbnf.format(amount) + "원정";
    }
}
