package com.sp.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class ApiApplication {

	public static void main(String[] args) {
		// 이 서버가 주는 시간대 없는 시각(LocalDateTime)은 UTC 라고 약속한다. 화면이 그렇게 읽는다.
		// 서버를 돌리는 PC 의 시간대(예: 한국)에 따라 값이 달라지지 않게 여기서 못 박는다.
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

		SpringApplication.run(ApiApplication.class, args);
	}

}
