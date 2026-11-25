
package com.example.brawlcoach

data class CoachRecommendation(
    val brawler: Brawler,
    val score: Int,
    val reasons: List<String>
)

object BrawlCoachAI {

    fun recommend(
        brawlers: List<Brawler>,
        modeName: String,
        mapName: String,
        allies: List<String>,
        enemies: List<String>,
        limit: Int = 3
    ): List<CoachRecommendation> {

        if (brawlers.isEmpty() || limit <= 0) {
            return emptyList()
        }

        val allyNames = allies.filter {
            it.isNotBlank() &&
            !it.equals("Inconnu", true) &&
            !it.equals("Non choisi", true)
        }

        val enemyNames = enemies.filter {
            it.isNotBlank() &&
            !it.equals("Inconnu", true) &&
            !it.equals("Non choisi", true)
        }

        val allyClasses = allyNames.mapNotNull { name ->
            brawlers.find { it.name.equals(name, true) }?.className
        }

        val enemyClasses = enemyNames.mapNotNull { name ->
            brawlers.find { it.name.equals(name, true) }?.className
        }

        val mode = modeName.lowercase()

        return brawlers
            .filter { candidate ->
                allyNames.none { it.equals(candidate.name, true) } &&
                enemyNames.none { it.equals(candidate.name, true) }
            }
            .map { candidate ->

                var score = 50
                val reasons = mutableListOf<String>()
                val role = candidate.className.lowercase()

                // Complémentarité avec les alliés
                val sameRole = allyClasses.count {
                    it.equals(candidate.className, true)
                }

                if (allyClasses.isNotEmpty()) {
                    if (sameRole == 0) {
                        score += 12
                        reasons.add("Complète les rôles de l'équipe")
                    } else if (sameRole >= 2) {
                        score -= 10
                        reasons.add("Rôle déjà très présent chez les alliés")
                    }
                }

                // Adaptation au mode de jeu
                when {
                    mode.contains("gem") ||
                    mode.contains("surviv") -> {
                        if (role.contains("support") ||
                            role.contains("controller")) {
                            score += 10
                            reasons.add("Intéressant pour le contrôle de zone")
                        }
                    }

                    mode.contains("brawl ball") ||
                    mode.contains("foot") -> {
                        if (role.contains("tank") ||
                            role.contains("assassin")) {
                            score += 10
                            reasons.add("Potentiel offensif pour le mode")
                        }
                    }

                    mode.contains("heist") ||
                    mode.contains("braquage") -> {
                        if (role.contains("damage") ||
                            role.contains("tank")) {
                            score += 10
                            reasons.add("Potentiel de dégâts sur le coffre")
                        }
                    }

                    mode.contains("knockout") ||
                    mode.contains("prime") ||
                    mode.contains("bounty") -> {
                        if (role.contains("marksman") ||
                            role.contains("damage")) {
                            score += 10
                            reasons.add("Intéressant pour les combats à distance")
                        }
                    }
                }

                // Analyse des rôles adverses visibles
                val enemyRoleCount = enemyClasses.count {
                    it.equals(candidate.className, true)
                }

                if (enemyRoleCount > 0) {
                    score -= 3 * enemyRoleCount
                    reasons.add("Évite de reproduire les rôles adverses")
                }

                // Analyse des gadgets et pouvoirs star
                val abilities = candidate.gadgets + candidate.starPowers

                val abilityText = abilities.joinToString(" ") {
                    "${it.name} ${it.description}"
                }.lowercase()

                if (abilityText.contains("heal") ||
                    abilityText.contains("soin") ||
                    abilityText.contains("healing")) {
                    score += 5
                    reasons.add("Dispose d'une capacité de soin")
                }

                if (abilityText.contains("shield") ||
                    abilityText.contains("bouclier")) {
                    score += 5
                    reasons.add("Dispose d'une capacité défensive")
                }

                if (abilityText.contains("slow") ||
                    abilityText.contains("ralent")) {
                    score += 4
                    reasons.add("Dispose d'un ralentissement")
                }

                if (abilityText.contains("stun") ||
                    abilityText.contains("étour")) {
                    score += 4
                    reasons.add("Dispose d'une capacité de contrôle")
                }

                // Pas de bonus inventé pour la carte :
                // les statistiques spécifiques seront ajoutées
                // lorsqu'une source fiable sera disponible.

                if (reasons.isEmpty()) {
                    reasons.add("Profil général à comparer")
                }

                CoachRecommendation(
                    brawler = candidate,
                    score = score.coerceIn(0, 100),
                    reasons = reasons.distinct()
                )
            }
            .sortedByDescending { it.score }
            .take(limit)
    }
}
