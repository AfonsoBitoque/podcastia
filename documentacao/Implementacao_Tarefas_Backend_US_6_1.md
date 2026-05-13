# Relatório de Implementação: Backend-US-6-1 - Gestão de Relações de Amizade

Este documento detalha as tarefas executadas para implementar a funcionalidade de gestão de relações entre utilizadores, incluindo pedidos de amizade, bloqueios e o sistema de cooldown.

## Resumo da Funcionalidade

A User Story implementa um sistema robusto que permite aos utilizadores enviar pedidos de amizade, aceitá-los, cancelá-los e bloquear outros utilizadores. A lógica inclui regras de negócio para prevenir spam (cooldown) e garantir a integridade dos dados e a segurança.

 ---

### 1. Estrutura de Testes (TDD - Test-Driven Development)

Antes de qualquer implementação, foi criada uma suite de testes completa para validar o comportamento esperado da camada de serviço.

- **Ficheiro Criado:** `servidor/src/test/java/com/jep/servidor/UserRelationshipServiceTest.java`
- **Cobertura de Cenários de Teste:**
    - **Validações de Identidade:**
        - `testSelfFriendRequest`: Impede que um utilizador envie um pedido a si mesmo.
        - `testDuplicateFriendRequest`: Impede o envio de um pedido se já existir uma amizade ou um pedido pendente.
    - **Lógica de Cooldown:**
        - `testRecentRejectionCooldown`: Valida que o sistema bloqueia um novo pedido se uma rejeição for recente.
        - `testExpiredRejectionCooldown`: Valida que o sistema permite um novo pedido após o período de cooldown expirar.
    - **Lógica de Bloqueio:**
        - `testFriendRequestToBlockedUser`: Garante que um utilizador bloqueado não pode receber pedidos do bloqueador.
        - `testBlockUser`: Assegura que a ação de bloquear um utilizador limpa/substitui qualquer relação anterior.
    - **Fluxo de Aceitação e Cancelamento:**
        - `testAcceptNonExistentRequest`: Valida o erro ao tentar aceitar um pedido que não existe.
        - `testAcceptCanceledRequest`: Valida o erro ao tentar aceitar um pedido já cancelado.
    - **Notificações:**
        - `testNotificationTrigger`: Verifica se o serviço de notificação é chamado após um pedido bem-sucedido.
          
### 2. Evolução do Modelo de Dados (Entidade)

A entidade `UserRelation` foi modificada para suportar as novas regras de negócio.

- **Ficheiro Modificado:** `servidor/src/main/java/com/jep/servidor/model/UserRelation.java`
- **Alterações:**
    - O enum `RelationType` foi expandido para incluir `PEDIDO_REJEITADO` e `CANCELADO`.
    - Foram adicionados campos de auditoria `createdAt` e `updatedAt` com as anotações `@CreatedDate` e `@LastModifiedDate` para controlo temporal.
    - A anotação `@EntityListeners(AuditingEntityListener.class)` foi adicionada à entidade para ativar a auditoria automática.
    - Foram adicionados comentários e métodos de acesso (`getSender`, `getReceiver`) para clarificar a direção da relação.
      
### 3. Implementação da Camada de Serviço (Lógica de Negócio)

O núcleo da lógica de negócio foi implementado na camada de serviço, desacoplando-a do controller.
- **Ficheiros Criados:**
    - `servidor/src/main/java/com/jep/servidor/service/UserRelationshipService.java` (Interface)
    - `servidor/src/main/java/com/jep/servidor/service/impl/UserRelationshipServiceImpl.java` (Implementação)
- **Funcionalidades Implementadas:**
    - **`sendFriendRequest`**: Centraliza todas as validações (auto-pedido, duplicados, cooldown, bloqueio) antes de criar ou atualizar uma relação.
    - **`acceptFriendRequest`**: Altera o estado da relação para `AMIGO`.
    - **`cancelFriendRequest`**: Altera o estado da relação para `CANCELADO`.
    - **`blockUser`**: Garante que qualquer relação preexistente é substituída por uma nova relação de `BLOQUEADO`, assegurando a integridade.
    - **`getRelationStatus`**: Devolve um DTO (`RelationStatusDto`) com o estado da relação entre dois utilizadores, para uso do frontend.
      
### 4. Integração com Sistema de Notificações

O sistema foi configurado para notificar os utilizadores de novos pedidos de forma assíncrona.

- **Ficheiros Modificados:**
    - `servidor/src/main/java/com/jep/servidor/ServidorApplication.java`: Adicionada a anotação `@EnableAsync`.
    - `servidor/src/main/java/com/jep/servidor/service/impl/NotificationServiceImpl.java`: Adicionada a anotação `@Async` ao método `sendNotification`.
    - `servidor/src/main/java/com/jep/servidor/service/impl/UserRelationshipServiceImpl.java`: A chamada ao serviço de notificação foi enriquecida para incluir o nome do remetente na mensagem.
      
### 5. Exposição da API REST (Controller)

Os endpoints da API foram criados e/ou refatorados para expor a nova funcionalidade de forma segura.

- **Ficheiro Refatorado:** `servidor/src/main/java/com/jep/servidor/controller/UserRelationController.java`
- **Endpoints Implementados:**
  ```
  POST   /api/relations/friend-request/{friendId}
  GET    /api/relations/status/{targetUserId}
  DELETE /api/relations/friend-request/{friendId}/cancel
  ```
- **Melhorias:**
    - O controller foi refatorado para usar exclusivamente a `UserRelationshipService`.
    - A segurança foi implementada usando `@AuthenticationPrincipal Jwt jwt` para obter o ID do utilizador autenticado a partir do token, em vez de o passar como parâmetro.
      
### 6. Qualidade e Correção de Erros

Durante o desenvolvimento, foi realizado um ciclo iterativo de testes e correções para garantir a qualidade e robustez do código.

- **Correção de Dependências:** Adicionada a dependência `spring-boot-starter-oauth2-resource-server` ao `pom.xml` para suportar a extração de JWT nos controllers.
- **Resolução de Erros de Teste:**
    - Corrigidos erros de `NullPointerException` e `PotentialStubbingProblem` no `UserRelationshipServiceTest` através da adição de mocks em falta e da configuração correta do comportamento bidirecional das chamadas.
    - Resolvido um erro de `DataIntegrityViolationException` em `PlaylistIntegrationTest`, que foi causado pelas novas restrições `NOT NULL` na entidade `UserRelation`.
      
 ---

Com a conclusão destas tarefas, a User Story `Backend-US-6-1` foi totalmente implementada, testada e está pronta para ser integrada.