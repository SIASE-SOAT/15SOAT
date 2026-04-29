package br.com.fiap.siase.repository.testdata;

import br.com.fiap.siase.model.Cliente;
import br.com.fiap.siase.model.OrdemDeServico;
import br.com.fiap.siase.model.Veiculo;
import br.com.fiap.siase.model.enums.StatusOS;
import br.com.fiap.siase.model.enums.TipoPessoa;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrdemDeServicoTestData {

    public static final String NUMERO_PADRAO = "OS-2026-0001";
    public static final String CLIENTE_NOME = "Maria Santos";
    public static final String CLIENTE_CPF = "98765432101";
    public static final String VEICULO_PLACA = "XYZ9876";

    public static Cliente criarCliente() {
        return criarCliente(CLIENTE_NOME, CLIENTE_CPF, "maria@example.com");
    }

    public static Cliente criarCliente(String nome, String cpfCnpj, String email) {
        Cliente cliente = new Cliente();
        cliente.setNome(nome);
        cliente.setDocumento(cpfCnpj);
        cliente.setTipoPessoa(TipoPessoa.PF);
        cliente.setEmail(email);
        cliente.setTelefone("11999999999");
        return cliente;
    }

    public static Veiculo criarVeiculo(Cliente cliente) {
        return criarVeiculo(cliente, VEICULO_PLACA, "Honda", "Civic", 2022);
    }

    public static Veiculo criarVeiculo(Cliente cliente, String placa, String marca, String modelo, int ano) {
        Veiculo veiculo = new Veiculo();
        veiculo.setCliente(cliente);
        veiculo.setPlaca(placa);
        veiculo.setMarca(marca);
        veiculo.setModelo(modelo);
        veiculo.setAno(ano);
        return veiculo;
    }

    public static OrdemDeServico criarOrdemDeServico(Cliente cliente, Veiculo veiculo) {
        return criarOrdemDeServico(cliente, veiculo, NUMERO_PADRAO);
    }

    public static OrdemDeServico criarOrdemDeServico(Cliente cliente, Veiculo veiculo, String numero) {
        OrdemDeServico os = new OrdemDeServico();
        os.setNumero(numero);
        os.setCliente(cliente);
        os.setVeiculo(veiculo);
        os.setStatus(StatusOS.RECEBIDA);
        os.setDataAbertura(LocalDateTime.now());
        os.setTotal(BigDecimal.ZERO);
        os.setTotalServicos(BigDecimal.ZERO);
        os.setTotalPecas(BigDecimal.ZERO);
        return os;
    }

    public static OrdemDeServicoBuilder builder() {
        return new OrdemDeServicoBuilder();
    }

    public static class OrdemDeServicoBuilder {
        private String numero = NUMERO_PADRAO;
        private Cliente cliente;
        private Veiculo veiculo;
        private StatusOS status = StatusOS.RECEBIDA;
        private LocalDateTime dataAbertura = LocalDateTime.now();
        private LocalDateTime dataFechamento;
        private BigDecimal total = BigDecimal.ZERO;
        private BigDecimal totalServicos = BigDecimal.ZERO;
        private BigDecimal totalPecas = BigDecimal.ZERO;
        private String observacoes;

        public OrdemDeServicoBuilder numero(String numero) {
            this.numero = numero;
            return this;
        }

        public OrdemDeServicoBuilder cliente(Cliente cliente) {
            this.cliente = cliente;
            return this;
        }

        public OrdemDeServicoBuilder veiculo(Veiculo veiculo) {
            this.veiculo = veiculo;
            return this;
        }

        public OrdemDeServicoBuilder status(StatusOS status) {
            this.status = status;
            return this;
        }

        public OrdemDeServicoBuilder dataAbertura(LocalDateTime dataAbertura) {
            this.dataAbertura = dataAbertura;
            return this;
        }

        public OrdemDeServicoBuilder dataFechamento(LocalDateTime dataFechamento) {
            this.dataFechamento = dataFechamento;
            return this;
        }

        public OrdemDeServicoBuilder total(BigDecimal total) {
            this.total = total;
            return this;
        }

        public OrdemDeServicoBuilder totalServicos(BigDecimal totalServicos) {
            this.totalServicos = totalServicos;
            return this;
        }

        public OrdemDeServicoBuilder totalPecas(BigDecimal totalPecas) {
            this.totalPecas = totalPecas;
            return this;
        }

        public OrdemDeServicoBuilder observacoes(String observacoes) {
            this.observacoes = observacoes;
            return this;
        }

        public OrdemDeServico build() {
            if (cliente == null || veiculo == null) {
                throw new IllegalStateException("Cliente e Veiculo são obrigatórios");
            }
            
            OrdemDeServico os = new OrdemDeServico();
            os.setNumero(numero);
            os.setCliente(cliente);
            os.setVeiculo(veiculo);
            os.setStatus(status);
            os.setDataAbertura(dataAbertura);
            os.setDataFechamento(dataFechamento);
            os.setTotal(total);
            os.setTotalServicos(totalServicos);
            os.setTotalPecas(totalPecas);
            os.setObservacoes(observacoes);
            return os;
        }
    }
}
