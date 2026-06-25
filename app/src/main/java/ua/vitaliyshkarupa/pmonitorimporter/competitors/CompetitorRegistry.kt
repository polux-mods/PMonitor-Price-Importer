package ua.vitaliyshkarupa.pmonitorimporter.competitors

object CompetitorRegistry {
    const val ATB = "АТБ"
    const val SILPO = "Сільпо"
    const val EVA = "Єва"
    const val AVRORA = "Аврора"

    val supported = listOf(ATB, SILPO, EVA, AVRORA)

    fun normalize(raw: String?): String? {
        val value = raw?.trim()?.lowercase().orEmpty()
        return when {
            value in listOf("атб", "atb", "атб-маркет", "атб маркет") -> ATB
            value in listOf("сільпо", "сильпо", "silpo") -> SILPO
            value in listOf("єва", "ева", "eva", "eva.ua") -> EVA
            value in listOf("аврора", "avrora", "avrora.ua", "aurora") -> AVRORA
            else -> null
        }
    }

    fun adapters(): Map<String, CompetitorAdapter> = listOf(
        AtbAdapter(),
        SilpoAdapter(),
        EvaAdapter(),
        AvroraAdapter()
    ).associateBy { it.canonicalName }
}
