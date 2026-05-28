# CLAUDE.md (Frontend - CDB Finance)

## Comandos Úteis
Como o frontend é servido pelo backend Spring Boot, use comandos Maven na raiz:
* Executar aplicação: `mvn spring-boot:run`
* Build completo: `mvn clean package`

## Diretrizes de Codificação e Estilo
* **Tecnologias**: HTML5, Vanilla CSS, Vanilla JS (ES6+), jQuery 4.
* **Arquitetura JavaScript**:
  * `_1_domain`: Regras de negócio, entidades, validações client-side (sem dependências de infraestrutura).
  * `_2_application`: Serviços, orquestração, controle de estado, fluxos e páginas.
  * `_3_infrastructure`: Requisições REST API (Fetch/Ajax), adaptadores externos.
* **Estilo e Nomenclatura**:
  * JS: CamelCase para variáveis/funções, PascalCase para classes.
  * CSS: Kebab-case para classes, variáveis no `:root` para cores e suporte a Dark Mode.
  * HTML: Semântico, IDs únicos para elementos interativos.
