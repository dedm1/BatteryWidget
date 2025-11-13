# Battery Widget

Виджеты для Samsung устройств: системный статус, дата и время.

## Лицензия

Этот проект распространяется под лицензией [MIT License](LICENSE).

## Проект

Минимальный проект на Kotlin/Compose под Android 14+, стартовая точка для разработки виджета батареи.

**Размер production APK:** ~3 МБ (оптимизированный release build)

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

**Оптимизации release сборки:**
- ✅ Минификация кода (R8/ProGuard)
- ✅ Удаление неиспользуемых ресурсов
- ✅ Оптимизация размера (debugSymbolLevel = SYMBOL_TABLE)
- ✅ Автоматическая подпись через debug keystore (для локальной установки)

> **Примечание:** Для публикации в Google Play используй `./gradlew bundleRelease` для создания AAB файла. AAB будет еще меньше (~11 МБ), так как Google Play генерирует оптимизированные APK для каждого устройства.

### Тесты и проверка кода

```bash
./gradlew detekt
```  


> Примечание: команды предполагают, что Android SDK/ADB уже установлены и прописаны в PATH.

Дальнейшие задачи — добавить Glance-виджет и логику отображения температуры аккумулятора.

