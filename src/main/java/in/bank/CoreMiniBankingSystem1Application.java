package in.bank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

//In your main app class or a @Configuration class
@SpringBootApplication
@EnableScheduling
@EnableAsync   // ✅ add this
public class CoreMiniBankingSystem1Application {

	public static void main(String[] args) {
		SpringApplication.run(CoreMiniBankingSystem1Application.class, args);
	}

}
