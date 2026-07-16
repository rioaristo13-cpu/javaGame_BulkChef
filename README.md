![Logo](assets/ui/skin/cover.png)

# BulkChef

Proyek [libGDX](https://libgdx.com/) yang dihasilkan dengan [gdx-liftoff](https://github.com/libgdx/gdx-liftoff).

Proyek ini dihasilkan dengan templat yang mencakup peluncur aplikasi sederhana dan ekstensi `ApplicationAdapter` yang menggambar logo libGDX.

## Platform

- `core`: Modul utama dengan logika aplikasi yang digunakan bersama oleh semua platform.

- `lwjgl3`: Platform desktop utama yang menggunakan LWJGL3; sebelumnya disebut 'desktop' dalam dokumentasi lama.

## Gradle

Proyek ini menggunakan [Gradle](https://gradle.org/) untuk mengelola dependensi.
Wrapper Gradle disertakan, sehingga Anda dapat menjalankan tugas Gradle menggunakan perintah `gradlew.bat` atau `./gradlew`.

Tugas dan flag Gradle yang berguna:

- `--continue`: saat menggunakan flag ini, kesalahan tidak akan menghentikan tugas berjalan.

- `--daemon`: berkat flag ini, daemon Gradle akan digunakan untuk menjalankan tugas yang dipilih.

- `--offline`: saat menggunakan flag ini, arsip dependensi yang di-cache akan digunakan.

- `--refresh-dependencies`: flag ini memaksa validasi semua dependensi. Berguna untuk versi snapshot.

- `build`: membangun sumber dan arsip setiap proyek.

- `cleanEclipse`: menghapus data proyek Eclipse.

- `cleanIdea`: menghapus data proyek IntelliJ.

- `clean`: menghapus folder `build`, yang menyimpan kelas yang dikompilasi dan arsip yang dibangun.

- `eclipse`: menghasilkan data proyek Eclipse.

- `idea`: menghasilkan data proyek IntelliJ.

- `lwjgl3:jar`: membangun file JAR aplikasi yang dapat dijalankan, yang dapat ditemukan di `lwjgl3/build/libs`.

- `lwjgl3:run`: memulai aplikasi.

- `test`: menjalankan pengujian unit (jika ada).

Perhatikan bahwa sebagian besar tugas yang tidak spesifik untuk satu proyek dapat dijalankan dengan awalan `name:`, di mana `name` harus diganti dengan ID proyek tertentu.
Misalnya, `core:clean` hanya menghapus folder `build` dari proyek `core`.
