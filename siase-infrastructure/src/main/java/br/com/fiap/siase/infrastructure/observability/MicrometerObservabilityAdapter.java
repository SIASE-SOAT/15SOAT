package br.com.fiap.siase.infrastructure.observability;

import br.com.fiap.siase.application.port.ObservabilityPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class MicrometerObservabilityAdapter implements ObservabilityPort {
  private final Counter ordensCriadas;
  private final MeterRegistry registry;

  public MicrometerObservabilityAdapter(MeterRegistry registry) {
    this.registry = registry;
    this.ordensCriadas = Counter.builder("siase.ordens.servico.criadas")
            .description("Volume de ordens de servico criadas")
            .register(registry);
  }

  @Override
  public void ordemServicoCriada() {
    ordensCriadas.increment();
  }

  @Override
  public void tempoStatus(String status, long duracaoNanos) {
    Timer.builder("siase.ordem.servico.tempo.status")
            .description("Tempo que uma ordem permaneceu em cada status")
            .tag("status", status)
            .publishPercentileHistogram()
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry)
            .record(duracaoNanos, TimeUnit.NANOSECONDS);
  }

  @Override
  public void itemExecucaoIniciada() {
    Counter.builder("siase.execucao.item.iniciadas")
            .description("Quantidade de itens de servico cuja execucao foi iniciada")
            .register(registry)
            .increment();
  }

  @Override
  public void tempoExecucaoItem(long duracaoNanos) {
    Timer.builder("siase.execucao.item.tempo")
            .description("Tempo de execucao dos itens de servico")
            .publishPercentileHistogram()
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry)
            .record(duracaoNanos, TimeUnit.NANOSECONDS);
  }

  @Override
  public void falhaIntegracao(String integracao) {
    Counter.builder("siase.falhas.integracao")
            .description("Falhas em integracoes externas")
            .tag("integracao", integracao)
            .register(registry)
            .increment();
  }
}
