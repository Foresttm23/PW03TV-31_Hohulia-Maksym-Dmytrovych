# ЗВІТ З ПРАКТИЧНИХ РОБІТ №5, №6, №7, №8

**Тема проєкту:** Гідроелектростанція з контролем рівня води  
**Предмет:** Розробка мобільних застосунків на Jetpack Compose (Android)  
**Розробник:** Оператор / Студент  

---

## Вступ

У даному звіті представлено результати виконання практичних робіт №5, №6, №7 та №8. Головна мета розробленого застосунку — створення комплексної автоматизованої системи диспетчеризації та контролю параметрів водного балансу, затворів холостого скиду та стану водосховища гідроелектростанції (ГЕС).

Застосунок розроблено мовою **Kotlin** із використанням сучасного фреймворку **Jetpack Compose**, архітектури **MVVM (Model-View-ViewModel)**, реактивних потоків **StateFlow**, компонентів навігації **Navigation Compose** та локальної бази даних **Room Database**.

---

## 1. Практична робота №5: Навігація та адаптивні layouts (Task 5)

### 1.1. Що було зроблено та як
- **Базова та розширена навігація:** Реалізовано декларативну навігацію за допомогою `NavController` та `NavHost`. Налаштовано стартовий маршрут `"main_menu"` та параметризовані маршрути `"gate_detail/{gateId}/{waterLevel}/{isOpen}/{openingPct}"` і `"reservoir_config/{currentThreshold}"`.
- **Передача даних між екранами:** Реалізовано передачу аргументів різних типів (`NavType.IntType`, `NavType.FloatType`, `NavType.BoolType`) через самі URL-маршрути та отримання їх у `backStackEntry.arguments`.
- **Динамічне оновлення UI:** На екрані налаштування порогу тривоги водосховища (`ReservoirConfigScreen`) через слайдер змінюється значення, яке при поверненні на головний екран (`popBackStack()`) миттєво оновлює стан порогу у головному меню.
- **Розділення на Composable-функції:** UI повністю структуровано на окремі модульні функції: `HeaderTitleSection`, `ThresholdStatusCard`, `GateStatusCard`, `GateDetailScreen`, `ReservoirConfigScreen`.
- **Адаптивний Layout:** Використано `BoxWithConstraints` для перевірки ширини екрана (`maxWidth`). Якщо ширина перевищує `600.dp` (планшет або альбомна орієнтація), затвори розташовуються горизонтально у `Row`, інакше — вертикально у `Column`.

---

### 1.2. Інструкція для додавання скріншотів (Практична 5)
> **Як зробити скріншоти:**  
> 1. Запустіть застосунок на емуляторі чи пристрої та відкрийте вкладку **"Task 5: Навігація"**.  
> 2. **Скріншот 5.1:** Головний екран меню диспетчера з переліком затворів шлюзів ГЕС та карткою порогу тривоги.  
> 3. **Скріншот 5.2:** Натисніть на один із затворів (наприклад, "Шлюз №1") — зробіть скріншот екрана деталей `GateDetailScreen` з переданими аргументами.  
> 4. **Скріншот 5.3:** Поверніться назад та натисніть "Налаштувати" порогу тривоги — зробіть скріншот екрана `ReservoirConfigScreen` зі слайдером.

*(Вставте скріншоти нижче)*
- `[Скріншот 5.1: Головне меню диспетчера шлюзів]`
- `[Скріншот 5.2: Екран деталей затвора шлюзу]`
- `[Скріншот 5.3: Екран налаштування порогу водосховища]`

---

### 1.3. Основний код таски (Практична 5)

```kotlin
// Файл: HydroNavMenuScreen.kt (фрагмент навігації та головного меню)
@Composable
fun HydroNavMenuScreen(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    var alertThreshold by remember { mutableFloatStateOf(122.0f) }

    val gates = remember {
        listOf(
            GateInfo(1, "Шлюз №1 (Основний скид)", 121.2f, true, 75),
            GateInfo(2, "Шлюз №2 (Резервний)", 123.8f, false, 0),
            GateInfo(3, "Шлюз №3 (Аварійний)", 122.4f, true, 40)
        )
    }

    Surface(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "main_menu"
        ) {
            composable("main_menu") {
                HydroMainMenuScreen(
                    navController = navController,
                    gates = gates,
                    alertThreshold = alertThreshold
                )
            }

            composable(
                route = "gate_detail/{gateId}/{waterLevel}/{isOpen}/{openingPct}",
                arguments = listOf(
                    navArgument("gateId") { type = NavType.IntType },
                    navArgument("waterLevel") { type = NavType.FloatType },
                    navArgument("isOpen") { type = NavType.BoolType },
                    navArgument("openingPct") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val gateId = backStackEntry.arguments?.getInt("gateId") ?: 1
                val waterLevel = backStackEntry.arguments?.getFloat("waterLevel") ?: 120.0f
                val isOpen = backStackEntry.arguments?.getBoolean("isOpen") ?: false
                val openingPct = backStackEntry.arguments?.getInt("openingPct") ?: 0

                GateDetailScreen(
                    navController = navController,
                    gateId = gateId,
                    waterLevel = waterLevel,
                    isOpen = isOpen,
                    openingPercentage = openingPct
                )
            }

            composable(
                route = "reservoir_config/{currentThreshold}",
                arguments = listOf(navArgument("currentThreshold") { type = NavType.FloatType })
            ) { backStackEntry ->
                val current = backStackEntry.arguments?.getFloat("currentThreshold") ?: 122.0f
                ReservoirConfigScreen(
                    navController = navController,
                    initialThreshold = current,
                    onSaveThreshold = { alertThreshold = it }
                )
            }
        }
    }
}
```

---

## 2. Практична робота №6: Інтерактивні форми, валідація та підказки (Task 6)

### 2.1. Що було зроблено та як
- **Форма введення показників:** Розроблено інтерактивну форму для розрахунку водного балансу ГЕС з чотирма полями введення `OutlinedTextField`: приплив води $Q_{in}$ (м³/с), витрата турбін $Q_{turb}$ (м³/с), холостий скид $Q_{spill}$ (м³/с) та поточний рівень водосховища $H$ (м).
- **Валідація та обробка помилок:** Реалізовано функцію `validateAndCalculate()`, яка перевіряє введені значення на числову коректність, відсутність від'ємних значень та допустимий діапазон рівня (100–130 м). У разі помилки користувачеві виводиться червоне повідомлення під відповідним текстовим полем (`supportingText`), а поле підсвічується (`isError = true`).
- **Підказки та стилізація:** Додано статичні та динамічні підказки для кожного поля, використано обмеження клавіатури `KeyboardOptions(keyboardType = KeyboardType.Number)`.
- **Бізнес-логіка та реактивний UI:** Логіка обчислення водного балансу винесена в об'єкт `HydroOperationsLogic.calculateWaterBalance()`. У разі виникнення загрози переповнення динамічно з'являється анімований аварійний баннер `EmergencyWarningBanner`. Результати виводяться у розрахованих метричних картках `MetricCard`.

---

### 2.2. Інструкція для додавання скріншотів (Практична 6)
> **Як зробити скріншоти:**  
> 1. Відкрийте вкладку **"Task 6: Баланс води"**.  
> 2. **Скріншот 6.1:** Форма з заповненими нормальними даними (наприклад: Приплив 520, Турбіни 320, Скид 50, Рівень 122.8) та виведеними картками результатів.  
> 3. **Скріншот 6.2:** Введіть некоректні дані (наприклад, букви або від'ємне число) та натисніть "Перевірити та розрахувати баланс" — зробіть скріншот виведених помилок валідації під полями та аварійного баннера.

*(Вставте скріншоти нижче)*
- `[Скріншот 6.1: Форма розрахунку водного балансу ГЕС з коректними даними]`
- `[Скріншот 6.2: Демонстрація помилок валідації та аварійного баннера]`

---

### 2.3. Основний код таски (Практична 6)

```kotlin
// Файл: HydroWaterControlScreen.kt (фрагмент валідації та форми)
@Composable
fun HydroWaterControlScreen(modifier: Modifier = Modifier) {
    var inflowInput by remember { mutableStateOf("520.0") }
    var turbineInput by remember { mutableStateOf("320.0") }
    var spillwayInput by remember { mutableStateOf("50.0") }
    var currentLevelInput by remember { mutableStateOf("122.8") }

    var inflowError by remember { mutableStateOf<String?>(null) }
    var turbineError by remember { mutableStateOf<String?>(null) }
    var spillwayError by remember { mutableStateOf<String?>(null) }
    var levelError by remember { mutableStateOf<String?>(null) }

    var balanceResult by remember {
        mutableStateOf(
            HydroOperationsLogic.calculateWaterBalance(
                inflowM3PerSec = 520.0,
                turbineFlowM3PerSec = 320.0,
                spillwayFlowM3PerSec = 50.0,
                currentWaterLevelMeters = 122.8
            )
        )
    }

    fun validateAndCalculate() {
        val inVal = inflowInput.toDoubleOrNull()
        val turbVal = turbineInput.toDoubleOrNull()
        val spillVal = spillwayInput.toDoubleOrNull()
        val lvlVal = currentLevelInput.toDoubleOrNull()

        inflowError = when {
            inVal == null -> "Введіть числове значення припливу"
            inVal < 0 -> "Приплив не може бути від'ємним"
            else -> null
        }
        turbineError = when {
            turbVal == null -> "Введіть числове значення витрати турбін"
            turbVal < 0 -> "Витрата турбін не може бути від'ємною"
            else -> null
        }
        spillwayError = when {
            spillVal == null -> "Введіть числове значення холостого скиду"
            spillVal < 0 -> "Скидання не може бути від'ємним"
            else -> null
        }
        levelError = when {
            lvlVal == null -> "Введіть рівень водосховища"
            lvlVal !in 100.0..130.0 -> "Рівень має бути в межах 100-130 м"
            else -> null
        }

        if (inflowError == null && turbineError == null && spillwayError == null && levelError == null) {
            balanceResult = HydroOperationsLogic.calculateWaterBalance(
                inflowM3PerSec = inVal!!,
                turbineFlowM3PerSec = turbVal!!,
                spillwayFlowM3PerSec = spillVal!!,
                currentWaterLevelMeters = lvlVal!!
            )
        }
    }

    // UI розкладка з викликом WaterBalanceInputForm та WaterBalanceMetricsGrid...
}
```

---

## 3. Практична робота №7: Архітектурний паттерн MVVM (ViewModel & StateFlow) (Task 7)

### 3.1. Що було зроблено та як
- **Архітектура MVVM:** Реалізовано чітке розділення відповідальності між UI та бізнес-логікою станції. Створено `HydroSpillwayViewModel`, яка успадковується від Android Jetpack `ViewModel`.
- **Управління станом через StateFlow:** Стан UI описано immutable дата-класом `HydroSpillwayUiState`. Стан зберігається у приватній `MutableStateFlow` та надається назовні як `StateFlow`. UI підписується на потік через `viewModel.uiState.collectAsState()`.
- **Регулювання затворів ГЕС:** Надано можливість операторові індивідуально регулювати відсоток відкриття 4 незалежних холостих затворів за допомогою `Slider` (від 0% до 100%).
- **Аварійний протокол:** Реалізовано функцію `toggleEmergencyRelease()`, яка миттєво переводить всі 4 затвори у стан 100% відкриття при загрозі затоплення або закриває їх при скасуванні аварії.
- **Динамічний перерахунок у ViewModel:** Кожна зміна слайдера тригерить `recalculateFlow()`, що автоматично обчислює сумарне холосте скидання та аналізує статус безпеки нижнього б'єфу річки.

---

### 3.2. Інструкція для додавання скріншотів (Практична 7)
> **Як зробити скріншоти:**  
> 1. Відкрийте вкладку **"Task 7: Затвори"**.  
> 2. **Скріншот 7.1:** Звичайне планове регулювання затворів (наприклад, Затвор 1 на 30%, Затвор 3 на 50%) із відображеним сумарним скидом та статусом нижнього б'єфу.  
> 3. **Скріншот 7.2:** Натисніть кнопку "АКТИВУВАТИ МАКСИМАЛЬНЕ ХОЛОСТЕ СКИДАННЯ (100%)" — зробіть скріншот червого аварійного блоку та повністю відкритих затворів.

*(Вставте скріншоти нижче)*
- `[Скріншот 7.1: Регулювання затворів шлюзу через StateFlow у звичайному режимі]`
- `[Скріншот 7.2: Активований аварійний режим максимального скидання води]`

---

### 3.3. Основний код таски (Практична 7)

```kotlin
// Файл: HydroSpillwayViewModel.kt
class HydroSpillwayViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HydroSpillwayUiState())
    val uiState: StateFlow<HydroSpillwayUiState> = _uiState.asStateFlow()

    fun updateGate1(opening: Float) {
        _uiState.update { it.copy(gate1Opening = opening) }
        recalculateFlow()
    }

    fun updateGate2(opening: Float) {
        _uiState.update { it.copy(gate2Opening = opening) }
        recalculateFlow()
    }

    fun toggleEmergencyRelease() {
        _uiState.update { current ->
            val newEmergencyState = !current.isEmergencyActive
            if (newEmergencyState) {
                current.copy(
                    isEmergencyActive = true,
                    gate1Opening = 100f, gate2Opening = 100f, gate3Opening = 100f, gate4Opening = 100f
                )
            } else {
                current.copy(
                    isEmergencyActive = false,
                    gate1Opening = 0f, gate2Opening = 0f, gate3Opening = 0f, gate4Opening = 0f
                )
            }
        }
        recalculateFlow()
    }

    private fun recalculateFlow() {
        _uiState.update { current ->
            val totalFlow = ((current.gate1Opening + current.gate2Opening + current.gate3Opening + current.gate4Opening) / 100.0 * 250.0)
            val safety = HydroOperationsLogic.calculateDownstreamRiverLevel(totalFlow)
            current.copy(
                totalSpillwayFlowM3 = totalFlow,
                downstreamSafetyStatus = safety
            )
        }
    }
}
```

---

## 4. Практична робота №8: Локальне збереження даних у Room Database (Task 8)

### 4.1. Що було зроблено та як
- **Інтеграція Room Database:** Створено структуру локального збереження даних спостережень оператора ГЕС:
  - `@Entity class HydroRecordEntity`: таблиця записів показників (імплементує ID, timestamp, рівень води, згенеровану потужність МВт, витрату м³/с, режим ГЕС, примітки).
  - `@Dao interface HydroRecordDao`: методи для запиту `getAllRecords(): Flow<List<HydroRecordEntity>>`, вставки `insertRecord` та видалення `deleteRecordById`.
  - `@Database abstract class HydroDatabase`: головний клас Room DB із синглтон-ініціалізацією `getInstance(context)`.
- **ViewModel із Room Integration:** Створено `HydroOperationsViewModel` (успадкована від `AndroidViewModel`), що зв'язує Room DAO та UI через реактивний потік `StateFlow<List<HydroRecordEntity>>` з використанням `stateIn(viewModelScope, ...)`.
- **Функціонал Центру Диспетчеризації:** Оператор може додавати нові примітки та натискати кнопку "Зберегти показники у Room DB". Список миттєво реагує та виводить збережені записи у картках із форматованим часом `SimpleDateFormat`. Також реалізовано можливість вилучення окремого запису або очищення всього журналу.
- **Адаптивний UI та композиція:** Для забезпечення адаптивності компонування під різні розміри екранів використано `BoxWithConstraints` у `TelemetryOverviewGrid`. При ширині екрана `maxWidth > 500.dp` картки показників розміщуються в `Row`, а при вужчому екрані — вертикально у `Column`.
- **Генерація офіційного звіту:** Реалізовано функцію генерації форматованого підсумкового текстового звіту чергового диспетчера ГЕС із часовою міткою, ім'ям оператора, режимом роботи та кількістю збережених записів.

---

### 4.2. Інструкція для додавання скріншотів (Практична 8)
> **Як зробити скріншоти:**  
> 1. Відкрийте вкладку **"Task 8: Диспетчер"**.  
> 2. **Скріншот 8.1:** Напишіть примітку у текстове поле (наприклад "Штатний моніторинг") та натисніть "Зберегти показники у Room DB" — зробіть скріншот журналу з доданими записами з Room DB.  
> 3. **Скріншот 8.2:** Натисніть кнопку "Сформувати підсумковий звіт" — зробіть скріншот згенерованого текстового блоку звіту диспетчера ГЕС.

*(Вставте скріншоти нижче)*
- `[Скріншот 8.1: Журнал спостережень з даними, збереженими у Room DB]`
- `[Скріншот 8.2: Згенерований офіційний звіт диспетчера ГЕС]`

---

### 4.3. Основний код таски (Практична 8)

```kotlin
// Файл: HydroOperationsViewModel.kt
class HydroOperationsViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = HydroDatabase.getInstance(application).hydroRecordDao()

    val savedRecords: StateFlow<List<HydroRecordEntity>> = dao.getAllRecords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveCurrentMetrics(
        waterLevel: Float,
        powerMw: Float,
        flowRate: Float,
        mode: HydroOperatingMode,
        notes: String
    ) {
        viewModelScope.launch {
            val record = HydroRecordEntity(
                waterLevelMeters = waterLevel,
                generatedPowerMw = powerMw,
                waterFlowRateM3PerSec = flowRate,
                operatingMode = mode.title,
                operatorNotes = notes.ifBlank { "Штатний моніторинг оператора" }
            )
            dao.insertRecord(record)
        }
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            dao.deleteRecordById(id)
        }
    }

    fun clearAllRecords() {
        viewModelScope.launch {
            dao.clearAllRecords()
        }
    }
}
```

---

## Висновок

У ході виконання практичних робіт №5–8 було повністю реалізовано багатофункціональну автоматизовану систему диспетчеризації та контролю гідроелектростанції (ГЕС) на базі сучасної технології **Jetpack Compose**.

**Основні досягнуті результати:**
1. **Багатоекранна Навігація (Практична 5):** Реалізовано гнучку та надійну навігацію за допомогою `NavController`/`NavHost` із передачею різнотипних аргументов та динамічним оновленням UI. UI розроблено з урахуванням адаптивності для різних розмірів екранів та орієнтацій.
2. **Форми та Валідація (Практична 6):** Створено зручну форму для розрахунку параметрів водного балансу ГЕС з повноцінною валідацією полів введення, виведенням підказок, повідомлень про помилки та анімованих застережних баннерів.
3. **Архітектура MVVM та Реактивність (Практична 7):** Застосовано архітектурний шаблон **MVVM**. Реалізовано управління станом холостих затворів через `ViewModel` та реактивні потоки `StateFlow`, що забезпечило ізоляцію UI від бізнес-логіки та миттєвий відгук на події регулювання й аварійні ситуації.
4. **Локальне Збереження у Room DB (Практична 8):** Налаштовано повноцінну локальну базу даних **Room Database** для збереження журналів спостережень оператора ГЕС з підтримкою асинхронного зчитування у `StateFlow`, видалення записів та генерації підсумкових звітів.

Побудована архітектура є легко масштабованою, чистою та відповідає сучасним рекомендаціям Android Development Best Practices.
