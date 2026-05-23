# 🚀 Guia de Comandos - Podcastia

Este guia contém os comandos necessários para compilar, testar, formatar e executar os vários componentes do projeto Podcastia (Servidor e Frontend).

---

## 💻 Comandos do Frontend

Todos os comandos do frontend devem ser executados dentro da pasta `frontend`.

```bash
cd frontend
```

### 1. Executar a Aplicação (Desenvolvimento)
Para iniciar o servidor de desenvolvimento com hot-reload:
```bash
npm run dev
```
*A aplicação ficará disponível, por defeito, em `http://localhost:5173`.*

### 2. Instalar Dependências
Se acabaste de clonar o projeto ou se foram adicionadas novas dependências:
```bash
npm install
```

### 3. Testes Unitários e de Componente (Vitest)
Executa os testes num modo interativo (*watch mode*), ideal enquanto desenvolves:
```bash
npm run test
```
Para executar os testes apenas uma vez (modo CI):
```bash
npm run test:ci
```

### 4. Testes End-to-End (Playwright)
*Nota: Na primeira vez, é necessário instalar os browsers executando: `npx playwright install`.*

Para executar os testes E2E num navegador real (com interface gráfica), ideal para depuração:
```bash
npm run test:e2e:ui
```
Para executar os testes E2E de forma silenciosa e rápida no terminal (modo CI):
```bash
npm run test:e2e
```

### 5. Qualidade de Código (Linting & Formatação)
Para verificar se existem erros de estilo ou más práticas no código:
```bash
npm run lint
```
Para **formatar automaticamente** todo o código do frontend (aplica regras de espaçamento e estética uniformes):
```bash
npm run format
```
Para apenas verificar se o código está formatado corretamente (usado em CI/CD):
```bash
npm run format:check
```

### 6. Compilar para Produção
Para criar a versão otimizada e final do frontend (os ficheiros gerados ficarão na pasta `dist/`):
```bash
npm run build
```

---

## ⚙️ Comandos do Servidor (Backend)

Todos os comandos do servidor devem ser executados dentro da pasta `servidor`.

```bash
cd servidor
```

O projeto utiliza o **Maven Wrapper (`mvnw`)**, o que significa que não precisas de ter o Maven instalado globalmente no sistema.

### 1. Executar a Aplicação
Para iniciar o servidor em modo de desenvolvimento:
```bash
./mvnw spring-boot:run
```
*A aplicação ficará disponível em `http://localhost:8080`.*

### 2. Compilar o Projeto
Para compilar o código e descarregar as dependências:
```bash
./mvnw clean install
```

### 3. Executar Testes
Para correr todos os testes unitários e de integração do lado do servidor:
```bash
./mvnw test
```

### 4. Gerar o Ficheiro JAR (Produção)
Para criar um pacote executável na pasta `target/`:
```bash
./mvnw package
```

### 5. Limpar a Build
Para remover a pasta `target/` e ficheiros temporários:
```bash
./mvnw clean
```

### 6. Verificação de Qualidade e Segurança
Para executar verificações de estilo de código e vulnerabilidades de dependências:
```bash
./mvnw verify
./mvnw validate
```

---

## 🗄️ Base de Dados (H2 Console)
Como o projeto usa uma base de dados H2 em memória, podes aceder à consola de gestão enquanto o servidor estiver a correr:

- **URL**: `http://localhost:8080/h2-console`
- **JDBC URL**: `jdbc:h2:./data/podcastia`
- **User**: `sa`
- **Password**: *(vazio)*

---

## ⚠️ Notas Importantes
- Se o comando `./mvnw` der erro de permissão no Linux/Mac, executa: `chmod +x mvnw`.
- Certifica-te de que estás na diretoria correta (`podcastia/servidor` ou `podcastia/frontend`) antes de executar os respetivos comandos.