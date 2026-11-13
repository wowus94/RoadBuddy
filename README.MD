# RoadBuddy

**RoadBuddy** — кроссплатформенное приложение взаимопомощи на дороге. Пользователи публикуют запросы о помощи (эвакуатор, топливо, аккумулятор, колесо), а другие участники сообщества могут взять запрос в работу и помочь.

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%20%7C%20iOS-blue" alt="Platform"/>
  <img src="https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF" alt="Kotlin Multiplatform"/>
  <img src="https://img.shields.io/badge/UI-Compose%20Multiplatform-4285F4" alt="Compose Multiplatform"/>
</p>

---

## Возможности

### Запросы о помощи
- Создание заявки с типом проблемы: **эвакуатор, топливо, аккумулятор, колесо, прочее**
- Привязка к геолокации через карту или кнопку «рядом со мной»
- Жизненный цикл заявки: `OPEN → IN_PROGRESS → COMPLETED / CANCELLED`
- Чужой запрос можно «взять в работу» — вы становитесь помощником
- Редактирование и удаление собственных заявок

### Каталог и поиск
- Пагинированный список всех заявок (подгрузка по 20)
- Фильтры по типу и статусу, сортировка новые/старые
- Гео-фильтр «рядом»: радиус в км от выбранной или текущей точки
- Карта всех заявок с **кластеризацией маркеров** (geohash)

### Аккаунт
- Регистрация и вход по email/паролю (Firebase Authentication)
- Сброс пароля
- Экран «Мои заявки» с фильтрами и сортировкой
- Локализация интерфейса: **русский / английский**

### Офлайн-кэш
- Локальная база **Room** — единый источник данных для UI
- Интерфейс мгновенно отображает кэш и обновляется реактивно
- Firestore используется для синхронизации кэша и выполнения мутаций
- Работает офлайн: список и детали читаются из кэша без сети

---

## Стек

| Слой | Технологии |
|---|---|
| UI | Jetpack Compose Multiplatform, Material 3, Navigation 3 |
| Локальные данные | Room (KMP) + SQLite Bundled |
| Удалённые данные | Firebase Authentication, Cloud Firestore (GitLive) |
| Карты | Yandex MapKit (Android), Apple MapKit (iOS) |
| Геолокация | expect/actual LocationProvider |
| DI | Koin |
| Асинхронность | Kotlin Coroutines, Flow, StateFlow |
| Сеть | Ktor Client (Coil, загрузка изображений) |
| Архитектура | Clean Architecture + MVI-подобные ViewModel'и |

---

## Архитектура

```
┌─────────────────────────────────────────────────────┐
│                    Presentation                      │
│   Screens + ViewModels (StateFlow, UiState)         │
├─────────────────────────────────────────────────────┤
│                      Domain                         │
│   UseCases · Repository interfaces · Models         │
├─────────────────────────────────────────────────────┤
│                       Data                          │
│  RepositoryImpl → Room (UI source of truth)         │
│                → Firestore (sync + mutations)       │
└─────────────────────────────────────────────────────┘
```

### Поток данных (Room-first)

```
Firestore ──sync──▶ Room ──Flow──▶ Repository ──▶ UseCase ──▶ ViewModel ──▶ UI
```

- **Чтение** — только из Room: `Flow` эмитит изменения, UI обновляется автоматически
- **Синхронизация** — `refreshRequests()` / `refreshRequest(id)` тянут данные из Firestore и атомарно заменяют кэш (`@Transaction`)
- **Мутации** (create/update/delete/status) — write-through: Firestore → кэш Room → Flow обновляет UI
- **Пагинация** — офсетная (`LIMIT/OFFSET`) с фильтрами и сортировкой на уровне SQL

### Структура проекта

```
RoadBuddy/
├── sharedUI/                  # Compose Multiplatform модуль (Android + iOS)
│   └── src/
│       ├── commonMain/        # общий код: UI, domain, data, DI
│       ├── androidMain/       # Yandex MapKit, Android Room builder
│       ├── iosMain/           # Apple MapKit, iOS Room builder
│       └── commonTest/        # unit-тесты, фейки
├── androidApp/                # Android-приложение
│   ├── google-services.json  # конфигурация Firebase
│   └── src/main/...
└── iosApp/                    # iOS-приложение (Xcode)
    └── iosApp/GoogleService-Info.plist
```

Пакеты `sharedUI`:

| Пакет | Назначение |
|---|---|
| `presentation/` | Экраны, ViewModel'и, компоненты, навигация |
| `domain/` | UseCase'ы, модели, репозитории-интерфейсы, валидация |
| `data/` | Room (entity/dao/mapper), Firestore, мапперы ошибок |
| `di/` | Koin-модуль `appModule` |
| `theme/` | Цвета, отступы, Material 3 тема |

---

## Сборка и запуск

### Требования
- JDK 17+
- Android Studio (последняя стабильная)
- Xcode 15+ (для iOS)
- Firebase-проект с включёнными **Authentication** и **Cloud Firestore**

### Android

```bash
# эмулятор/устройство
./gradlew :androidApp:installDebug

# или собрать APK
./gradlew :androidApp:assembleDebug
```

Firebase уже настроен: `androidApp/google-services.json` и `iosApp/iosApp/GoogleService-Info.plist` в репозитории. При смене проекта Firebase обновите оба файла.

### iOS

1. Откройте `iosApp/iosApp.xcodeproj` в Xcode
2. Дождитесь завершения Kotlin/Native сборки shared-фреймворка
3. Запустите схему `iosApp` на симуляторе или устройстве

### Тесты

```bash
./gradlew :sharedUI:testAndroidHostTest
```

---

## Ключевые решения

- **Room как single source of truth** — UI никогда не читает Firestore напрямую, поэтому списки работают офлайн и мгновенно реагируют на локальные изменения
- **Деструктивная миграция БД** — кэш пересоздаётся из сети, миграции не нужны (`fallbackToDestructiveMigration`)
- **Geohash-кластеризация** — маркеры на карте группируются по префиксам geohash, радиус считается через haversine
- **Navigation 3** — типобезопасные маршруты (`@Serializable Route : NavKey`)

---

## Лицензия

MIT
