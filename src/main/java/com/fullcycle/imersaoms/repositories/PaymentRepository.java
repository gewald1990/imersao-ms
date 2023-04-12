package com.fullcycle.imersaoms.repositories;

import com.fullcycle.imersaoms.models.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentRepository {



    //Podemos substituir pelo nosso scheduler, onde esta chamando
    //.subscribeOn(Schedulers.boundedElastic()), no projeto
    private static final ThreadFactory THREAD_FACTORY = new CustomizableThreadFactory("database-");

    private  static final Scheduler DB_SCHEDULER = Schedulers.fromExecutor(Executors.newFixedThreadPool(8,THREAD_FACTORY));


    private final Database database;

    //Tipos de schedulers
    static {
        //O scheduler eh uma abstracao de um pool de threads,
        //ele eh um cara que vai trabalhar nas coisas
        Schedulers.parallel(); // feito especificamente para tarefas que mexem com cpu
        Schedulers.boundedElastic();// especifcamente um schedular feito para IO (ir pra banco, ir pra disco)
        Schedulers.immediate();
        Schedulers.single();
    }

    //Mono eh uma promessa, ou seja uma fonte observavel
    public Mono<Payment> createPayment(final String userId) {
        final Payment payment = Payment.builder().
                id(UUID.randomUUID().toString())
                .userId(userId).
                status(Payment.PaymentStatus.PENDING).build();

        return Mono.fromCallable(() -> {
                    log.info("saving payment transaction for user {}", userId);
                    return this.database.save(userId, payment);
                })//quem se sobrescrever nessa minha fonte observavel, vai ler o valor de um outro lugar
                //que lugar eh esse?
                //quando vc for fazer uma tarefa, faz dentro de outra thread, que seria o boudedElastic
                //eu ainda nao estou dando o subscribe, eu apenas estou dizendo para qual threat ele vai
                //se eu nao tivesse especificado a thread aqui, eu estaria nas threads do eventloop do netty
                .subscribeOn(Schedulers.boundedElastic())
                //quando vier um evento, ou seja, proximo evento evento, eu quero logar de novo
                .doOnNext(next -> log.info("payment receied {} ", payment.getUserId()));
    }

    public Mono<Payment> getPayment(final String userId) {
        /*
        * Qual a diferenca entre o Mono.fromCallable e o Mono.defer ?
        * O Mono.fromCallable ele recebe uma interface que eh uma interface callable
        * que eh uma interface java que eh uma funcao public interface Callable<V> {V call() throws Exception;}
        * public static <T> Mono<T> fromCallable(Callable<? extends T> supplier) {
		return onAssembly(new MonoCallable<>(supplier));
	}
        * que ao ser chamado ele retorna um valor
        * O defer, ele recebe um Supplier que eh uma interface parecida com o callable, que quando eh executado retorna um valor
        * mas no caso do defer ele recebe um Mono
        * public static <T> Mono<T> defer(Supplier<? extends Mono<? extends T>> supplier) {
		return onAssembly(new MonoDefer<>(supplier));
	}
        *
        *
        * */


        return Mono.defer(() -> {
                    log.info("Getting payment from database - {}", userId);
                    final Optional<Payment> payment = this.database.get(userId, Payment.class);
                    return Mono.justOrEmpty(payment);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(it -> log.info("Payment received - {}", userId));

    }

    public Mono<Payment> processPayment(final String key, final Payment.PaymentStatus status) {
        return getPayment(key)
                .flatMap(payment -> Mono.fromCallable(() -> {
                    log.info("Processing payment {} to status {}", key, status);
                        return this.database.save(key, payment.withStatus(status));
                    }).subscribeOn(Schedulers.boundedElastic())
                );
    }
}
