import requests
from fpdf import FPDF
import matplotlib.pyplot as plt
import os

# Configurações do SonarQube
SONAR_URL = "http://localhost:9000"
PROJECT_KEY = "br.com.fiap:siase"
SONAR_TOKEN = "sonar-token-1234567890abcdef"  # Substitua pelo seu token real

# Diretório de saída: scripts/target/sonar/
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
OUTPUT_DIR = os.path.join(SCRIPT_DIR, "target", "sonar")
os.makedirs(OUTPUT_DIR, exist_ok=True)

def get_metrics():
    url = f"{SONAR_URL}/api/measures/search"
    params = {
        "projectKeys": PROJECT_KEY,
        "metricKeys": "bugs,vulnerabilities,code_smells,coverage,duplicated_lines_density,ncloc,lines,reliability_rating,security_rating,sqale_rating,alert_status,security_hotspots,classes,files,functions,comment_lines,complexity",
        "branch": "main"
    }
    auth = (SONAR_TOKEN, "") if SONAR_TOKEN else None
    response = requests.get(url, params=params, auth=auth)
    try:
        response.raise_for_status()
    except requests.exceptions.HTTPError as e:
        print(f"Erro ao buscar métricas: {e}\nURL: {response.url}\nResposta: {response.text}")
        raise
    print("Resposta bruta da API SonarQube:\n", response.text)
    return response.json()

def list_all_metrics():
    url = f"{SONAR_URL}/api/metrics/search"
    auth = (SONAR_TOKEN, "") if SONAR_TOKEN else None
    response = requests.get(url, auth=auth)
    try:
        response.raise_for_status()
    except requests.exceptions.HTTPError as e:
        print(f"Erro ao buscar todas as métricas: {e}\nURL: {response.url}\nResposta: {response.text}")
        raise
    print("Todas as métricas disponíveis no SonarQube:\n", response.text)
    return response.json()

def plot_pie_chart(labels, sizes, colors, title, filename):
    plt.figure(figsize=(3, 3))
    plt.pie(sizes, labels=labels, colors=colors, autopct='%1.1f%%', startangle=140, textprops={'fontsize': 10})
    plt.title(title, fontsize=12)
    plt.tight_layout()
    plt.savefig(filename, bbox_inches='tight', transparent=True)
    plt.close()

def generate_pdf(metrics):
    pdf = FPDF()
    pdf.add_page()
    # Título destacado
    pdf.set_font("Arial", 'B', 18)
    pdf.set_text_color(30, 30, 90)
    pdf.cell(0, 16, txt="Relatório SonarQube", ln=True, align="C")
    pdf.ln(2)
    pdf.set_draw_color(100, 100, 180)
    pdf.set_line_width(0.8)
    pdf.line(15, pdf.get_y(), 195, pdf.get_y())
    pdf.ln(8)
    pdf.set_font("Arial", size=12)
    pdf.set_text_color(0, 0, 0)
    measures = metrics.get('measures', [])
    if not measures:
        pdf.cell(0, 10, "Nenhuma métrica encontrada na resposta da API.", ln=True)
    else:
        categorias = {
            'Qualidade': ['alert_status', 'sqale_rating', 'reliability_rating', 'security_rating'],
            'Manutenibilidade': ['code_smells', 'complexity', 'classes', 'functions', 'files', 'comment_lines'],
            'Cobertura e Duplicação': ['coverage', 'duplicated_lines_density'],
            'Segurança': ['security_hotspots', 'vulnerabilities'],
            'Confiabilidade': ['bugs'],
            'Tamanho': ['lines', 'ncloc']
        }
        nomes = {
            'alert_status': 'Quality Gate',
            'sqale_rating': 'Maintainability Rating',
            'reliability_rating': 'Reliability Rating',
            'security_rating': 'Security Rating',
            'code_smells': 'Code Smells',
            'complexity': 'Complexidade',
            'classes': 'Classes',
            'functions': 'Funções',
            'files': 'Arquivos',
            'comment_lines': 'Linhas de Comentário',
            'coverage': 'Cobertura (%)',
            'duplicated_lines_density': 'Duplicação (%)',
            'security_hotspots': 'Security Hotspots',
            'vulnerabilities': 'Vulnerabilidades',
            'bugs': 'Bugs',
            'lines': 'Linhas',
            'ncloc': 'Linhas de Código'
        }
        valores = {m['metric']: m['value'] for m in measures}
        # Destaques de status
        status = valores.get('alert_status', 'N/A')
        pdf.set_font("Arial", style="B", size=13)
        pdf.set_text_color(0, 100, 0) if status == 'OK' else pdf.set_text_color(200, 0, 0)
        pdf.cell(0, 10, f"Quality Gate: {status}", ln=True, align="L")
        pdf.set_text_color(0, 0, 0)
        pdf.ln(2)
        # Ratings
        for rating in ['sqale_rating', 'reliability_rating', 'security_rating']:
            if rating in valores:
                val = valores[rating]
                cor = (0, 150, 0) if val == '1.0' else (200, 120, 0) if val == '2.0' else (200, 0, 0)
                pdf.set_font("Arial", style="B", size=12)
                pdf.set_text_color(*cor)
                pdf.cell(0, 8, f"{nomes[rating]}: {val}", ln=True, align="L")
        pdf.set_text_color(0, 0, 0)
        pdf.ln(3)
        # Tabela de métricas
        pdf.set_font("Arial", style="B", size=12)
        pdf.cell(0, 8, "Resumo das Métricas", ln=True, align="L")
        pdf.set_font("Arial", size=11)
        for categoria, chaves in categorias.items():
            pdf.set_font("Arial", style="B", size=11)
            pdf.cell(0, 8, categoria, ln=True, align="L")
            pdf.set_font("Arial", size=11)
            for chave in chaves:
                if chave in valores and chave not in ['alert_status', 'sqale_rating', 'reliability_rating', 'security_rating']:
                    pdf.cell(0, 8, f"{nomes.get(chave, chave)}: {valores[chave]}", ln=True, align="L")
            pdf.ln(1)
        pdf.ln(2)
        # Gráficos centralizados
        # Cobertura
        if 'coverage' in valores:
            try:
                cov = float(valores['coverage'])
                cov_chart = os.path.join(OUTPUT_DIR, 'coverage_pie.png')
                plot_pie_chart(['Coberto', 'Não Coberto'], [cov, 100-cov], ['#4CAF50', '#F44336'], 'Cobertura de Testes', cov_chart)
                x_center = (210 - 50) / 2
                pdf.image(cov_chart, x=x_center, y=pdf.get_y(), w=50)
                pdf.ln(55)
                os.remove(cov_chart)
            except Exception as e:
                pdf.cell(0, 8, f"Erro ao gerar gráfico de cobertura: {e}", ln=True)
        # Duplicação
        if 'duplicated_lines_density' in valores:
            try:
                dup = float(valores['duplicated_lines_density'])
                dup_chart = os.path.join(OUTPUT_DIR, 'dup_pie.png')
                plot_pie_chart(['Duplicado', 'Único'], [dup, 100-dup], ['#FF9800', '#2196F3'], 'Duplicação de Código', dup_chart)
                x_center = (210 - 50) / 2
                pdf.image(dup_chart, x=x_center, y=pdf.get_y(), w=50)
                pdf.ln(55)
                os.remove(dup_chart)
            except Exception as e:
                pdf.cell(0, 8, f"Erro ao gerar gráfico de duplicação: {e}", ln=True)
    output_path = os.path.join(OUTPUT_DIR, "relatorio_sonarqube.pdf")
    pdf.output(output_path)
    print(f"PDF gerado: {output_path}")

if __name__ == "__main__":
    data = get_metrics()
    generate_pdf(data)
