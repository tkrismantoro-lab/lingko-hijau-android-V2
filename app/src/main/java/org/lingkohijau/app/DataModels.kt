package org.lingkohijau.app

enum class UserRole(val title: String, val subtitle: String, val badge: String) {
    PETUGAS("Petugas Lapangan", "Input monitoring bibit, patroli hutan & geotagging", "FIELD OPS"),
    RELAWAN("Relawan Konservasi", "Dokumentasi kegiatan komunitas & edukasi desa", "VOLUNTEER"),
    MITRA("Mitra & Donor", "Pantau metrik dampak real-time & ajukan kolaborasi", "PARTNER")
}

data class SiteLocation(
    val id: String,
    val name: String,
    val district: String,
    val program: String,
    val areaHa: Int,
    val treesPlanted: Int,
    val status: String,
    val coordinates: String,
    val mapXFraction: Float,
    val mapYFraction: Float,
    val description: String
)

data class FieldReport(
    val id: String,
    val title: String,
    val siteName: String,
    val category: String,
    val notes: String,
    val reporterName: String,
    val reporterRole: String,
    val timestamp: String,
    val treesCount: Int,
    val hasPhoto: Boolean,
    val isSynced: Boolean = true
)

data class ProgramItem(
    val title: String,
    val tag: String,
    val progress: Float,
    val summary: String,
    val targetInfo: String
)

object LingkoRepository {
    val defaultSites = listOf(
        SiteLocation(
            id = "ruteng",
            name = "Lingko Cara Cancar",
            district = "Ruteng",
            program = "Ketahanan Pangan & Agroforestri",
            areaHa = 420,
            treesPlanted = 38500,
            status = "Aktif • Fase Panen",
            coordinates = "-8.6042, 120.3821",
            mapXFraction = 0.34f,
            mapYFraction = 0.42f,
            description = "Sistem pembagian lahan adat Lodok berbentuk jaring laba-laba dengan integrasi kopi arabika naungan dan tanaman pangan lokal."
        ),
        SiteLocation(
            id = "cibal",
            name = "Koridor Hutan Pagal",
            district = "Cibal",
            program = "Konservasi Hutan & Biodiversitas",
            areaHa = 680,
            treesPlanted = 64200,
            status = "Aktif • Penanaman",
            coordinates = "-8.5120, 120.4615",
            mapXFraction = 0.54f,
            mapYFraction = 0.28f,
            description = "Restorasi zona penyangga mata air dan habitat burung endemik Flores bersama kelompok tani hutan adat."
        ),
        SiteLocation(
            id = "satarmese",
            name = "Wae Rebo & Ulumbu",
            district = "Satar Mese",
            program = "Energi Terbarukan & Pendidikan",
            areaHa = 310,
            treesPlanted = 24800,
            status = "Monitoring Rutin",
            coordinates = "-8.7689, 120.2840",
            mapXFraction = 0.25f,
            mapYFraction = 0.72f,
            description = "Pemberdayaan mikrohidro komunitas, sekolah alam desa, dan pelestarian hutan bambu adat."
        ),
        SiteLocation(
            id = "reok",
            name = "Pesisir Mangrove Torong Besi",
            district = "Reok",
            program = "Penguatan Masyarakat Adat",
            areaHa = 245,
            treesPlanted = 52000,
            status = "Aktif • Pembibitan",
            coordinates = "-8.2891, 120.4712",
            mapXFraction = 0.68f,
            mapYFraction = 0.18f,
            description = "Rehabilitasi sabuk hijau pesisir utara Manggarai dan penguatan ekonomi silvofishery warga."
        )
    )

    val programs = listOf(
        ProgramItem(
            title = "Ketahanan Pangan & Agroforestri",
            tag = "Pilar 1 • Lodok Adat",
            progress = 0.84f,
            summary = "Optimalisasi pertanian pola jaring laba-laba (Lingko) dengan tumpang sari pangan lokal & kopi.",
            targetInfo = "420 / 500 Ha Terkelola"
        ),
        ProgramItem(
            title = "Konservasi Hutan & Biodiversitas",
            tag = "Pilar 2 • Mata Air",
            progress = 0.76f,
            summary = "Perlindungan daerah tangkapan air pegunungan Ruteng & pemantauan satwa endemik.",
            targetInfo = "64.200 / 85.000 Pohon"
        ),
        ProgramItem(
            title = "Energi Terbarukan & Pendidikan",
            tag = "Pilar 3 • Desa Mandiri",
            progress = 0.68f,
            summary = "Instalasi energi bersih skala desa dan kurikulum literasi ekologi untuk generasi muda Flores.",
            targetInfo = "14 / 20 Desa Dampingan"
        ),
        ProgramItem(
            title = "Penguatan Masyarakat Adat",
            tag = "Pilar 4 • Tata Kelola",
            progress = 0.91f,
            summary = "Pemetaan partisipatif wilayah adat dan penguatan kelembagaan tua-tua adat (Tu'a Golo).",
            targetInfo = "22 Komunitas Terpetakan"
        )
    )

    val initialReports = listOf(
        FieldReport(
            id = "RPT-001",
            title = "Monitoring Bibit Kopi & Pohon Pelindung",
            siteName = "Lingko Cara Cancar (Ruteng)",
            category = "Agroforestri",
            notes = "Tingkat hidup bibit mencapai 94% pasca musim hujan awal. Irigasi subak adat berjalan lancar.",
            reporterName = "Yohanes Jehamat",
            reporterRole = "Petugas Lapangan",
            timestamp = "Hari ini, 09:40 WITA",
            treesCount = 350,
            hasPhoto = true,
            isSynced = true
        ),
        FieldReport(
            id = "RPT-002",
            title = "Patroli Mata Air Wae Laku",
            siteName = "Koridor Hutan Pagal (Cibal)",
            category = "Konservasi Hutan",
            notes = "Debit mata air stabil. Ditemukan 3 sarang burung Serindit Flores di zona inti.",
            reporterName = "Maria Nggong",
            reporterRole = "Relawan Konservasi",
            timestamp = "Kemarin, 15:20 WITA",
            treesCount = 120,
            hasPhoto = true,
            isSynced = true
        )
    )
}
