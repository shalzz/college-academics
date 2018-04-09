# autovalue gson extension
# Retain generated classes that end in the suffix
-keepnames class **_GsonTypeAdapter

# Prevent obfuscation of types which use @GenerateTypeAdapter since the simple name
# is used to reflectively look up the generated adapter.
-keepnames @com.ryanharter.auto.value.gson.GenerateTypeAdapter class *

 # See https://github.com/rharter/auto-value-gson/issues/162
-dontwarn com.ryanharter.auto.value.gson.GenerateTypeAdapter
-dontwarn com.google.gson.TypeAdapterFactory