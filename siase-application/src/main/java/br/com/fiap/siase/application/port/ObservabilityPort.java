package br.com.fiap.siase.application.port;

public interface ObservabilityPort {
  void ordemServicoCriada();

  void tempoStatus(String status, long duracaoNanos);

  void itemExecucaoIniciada();

  void tempoExecucaoItem(long duracaoNanos);

  void falhaIntegracao(String integracao);
}
