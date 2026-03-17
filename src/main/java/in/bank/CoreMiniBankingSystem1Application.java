package in.bank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class CoreMiniBankingSystem1Application {

	public static void main(String[] args) {
		SpringApplication.run(CoreMiniBankingSystem1Application.class, args);
	}

}
