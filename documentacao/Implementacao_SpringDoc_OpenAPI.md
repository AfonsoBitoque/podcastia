# Implementação do SpringDoc OpenAPI

## Objetivo

A integração do SpringDoc OpenAPI visa gerar a documentação da nossa API REST (servidor Spring Boot) de forma automática e mantê-la sincronizada com o código fonte. Adicionalmente, disponibiliza o **Swagger UI**, uma interface visual interativa para exploração e testes diretos nos *endpoints* da API.

## O Que Foi Feito

1. **Adição de Dependência no Projeto**
   Atualizou-se o `pom.xml` para incluir o pacote oficial `springdoc-openapi-starter-webmvc-ui`.
   ```xml
   <dependency>
       <groupId>org.springdoc</groupId>
       <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
       <version>2.8.5</version>
   </dependency>
   ```

2. **Configuração dos Metadados da API**
   Foi criada a classe de configuração `OpenApiConfig.java` em `com.jep.servidor.config` para customizar as informações principais exibidas no Swagger UI, como o Título, Descrição, e Versão da nossa aplicação.
   
3. **Ajuste de Segurança (Spring Security)**
   Como o sistema utiliza proteção via JSON Web Token (JWT) e bloqueia o acesso anónimo por predefinição, atualizou-se as regras no `SecurityConfig.java`. Adicionaram-se as rotas relativas à documentação do Swagger UI na `whitelist` (permitidas para acessos públicos). Desta forma, programadores do frontend (e backend) podem inspecionar os *endpoints* sem necessidade de se autenticarem primeiro.
   As seguintes rotas foram autorizadas:
   * `/v3/api-docs/**`
   * `/swagger-ui/**`
   * `/swagger-ui.html`

4. **Implementação de Testes Automatizados**
   Criou-se a suite de testes `OpenApiIntegrationTest.java` com a finalidade de garantir que os acessos ao Swagger e à documentação JSON nativa devolvem respostas HTTP `200 OK` em vez de falhas por proteção restrita de segurança (ex: `401 Unauthorized` / `403 Forbidden`).

## Acesso e Utilização Local

Com a aplicação de backend a correr localmente (na porta `8080`), a documentação pode ser acedida através de:

- **Swagger UI (Visual):** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **JSON Bruto da OpenAPI (Estrutural):** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

## Como Documentar Novos Endpoints (Exemplo)

Agora, sempre que criarem um novo `RestController`, ele ficará automaticamente visível. Para melhorar os detalhes expostos, os programadores podem utilizar anotações específicas do Swagger nos seus controladores. Exemplo:

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/exemplo")
@Tag(name = "Exemplo de Controlador", description = "Operações de demonstração")
public class ExemploController {

    @GetMapping
    @Operation(summary = "Obter dados", description = "Retorna uma lista de dados exemplos.")
    @ApiResponse(responseCode = "200", description = "Operação bem sucedida")
    public ResponseEntity<List<String>> obterDados() {
        return ResponseEntity.ok(List.of("Dado 1", "Dado 2"));
    }
}
```