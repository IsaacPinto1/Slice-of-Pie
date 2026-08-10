package com.isaac.sliceofpie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // powers PriceRefreshScheduler's background price refresh
public class SliceofpieApplication {

	public static void main(String[] args) {
		SpringApplication.run(SliceofpieApplication.class, args);
	}

}
