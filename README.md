# Lingko Hijau — Aplikasi Mobile Konservasi & Pelaporan Lapangan (Android)

Proyek aplikasi Android fungsional berbasis **Kotlin & Jetpack Compose (Material 3)** yang dibangun langsung dari *UI/UX Blueprint Lingko Hijau*.

## Fitur Utama
1. **Onboarding & Pemilihan Peran (Tahap 1):** Mendukung 3 peran pengguna (*Petugas Lapangan*, *Relawan Konservasi*, dan *Mitra & Donor*) dengan penyimpanan lokal (`SharedPreferences`).
2. **Beranda & Metrik Dampak Real-Time:** Menampilkan capaian hektar lahan Lingko, total pohon tertanam yang otomatis bertambah dari laporan baru, serta progres 4 Pilar Program Kerja.
3. **Eksplorasi Peta Kerja Flores (Tahap 2):** Peta interaktif Kabupaten Manggarai (Ruteng, Cibal, Satar Mese, Reok) lengkap dengan filter wilayah, koordinat GPS, dan tombol pelaporan cepat per lokasi.
4. **Pelaporan Lapangan & Kamera (Tahap 3 & 4):** Formulir input catatan lapangan, jumlah bibit, integrasi kamera (`TakePicturePreview`), serta penyimpanan lokal *offline-first* dengan tombol sinkronisasi.
5. **Kolaborasi & Kemitraan:** Formulir pengajuan kemitraan donor/relawan serta manajemen ganti peran pengguna.

## Cara Build File `.apk` Otomatis di GitHub
1. Buat repository baru di GitHub (misalnya `lingko-hijau-android`).
2. Ekstrak seluruh isi file ZIP ini dan unggah ke branch `main`.
3. Buka tab **Actions** pada repository GitHub Anda, pilih workflow **Build Lingko Hijau Android APK**, dan tunggu hingga selesai (~2-3 menit).
4. Unduh file **`LingkoHijau-App-Debug.zip`** pada bagian **Artifacts** di halaman workflow tersebut untuk mendapatkan file `app-debug.apk` siap instal di HP Android.
