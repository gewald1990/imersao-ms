package com.fullcycle.imersaoms.listeners;

import com.fullcycle.imersaoms.models.Payment;
import com.fullcycle.imersaoms.models.PubSubMessage;
import com.fullcycle.imersaoms.repositories.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

@RequiredArgsConstructor
@Component
@Slf4j
public class PaymentListener implements InitializingBean {

    /*
    * Essa interface eh simples que contem um metodo que ele fala para o Spring...
    * "O spring, depois que vc injetar as minhas depencias no bean, vc chama esse metodo"
    * */


    private final Sinks.Many<PubSubMessage> sink;
    private final PaymentRepository paymentRepository;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.sink.asFlux().subscribe(
                next -> {
                    log.info("On next message - {}", next.getKey());
                    this.paymentRepository.processPayment(next.getKey(), Payment.PaymentStatus.APPROVED)
                            .doOnNext(it -> log.info("Payment processed on listener"))
                            .subscribe();
                },
                error -> {
                    log.error("On pub-sub listener observe error ", error);
                },
                () -> {
                    log.info("On pub-sub listener complete");
                }
        );
    }
}
