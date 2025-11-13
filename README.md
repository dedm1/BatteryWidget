# Battery Widget

Виджеты для Samsung устройств: системный статус, дата и время.

## Лицензия

Этот проект распространяется под лицензией [MIT License](LICENSE).

## ИИ
Написано с использование ии

### Версионирование

Версия приложения задается в `app/build.gradle.kts`:
- `versionCode` — числовой код версии (увеличивать при каждом релизе)
- `versionName` — строковое имя версии (например, "0.0.4")

```kotlin
defaultConfig {
    versionCode = 4
    versionName = "0.0.4"
}
```

## Сборка APK

### Debug режим (для разработки)

Сборка debug APK без минификации и оптимизаций:

```bash
./gradlew assembleDebug
```

APK будет в: `app/build/outputs/apk/debug/app-debug.apk`

Установка на устройство:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Release режим (production, минифицированный)

Сборка оптимизированного release APK с минификацией и сжатием ресурсов:

```bash
./gradlew clean assembleRelease
```

APK будет в: `app/build/outputs/apk/release/app-release.apk`

Установка на устройство:
```bash
adb install app/build/outputs/apk/release/app-release.apk
```

### Тесты и проверка кода

```bash
./gradlew detekt
```  