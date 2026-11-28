
package com.example.brawlcoach

import java.text.Normalizer
import java.util.Locale

data class CoachRecommendation(
    val brawler: Brawler,
    val score: Int,
    val reasons: List<String>
)

object BrawlCoachAI {

    private enum class ModeType {
        GEM_GRAB,
        SHOWDOWN,
        BRAWL_BALL,
        BASKET,
        VOLLEY,
        HOCKEY,
        HEIST,
        HOT_ZONE,
        KNOCKOUT,
        BOUNTY,
        WIPEOUT,
        DUELS,
        PAYLOAD,
        BIG_GAME,
        BOSS_FIGHT,
        ROBO_RUMBLE,
        UNKNOWN
    }

    private fun normalize(value: String): String {
        return Normalizer.normalize(
            value.lowercase(Locale.ROOT),
            Normalizer.Form.NFD
        )
            .replace(Regex("\\p{Mn}+"), "")
            .replace("-", " ")
            .replace("_", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun detectMode(modeName: String): ModeType {
        val mode = normalize(modeName)

        return when {
            mode.contains("razzia") ||
            mode.contains("gem grab") ||
            mode.contains("gemme") -> ModeType.GEM_GRAB

            mode.contains("survivant") ||
            mode.contains("showdown") ||
            mode.contains("solo") ||
            mode.contains("duo") -> ModeType.SHOWDOWN

            mode.contains("brawl ball") ||
            mode.contains("foot") -> ModeType.BRAWL_BALL

            mode.contains("basket") -> ModeType.BASKET

            mode.contains("volley") -> ModeType.VOLLEY

            mode.contains("hockey") ||
            mode.contains("brawl hockey") -> ModeType.HOCKEY

            mode.contains("braquage") ||
            mode.contains("heist") -> ModeType.HEIST

            mode.contains("zone reservee") ||
            mode.contains("hot zone") -> ModeType.HOT_ZONE

            mode.contains("hors jeu") ||
            mode.contains("knockout") -> ModeType.KNOCKOUT

            mode.contains("prime") ||
            mode.contains("bounty") -> ModeType.BOUNTY

            mode.contains("chasse ouverte") ||
            mode.contains("wipeout") -> ModeType.WIPEOUT

            mode.contains("duel") -> ModeType.DUELS

            mode.contains("cargaison") ||
            mode.contains("payload") -> ModeType.PAYLOAD

            mode.contains("gros jeu") ||
            mode.contains("big game") -> ModeType.BIG_GAME

            mode.contains("combat de boss") ||
            mode.contains("boss fight") -> ModeType.BOSS_FIGHT

            mode.contains("robot") ||
            mode.contains("robo rumble") -> ModeType.ROBO_RUMBLE

            else -> ModeType.UNKNOWN
        }
    }

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

        val mode = detectMode(modeName)

        val allyNames = allies.filter { it.isNotBlank() }
        val enemyNames = enemies.filter { it.isNotBlank() }

        val allyClasses = allyNames.mapNotNull { name ->
            brawlers.find {
                it.name.equals(name, true)
            }?.className
        }

        val enemyClasses = enemyNames.mapNotNull { name ->
            brawlers.find {
                it.name.equals(name, true)
            }?.className
        }

        return brawlers
            .filter { candidate ->
                allyNames.none {
                    it.equals(candidate.name, true)
                } && enemyNames.none {
                    it.equals(candidate.name, true)
                }
            }
            .map { candidate ->

                var score = 50
                val reasons = mutableListOf<String>()

                val role = normalize(candidate.className)

                val abilities = (
                    candidate.gadgets + candidate.starPowers
                ).joinToString(" ") {
                    "${it.name} ${it.description}"
                }.let { normalize(it) }

                fun addScore(points: Int, reason: String) {
                    score += points
                    reasons.add(reason)
                }

                val isTank = role.contains("tank")
                val isAssassin = role.contains("assassin")
                val isSupport = role.contains("support")
                val isController = role.contains("controller")
                val isMarksman = role.contains("marksman")
                val isDamage = role.contains("damage")
                val isArtillery = role.contains("artillery")

                // Adaptation au mode de jeu

                when (mode) {

                    ModeType.GEM_GRAB -> {
                        if (isController) {
                            addScore(13, "Bon contrôle du centre")
                        }
                        if (isSupport) {
                            addScore(9, "Utile pour protéger le porteur de gemmes")
                        }
                        if (isMarksman) {
                            addScore(7, "Bonne pression à distance")
                        }
                    }

                    ModeType.SHOWDOWN -> {
                        if (isTank || isAssassin) {
                            addScore(10, "Potentiel en combat rapproché")
                        }
                        if (isDamage) {
                            addScore(7, "Bonne capacité de combat")
                        }
                        if (abilities.contains("shield") ||
                            abilities.contains("bouclier") ||
                            abilities.contains("heal") ||
                            abilities.contains("soin")) {
                            addScore(5, "Capacité utile pour survivre")
                        }
                    }

                    ModeType.BRAWL_BALL -> {
                        if (isTank) {
                            addScore(13, "Résistance utile pour avancer vers le but")
                        }
                        if (isAssassin) {
                            addScore(10, "Potentiel d'infiltration offensive")
                        }
                        if (isController) {
                            addScore(8, "Peut contrôler les accès au but")
                        }
                    }

                    ModeType.BASKET -> {
                        if (isTank) {
                            addScore(12, "Résistance utile près du panier")
                        }
                        if (isAssassin) {
                            addScore(9, "Mobilité et pression offensive")
                        }
                        if (isController) {
                            addScore(8, "Contrôle de la zone du panier")
                        }
                    }

                    ModeType.VOLLEY,
                    ModeType.HOCKEY -> {
                        if (isController) {
                            addScore(12, "Contrôle de la zone de jeu")
                        }
                        if (isMarksman || isArtillery) {
                            addScore(9, "Pression à distance")
                        }
                        if (isSupport) {
                            addScore(7, "Soutien de l'équipe")
                        }
                    }

                    ModeType.HEIST -> {
                        if (isDamage) {
                            addScore(13, "Potentiel de dégâts sur le coffre")
                        }
                        if (isTank) {
                            addScore(9, "Peut avancer sous la pression")
                        }
                        if (isAssassin) {
                            addScore(7, "Potentiel d'attaque rapide")
                        }
                    }

                    ModeType.HOT_ZONE -> {
                        if (isController) {
                            addScore(14, "Excellent profil pour contrôler une zone")
                        }
                        if (isTank) {
                            addScore(10, "Peut tenir une zone")
                        }
                        if (isSupport) {
                            addScore(8, "Peut soutenir les alliés dans la zone")
                        }
                    }

                    ModeType.KNOCKOUT,
                    ModeType.BOUNTY,
                    ModeType.WIPEOUT -> {
                        if (isMarksman) {
                            addScore(13, "Profil adapté aux combats à distance")
                        }
                        if (isArtillery) {
                            addScore(10, "Peut mettre la pression à distance")
                        }
                        if (isController) {
                            addScore(8, "Peut limiter les déplacements adverses")
                        }
                    }

                    ModeType.DUELS -> {
                        if (isAssassin) {
                            addScore(10, "Potentiel en duel individuel")
                        }
                        if (isDamage) {
                            addScore(9, "Capacité à éliminer rapidement")
                        }
                        if (isTank) {
                            addScore(7, "Bonne résistance en combat")
                        }
                    }

                    ModeType.PAYLOAD -> {
                        if (isTank) {
                            addScore(12, "Peut accompagner la cargaison")
                        }
                        if (isController) {
                            addScore(11, "Peut contrôler le parcours")
                        }
                        if (isDamage) {
                            addScore(8, "Pression offensive")
                        }
                    }

                    ModeType.BIG_GAME,
                    ModeType.BOSS_FIGHT,
                    ModeType.ROBO_RUMBLE -> {
                        if (isDamage) {
                            addScore(12, "Potentiel de dégâts important")
                        }
                        if (isSupport) {
                            addScore(9, "Peut aider à maintenir l'équipe")
                        }
                        if (isTank) {
                            addScore(8, "Bonne résistance")
                        }
                    }

                    ModeType.UNKNOWN -> {
                        if (isDamage || isController) {
                            addScore(4, "Profil polyvalent")
                        }
                    }
                }

                // Analyse des capacités

                if (abilities.contains("heal") ||
                    abilities.contains("healing") ||
                    abilities.contains("soin") ||
                    abilities.contains("healing")) {
                    addScore(5, "Dispose d'une capacité de soin")
                }

                if (abilities.contains("shield") ||
                    abilities.contains("bouclier")) {
                    addScore(4, "Dispose d'une capacité défensive")
                }

                if (abilities.contains("slow") ||
                    abilities.contains("ralent")) {
                    addScore(4, "Dispose d'un ralentissement")
                }

                if (abilities.contains("stun") ||
                    abilities.contains("etour")) {
                    addScore(4, "Dispose d'une capacité de contrôle")
                }

                if (abilities.contains("dash") ||
                    abilities.contains("teleport") ||
                    abilities.contains("teleportation")) {
                    addScore(3, "Dispose d'une capacité de mobilité")
                }

                // Complémentarité des rôles si les alliés sont renseignés

                if (allyClasses.isNotEmpty()) {
                    val sameRole = allyClasses.count {
                        normalize(it) == role
                    }

                    if (sameRole == 0) {
                        addScore(5, "Complète les rôles de l'équipe")
                    } else if (sameRole >= 2) {
                        score -= 5
                        reasons.add("Rôle déjà présent chez les alliés")
                    }
                }

                // Diversité face aux adversaires si renseignés

                val enemyRoleCount = enemyClasses.count {
                    normalize(it) == role
                }

                if (enemyRoleCount > 0) {
                    score -= enemyRoleCount * 2
                    reasons.add("Diversifie les rôles face aux adversaires")
                }

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
