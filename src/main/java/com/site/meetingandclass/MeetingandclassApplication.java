package com.site.meetingandclass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MeetingandclassApplication {

	public static void main(String[] args) {
		SpringApplication.run(MeetingandclassApplication.class, args);
	}

}
