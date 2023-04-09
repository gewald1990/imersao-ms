package com.fullcycle.imersaoms;

import com.fullcycle.imersaoms.models.PubSubMessage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Sinks;

@SpringBootApplication
public class ImersaoMsApplication {

	public static void main(String[] args) {
		SpringApplication.run(ImersaoMsApplication.class, args);
	}

	//O Sink -> do ingles significa pia, mas no nosso contexto seria uma torneira,
	//esse cara eh o q eu ou usar para publicas mensagens, ele eh o meu criador/producer
	@Bean
	public Sinks.Many<PubSubMessage> sink(){
		return Sinks.many().multicast()
				//pode enviar e receber varios de uma unica vez...
				//quando eu nao conseguir entregar.. quando o consultar estvier lento, ou seja..
				// "me da 20, me da 20". mas eu estou produzendo de 100 em 100, ele vai bufferizar pra mim e colocar em uma fila
				//pra nao matar a outra maquina
				.onBackpressureBuffer(100);

	}
}
