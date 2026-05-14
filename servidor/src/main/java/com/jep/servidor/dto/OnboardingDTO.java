package com.jep.servidor.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * DTO for onboarding process
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
