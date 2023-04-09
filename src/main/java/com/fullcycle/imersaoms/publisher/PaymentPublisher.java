package com.fullcycle.imersaoms.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullcycle.imersaoms.models.Payment;
import com.fullcycle.imersaoms.models.PubSubMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

@Component
@RequiredArgsConstructor
public class PaymentPublisher {
    private final Sinks.Many<PubSubMessage> sink;

    private final ObjectMapper mapper;

    public Mono<Payment> onPaymentCreate(final Payment payment){
        //eu preciso gerar esse PubSubMessage...e como eu vou gerar esse PubSubMessage?
       return Mono.fromCallable(() ->{
            final String userId = payment.getUserId();
            final String data = mapper.writeValueAsString(payment);
            return new PubSubMessage(userId,data);
        }).subscribeOn(Schedulers.parallel())
                .doOnNext(next -> this.sink.tryEmitNext(next))
               .thenReturn(payment);

       //Repassando o que aconteceu aqui em cima...
        /*
        nessa parte eu estou gerando uma noa instancia do  PubSubMessage...
        como envolve muito cpu, eu nao quero bloquear minha thread com cpu, eu mando ele ir pra thread parallel
        ou seja, vai para o scheduler especifico para trabalhar com cpu
        * return Mono.fromCallable(() ->{
            final String userId = payment.getUserId();
            final String data = mapper.writeValueAsString(payment);
            return new PubSubMessage(userId,data);
        }).subscribeOn(Schedulers.parallel())
        // depois disso quando eu tiver um proximo evento,
        //eu ou passar ele pro sink, ou seja, eu vou dar pra minha torneira "Oh torneira, abre e emit esse cara"
        //entao ele vai emitir esse next pra mim
        .doOnNext(next -> this.sink.tryEmitNext(next))
        e entao ele vai retornar, esse threnReturn eh o seguinte...
        "eu nao me importo com o valor anterior da pipeline, que no caso eh o PubSub"
        eu vou retornar um Mono de payment... se entrarmos no metodo thenReturn, nos iremos perceber que ele retorna um Mono
        .thenReturn(payment);
        *
        *
        *
        * */

    }
}
