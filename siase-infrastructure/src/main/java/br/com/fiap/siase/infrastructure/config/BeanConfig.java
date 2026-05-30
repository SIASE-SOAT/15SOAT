package br.com.fiap.siase.infrastructure.config;

import br.com.fiap.siase.application.usecase.AprovarOrcamentoUC;
import br.com.fiap.siase.application.usecase.AtualizarStatusViaWebhookUC;
import br.com.fiap.siase.application.usecase.ConsultarStatusOSUC;
import br.com.fiap.siase.application.usecase.CriarOrdemServicoUC;
import br.com.fiap.siase.application.usecase.ListarOrdensServicoUC;
import br.com.fiap.siase.domain.port.ClienteRepositoryPort;
import br.com.fiap.siase.domain.port.EmailPort;
import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;
import br.com.fiap.siase.domain.port.PecaRepositoryPort;
import br.com.fiap.siase.domain.port.ServicoRepositoryPort;
import br.com.fiap.siase.domain.port.VeiculoRepositoryPort;
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
            PecaRepositoryPort pecaRepository) {
        return new CriarOrdemServicoUC(ordemServicoRepository, clienteRepository, veiculoRepository, servicoRepository, pecaRepository);
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
    public AtualizarStatusViaWebhookUC atualizarStatusViaWebhookUC(OrdemServicoRepositoryPort ordemServicoRepository, EmailPort emailPort) {
        return new AtualizarStatusViaWebhookUC(ordemServicoRepository, emailPort);
    }
}
