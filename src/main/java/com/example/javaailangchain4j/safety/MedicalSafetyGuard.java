package com.example.javaailangchain4j.safety;

import java.util.List;

public class MedicalSafetyGuard {

    private static final List<String> EMERGENCY_KEYWORDS = List.of(
            "胸痛", "胸闷", "呼吸困难", "喘不上气", "窒息", "昏迷", "意识不清", "抽搐",
            "大出血", "严重外伤", "车祸", "中风", "卒中", "口角歪斜", "一侧无力",
            "剧烈头痛", "自杀", "服毒", "过敏性休克", "休克"
    );

    private MedicalSafetyGuard() {
    }

    public static boolean isEmergency(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        return EMERGENCY_KEYWORDS.stream().anyMatch(message::contains);
    }

    public static String emergencyResponse() {
        return """
                你描述的情况可能属于急症风险。请立即拨打 120 或尽快前往最近医院急诊科，不要等待线上回复。
                如果现场有人陪同，请让对方协助记录症状开始时间、既往病史、正在服用的药物，并保持电话畅通。
                """;
    }
}
