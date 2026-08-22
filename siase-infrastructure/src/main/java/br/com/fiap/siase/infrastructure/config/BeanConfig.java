package br.com.fiap.siase.infrastructure.config;

import br.com.fiap.siase.application.usecase.AdicionarPecaUC;
import br.com.fiap.siase.application.usecase.AdicionarServicoUC;
import br.com.fiap.siase.application.usecase.AprovarOrcamentoUC;
import br.com.fiap.siase.application.usecase.AtualizarStatusViaWebhookUC;
import br.com.fiap.siase.application.usecase.AvancarStatusUC;
import br.com.fiap.siase.application.usecase.CancelarOrdemUC;
import br.com.fiap.siase.application.usecase.ConsultarStatusOSUC;
import br.com.fiap.siase.application.usecase.ConsultarTempoMedioUC;
import br.com.fiap.siase.application.usecase.CriarOrdemServicoUC;
import br.com.fiap.siase.application.usecase.FinalizarExecucaoItemUC;
import br.com.fiap.siase.application.usecase.IniciarExecucaoItemUC;
import br.com.fiap.siase.application.usecase.ListarOrdensServicoUC;
import br.com.fiap.siase.application.usecase.PrepararAberturaOSUC;
import br.com.fiap.siase.domain.port.ClienteRepositoryPort;
import br.com.fiap.siase.domain.port.EmailPort;
import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;
import br.com.fiap.siase.domain.port.PecaRepositoryPort;
import br.com.fiap.siase.domain.port.ServicoRepositoryPort;
import br.com.fiap.siase.domain.port.VeiculoRepositoryPort;
import br.com.fiap.siase.application.port.ObservabilityPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {

    @Bean
    public CriarOrdemServicoUC criarOrdemServicoUC(
            OrdemServicoRepositoryPort ordemServicoRepository,
            ClienteRepositoryPort clienteRepository,
            VeiculoRepositoryPort veiculoRepository,
            ServicoRepositoryPort servicoRepository,
            PecaRepositoryPort pecaRepository, ObservabilityPort observability) {
        return new CriarOrdemServicoUC(ordemServicoRepository, clienteRepository, veiculoRepository, servicoRepository, pecaRepository, observability);
    }

    @Bean
    public ListarOrdensServicoUC listarOrdensServicoUC(OrdemServicoRepositoryPort ordemServicoRepository) {
        return new ListarOrdensServicoUC(ordemServicoRepository);
    }

    @Bean
    public ConsultarStatusOSUC consultarStatusOSUC(OrdemServicoRepositoryPort ordemServicoRepository) {
        return new ConsultarStatusOSUC(ordemServicoRepository);
    }

    @Bean
    public AprovarOrcamentoUC aprovarOrcamentoUC(OrdemServicoRepositoryPort ordemServicoRepository, EmailPort emailPort) {
        return new AprovarOrcamentoUC(ordemServicoRepository, emailPort);
    }

    @Bean
    public AtualizarStatusViaWebhookUC atualizarStatusViaWebhookUC(
            OrdemServicoRepositoryPort ordemServicoRepository,
            EmailPort emailPort,
            @Value("${webhook.token}") String webhookToken, ObservabilityPort observability) {
        return new AtualizarStatusViaWebhookUC(ordemServicoRepository, emailPort, webhookToken, observability);
    }

    @Bean
    public AvancarStatusUC avancarStatusUC(OrdemServicoRepositoryPort ordemServicoRepository, ObservabilityPort observability) {
        return new AvancarStatusUC(ordemServicoRepository, observability);
    }

    @Bean
    public CancelarOrdemUC cancelarOrdemUC(OrdemServicoRepositoryPort ordemServicoRepository) {
        return new CancelarOrdemUC(ordemServicoRepository);
    }

    @Bean
    public AdicionarPecaUC adicionarPecaUC(OrdemServicoRepositoryPort ordemServicoRepository, PecaRepositoryPort pecaRepository) {
        return new AdicionarPecaUC(ordemServicoRepository, pecaRepository);
    }

    @Bean
    public AdicionarServicoUC adicionarServicoUC(OrdemServicoRepositoryPort ordemServicoRepository, ServicoRepositoryPort servicoRepository) {
        return new AdicionarServicoUC(ordemServicoRepository, servicoRepository);
    }

    @Bean
    public ConsultarTempoMedioUC consultarTempoMedioUC(OrdemServicoRepositoryPort ordemServicoRepository) {
        return new ConsultarTempoMedioUC(ordemServicoRepository);
    }

    @Bean
    public PrepararAberturaOSUC prepararAberturaOSUC(ClienteRepositoryPort clienteRepository, VeiculoRepositoryPort veiculoRepository) {
        return new PrepararAberturaOSUC(clienteRepository, veiculoRepository);
    }

    @Bean
    public IniciarExecucaoItemUC iniciarExecucaoItemUC(OrdemServicoRepositoryPort ordemServicoRepository, ObservabilityPort observability) {
        return new IniciarExecucaoItemUC(ordemServicoRepository, observability);
    }

    @Bean
    public FinalizarExecucaoItemUC finalizarExecucaoItemUC(OrdemServicoRepositoryPort ordemServicoRepository, ObservabilityPort observability) {
        return new FinalizarExecucaoItemUC(ordemServicoRepository, observability);
    }
}
