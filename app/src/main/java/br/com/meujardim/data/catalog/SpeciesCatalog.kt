package br.com.meujardim.data.catalog

import br.com.meujardim.data.db.LightNeed
import br.com.meujardim.util.normalizedForSearch

/**
 * Sugestão de cuidados por espécie. Os valores são pontos de partida para vasos
 * médios em ambiente interno; o usuário sempre pode ajustar.
 */
data class SpeciesInfo(
    val popularName: String,
    val scientificName: String,
    val frequencyDays: Int,
    val waterAmountMl: Int,
    val light: LightNeed,
    val tip: String,
    val aliases: List<String> = emptyList()
)

object SpeciesCatalog {

    val all: List<SpeciesInfo> = listOf(
        SpeciesInfo("Jiboia", "Epipremnum aureum", 7, 300, LightNeed.LUZ_INDIRETA,
            "Regue quando os 2 a 3 cm de cima do solo estiverem secos.", listOf("pothos")),
        SpeciesInfo("Espada-de-são-jorge", "Dracaena trifasciata", 14, 250, LightNeed.MEIA_SOMBRA,
            "Tolera seca; excesso de água é o maior risco.", listOf("sansevieria", "espada de sao jorge", "lanca de sao jorge")),
        SpeciesInfo("Zamioculca", "Zamioculcas zamiifolia", 14, 250, LightNeed.MEIA_SOMBRA,
            "Armazena água nos rizomas; espere o solo secar por completo.", listOf("zz", "zamioculcas")),
        SpeciesInfo("Costela-de-adão", "Monstera deliciosa", 7, 500, LightNeed.LUZ_INDIRETA,
            "Gosta de solo levemente úmido, nunca encharcado.", listOf("monstera", "costela de adao")),
        SpeciesInfo("Lírio-da-paz", "Spathiphyllum wallisii", 4, 300, LightNeed.MEIA_SOMBRA,
            "As folhas murcham quando falta água e se recuperam após a rega.", listOf("lirio da paz", "espatifilo")),
        SpeciesInfo("Samambaia", "Nephrolepis exaltata", 3, 300, LightNeed.MEIA_SOMBRA,
            "Prefere solo sempre úmido e ar com boa umidade.", listOf("samambaia americana")),
        SpeciesInfo("Suculenta", "Echeveria spp.", 10, 100, LightNeed.SOL_PLENO,
            "Regue bem e só repita quando o substrato estiver totalmente seco.", listOf("echeveria", "suculentas")),
        SpeciesInfo("Cacto", "Cactaceae", 21, 100, LightNeed.SOL_PLENO,
            "No inverno, reduza ainda mais as regas.", listOf("cactos", "cactus")),
        SpeciesInfo("Babosa", "Aloe vera", 14, 150, LightNeed.SOL_PLENO,
            "Deixe o solo secar entre as regas.", listOf("aloe", "aloe vera")),
        SpeciesInfo("Orquídea", "Phalaenopsis spp.", 7, 150, LightNeed.LUZ_INDIRETA,
            "Regue quando as raízes estiverem prateadas; deixe escorrer bem.", listOf("orquidea", "phalaenopsis")),
        SpeciesInfo("Antúrio", "Anthurium andraeanum", 4, 250, LightNeed.LUZ_INDIRETA,
            "Mantenha o solo levemente úmido.", listOf("anturio")),
        SpeciesInfo("Maranta", "Maranta leuconeura", 4, 250, LightNeed.MEIA_SOMBRA,
            "Sensível a solo seco por muito tempo; gosta de umidade no ar.", listOf("planta rezadeira")),
        SpeciesInfo("Calathea", "Goeppertia spp.", 4, 250, LightNeed.MEIA_SOMBRA,
            "Prefere água filtrada ou descansada e solo úmido.", listOf("calateia", "goeppertia")),
        SpeciesInfo("Ficus lyrata", "Ficus lyrata", 7, 500, LightNeed.LUZ_INDIRETA,
            "Regue quando o topo do solo estiver seco; evite mudar de lugar.", listOf("figueira lira", "figueira-lira")),
        SpeciesInfo("Ficus elastica", "Ficus elastica", 10, 400, LightNeed.LUZ_INDIRETA,
            "Deixe secar parcialmente entre as regas.", listOf("falsa seringueira", "borracheira")),
        SpeciesInfo("Filodendro", "Philodendron spp.", 7, 300, LightNeed.LUZ_INDIRETA,
            "Regue quando a camada superior estiver seca.", listOf("philodendron", "filodendro brasil")),
        SpeciesInfo("Clorofito", "Chlorophytum comosum", 5, 250, LightNeed.LUZ_INDIRETA,
            "Pontas marrons podem indicar água com muito cloro.", listOf("gravatinha", "planta aranha")),
        SpeciesInfo("Palmeira-ráfis", "Rhapis excelsa", 6, 400, LightNeed.MEIA_SOMBRA,
            "Gosta de solo úmido, sem acumular água no pratinho.", listOf("rafis", "palmeira rafis")),
        SpeciesInfo("Areca-bambu", "Dypsis lutescens", 4, 500, LightNeed.LUZ_INDIRETA,
            "Aprecia umidade; borrife as folhas em dias secos.", listOf("areca")),
        SpeciesInfo("Pacová", "Philodendron martianum", 5, 400, LightNeed.MEIA_SOMBRA,
            "Prefere solo levemente úmido.", listOf("pacova")),
        SpeciesInfo("Begônia", "Begonia spp.", 4, 200, LightNeed.LUZ_INDIRETA,
            "Regue o solo, evitando molhar as folhas.", listOf("begonia")),
        SpeciesInfo("Violeta-africana", "Saintpaulia ionantha", 4, 100, LightNeed.LUZ_INDIRETA,
            "Regue por baixo ou direto no solo, sem molhar as folhas.", listOf("violeta", "violeta africana")),
        SpeciesInfo("Peperômia", "Peperomia spp.", 8, 150, LightNeed.MEIA_SOMBRA,
            "Folhas carnudas guardam água; espere o solo secar.", listOf("peperomia")),
        SpeciesInfo("Hera", "Hedera helix", 5, 200, LightNeed.MEIA_SOMBRA,
            "Gosta de solo levemente úmido e temperaturas amenas.", listOf("hera inglesa")),
        SpeciesInfo("Manjericão", "Ocimum basilicum", 2, 200, LightNeed.SOL_PLENO,
            "Em vaso, costuma precisar de água quase todo dia no calor.", listOf("manjericao")),
        SpeciesInfo("Alecrim", "Salvia rosmarinus", 5, 200, LightNeed.SOL_PLENO,
            "Prefere solo bem drenado; evite encharcar.", listOf("rosmarinus")),
        SpeciesInfo("Hortelã", "Mentha spp.", 2, 200, LightNeed.MEIA_SOMBRA,
            "Gosta de solo sempre úmido.", listOf("hortela", "menta")),
        SpeciesInfo("Cebolinha", "Allium schoenoprasum", 3, 200, LightNeed.SOL_PLENO,
            "Mantenha o solo úmido nos dias quentes.", listOf("cebolinha verde")),
        SpeciesInfo("Lavanda", "Lavandula angustifolia", 6, 200, LightNeed.SOL_PLENO,
            "Tolera seca; regue quando o solo estiver seco.", listOf("alfazema")),
        SpeciesInfo("Rosa", "Rosa spp.", 3, 400, LightNeed.SOL_PLENO,
            "Regue profundamente na base, de preferência pela manhã.", listOf("roseira", "mini rosa")),
        SpeciesInfo("Tomate-cereja", "Solanum lycopersicum", 2, 500, LightNeed.SOL_PLENO,
            "Regas regulares evitam rachaduras nos frutos.", listOf("tomate", "tomate cereja")),
        SpeciesInfo("Bromélia", "Bromeliaceae", 7, 150, LightNeed.LUZ_INDIRETA,
            "Mantenha um pouco de água no copo central e troque com frequência.", listOf("bromelia")),
        SpeciesInfo("Dracena", "Dracaena fragrans", 10, 300, LightNeed.MEIA_SOMBRA,
            "Deixe a parte superior do solo secar entre as regas.", listOf("pau d'agua", "pau-d'água", "dracaena")),
        SpeciesInfo("Comigo-ninguém-pode", "Dieffenbachia seguine", 6, 300, LightNeed.MEIA_SOMBRA,
            "Tóxica para pets; mantenha o solo levemente úmido.", listOf("comigo ninguem pode", "dieffenbachia")),
        SpeciesInfo("Singônio", "Syngonium podophyllum", 5, 250, LightNeed.LUZ_INDIRETA,
            "Gosta de solo úmido, sem encharcar.", listOf("singonio", "syngonium"))
    )

    fun search(query: String, limit: Int = 6): List<SpeciesInfo> {
        val q = query.normalizedForSearch()
        if (q.isBlank()) return emptyList()
        return all.filter { info ->
            info.popularName.normalizedForSearch().contains(q) ||
                info.scientificName.normalizedForSearch().contains(q) ||
                info.aliases.any { it.normalizedForSearch().contains(q) }
        }.sortedBy { if (it.popularName.normalizedForSearch().startsWith(q)) 0 else 1 }
            .take(limit)
    }

    /** Busca exata (popular, científica ou apelido) para aplicar a sugestão automática. */
    fun find(name: String): SpeciesInfo? {
        val q = name.normalizedForSearch()
        if (q.isBlank()) return null
        return all.firstOrNull { info ->
            info.popularName.normalizedForSearch() == q ||
                info.scientificName.normalizedForSearch() == q ||
                info.aliases.any { it.normalizedForSearch() == q }
        }
    }

    /** Valores usados quando a espécie não está no catálogo. */
    val fallback = SpeciesInfo(
        popularName = "",
        scientificName = "",
        frequencyDays = 7,
        waterAmountMl = 250,
        light = LightNeed.LUZ_INDIRETA,
        tip = "Espécie fora do catálogo: comece com uma rega por semana e ajuste conforme a umidade do solo."
    )
}

val DEFAULT_ROOMS = listOf("Sala", "Quarto", "Cozinha", "Varanda", "Banheiro", "Escritório", "Jardim", "Área de serviço")
