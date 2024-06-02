package com.example.EvaluationStu.models

data class SkillDetail(
    val name: String,
    val description: String,
    val score: Double,
    val comment: String,
    val scoreHistory: List<Double>
)
