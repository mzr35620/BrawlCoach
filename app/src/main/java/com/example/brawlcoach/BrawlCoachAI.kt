
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

        val allyNames = allies
            .filter { it.isNotBlank() && !it.equals("Inconnu", true) }

        val enemyNames = enemies
            .filter { it.isNotBlank() && !it.equals("Inconnu", true) }

        val allyClasses = allyNames.mapNotNull { name ->
            brawlers.find { it.name.equals(name, true) }?.className
        }

        val enemyClasses = enemyNames.mapNotNull { name ->
            brawlers.find { it.name.equals(name, true) }?.className
        }

        return brawlers
            .filter { candidate ->
                allyNames.none { it.equals(candidate.name, true) } &&
                enemyNames.none { it.equals(candidate.name, true) }
            }
            .map { candidate ->

                var score = 50
                val reasons = mutableListOf<String>()

                val role = candidate.className.lowercase()
                val mode = modeName.lowercase()
                val map = mapName.lowercase()

                // Équilibre de l'équipe
                if (role.isNotBlank()) {
                    val sameRole = allyClasses.count {
                        it.equals(candidate.className, true)
                    }

                    if (sameRole == 0) {
                        score += 12
                        reasons.add("Apporte un rôle différent")
                    } else if (sameRole >= 2) {
                        score -= 8
                    }
                }

                // Analyse générale du mode
                when {
                    mode.contains("gem") || mode.contains("surviv") -> {
                        if (role.contains("support") ||
                            role.contains("controller")) {
                            score += 10
                            reasons.add("Rôle intéressant pour le contrôle")
                        }
                    }

                    mode.contains("brawl ball") ||
                    mode.contains("foot") -> {
                        if (role.contains("tank") ||
                            role.contains("assassin")) {
                            score += 10
                            reasons.add("Potentiel offensif")
                        }
                    }

                    mode.contains("heist") ||
                    mode.contains("braquage") -> {
                        if (role.contains("damage") ||
                            role.contains("tank")) {
                            score += 10
                            reasons.add("Potentiel de dégâts")
                        }
                    }

                    mode.contains("knockout") ||
                    mode.contains("prime") ||
                    mode.contains("bounty") -> {
                        if (role.contains("marksman") ||
                            role.contains("damage")) {
                            score += 10
                            reasons.add("Adapté aux combats à distance")
                        }
                    }
                }

                // Analyse des adversaires visibles
                val repeatedEnemyRoles = enemyClasses.count {
                    it.equals(candidate.className, true)
                }

                if (repeatedEnemyRoles > 0) {
                    score += 3
                    reasons.add("Présence de rôles similaires chez les adversaires")
                }

                // Recherche de mots-clés dans les capacités
                val abilities = candidate.gadgets + candidate.starPowers

                val abilityText = abilities.joinToString(" ") {
                    "${it.name} ${it.description}"
                }.lowercase()

                if (abilityText.contains("heal") ||
                    abilityText.contains("soin") ||
                    abilityText.contains("healing")) {
                    score += 5
                    reasons.add("Capacité de soin")
                }

                if (abilityText.contains("shield") ||
                    abilityText.contains("bouclier")) {
                    score += 5
                    reasons.add("Capacité défensive")
                }

                if (abilityText.contains("slow") ||
                    abilityText.contains("ralent")) {
                    score += 4
                    reasons.add("Capacité de ralentissement")
                }

                if (abilityText.contains("stun") ||
                    abilityText.contains("étour")) {
                    score += 4
                    reasons.add("Capacité de contrôle")
                }

                // La carte est prise en compte dans l'analyse.
                // Les bonus spécifiques seront ajoutés avec des données fiables.
                if (map.isNotBlank()) {
                    reasons.add("Carte prise en compte : $mapName")
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
