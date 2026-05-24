package com.jep.servidor.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * DTO do processo de onboarding de novos utilizadores.
 *
 * <p>Usado no endpoint {@code POST /api/users/onboarding} de
 * {@link com.jep.servidor.controller.AuthUserController} para registar a
 * seleção inicial de tópicos de interesse do utilizador.
 *
 * <p>Exige a seleção de pelo menos 3 tópicos (valores de {@link com.jep.servidor.model.PodcastTag}).
 * Após o onboarding, o campo {@code hasCompletedOnboarding} do utilizador é marcado
 * como {@code true} e os pontos de afinidade por categoria são inicializados.
 */
public class OnboardingDTO {
    
    @NotNull
    @Size(min = 3, message = "É necessário selecionar pelo menos 3 temas")
    private List<String> topics;
    
    private boolean hasCompletedOnboarding;

    public OnboardingDTO() {}

    public OnboardingDTO(List<String> topics, boolean hasCompletedOnboarding) {
        this.topics = topics;
        this.hasCompletedOnboarding = hasCompletedOnboarding;
    }

    public List<String> getTopics() {
        return topics;
    }

    public void setTopics(List<String> topics) {
        this.topics = topics;
    }

    public boolean isHasCompletedOnboarding() {
        return hasCompletedOnboarding;
    }

    public void setHasCompletedOnboarding(boolean hasCompletedOnboarding) {
        this.hasCompletedOnboarding = hasCompletedOnboarding;
    }
}
