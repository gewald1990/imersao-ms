package com.fullcycle.imersaoms.controllers;

import com.fullcycle.imersaoms.models.NewPaymentInput;
import com.fullcycle.imersaoms.models.Payment;
import com.fullcycle.imersaoms.publisher.PaymentPublisher;
import com.fullcycle.imersaoms.repositories.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.awt.*;
import java.time.Duration;

@RestController
@RequestMapping(value = "payments")
//O requiredArgsContructor, ele vai ver todas as propriedades final no meu objeto,
// e vai criar um construtor com essas propriedades, e so de ter um construtor com essas propriedades
// o string ja sabe que ele precisa usar esse construtor pra injetar as propriedades, ou seja agente faz
//injecao via construtor que eh bem mais seguro, e eh uma boa pratica inclusive
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final PaymentPublisher paymentPublisher;

    //sobre erros, ver minuto 1:38
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Payment> createPayment(@RequestBody final NewPaymentInput input){
        final String userId = input.getUserId();
        log.info("Payment to be processed {}", userId);
        return this.paymentRepository.createPayment(userId)
                /*
                * Uma observacao importante
                * Quando estamos trabalhando com essas bibliotecas mais funcionais,
                * principalmente "monadas" que sao esses comportamentos de flatMap, map
                * nos temos dois principais, que sao o flatMap e o map
                * O map -> ele pega o objeto e retorna outro objeto .map(payment -> new PubSubMessage())
                * O flatMap -> ele recebe um objeto e ele retorna uma outra cadeia inteira... ele retorna um Mono
                * eh a mesa ideia do Optional, ele permite desaclopar a minha inscricao, e devolver um outro cara assincrono
                *
                * */
                //no onPaymentCreate() basicamente eu estou me inscrevendo no evento
                //eu estou chamando uma outra cadeia de monos, eh uma outra fonteObservadora
                //que eu ou falar pra minha pipeline observar,
                //nessa
                .flatMap(payment -> this.paymentPublisher.onPaymentCreate(payment))
                .flatMap(payment ->
                {
                    //eu estou criando um laco de repeticao (um pooling), para ficar indo no banco de dados
                    //buscando esse meu payment e so vou passar esse evento pra frente quando o meu payment estiver aprovado
                    //quando passar o primeiro evento eh esse cara que eu quero usar e esta suficiente pra mim

                    Mono<Payment> mono = Flux.interval(Duration.ofSeconds(1))
                            .doOnNext(it -> log.info("Next tick {}",it))
                            .flatMap(tick -> this.paymentRepository.getPayment(userId))
                            .filter(it -> Payment.PaymentStatus.APPROVED == it.getStatus())
                            .next();
                    return mono;
                })
                .doOnNext(next -> log.info("Payment processed {}",userId))
                .timeout(Duration.ofSeconds(20))
                .retryWhen(
                        Retry.backoff(2,Duration.ofSeconds(1))
                                .doAfterRetry(signal -> log.info("Execution failed"))
                );
    }
}
