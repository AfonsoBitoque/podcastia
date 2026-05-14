# Análise Crítica - Podcastia
## Relatório de Bugs, Erros e Problemas de Arquitetura

**Data da análise:** 13 de Maio de 2026  
**Versão do software:** Atual (checkpoint 2)  
**Severidade:** Crítica / Alta / Média / Baixa  

---

## 1. ERROS CRÍTICOS (Impeditivos para produção)

### 1.1 Loop Infinito de Redirecionamento (RESOLVIDO PARCIALMENTE)
**Ficheiro:** `frontend/src/App.jsx`, `frontend/src/pages/OnboardingSurvey.jsx`  
**Severidade:** CRÍTICA  
**Estado:** Parcialmente resolvido com dispatch de evento `auth-change`  

**Problema:** Após completar o onboarding, o sistema entrava em loop infinito entre `/home` e `/onboarding` porque:
- O `localStorage` era actualizado mas o componente `ProtectedRoute` não re-renderizava
- A verificação `hasCompletedOnboarding` não era reactiva

**Solução aplicada:** Adicionado `window.dispatchEvent(new Event('auth-change'))` após actualizar o localStorage.

**Risco residual:** Se o evento não for capturado ou houver race conditions, o loop pode persistir.

---

### 1.2 Endpoint Inexistente - GET /api/podcasts (RESOLVIDO)
**Ficheiro:** `servidor/src/main/java/com/jep/servidor/controller/PodcastGenerationController.java`  
**Severidade:** CRÍTICA  
**Estado:** Resolvido  

**Problema:** A homepage chamava `GET /api/podcasts` mas esse endpoint não existia, retornando 403/404.

**Impacto:** Utilizadores não conseguiam ver podcasts na homepage.

**Solução aplicada:** Adicionado método `getAllPublicPodcasts()` e permitido acesso público no `SecurityConfig`.

---

### 1.3 Recursos Estáticos Não Servidos (RESOLVIDO)
**Ficheiro:** `servidor/src/main/java/com/jep/servidor/config/StaticResourceConfig.java` (criado)  
**Severidade:** CRÍTICA  
**Estado:** Resolvido  

**Problema:** 
- Imagens em `/images/` retornavam 403
- Ficheiros de áudio não eram servidos correctamente

**Causa:** Não havia configuração de `ResourceHandler` no Spring Boot.

**Solução aplicada:** 
- Criado `StaticResourceConfig.java` com handlers para `/images/**` e `/audio/**`
- Adicionadas permissões públicas no `SecurityConfig`
- Criada imagem SVG default em `static/images/`

---

### 1.4 ImmutableList Save Exception (RESOLVIDO)
**Ficheiro:** `servidor/src/main/java/com/jep/servidor/controller/AuthUserController.java`  
**Severidade:** CRÍTICA  
**Estado:** Resolvido  

**Problema:** `UnsupportedOperationException` ao salvar topics do onboarding.

**Causa:** `stream().toList()` retorna lista imutável; Hibernate precisa de `ArrayList`.

**Solução aplicada:** Conversão explícita: `new java.util.ArrayList<>(podcastTags)`

---

## 2. ERROS DE SEGURANÇA (Alta Severidade)

### 2.1 Logging de Tokens JWT
**Ficheiros:** 
- `frontend/src/pages/LoginPage.jsx:60-62`
- `frontend/src/pages/OnboardingSurvey.jsx:76`

**Severidade:** ALTA  

**Problema:** Tokens JWT completos são logados no console do browser:
```javascript
console.log('Submitting onboarding with token:', token ? token.substring(0, 20) + '...' : 'MISSING')
console.log('Login response:', data)
console.log('hasCompletedOnboarding value:', data.hasCompletedOnboarding)
```

**Impacto:** Qualquer pessoa com acesso ao DevTools (ou extensões maliciosas) pode ver tokens parciais.

**Recomendação:** Remover todos os `console.log` de dados sensíveis antes de produção.

---

### 2.2 CORS Configurado para Permitir Tudo
**Ficheiro:** `servidor/src/main/java/com/jep/servidor/config/CorsConfig.java`  
**Severidade:** MÉDIA  

**Problema:** Configuração CORS permite qualquer origem (`*`), o que pode levar a ataques CSRF em certos cenários.

**Recomendação:** Restringir a origens específicas em produção.

---

### 2.3 Exposição de Stack Traces
**Ficheiro:** `servidor/src/main/java/com/jep/servidor/controller/PodcastGenerationController.java:83-84`

**Severidade:** MÉDIA  

**Problema:** Mensagens de erro expõem detalhes internos:
```java
.body(Map.of("error", "Erro ao gerar podcast: " + e.getMessage()));
```

**Impacto:** Informação sobre estrutura interna do sistema pode ser exposta.

---

## 3. BUGS FUNCIONAIS (Média Severidade)

### 3.1 Service Worker Cache Fail
**Ficheiro:** `frontend/public/sw.js`  
**Severidade:** MÉDIA  
**Estado:** Não resolvido  

**Problema:**
```
sw.js:1 Uncaught (in promise) TypeError: Failed to execute 'addAll' on 'Cache': Request failed
```

**Causa:** O service worker tenta cachear recursos que não existem ou não estão acessíveis durante o build.

**Impacto:** Funcionalidade offline pode não funcionar correctamente.

---

### 3.2 Áudio 404 - Ficheiros Não Encontrados
**Ficheiro:** Vários - problema de dados  
**Severidade:** MÉDIA  

**Problema:** 
```
GET http://localhost:8080/api/podcasts/39/audio 404 (Not Found)
```

**Causas possíveis:**
1. Podcasts criados pelo `DataSeeder` apontam para ficheiros inexistentes (`test/*.mp3`)
2. Ficheiros gerados foram apagados
3. Caminhos na base de dados não correspondem aos ficheiros físicos

**Solução:** Verificar consistência entre BD e sistema de ficheiros; regenerar podcasts se necessário.

---

### 3.3 Duração Estimada Imprecisa
**Ficheiro:** `servidor/src/main/java/com/jep/servidor/service/PodcastGenerationService.java:57-58`

**Severidade:** BAIXA  

**Problema:**
```java
int estimatedDurationMinutes = Math.max(1, wordCount / 150);
```

**Causa:** Estimativa fixa de 150 palavras/minuto pode não corresponder à velocidade real do TTS.

---

### 3.4 Race Condition no Onboarding
**Ficheiro:** `frontend/src/App.jsx`  
**Severidade:** MÉDIA  

**Problema:** O `useOnboardingGuard` usa `localStorage` diretamente no render, o que pode causar inconsistências se múltiplos tabs estiverem abertos.

**Recomendação:** Usar um state management centralizado (Context API ou Redux).

---

## 4. PROBLEMAS DE ARQUITETURA

### 4.1 Mistura de Responsabilidades
**Ficheiro:** `servidor/src/main/java/com/jep/servidor/controller/AuthUserController.java`

**Problema:** O controller de autenticação também gere onboarding e preferências de utilizador. Violha o Single Responsibility Principle.

**Recomendação:** Criar `OnboardingController` separado.

---

### 4.2 Hardcoded API URLs
**Ficheiros:** Múltiplos no frontend  

**Problema:** URLs como `http://localhost:8080` estão hardcoded em vários ficheiros em vez de usar variáveis de ambiente consistentemente.

**Exemplo:**
```javascript
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
```

Funciona, mas deveria ser obrigatório definir a variável.

---

### 4.3 Tratamento de Erros Inconsistente
**Ficheiros:** Vários no frontend  

**Problema:** Alguns erros mostram mensagens ao utilizador, outros apenas logam no console.

**Exemplo inconsistente:**
```javascript
// LoginPage.jsx - boa mensagem
setMessage(data?.error || 'Credenciais invalidas...')

// HomePage.jsx - mensagem genérica
setError('Failed to load podcasts')
```

---

### 4.4 Estado Duplicado
**Ficheiro:** `frontend/src/App.jsx`  

**Problema:** O estado de onboarding existe em dois lugares:
1. `localStorage` (chave `user`)
2. `localStorage` (chave `topicsOnboardingComplete`) - redundante

A chave `topicsOnboardingComplete` deveria ser removida para evitar inconsistências.

---

## 5. PROBLEMAS DE UX/UI

### 5.1 Feedback Visual Insuficiente
**Ficheiro:** `frontend/src/pages/OnboardingSurvey.jsx`

**Problema:** Durante o submit do onboarding, não há indicador de progresso claro para o utilizador saber que algo está a acontecer.

---

### 5.2 Retry Button Sem Lógica de Retry
**Ficheiro:** `frontend/src/pages/HomePage.jsx:323`

**Problema:** Botão "Tentar novamente" chama `fetchPodcasts` mas não limpa o estado de erro primeiro, podendo mostrar mensagem de erro persistente.

---

## 6. PROBLEMAS DE PERFORMANCE

### 6.1 Re-renderizações Desnecessárias
**Ficheiro:** `frontend/src/App.jsx`

**Problema:** O `useOnboardingGuard` é chamado em cada render do `ProtectedRoute`, lendo do `localStorage` repetidamente.

**Recomendação:** Memoizar o resultado ou usar Context API.

---

### 6.2 Sem Lazy Loading de Componentes
**Ficheiro:** `frontend/src/App.jsx`

**Problema:** Todos os componentes de páginas são importados sincronamente, aumentando o bundle size inicial.

**Recomendação:** Usar `React.lazy()` para code-splitting.

---

## 7. PROBLEMAS DE DEPENDÊNCIAS

### 7.1 Python edge-tts Não Verificado
**Ficheiro:** `servidor/src/main/java/com/jep/servidor/service/PodcastGenerationService.java`

**Problema:** O sistema assume que `python3` e `edge-tts` estão instalados, mas não verifica no startup.

**Impacto:** Se não estiver instalado, a geração de podcasts falha silenciosamente ou com erro genérico.

---

### 7.2 API Key Gemini Expõe Limites
**Ficheiro:** `servidor/src/main/resources/application.properties`

**Problema:** A API key do Gemini está em ficheiro de properties. Se o repositório for público, a key pode ser exposta.

**Recomendação:** Usar variáveis de ambiente ou secret management.

---

## 8. PROBLEMAS DE TESTE/DEV

### 8.1 Console.log em Produção
**Ficheiros:** Múltiplos  
**Severidade:** BAIXA  

**Lista completa de console.log que devem ser removidos:**
- `App.jsx` (logs de routing)
- `LoginPage.jsx:60-62, 82, 85`
- `OnboardingSurvey.jsx:76, 83`
- `BackgroundAudioService.js:127, 160, 299`

---

### 8.2 Comentários de Debug
**Ficheiro:** `servidor/src/main/java/com/jep/servidor/config/JwtAuthenticationFilter.java`

**Problema:** Existem comentários de debug detalhados que deveriam ser removidos ou convertidos para logging nível DEBUG.

---

## 9. RESUMO EXECUTIVO

### Bugs Críticos (Bloqueantes)
| Bug | Estado | Ficheiros Afetados |
|-----|--------|-------------------|
| Loop de redirecionamento | Parcialmente resolvido | App.jsx, OnboardingSurvey.jsx |
| Endpoint /api/podcasts inexistente | Resolvido | PodcastGenerationController.java |
| Recursos estáticos 403 | Resolvido | StaticResourceConfig.java (novo) |
| ImmutableList exception | Resolvido | AuthUserController.java |

### Vulnerabilidades de Segurança
| Issue | Severidade | Ação Recomendada |
|-------|------------|------------------|
| Logging de tokens | Alta | Remover console.log |
| CORS permissivo | Média | Restringir origens |
| Stack traces expostos | Média | Sanitizar mensagens de erro |

### Dívida Técnica
| Issue | Impacto |
|-------|---------|
| Mistura de responsabilidades | Dificulta manutenção |
| Estado duplicado localStorage | Pode causar inconsistências |
| Hardcoded URLs | Dificulta deploy em múltiplos ambientes |
| Sem lazy loading | Bundle size excessivo |

---

## 10. RECOMENDAÇÕES PRIORITÁRIAS

### Curto Prazo (1-2 semanas)
1. **Remover todos os console.log** de dados sensíveis
2. **Adicionar testes unitários** para o fluxo de onboarding
3. **Criar endpoint de health check** para verificar dependências (Python, edge-tts)
4. **Implementar error boundaries** no React

### Médio Prazo (1 mês)
1. **Refactor AuthUserController** - separar onboarding
2. **Implementar Context API** para estado de autenticação
3. **Adicionar lazy loading** para páginas
4. **Criar sistema de logging estruturado** (SLF4J + Logback)

### Longo Prazo (3+ meses)
1. **Implementar testes de integração** (Playwright/Cypress)
2. **Adicionar rate limiting** na API
3. **Implementar caching** com Redis
4. **Criar pipeline CI/CD** com testes automatizados

---

## 11. MÉTRICAS DO CÓDIGO

### Frontend
- **Total de ficheiros JSX:** ~35
- **Console.log statements:** ~15 (devem ser removidos)
- **TODO/FIXME comentários:** 0
- **Hooks customizados:** 3 (useBackgroundAudio, useOnboardingGuard, etc.)

### Backend
- **Total de classes Java:** ~80
- **Controllers:** 8
- **Services:** 10
- **Modelos:** 12
- **Cobertura de testes:** Desconhecida (não verificada)

---

**Fim do Relatório**

*Este documento deve ser actualizado conforme os bugs são resolvidos e novos são descobertos.*
