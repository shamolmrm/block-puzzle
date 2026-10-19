# R8 rules for release builds. Libraries (Hilt, Room, Firebase, Ads, Billing, Compose,
# kotlinx.serialization) ship their own consumer rules; only app-specific needs live here.

# Readable crash stack traces (Crashlytics uploads the mapping file when Firebase is configured).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# kotlinx.serialization: saved games, level packs and type-safe navigation routes are decoded
# reflectively through their generated serializers. Keep the serializer lookups for our classes.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class com.puzzleblockblast.game.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.puzzleblockblast.game.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.puzzleblockblast.game.**$$serializer { *; }
