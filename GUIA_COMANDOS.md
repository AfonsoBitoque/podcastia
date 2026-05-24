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

### 7. Pré-visualizar Build de Produção
Para servir localmente a pasta `dist/` gerada pelo build:
```bash
npm run preview
```
*Útil para testar a build final antes de fazer deploy.*

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

## 🔑 Variáveis de Ambiente e Configuração

### Backend — Chave da API Gemini
A geração de podcasts por IA requer uma chave da Google Gemini API. Cria o ficheiro `servidor/env.properties` (não versionado) com o seguinte conteúdo:
```properties
GEMINI_API_KEY=a_tua_chave_aqui
```
Em alternativa, define a variável de ambiente do sistema antes de arrancar o servidor:
```bash
export GEMINI_API_KEY=a_tua_chave_aqui
./mvnw spring-boot:run
```
*Sem esta chave, o servidor arranca normalmente mas a funcionalidade de geração de podcasts retorna erro.*

### Frontend — URL do Backend
Se o backend não estiver em `http://localhost:8080`, cria o ficheiro `frontend/.env.local` com:
```
VITE_API_BASE_URL=http://localhost:8080
```
*Por defeito, o frontend assume `http://localhost:8080` se esta variável não estiver definida.*

---

## 🐍 Dependência Python — edge-tts (Síntese de Voz)

O servidor usa o Python para sintetizar áudio MP3 com a voz portuguesa. É necessário ter Python 3 e pip instalados.

Para instalar o `edge-tts`:
```bash
pip install edge-tts
```
Verificar se está disponível:
```bash
edge-tts --version
```
*Sem o `edge-tts`, o servidor arranca mas a geração de áudio falha com erro no processo de síntese.*

---

## 👤 Credenciais Padrão

No primeiro arranque, o sistema cria automaticamente um utilizador administrador:

| Campo    | Valor                   |
|----------|-------------------------|
| Email    | `admin@podcastia.com`   |
| Password | `admin`                 |
| Papel    | `USER_ADMIN`            |

---

## ⚠️ Notas Importantes
- Se o comando `./mvnw` der erro de permissão no Linux/Mac, executa: `chmod +x mvnw`.
- Certifica-te de que estás na diretoria correta (`podcastia/servidor` ou `podcastia/frontend`) antes de executar os respetivos comandos.
- O backend guarda os dados em `servidor/data/podcastia.mv.db` (H2 ficheiro local). Este ficheiro persiste entre arranques.
- As imagens de perfil são guardadas em `servidor/profile-images/` e os podcasts gerados em `servidor/generated-podcasts/`.