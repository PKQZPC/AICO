"""Safety and boundary checks for AICO responses."""

from __future__ import annotations

from aico.api.schemas import RiskLevel


class SafetyGuard:
    high_risk_terms = [
        "suicide",
        "self-harm",
        "kill myself",
        "自杀",
        "自伤",
        "伤害自己",
        "不想活",
    ]

    boundary_terms = [
        "diagnose",
        "medicine",
        "guarantee",
        "诊断",
        "药",
        "保证",
    ]

    def assess(self, client_text: str, response_text: str) -> tuple[RiskLevel, list[str]]:
        text = f"{client_text}\n{response_text}".lower()
        reasons: list[str] = []
        if any(term.lower() in text for term in self.high_risk_terms):
            reasons.append("High-risk self-harm or crisis term detected.")
            return RiskLevel.HIGH, reasons
        if any(term.lower() in response_text.lower() for term in self.boundary_terms):
            reasons.append("Potential professional boundary issue detected.")
            return RiskLevel.MEDIUM, reasons
        return RiskLevel.LOW, reasons
