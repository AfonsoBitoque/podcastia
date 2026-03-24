# 📖 Documentação: Consumo Automático de Feeds RSS

Esta funcionalidade permite que a plataforma **Podcastia** consuma automaticamente artigos de fontes RSS parceiras. Estes artigos são extraídos a intervalos regulares e guardados na base de dados para posterior exibição, assegurando que o conteúdo disponível está sempre atualizado sem intervenção manual de um administrador.

---

## 🎯 Critérios de Aceitação Atendidos
A implementação cumpriu estritamente com os seguintes requisitos:

1. **Extração de Campos**: O sistema extrai com sucesso: `Título`, `Autor`, `Data de Publicação`, `URL Original` e `Conteúdo Principal`.
2. **Processamento Agendado**: O sistema usa o agendador do Spring Boot para verificar e processar novas entradas a cada 2 horas (configurável via *cron expression*).
3. **Resiliência de Dados**:
   - Falta de Autor: Se o feed não providenciar um autor, é inserida a string `"Desconhecido"`.
   - Falta de Data de Publicação: Se o feed não providenciar uma data de publicação, o sistema usa a data/hora exata do momento do processamento.
   - Prevenção de Duplicados: Validação prévia pelo `urlOriginal` antes da gravação de um novo artigo.

---

## 🛠️ Arquitetura e Decisões Técnicas

Para alcançar os objetivos da forma mais robusta e modular, a implementação foi dividida nos seguintes elementos:

### 1. Entidades da Base de Dados (`Model`)
Em vez de se fixar links diretamente no código (hardcoded), foi introduzida a possibilidade de gerir múltiplas fontes de feeds dinamicamente.

*   **`RssSource.java`**: Tabela responsável por guardar as fontes de RSS ativas e inativas (ex: *Observador*, *TechCrunch*, etc.).
*   **`Article.java`**: Tabela responsável por armazenar as notícias extraídas de cada uma dessas fontes.

### 2. Adição da Biblioteca `ROME`
O parsing de XMLs na mão é passível de falhas devido a diferenças nas especificações RSS (RSS 1.0, RSS 2.0, Atom, etc.). Adicionámos a dependência do `com.rometools:rome` no `pom.xml`, que atua como uma interface unificada para consumo destas *feeds*.

### 3. Serviço Principal: `RssService.java`
Serviço anotado com `@Service` e que contém o método `consumeRssFeeds()` gerido pelo agendador (Task Scheduler).
O motor do fluxo funciona nos seguintes passos:
1. Faz a leitura de todas as `RssSource` que estejam marcadas com `ativa = true`.
2. Conecta-se aos URLs guardados, e usa a biblioteca *ROME* para obter a lista de notícias (Entries).
3. Itera sobre cada notícia e aplica as devidas regras de negócio (limpeza de conteúdo, injeção de dados de *fallback* e verificação de duplicados através do Repositório).
4. Guarda e relaciona o `Article` à `RssSource` originária.

### 4. `DataSeeder.java` (Bootstrapping)
Para garantir que o ambiente tenha logo dados prontos para o agendador trabalhar, adicionou-se a injeção inicial de 4 fontes parceiras padrão de forma a que a lógica seja validada no arranque do servidor de testes/local.

---

## 🧪 Estratégia de Testes

Para garantir o contínuo funcionamento e prevenir futuras quebras ou regressões, foi desenvolvido o teste de integração:

*   **`RssServiceTest.java`**
    *   Este teste apaga temporariamente dados de teste, introduz uma Fonte Parceira e corre o método de consumo à força.
    *   No final, valida através de *Assertions* não apenas se os dados foram inseridos, mas também garante se não existem campos obrigatórios Nulos e se a fonte foi devidamente vinculada (Cumprindo o critério 1 e 3 de aceitação).

---

## 🚀 Como testar/executar localmente

1. Garantir que a anotação `@EnableScheduling` se mantém na classe principal do Servidor (`ServidorApplication.java`).
2. Arrancar o servidor Spring Boot: `./mvnw spring-boot:run`
3. Observar nos *logs* (consola) a inicialização das tabelas e o aviso `"Iniciando o consumo automático de feeds RSS..."`.
4. Acessar à base de dados para verificar os artigos recém descarregados.
5. Para testar o código através de testes unitários de integração:
   ```bash
   ./mvnw test -Dtest=RssServiceTest
   ```