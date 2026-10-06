package com.aeromaintenance.ai.engine;

import com.aeromaintenance.ai.data.ComponentType;
import com.aeromaintenance.ai.data.RiskLevel;

import java.util.Collections;
import java.util.List;

/** Health, risk and remaining useful life of one component. */
public final class ComponentAssessment {
    public final ComponentType component;
    public final int health;
    public final RiskLevel risk;
    public final int rulHours;
    public final String indicatorName;
    public final String indicatorUnit;
    public final int indicatorDecimals;
    public final double indicatorValue;
    public final double indicatorLimit;
    public final double trendPerHour;
    public final double trendR2;
    public final String condition;
    public final String recommendation;
    public final String shortAction;
    public final List<String> workPackage;
    public final List<Contribution> contributions;

    public ComponentAssessment(ComponentType component, int health, RiskLevel risk, int rulHours,
                               String indicatorName, String indicatorUnit, int indicatorDecimals,
                               double indicatorValue, double indicatorLimit, double trendPerHour,
                               double trendR2, String condition, String recommendation,
                               String shortAction, List<String> workPackage,
                               List<Contribution> contributions) {
        this.component = component;
        this.health = health;
        this.risk = risk;
        this.rulHours = rulHours;
        this.indicatorName = indicatorName;
        this.indicatorUnit = indicatorUnit;
        this.indicatorDecimals = indicatorDecimals;
        this.indicatorValue = indicatorValue;
        this.indicatorLimit = indicatorLimit;
        this.trendPerHour = trendPerHour;
        this.trendR2 = trendR2;
        this.condition = condition;
        this.recommendation = recommendation;
        this.shortAction = shortAction;
        this.workPackage = Collections.unmodifiableList(workPackage);
        this.contributions = Collections.unmodifiableList(contributions);
    }
}
