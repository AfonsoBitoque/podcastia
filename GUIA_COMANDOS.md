# 🚀 Guia de Comandos - Podcastia

Este guia contém os comandos necessários para compilar, testar e executar o servidor do projeto Podcastia.

## 📂 Localização do Projeto
Todos os comandos do servidor devem ser executados dentro da pasta `servidor`.

```bash
cd /home/predm/Git/University/LES/podcastia/servidor
```

## 🛠️ Comandos Maven (Usando o Wrapper)

O projeto utiliza o **Maven Wrapper (`mvnw`)**, o que significa que não precisas de ter o Maven instalado globalmente no sistema.

### 1. Executar a Aplicação
Para iniciar o servidor em modo de desenvolvimento:
```bash
./mvnw spring-boot:run
```
*A aplicação ficará disponível em `http://localhost:8080` (por defeito).*

### 2. Compilar o Projeto
Para compilar o código e descarregar as dependências:
```bash
./mvnw clean install
```

### 3. Executar Testes
Para correr todos os testes unitários e de integração:
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
```

---

## 🛠️ Base de Dados (H2 Console)
Como o projeto usa uma base de dados H2 em memória, podes aceder à consola de gestão enquanto o servidor estiver a correr:

- **URL**: `http://localhost:8080/h2-console`
- **JDBC URL**: `jdbc:h2:./data/podcastia`
- **User**: `sa`
- **Password**: *(vazio)*

---

## ⚠️ Notas Importantes
- Se o comando `./mvnw` der erro de permissão, executa: `chmod +x mvnw`.
- Certifica-te de que estás na diretoria `podcastia/servidor` antes de correr qualquer comando `./mvnw`.
