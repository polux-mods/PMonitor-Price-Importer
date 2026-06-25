# PMonitor Importer

Android/Kotlin програма для імпорту цін конкурентів у Excel-файл моніторингу.

## Що вже реалізовано

- WebView-вибір магазину/міста для `АТБ`, `Сільпо`, `Єва`, `Аврора`: відкриває сайт конкурента всередині програми, зберігає cookies вибраного магазину і додає їх до пошукових запитів імпортера.
- Відкриття `.xlsx` і `.xls` через системний вибір файлу Android.
- Автоматичне визначення колонок:
  - `Товар`
  - `Штрих-код`
  - `ТМ`
  - `Артикул`
  - `Конкурент`
  - `Цена конкурента`
  - `Акция`
- Формування списку конкурентів з Excel-файлу.
- Підтримувані конкуренти: `АТБ`, `Сільпо`, `Єва`, `Аврора`.
- Імпорт ціни в колонку `Цена конкурента`.
- Якщо знайдена акція — запис `Да` в колонку `Акция`, інакше `Нет`.
- Кеш незбереженого результату: після імпорту файл зберігається у кеші, а при наступному вході програма пропонує відкрити останній незбережений імпорт.
- Кнопка `Зберегти файл` після імпорту.
- GitHub Actions workflow для автоматичної збірки APK.

## Важливо про точність

Абсолютно безпомилкового пошуку товарів не існує, бо назви на сайтах конкурентів можуть змінюватись, а сайти можуть повертати неповні або регіональні ціни. У програмі вже є інтелектуальний matcher:

- точний збіг штрих-коду, якщо він є;
- порівняння ваги/об'єму/кількості;
- порівняння бренду;
- токенний збіг назв;
- Levenshtein-схожість;
- відсікання товарів з іншою вагою/об'ємом;
- поріг впевненості перед записом у таблицю.

Якщо збіг сумнівний, програма не записує ціну і додає рядок у лог `Не впевнений збіг`.

## Важливо про сайти конкурентів

У проєкті є WebView-екран для вибору магазину/міста і готові HTML-адаптери для:

- `АТБ` — `https://www.atbmarket.com/catalog/search?query=...`
- `Сільпо` — `https://silpo.ua/search?find=...`
- `Єва` — `https://eva.ua/ua/search/?q=...`
- `Аврора` — `https://avrora.ua/ua/search/?q=...`

Перед імпортом бажано в додатку натиснути `Вибрати магазин` для потрібного конкурента, вибрати магазин на сайті й натиснути `Готово`. Якщо сайт змінить пошук або почне віддавати товари тільки через внутрішній API, потрібно оновити відповідний адаптер у папці:

```text
app/src/main/java/ua/vitaliyshkarupa/pmonitorimporter/competitors/
```

Найкращий варіант для стабільності — замінити HTML-парсинг на офіційні або знайдені API-ендпоінти конкурентів.

## Як залити на GitHub

1. Створи новий репозиторій на GitHub.
2. Завантаж усі файли з цього проєкту в корінь репозиторію.
3. Зроби commit у гілку `main`.
4. Відкрий вкладку **Actions**.
5. Дочекайся workflow **Build Android APK**.
6. У результаті workflow відкрий **Artifacts** і скачай `PMonitorImporter-debug-apk`.

APK буде тут:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Локальна збірка

Якщо на ПК встановлений Android Studio або Gradle:

```bash
gradle :app:assembleDebug
```

## Перевірка на твоєму файлі

У прикладеному файлі `АТ - 22.06.2026.xlsx` знайдено 2096 товарних рядків. Конкуренти з файлу:

- `Сільпо` — 945 рядків
- `АТБ` — 935 рядків
- `Єва` — 216 рядків
- `Аврора` — у цьому файлі не знайдено

## Структура проєкту

```text
PMonitorPriceImporter/
├── .github/workflows/android-apk.yml
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
├── README.md
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/values/strings.xml
        ├── res/values/styles.xml
        └── java/ua/vitaliyshkarupa/pmonitorimporter/
            ├── MainActivity.kt
            ├── MainViewModel.kt
            ├── PMonitorApp.kt
            ├── cache/CacheService.kt
            ├── competitors/
            │   ├── AtbAdapter.kt
            │   ├── AvroraAdapter.kt
            │   ├── CompetitorAdapter.kt
            │   ├── CompetitorRegistry.kt
            │   ├── CompetitorWebConfig.kt
            │   ├── EvaAdapter.kt
            │   ├── GenericHtmlAdapter.kt
            │   ├── HtmlProductExtractor.kt
            │   ├── PriceImportRepository.kt
            │   └── SilpoAdapter.kt
            ├── excel/
            │   ├── ExcelService.kt
            │   └── LoadedWorkbook.kt
            ├── matcher/ProductMatcher.kt
            ├── model/
            │   ├── ExcelColumns.kt
            │   ├── ExcelProduct.kt
            │   ├── ImportLogItem.kt
            │   ├── ImportStats.kt
            │   ├── ProductAmount.kt
            │   ├── WebProductCandidate.kt
            │   └── WorkbookSession.kt
            ├── store/
            │   └── StoreSessionService.kt
            ├── ui/
            │   ├── AppUiState.kt
            │   ├── PMonitorAppScreen.kt
            │   └── PMonitorTheme.kt
            └── util/
                ├── ProductTextParser.kt
                └── StringSimilarity.kt
```
