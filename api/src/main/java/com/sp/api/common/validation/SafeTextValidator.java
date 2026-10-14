package com.sp.api.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class SafeTextValidator implements ConstraintValidator<SafeText, CharSequence> {

    /**
     * 여는 태그·닫는 태그·주석·처리 명령(&lt;?xml)·선언(&lt;!DOCTYPE).
     * 브라우저는 "&lt;" 바로 뒤에 글자가 와야 태그로 읽는다. 그래서 "&lt;3", "1 &lt; 2 &gt; 0" 처럼
     * 부등호 뒤가 숫자나 공백이면 태그가 아니라서 통과한다.
     *
     * "javascript:" 같은 스킴은 여기서 보지 않는다. 일반 글에 쓸 수 있는 말이고(채팅은 링크로 만들지 않는다),
     * 실제로 실행될 수 있는 주소 칸은 {@link SafeUrl} 이 막는다.
     */
    private static final Pattern MARKUP = Pattern.compile("</?[a-zA-Z!?][^>]*>", Pattern.DOTALL);

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {

        if (value == null) {
            return true;
        }

        String text = value.toString();

        return !hasForbiddenCharacter(text) && !MARKUP.matcher(text).find();
    }

    /**
     * 줄바꿈·탭을 뺀 제어 문자, 그리고 글 방향을 뒤집어 속이는 문자(우→좌 덮어쓰기 등).
     * 이모지를 이어 붙이는 ZWJ(U+200D)와 변형 선택자는 정상이라 막지 않는다.
     */
    static boolean hasForbiddenCharacter(String text) {

        for (int i = 0; i < text.length(); i++) {

            char c = text.charAt(i);

            if (c == '\n' || c == '\r' || c == '\t') {
                continue;
            }

            if (c < 0x20 || c == 0x7F || (c >= 0x80 && c <= 0x9F)) {
                return true;
            }

            // U+202A~202E 방향 덮어쓰기, U+2066~2069 방향 격리, U+2028/2029 줄·문단 구분자
            if ((c >= 0x202A && c <= 0x202E) || (c >= 0x2066 && c <= 0x2069)
                    || c == 0x2028 || c == 0x2029) {
                return true;
            }
        }

        return false;
    }
}
